package com.github.ibanetchep.artisanguilds.model;

import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository.GuildRow;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository.MemberRow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GuildRegistryTest {

    @Test
    void aGuildStoredWithoutRanksGetsTheDefaultOnesAndIsReportedForSaving() {
        UUID guildId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        GuildRow row = new GuildRow(guildId, "Testeurs", owner, 0, 0, 0, null, Instant.now(), List.of(),
                List.of(new MemberRow(owner, guildId, null, ChatMode.PUBLIC), new MemberRow(member, guildId, null, ChatMode.PUBLIC)),
                Set.of());

        GuildRegistry registry = new GuildRegistry();
        List<GuildData> repaired = registry.load(List.of(row));

        GuildData guild = registry.get(guildId);
        assertEquals(List.of(guild), repaired);
        assertEquals(4, guild.getRanks().size());
        assertSame(guild.getRanks().getLast(), guild.getMember(owner).getRank());
        assertSame(guild.defaultRank(), guild.getMember(member).getRank());
    }
}
