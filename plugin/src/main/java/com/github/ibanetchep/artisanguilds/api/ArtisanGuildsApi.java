package com.github.ibanetchep.artisanguilds.api;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * What other plugins (MSLands, ArtisanGuildQuests) use from ArtisanGuilds. Registered in Bukkit's
 * {@code ServicesManager} once the guilds are loaded, and reachable through {@link #get()}.
 * Lookups read memory and may be called from any thread; changes save asynchronously and reach
 * the other servers of the network.
 *
 * <p>Events: {@code api.event.GuildCreateEvent}, {@code GuildDisbandEvent} (cancellable, local
 * only), {@code GuildPostDisbandEvent}, {@code GuildMemberJoinEvent}, {@code GuildMemberLeaveEvent}.
 * They also fire on every other server when the change comes from there, with {@code isRemote()}.
 */
public interface ArtisanGuildsApi {

    /** Null while ArtisanGuilds is not enabled. */
    static @Nullable ArtisanGuildsApi get() {
        return Bukkit.getServicesManager().load(ArtisanGuildsApi.class);
    }

    Collection<Guild> getGuilds();

    @Nullable Guild getGuild(UUID guildId);

    /** Case-insensitive. */
    @Nullable Guild getGuildByName(String name);

    @Nullable Guild getGuildByPlayer(UUID playerId);

    @Nullable GuildMember getMember(UUID playerId);

    /** Adds to the guild's bank; false for an amount not above zero. */
    boolean deposit(Guild guild, double amount);

    /**
     * Takes from the guild's bank and logs {@code reason}; false when the bank lacks the amount,
     * the amount is not above zero, or no economy is installed.
     */
    boolean withdraw(Guild guild, double amount, String reason);

    GuildLevelInfo getLevelInfo(Guild guild);

    /** Null for a level data/settings.yml does not define. */
    @Nullable GuildLevelInfo getLevelInfo(int level);

    void addExp(Guild guild, long exp);

    /** Sets the guild's experience to the level's start. */
    void setLevel(Guild guild, int level);

    void addLog(Guild guild, String log);

    /** Newest first, each line prefixed with its date. */
    CompletableFuture<List<String>> getLogs(Guild guild);
}
