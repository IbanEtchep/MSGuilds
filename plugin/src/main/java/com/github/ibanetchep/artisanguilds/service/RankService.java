package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.RankData;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import net.artisanmc.modules.api.text.TextApi;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/** A guild's ranks, managed by its members with {@link GuildPermission#MANAGE_RANKS}. Names are MiniMessage. */
public final class RankService {

    private final GuildRegistry registry;
    private final GuildStore store;
    private final GuildMessages messages;
    private final TextApi text;

    public RankService(GuildRegistry registry, GuildStore store, GuildMessages messages, TextApi text) {
        this.registry = registry;
        this.store = store;
        this.messages = messages;
        this.text = text;
    }

    public void moveUp(Player player, RankData rank) {
        GuildData guild = managed(player);
        if (guild == null) return;
        if (!guild.moveRank(rank, true)) {
            messages.send(player, Msg.ERROR_RANK_ALREADY_FIRST);
            return;
        }
        store.saveGuild(guild);
        messages.send(player, Msg.RANK_MOVED_UP, "rank", rank.getName());
    }

    public void moveDown(Player player, RankData rank) {
        GuildData guild = managed(player);
        if (guild == null) return;
        if (!guild.moveRank(rank, false)) {
            messages.send(player, Msg.ERROR_RANK_ALREADY_LAST);
            return;
        }
        store.saveGuild(guild);
        messages.send(player, Msg.RANK_MOVED_DOWN, "rank", rank.getName());
    }

    /** Only a rank no member holds. */
    public void delete(Player player, RankData rank) {
        GuildData guild = managed(player);
        if (guild == null) return;
        if (!guild.membersWithRank(rank).isEmpty() || guild.getRanks().size() <= 1) {
            messages.send(player, Msg.ERROR_RANK_NOT_EMPTY);
            return;
        }
        guild.removeRank(rank);
        store.saveGuild(guild);
        messages.send(player, Msg.RANK_DELETED, "rank", rank.getName());
    }

    public void rename(Player player, RankData rank, String miniMessage) {
        GuildData guild = managed(player);
        if (guild == null || !validName(player, guild, miniMessage)) return;
        messages.send(player, Msg.RANK_RENAMED, "oldName", rank.getName(), "newName", miniMessage);
        rank.setName(miniMessage);
        store.saveGuild(guild);
    }

    public void create(Player player, String miniMessage) {
        GuildData guild = managed(player);
        if (guild == null || !validName(player, guild, miniMessage)) return;
        RankData rank = new RankData(UUID.randomUUID(), miniMessage, 0, Set.of());
        guild.addRank(rank);
        store.saveGuild(guild);
        messages.send(player, Msg.RANK_CREATED, "rank", rank.getName());
    }

    public void setPermission(Player player, RankData rank, GuildPermission permission, boolean granted) {
        GuildData guild = managed(player);
        if (guild == null) return;
        rank.setPermission(permission, granted);
        store.saveGuild(guild);
    }

    private boolean validName(Player player, GuildData guild, String miniMessage) {
        String plain = text.plain(miniMessage);
        if (guild.getRanks().stream().anyMatch(r -> text.plain(r.getName()).equalsIgnoreCase(plain))) {
            messages.send(player, Msg.ERROR_RANK_ALREADY_EXISTS);
            return false;
        }
        if (plain.length() > 64 || plain.length() < 2) {
            messages.send(player, Msg.ERROR_RANK_NAME_LENGTH);
            return false;
        }
        return true;
    }

    private GuildData managed(Player player) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return null;
        }
        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.MANAGE_RANKS)) {
            messages.send(player, Msg.ERROR_INSUFFICIENT_RANK);
            return null;
        }
        return guild;
    }
}
