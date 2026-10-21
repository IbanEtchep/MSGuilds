package com.github.ibanetchep.artisanguilds.api;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A guild as this server holds it. Read-only: changes go through the plugin's services, which
 * save them and tell the other servers. The same instance stays valid while the guild exists;
 * a guild reloaded from another server's change is a new instance.
 */
public interface Guild {

    UUID getId();

    String getName();

    UUID getOwnerId();

    @Nullable GuildMember getOwner();

    double getBalance();

    long getExp();

    int getLevel();

    @Nullable GuildHome getHome();

    Instant getCreatedAt();

    /** By player id. */
    Map<UUID, GuildMember> getMembers();

    @Nullable GuildMember getMember(UUID playerId);

    default boolean isMember(UUID playerId) {
        return getMembers().containsKey(playerId);
    }

    /** From the lowest to the highest. */
    List<? extends GuildRank> getRanks();

    Collection<Guild> getAlliances();

    /** Members online anywhere on the network. */
    List<GuildMember> getOnlineMembers();
}
