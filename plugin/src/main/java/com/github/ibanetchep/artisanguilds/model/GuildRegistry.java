package com.github.ibanetchep.artisanguilds.model;

import com.github.ibanetchep.artisanguilds.service.DefaultRank;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository.GuildRow;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository.MemberRow;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository.RankRow;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The guilds this server holds, by id. Read from any thread. */
public final class GuildRegistry {

    private final Map<UUID, GuildData> guilds = new ConcurrentHashMap<>();

    public Collection<GuildData> all() {
        return Collections.unmodifiableCollection(guilds.values());
    }

    public @Nullable GuildData get(UUID id) {
        return guilds.get(id);
    }

    public @Nullable GuildData byName(String name) {
        return guilds.values().stream().filter(g -> g.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public @Nullable GuildData byPlayer(UUID playerId) {
        return guilds.values().stream().filter(g -> g.getMember(playerId) != null).findFirst().orElse(null);
    }

    public @Nullable MemberData member(UUID playerId) {
        GuildData guild = byPlayer(playerId);
        return guild == null ? null : guild.getMember(playerId);
    }

    public void put(GuildData guild) {
        guilds.put(guild.getId(), guild);
    }

    public @Nullable GuildData remove(UUID id) {
        GuildData removed = guilds.remove(id);
        if (removed != null) guilds.values().forEach(g -> g.allianceIds().remove(id));
        return removed;
    }

    /**
     * Replaces everything with what the database holds. Returns the guilds stored without any rank,
     * which were given the default ones and must be saved.
     */
    public List<GuildData> load(List<GuildRow> rows) {
        guilds.clear();
        List<GuildData> repaired = new ArrayList<>();
        for (GuildRow row : rows) {
            GuildData guild = create(row);
            guilds.put(guild.getId(), guild);
            if (row.ranks().isEmpty()) repaired.add(guild);
        }
        return repaired;
    }

    public GuildData create(GuildRow row) {
        GuildData guild = new GuildData(row.id(), row.name(), row.ownerId(), row.balance(), row.exp(), row.level(),
                row.home(), row.createdAt(), this::get);
        if (row.ranks().isEmpty()) DefaultRank.create().forEach(guild::addRank);
        apply(guild, row);
        return guild;
    }

    /**
     * Brings {@code guild} to {@code row} in place, so that a plugin holding the guild keeps a
     * valid instance. Returns the members that joined and those that left.
     */
    public Diff apply(GuildData guild, GuildRow row) {
        guild.setName(row.name());
        guild.setOwnerId(row.ownerId());
        guild.setBalance(row.balance());
        guild.setExp(row.exp(), row.level());
        guild.setHome(row.home());

        List<RankData> ranks = new ArrayList<>();
        for (RankRow r : row.ranks()) {
            RankData existing = guild.getRank(r.id());
            if (existing == null) {
                ranks.add(new RankData(r.id(), r.label(), r.order(), r.permissions()));
                continue;
            }
            existing.setName(r.label());
            existing.setOrder(r.order());
            for (var p : com.github.ibanetchep.artisanguilds.api.GuildPermission.values()) {
                existing.setPermission(p, r.permissions().contains(p));
            }
            ranks.add(existing);
        }
        if (!ranks.isEmpty()) guild.setRanks(ranks);

        Set<UUID> before = new HashSet<>(guild.getMembers().keySet());
        Set<UUID> after = new HashSet<>();
        for (MemberRow m : row.members()) {
            after.add(m.playerId());
            RankData rank = m.rankId() != null ? guild.getRank(m.rankId()) : null;
            if (rank == null) rank = m.playerId().equals(guild.getOwnerId()) ? guild.getRanks().getLast() : guild.defaultRank();
            MemberData member = guild.getMember(m.playerId());
            if (member == null) guild.putMember(new MemberData(m.playerId(), guild, rank, m.chatMode()));
            else {
                member.setRank(rank);
                member.setChatMode(m.chatMode());
            }
        }
        List<UUID> left = before.stream().filter(id -> !after.contains(id)).toList();
        left.forEach(guild::removeMember);

        guild.allianceIds().clear();
        guild.allianceIds().addAll(row.allies());
        return new Diff(after.stream().filter(id -> !before.contains(id)).toList(), left);
    }

    public record Diff(List<UUID> joined, List<UUID> left) {}
}
