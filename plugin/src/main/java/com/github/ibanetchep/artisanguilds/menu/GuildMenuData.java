package com.github.ibanetchep.artisanguilds.menu;

import com.github.ibanetchep.artisanguilds.api.GuildLevelInfo;
import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.model.RankData;
import com.github.ibanetchep.artisanguilds.service.GuildLeveling;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import com.github.ibanetchep.artisanguilds.service.MemberRules;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The rows of the guild menus. Each fetcher runs on the viewer's thread at every render, so it
 * reads memory only. A null viewer is the editor's preview: it gets sample rows.
 *
 * <p>The ranks and their permissions are the viewer's own guild's, and only for a member with
 * {@link GuildPermission#MANAGE_RANKS}: Artisan does not check a menu's open requirements, so
 * the menu stays empty for anyone else. The actions check again ({@link GuildMenuActions}).
 */
public final class GuildMenuData {

    /** Placeholder of a rank permission's name: {@code messages.permissions.manage_ranks}. */
    static final String PERMISSION_KEY = "messages.permissions.";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GuildRegistry registry;
    private final GuildLeveling leveling;
    private final GuildMessages messages;

    public GuildMenuData(GuildRegistry registry, GuildLeveling leveling, GuildMessages messages) {
        this.registry = registry;
        this.leveling = leveling;
        this.messages = messages;
    }

    /** The viewer's guild as the main menu shows it: one row, none outside a guild. */
    public List<Map<String, Object>> guild(@Nullable Player viewer) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (viewer == null) {
            row.put("id", "00000000-0000-0000-0000-000000000000");
            row.put("name", "Guild");
            row.put("level", 2);
            row.put("max_level", 10);
            row.put("balance", 1500L);
            row.put("members", 4);
            row.put("max_members", 7);
            row.put("online", 2);
            row.put("owner", "Steve");
            row.put("rank", "<color:#c0392b>Fondateur");
            row.put("chest_pages", 1);
            row.put("chest_rows", 3);
            row.put("has_home", true);
            row.put("created", "01/01/2026");
            row.put("can_manage_ranks", true);
            return List.of(row);
        }
        MemberData member = registry.member(viewer.getUniqueId());
        if (member == null) return List.of();
        GuildData guild = member.getGuild();
        GuildLevelInfo info = leveling.info(guild);
        MemberData owner = guild.getOwner();
        row.put("id", guild.getId().toString());
        row.put("name", guild.getName());
        row.put("level", info.level());
        row.put("max_level", leveling.maxLevel());
        row.put("balance", Math.round(guild.getBalance()));
        row.put("members", guild.getMembers().size());
        row.put("max_members", info.maxMembers());
        row.put("online", guild.getOnlineMembers().size());
        row.put("owner", owner != null && owner.getName() != null ? owner.getName() : "");
        row.put("rank", member.getRank().getName());
        row.put("chest_pages", info.chestPages());
        row.put("chest_rows", info.chestRows());
        row.put("has_home", guild.getHome() != null);
        row.put("created", DATE.format(guild.getCreatedAt().atZone(ZoneId.systemDefault())));
        row.put("can_manage_ranks", member.isGranted(GuildPermission.MANAGE_RANKS));
        return List.of(row);
    }

    /** One row: the viewer's guild and what they may do in it. */
    public List<Map<String, Object>> viewer(@Nullable Player viewer) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (viewer == null) {
            row.put("in_guild", true);
            row.put("guild_name", "Guild");
            row.put("rank", "Member");
            row.put("is_owner", true);
            row.put("can_manage_ranks", true);
            return List.of(row);
        }
        GuildData guild = registry.byPlayer(viewer.getUniqueId());
        MemberData member = guild != null ? guild.getMember(viewer.getUniqueId()) : null;
        row.put("in_guild", member != null);
        row.put("guild_name", member != null ? guild.getName() : "");
        row.put("rank", member != null ? member.getRank().getName() : "");
        row.put("is_owner", member != null && member.isOwner());
        row.put("can_manage_ranks", member != null && member.isGranted(GuildPermission.MANAGE_RANKS));
        return List.of(row);
    }

    /** The ranks of the viewer's guild, highest first. */
    public List<Map<String, Object>> ranks(@Nullable Player viewer) {
        if (viewer == null) {
            return List.of(rankRow("00000000-0000-0000-0000-000000000002", "<red>Officer", 1, 2, 1, 4, true),
                    rankRow("00000000-0000-0000-0000-000000000001", "<gray>Member", 2, 2, 5, 0, true));
        }
        GuildData guild = managed(viewer);
        if (guild == null) return List.of();
        MemberData self = guild.getMember(viewer.getUniqueId());
        List<RankData> ranks = new ArrayList<>(guild.getRanks());
        ranks.sort(Comparator.comparingInt(RankData::getOrder).reversed());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < ranks.size(); i++) {
            RankData rank = ranks.get(i);
            rows.add(rankRow(rank.getId().toString(), rank.getName(), i + 1, ranks.size(),
                    guild.membersWithRank(rank).size(), rank.getPermissions().size(), MemberRules.rankRefusal(self, rank) == null));
        }
        return rows;
    }

    private static Map<String, Object> rankRow(String id, String name, int position, int count, int members, int permissions,
                                               boolean canManage) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("position", position);
        row.put("members", members);
        row.put("permissions", permissions);
        row.put("top", position == 1);
        row.put("bottom", position == count);
        row.put("can_manage", canManage);
        return row;
    }

    /** The permissions of the rank {@code params.rank} (its id) of the viewer's guild, granted or not. */
    public List<Map<String, Object>> rankPermissions(@Nullable Player viewer, Map<String, Object> params) {
        RankData rank = null;
        if (viewer != null) {
            GuildData guild = managed(viewer);
            UUID id = uuid(params.get("rank"));
            rank = guild != null && id != null ? guild.getRank(id) : null;
            if (rank == null) return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (GuildPermission permission : GuildPermission.values()) {
            String key = permission.name().toLowerCase(Locale.ROOT);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", key);
            row.put("name", messages.text(viewer, PERMISSION_KEY + key, permission.getName()));
            row.put("granted", rank != null && rank.hasPermission(permission));
            rows.add(row);
        }
        return rows;
    }

    /**
     * The members of the viewer's guild, highest rank first, then by name, with what the viewer
     * may do to each ({@link MemberRules}, as the commands check it).
     */
    public List<Map<String, Object>> members(@Nullable Player viewer) {
        if (viewer == null) {
            return List.of(memberRow(UUID.randomUUID().toString(), "Steve", "<red>Officer", 1, true, true, false, false, false, false),
                    memberRow(UUID.randomUUID().toString(), "Alex", "<gray>Member", 0, false, false, false, true, false, true));
        }
        MemberData self = registry.member(viewer.getUniqueId());
        if (self == null) return List.of();
        return self.getGuild().members().stream()
                .sorted(Comparator.comparingInt((MemberData m) -> m.getRank().getOrder()).reversed()
                        .thenComparing(m -> m.getName() == null ? "" : m.getName(), String.CASE_INSENSITIVE_ORDER))
                .map(m -> memberRow(m.getPlayerId().toString(), m.getName(), m.getRank().getName(), m.getRank().getOrder(),
                        m.isOwner(), GuildData.online.test(m.getPlayerId()), m.getPlayerId().equals(self.getPlayerId()),
                        MemberRules.promoteRefusal(self, m) == null, MemberRules.demoteRefusal(self, m) == null,
                        MemberRules.kickRefusal(self, m) == null))
                .toList();
    }

    private static Map<String, Object> memberRow(String id, @Nullable String name, String rank, int rankOrder, boolean owner,
                                                 boolean online, boolean self, boolean canPromote, boolean canDemote,
                                                 boolean canKick) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name == null ? "" : name);
        row.put("rank", rank);
        row.put("rank_order", rankOrder);
        row.put("owner", owner);
        row.put("online", online);
        row.put("self", self);
        row.put("can_promote", canPromote);
        row.put("can_demote", canDemote);
        row.put("can_kick", canKick);
        return row;
    }

    private @Nullable GuildData managed(Player viewer) {
        GuildData guild = registry.byPlayer(viewer.getUniqueId());
        MemberData member = guild != null ? guild.getMember(viewer.getUniqueId()) : null;
        return member != null && member.isGranted(GuildPermission.MANAGE_RANKS) ? guild : null;
    }

    /** {@code kick_member}, ...: what {@code guilds:rank_permission} takes. */
    static List<String> permissionKeys() {
        return Arrays.stream(GuildPermission.values()).map(p -> p.name().toLowerCase(Locale.ROOT)).toList();
    }

    static @Nullable UUID uuid(@Nullable Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
