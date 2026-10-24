package com.github.ibanetchep.artisanguilds.storage;

import com.github.ibanetchep.artisanguilds.Fixture;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildChestRepositoryTest {

    @Test
    void aPageIsReplacedThenGoneOnceEmptiedAndGoesWithItsGuild() throws Exception {
        Fixture f = new Fixture();
        f.guilds.createGuild(f.player("Iban"), "Alpha");
        UUID guild = f.registry.byName("Alpha").getId();
        GuildChestRepository chests = new GuildChestRepository(f.dataSource, Fixture.TABLES);
        assertFalse(chests.hasItems(guild));

        chests.save(guild, 1, new byte[]{1});
        chests.save(guild, 1, new byte[]{2, 3});
        chests.save(guild, 2, new byte[]{4});
        assertTrue(chests.hasItems(guild));
        assertArrayEquals(new byte[]{2, 3}, chests.loadAll().get(guild).get(1));

        chests.save(guild, 1, null);
        assertEquals(1, chests.loadAll().get(guild).size());

        f.repository.deleteGuild(guild);
        assertFalse(chests.hasItems(guild));
    }
}
