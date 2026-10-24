package com.github.ibanetchep.artisanguilds;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** What the services need from the server, apart from Artisan's API: a seam for the tests. */
public interface GuildPlatform {

    /** The player when online on this server. */
    @Nullable Player player(UUID id);

    /** Online anywhere on the network. */
    boolean isOnline(UUID id);

    /** MiniMessage to a player on another server. */
    void sendRemote(UUID id, String miniMessage);

    /** On the global thread. */
    void callEvent(Event event);

    void global(Runnable task);

    void later(long ticks, Runnable task);
}
