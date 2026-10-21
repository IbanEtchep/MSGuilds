package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import com.github.ibanetchep.artisanguilds.sync.GuildSync;
import net.artisanmc.modules.api.network.NetworkApi;
import org.bukkit.entity.Player;

/** Alliances between guilds: one asks, the other accepts; either can end it. */
public final class AllianceService {

    private final GuildRegistry registry;
    private final GuildStore store;
    private final GuildMessages messages;
    private final NetworkApi network;
    private final GuildSync sync;

    public AllianceService(GuildRegistry registry, GuildStore store, GuildMessages messages, NetworkApi network, GuildSync sync) {
        this.registry = registry;
        this.store = store;
        this.messages = messages;
        this.network = network;
        this.sync = sync;
    }

    public void request(Player player, GuildData target) {
        GuildData guild = managed(player);
        if (guild == null) return;
        if (target.getId().equals(guild.getId())) {
            messages.send(player, Msg.ERROR_CANNOT_SELF_ALLY);
            return;
        }
        if (guild.allianceIds().contains(target.getId())) {
            messages.send(player, Msg.ERROR_ALREADY_ALLIED);
            return;
        }
        if (guild.allianceInvites().contains(target.getId())) {
            messages.send(player, Msg.ERROR_ALLIANCE_INVITE_ALREADY_SENT);
            return;
        }
        sync.expiring(guild.allianceInvites(), target.getId());
        network.publish(GuildSync.ALLIANCE_REQUEST, GuildSync.pair(guild.getId(), target.getId()));
        messages.sendToMembers(target, Msg.ALLIANCE_REQUEST_RECEIVED, "guild", guild.getName());
        messages.send(player, Msg.ALLIANCE_REQUEST_SENT, "guild", target.getName());
    }

    /** Accepts the request {@code target} sent to the player's guild. */
    public void accept(Player player, GuildData target) {
        GuildData guild = managed(player);
        if (guild == null) return;
        if (!target.allianceInvites().contains(guild.getId())) {
            messages.send(player, Msg.ERROR_NOT_INVITED);
            return;
        }
        if (guild.allianceIds().contains(target.getId())) {
            messages.send(player, Msg.ERROR_ALREADY_ALLIED);
            return;
        }
        target.allianceInvites().remove(guild.getId());
        guild.allianceIds().add(target.getId());
        target.allianceIds().add(guild.getId());
        messages.sendToMembers(guild, Msg.ALLIANCE_FORMED, "guild", target.getName());
        messages.sendToMembers(target, Msg.ALLIANCE_FORMED, "guild", guild.getName());
        store.addLog(guild, messages.plain(Msg.ALLIANCE_FORMED, "guild", target.getName()));
        store.saveGuild(guild);
        store.saveGuild(target);
    }

    public void revoke(Player player, GuildData target) {
        GuildData guild = managed(player);
        if (guild == null) return;
        if (!guild.allianceIds().contains(target.getId())) {
            messages.send(player, Msg.ERROR_NOT_ALLIED);
            return;
        }
        guild.allianceIds().remove(target.getId());
        target.allianceIds().remove(guild.getId());
        messages.sendToMembers(guild, Msg.ALLIANCE_ENDED, "guild", target.getName());
        messages.sendToMembers(target, Msg.ALLIANCE_ENDED, "guild", guild.getName());
        store.addLog(guild, messages.plain(Msg.ALLIANCE_ENDED, "guild", target.getName()));
        store.saveGuild(guild);
        store.saveGuild(target);
    }

    private GuildData managed(Player player) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return null;
        }
        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.MANAGE_ALLIANCES)) {
            messages.send(player, Msg.ERROR_INSUFFICIENT_RANK);
            return null;
        }
        return guild;
    }
}
