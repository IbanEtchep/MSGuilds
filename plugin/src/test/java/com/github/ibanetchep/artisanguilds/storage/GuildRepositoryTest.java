package com.github.ibanetchep.artisanguilds.storage;

import com.github.ibanetchep.artisanguilds.Fixture;
import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.api.GuildHome;
import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.model.RankData;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository.GuildRow;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository.RankRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildRepositoryTest {

    private Fixture f;
    private GuildRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture();
        repository = f.repository;
    }

    private GuildData guild(String name, UUID owner) {
        GuildData guild = new GuildData(UUID.randomUUID(), name, owner, 0, 0, 1, null,
                Instant.now().truncatedTo(ChronoUnit.SECONDS), f.registry::get);
        guild.addRank(new RankData(UUID.randomUUID(), "Membre", 0, Set.of()));
        guild.addRank(new RankData(UUID.randomUUID(), "Chef", 0, Set.of(GuildPermission.values())));
        guild.putMember(new MemberData(owner, guild, guild.getRanks().getLast(), ChatMode.PUBLIC));
        return guild;
    }

    @Test
    void savesAndReadsBackAGuild() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        GuildData guild = guild("Alpha", owner);
        GuildData ally = guild("Beta", UUID.randomUUID());
        guild.setBalance(1250.5);
        guild.setExp(150, 2);
        guild.setHome(new GuildHome("survie", "world", 1.5, 64, -3, 90f, 10f));
        guild.getRanks().getFirst().setPermission(GuildPermission.INVITE_MEMBER, true);
        guild.putMember(new MemberData(member, guild, guild.getRanks().getFirst(), ChatMode.GUILD));
        guild.allianceIds().add(ally.getId());
        repository.saveGuild(ally);
        repository.saveGuild(guild);
        for (MemberData m : guild.members()) repository.saveMember(m);

        GuildRow row = repository.load(guild.getId());

        assertEquals("Alpha", row.name());
        assertEquals(owner, row.ownerId());
        assertEquals(1250.5, row.balance());
        assertEquals(150, row.exp());
        assertEquals(2, row.level());
        assertEquals(new GuildHome("survie", "world", 1.5, 64, -3, 90f, 10f), row.home());
        assertEquals(guild.getCreatedAt(), row.createdAt());
        assertEquals(List.of("Membre", "Chef"), row.ranks().stream().sorted((a, b) -> a.order() - b.order()).map(RankRow::label).toList());
        RankRow lowest = row.ranks().stream().filter(r -> r.order() == 0).findFirst().orElseThrow();
        assertEquals(Set.of(GuildPermission.INVITE_MEMBER), lowest.permissions());
        assertEquals(2, row.members().size());
        assertEquals(ChatMode.GUILD, repository.loadMember(member).chatMode());
        assertEquals(lowest.id(), repository.loadMember(member).rankId());
        assertEquals(Set.of(ally.getId()), row.allies());
        assertEquals(2, repository.loadAll().size());

        // Read back into memory, the guild is the same.
        GuildData loaded = f.registry.create(row);
        assertEquals(2, loaded.getMembers().size());
        assertEquals(owner, loaded.getOwner().getPlayerId());
        assertTrue(loaded.getMember(member).getRank().hasPermission(GuildPermission.INVITE_MEMBER));
    }

    @Test
    void savingAgainUpdatesAndDropsRemovedRanks() throws Exception {
        GuildData guild = guild("Alpha", UUID.randomUUID());
        RankData extra = new RankData(UUID.randomUUID(), "Extra", 0, Set.of());
        guild.addRank(extra);
        repository.saveGuild(guild);

        guild.removeRank(extra);
        guild.setName("Alpha2");
        guild.setHome(null);
        repository.saveGuild(guild);

        GuildRow row = repository.load(guild.getId());
        assertEquals("Alpha2", row.name());
        assertNull(row.home());
        assertEquals(2, row.ranks().size());
    }

    @Test
    void deletesAGuildWithItsMembersLogsAndAlliances() throws Exception {
        GuildData guild = guild("Alpha", UUID.randomUUID());
        GuildData ally = guild("Beta", UUID.randomUUID());
        ally.allianceIds().add(guild.getId());
        repository.saveGuild(guild);
        repository.saveGuild(ally);
        for (MemberData m : guild.members()) repository.saveMember(m);
        repository.addLog(guild.getId(), "created");
        assertEquals(1, repository.logs(guild.getId()).size());
        assertTrue(repository.logs(guild.getId()).getFirst().endsWith(" : created"));

        repository.deleteGuild(guild.getId());

        assertNull(repository.load(guild.getId()));
        assertNull(repository.loadMember(guild.getOwnerId()));
        assertTrue(repository.logs(guild.getId()).isEmpty());
        assertTrue(repository.load(ally.getId()).allies().isEmpty());
    }

    @Test
    void deletesAMember() throws Exception {
        GuildData guild = guild("Alpha", UUID.randomUUID());
        repository.saveGuild(guild);
        MemberData owner = guild.getOwner();
        repository.saveMember(owner);

        repository.deleteMember(owner.getPlayerId());

        assertNull(repository.loadMember(owner.getPlayerId()));
    }
}
