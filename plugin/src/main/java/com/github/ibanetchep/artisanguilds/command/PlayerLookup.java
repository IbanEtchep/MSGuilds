package com.github.ibanetchep.artisanguilds.command;

import net.artisanmc.modules.api.network.NetworkPlayer;
import net.artisanmc.modules.api.network.NetworkPresence;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** A player named in a command, online anywhere on the network or known to this server. */
@FunctionalInterface
public interface PlayerLookup {

    record Found(UUID id, String name) {}

    @Nullable Found find(String name);

    /** The network's players first, then the players this server has seen (no lookup to Mojang). */
    static PlayerLookup of(NetworkPresence presence, Server server) {
        return name -> {
            NetworkPlayer online = presence.find(name);
            if (online != null) return new Found(online.getId(), online.getName());
            OfflinePlayer known = server.getOfflinePlayerIfCached(name);
            if (known == null || known.getName() == null) return null;
            return new Found(known.getUniqueId(), known.getName());
        };
    }
}
