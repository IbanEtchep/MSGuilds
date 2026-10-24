package com.github.ibanetchep.artisanguilds.chest;

import com.github.ibanetchep.artisanguilds.api.GuildLevelInfo;
import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.api.event.GuildPostDisbandEvent;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.service.GuildLeveling;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import com.github.ibanetchep.artisanguilds.service.Msg;
import com.github.ibanetchep.artisanguilds.storage.GuildChestRepository;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The guild chest ({@code /guild chest [page]}): pages of items shared by the members, sized by
 * the guild's level ({@code chest-rows}, {@code chest-pages}).
 *
 * <p>A page someone has open is one Bukkit inventory shared by all its viewers, so two members
 * never hold diverging copies. It is written when its last viewer closes it, and on shutdown.
 * Pages beyond what the level allows (an admin lowered it) stay readable: items come out, none
 * go in.
 *
 * <p>One server holds the chest: {@code chest.enabled: false} on every other server sharing the
 * database. There it is neither loaded nor written, and the disband check asks the database.
 * Runs on the main thread (Paper).
 */
public final class GuildChestService implements Listener {

    private static final int MAX_SIZE = 54;

    private final GuildRegistry registry;
    private final GuildLeveling leveling;
    private final GuildMessages messages;
    private final GuildStore store;
    private final GuildChestRepository repository;
    private final boolean enabled;
    private final Logger logger;
    /** Saved contents, by guild then page (from 1). */
    private final Map<UUID, Map<Integer, ItemStack[]>> pages = new HashMap<>();
    /** What a live page held when opened, and who opened it since: for the guild's log. */
    private final Map<GuildChestHolder, Session> sessions = new HashMap<>();

    private record Session(ItemStack[] before, Set<String> viewers) {}

    public GuildChestService(GuildRegistry registry, GuildLeveling leveling, GuildMessages messages, GuildStore store,
                             GuildChestRepository repository, boolean enabled, Logger logger) throws SQLException {
        this.registry = registry;
        this.leveling = leveling;
        this.messages = messages;
        this.store = store;
        this.repository = repository;
        this.enabled = enabled;
        this.logger = logger;
        if (!enabled) return;
        repository.loadAll().forEach((guildId, raw) -> raw.forEach((page, bytes) ->
                pages.computeIfAbsent(guildId, k -> new HashMap<>()).put(page, ItemStack.deserializeItemsFromBytes(bytes))));
    }

    public void open(Player player, @Nullable Integer requestedPage) {
        if (!enabled) {
            messages.send(player, Msg.CHEST_DISABLED);
            return;
        }
        MemberData member = registry.member(player.getUniqueId());
        if (member == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        GuildData guild = member.getGuild();
        GuildLevelInfo info = leveling.info(guild);
        int page = requestedPage == null ? 1 : requestedPage;
        boolean saved = !isEmpty(stored(guild.getId(), page));

        if (info.chestPages() == 0 && !saved && page == 1) {
            messages.send(player, Msg.CHEST_LOCKED, "level", leveling.chestUnlockLevel());
            return;
        }
        if (page < 1 || (page > info.chestPages() && !saved)) {
            messages.send(player, Msg.CHEST_PAGE_UNAVAILABLE, "page", page, "pages", info.chestPages());
            return;
        }
        if (!member.isGranted(GuildPermission.CHEST_DEPOSIT) && !member.isGranted(GuildPermission.CHEST_WITHDRAW)) {
            messages.send(player, Msg.ERROR_INSUFFICIENT_RANK);
            return;
        }

        Inventory inventory = liveInventory(player, guild, page, info);
        sessions.get((GuildChestHolder) inventory.getHolder()).viewers().add(player.getName());
        player.openInventory(inventory);
    }

    /** Nothing is stored in the guild's chest: disbanding is refused otherwise. */
    public boolean isEmpty(UUID guildId) {
        if (!enabled) {
            try {
                return !repository.hasItems(guildId);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Could not read the chest of guild " + guildId, e);
                return false; // unknown: refuse the disband rather than lose items
            }
        }
        for (GuildChestHolder holder : sessions.keySet()) {
            if (holder.guildId.equals(guildId) && !isEmpty(holder.inventory.getContents())) return false;
        }
        return pages.getOrDefault(guildId, Map.of()).values().stream().allMatch(GuildChestService::isEmpty);
    }

    /** Closes every open page and queues its write, before the writer stops. */
    public void shutdown() {
        for (GuildChestHolder holder : List.copyOf(sessions.keySet())) {
            List.copyOf(holder.inventory.getViewers()).forEach(HumanEntity::closeInventory);
            if (sessions.containsKey(holder)) close(holder);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GuildChestHolder holder)) return;
        if (event.getClickedInventory() == null) return;
        boolean inChest = event.getClickedInventory() == event.getView().getTopInventory();
        Rights rights = rights(event.getWhoClicked(), holder);
        if (!ChestAccess.allows(event.getAction(), inChest, rights.deposit(), rights.withdraw())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GuildChestHolder holder)) return;
        int chestSize = event.getView().getTopInventory().getSize();
        boolean intoChest = event.getRawSlots().stream().anyMatch(slot -> slot < chestSize);
        if (intoChest && !rights(event.getWhoClicked(), holder).deposit()) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuildChestHolder holder)) return;
        // The closing player is still counted among the viewers.
        if (event.getInventory().getViewers().size() <= 1 && sessions.containsKey(holder)) close(holder);
    }

    /** Here or on another server: the rows went with the guild (ON DELETE CASCADE). */
    @EventHandler
    public void onDisband(GuildPostDisbandEvent event) {
        UUID guildId = event.getGuild().getId();
        for (GuildChestHolder holder : List.copyOf(sessions.keySet())) {
            if (!holder.guildId.equals(guildId)) continue;
            sessions.remove(holder);
            List.copyOf(holder.inventory.getViewers()).forEach(HumanEntity::closeInventory);
        }
        pages.remove(guildId);
    }

    // --- Internals -------------------------------------------------------------------------------

    private record Rights(boolean deposit, boolean withdraw) {}

    /** Read at each click: a rank change or a kick applies to a chest already open. */
    private Rights rights(HumanEntity who, GuildChestHolder holder) {
        MemberData member = registry.member(who.getUniqueId());
        if (member == null || !member.getGuild().getId().equals(holder.guildId)) return new Rights(false, false);
        int allowedPages = leveling.info(member.getGuild()).chestPages();
        return new Rights(holder.page <= allowedPages && member.isGranted(GuildPermission.CHEST_DEPOSIT),
                member.isGranted(GuildPermission.CHEST_WITHDRAW));
    }

    private Inventory liveInventory(Player viewer, GuildData guild, int page, GuildLevelInfo info) {
        for (GuildChestHolder holder : sessions.keySet()) {
            if (holder.guildId.equals(guild.getId()) && holder.page == page) return holder.inventory;
        }
        ItemStack[] contents = stored(guild.getId(), page);
        // Never hide saved items: a page is at least as big as what it holds.
        int size = Math.max(page <= info.chestPages() ? info.chestRows() * 9 : 0, lastUsedRowEnd(contents));
        size = Math.min(MAX_SIZE, Math.max(9, size));

        GuildChestHolder holder = new GuildChestHolder(guild.getId(), page);
        holder.inventory = Bukkit.createInventory(holder, size, messages.component(viewer, Msg.CHEST_TITLE,
                "page", page, "pages", Math.max(page, info.chestPages())));
        holder.inventory.setContents(Arrays.copyOf(contents, size));
        sessions.put(holder, new Session(copy(holder.inventory.getContents()), new LinkedHashSet<>()));
        return holder.inventory;
    }

    private void close(GuildChestHolder holder) {
        Session session = sessions.remove(holder);
        ItemStack[] after = copy(holder.inventory.getContents());
        pages.computeIfAbsent(holder.guildId, k -> new HashMap<>()).put(holder.page, after);
        byte[] bytes = isEmpty(after) ? null : ItemStack.serializeItemsAsBytes(Arrays.stream(after)
                .map(item -> item == null ? ItemStack.empty() : item).toArray(ItemStack[]::new));
        store.writer().execute(() -> {
            try {
                repository.save(holder.guildId, holder.page, bytes);
            } catch (SQLException | RuntimeException e) {
                logger.log(Level.SEVERE, "[ArtisanGuilds] Could not save page " + holder.page + " of the chest of guild "
                        + holder.guildId, e);
            }
        });
        log(holder, session, after);
    }

    private void log(GuildChestHolder holder, Session session, ItemStack[] after) {
        Map<Material, Integer> delta = new TreeMap<>();
        count(after).forEach((material, n) -> delta.merge(material, n, Integer::sum));
        count(session.before()).forEach((material, n) -> delta.merge(material, -n, Integer::sum));
        delta.values().removeIf(n -> n == 0);
        GuildData guild = registry.get(holder.guildId);
        if (delta.isEmpty() || guild == null) return;
        store.addLog(guild, messages.plain(Msg.LOG_CHEST, "players", String.join(", ", session.viewers()),
                "page", holder.page, "added", describe(delta, true), "removed", describe(delta, false)));
    }

    private static String describe(Map<Material, Integer> delta, boolean added) {
        String text = delta.entrySet().stream()
                .filter(e -> added == (e.getValue() > 0))
                .map(e -> Math.abs(e.getValue()) + " " + e.getKey().name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
        return text.isEmpty() ? "-" : text;
    }

    private static Map<Material, Integer> count(ItemStack[] items) {
        Map<Material, Integer> counts = new HashMap<>();
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) counts.merge(item.getType(), item.getAmount(), Integer::sum);
        }
        return counts;
    }

    private ItemStack[] stored(UUID guildId, int page) {
        return pages.getOrDefault(guildId, Map.of()).getOrDefault(page, new ItemStack[0]);
    }

    private static int lastUsedRowEnd(ItemStack[] contents) {
        for (int i = contents.length - 1; i >= 0; i--) {
            if (contents[i] != null && !contents[i].isEmpty()) return (i / 9 + 1) * 9;
        }
        return 0;
    }

    private static boolean isEmpty(ItemStack[] contents) {
        return Arrays.stream(contents).allMatch(item -> item == null || item.isEmpty());
    }

    private static ItemStack[] copy(ItemStack[] contents) {
        return Arrays.stream(contents).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
    }
}
