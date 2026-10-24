package com.github.ibanetchep.artisanguilds.model;

import com.github.ibanetchep.artisanguilds.api.Guild;
import com.github.ibanetchep.artisanguilds.api.GuildHome;
import com.github.ibanetchep.artisanguilds.api.GuildMember;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * A guild in memory. Members, invites and alliances are concurrent: the game threads change them
 * and the network thread reads them. Ranks are kept in their order, the lowest first.
 */
public final class GuildData implements Guild {

    /** Who is online on the network: Artisan's presence, set at enable. */
    public static volatile Predicate<UUID> online = id -> false;

    private final UUID id;
    private volatile String name;
    private volatile UUID ownerId;
    private volatile double balance;
    private volatile long exp;
    private volatile int level;
    private volatile @Nullable GuildHome home;
    private final Instant createdAt;

    private final Map<UUID, MemberData> members = new ConcurrentHashMap<>();
    private final List<RankData> ranks = new ArrayList<>();
    private final Set<UUID> allianceIds = ConcurrentHashMap.newKeySet();
    /** Players invited, until they join or the invite expires. Memory only, like MSGuilds. */
    private final Set<UUID> invites = ConcurrentHashMap.newKeySet();
    /** Guilds this one offered an alliance to. */
    private final Set<UUID> allianceInvites = ConcurrentHashMap.newKeySet();

    /** Resolves alliance ids to the guilds this server holds. */
    private final java.util.function.Function<UUID, @Nullable GuildData> lookup;

    public GuildData(UUID id, String name, UUID ownerId, double balance, long exp, int level,
                     @Nullable GuildHome home, Instant createdAt,
                     java.util.function.Function<UUID, @Nullable GuildData> lookup) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.balance = balance;
        this.exp = exp;
        this.level = level;
        this.home = home;
        this.createdAt = createdAt;
        this.lookup = lookup;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public @Nullable MemberData getOwner() {
        return members.get(ownerId);
    }

    @Override
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public long getExp() {
        return exp;
    }

    @Override
    public int getLevel() {
        return level;
    }

    public void setExp(long exp, int level) {
        this.exp = exp;
        this.level = level;
    }

    @Override
    public @Nullable GuildHome getHome() {
        return home;
    }

    public void setHome(@Nullable GuildHome home) {
        this.home = home;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Map<UUID, GuildMember> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public Collection<MemberData> members() {
        return members.values();
    }

    @Override
    public @Nullable MemberData getMember(UUID playerId) {
        return members.get(playerId);
    }

    public void putMember(MemberData member) {
        members.put(member.getPlayerId(), member);
    }

    public @Nullable MemberData removeMember(UUID playerId) {
        return members.remove(playerId);
    }

    public List<MemberData> membersWithRank(RankData rank) {
        return members.values().stream().filter(m -> m.getRank().getId().equals(rank.getId())).toList();
    }

    @Override
    public synchronized List<RankData> getRanks() {
        return List.copyOf(ranks);
    }

    /** Appended above the highest rank. */
    public synchronized void addRank(RankData rank) {
        rank.setOrder(ranks.size());
        ranks.add(rank);
    }

    /** Ranks read from the database, in any order. */
    public synchronized void setRanks(List<RankData> loaded) {
        ranks.clear();
        ranks.addAll(loaded.stream().sorted(Comparator.comparingInt(RankData::getOrder)).toList());
        renumber();
    }

    public synchronized void removeRank(RankData rank) {
        ranks.removeIf(r -> r.getId().equals(rank.getId()));
        renumber();
    }

    /** Swaps the rank with the one above ({@code up}) or below; false at the end. */
    public synchronized boolean moveRank(RankData rank, boolean up) {
        int index = indexOf(rank);
        int other = up ? index + 1 : index - 1;
        if (index < 0 || other < 0 || other >= ranks.size()) return false;
        Collections.swap(ranks, index, other);
        renumber();
        return true;
    }

    public synchronized @Nullable RankData rankAbove(RankData rank) {
        int index = indexOf(rank);
        return index >= 0 && index + 1 < ranks.size() ? ranks.get(index + 1) : null;
    }

    public synchronized @Nullable RankData rankBelow(RankData rank) {
        int index = indexOf(rank);
        return index > 0 ? ranks.get(index - 1) : null;
    }

    public synchronized @Nullable RankData getRank(String name) {
        return ranks.stream().filter(r -> r.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public synchronized @Nullable RankData getRank(UUID id) {
        return ranks.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
    }

    /** The rank of new members: the lowest. */
    public synchronized RankData defaultRank() {
        return ranks.getFirst();
    }

    private int indexOf(RankData rank) {
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).getId().equals(rank.getId())) return i;
        }
        return -1;
    }

    private void renumber() {
        for (int i = 0; i < ranks.size(); i++) ranks.get(i).setOrder(i);
    }

    @Override
    public Collection<Guild> getAlliances() {
        return allianceIds.stream().map(lookup).filter(g -> g != null).map(g -> (Guild) g).toList();
    }

    public Set<UUID> allianceIds() {
        return allianceIds;
    }

    public Set<UUID> invites() {
        return invites;
    }

    public Set<UUID> allianceInvites() {
        return allianceInvites;
    }

    @Override
    public List<GuildMember> getOnlineMembers() {
        return members.values().stream().filter(m -> online.test(m.getPlayerId())).map(m -> (GuildMember) m).toList();
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GuildData g && g.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
