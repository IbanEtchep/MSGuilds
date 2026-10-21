package com.github.ibanetchep.artisanguilds.placeholder;

import com.github.ibanetchep.artisanguilds.Fixture;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuildPlaceholdersTest {

    private Fixture f;
    private GuildPlaceholders placeholders;

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture();
        GuildData.online = f.platform::isOnline;
        placeholders = new GuildPlaceholders(f.registry, f.messages);
    }

    @Test
    void describesThePlayersGuild() {
        Player owner = f.player("Iban");
        f.guilds.createGuild(owner, "L'Ordre");
        GuildData guild = f.registry.byName("L'Ordre");
        guild.setBalance(12500);

        assertEquals("L’Ordre", placeholders.resolve(owner, "name"));
        assertEquals("1", placeholders.resolve(owner, "level"));
        assertEquals("1", placeholders.resolve(owner, "members"));
        assertEquals("1", placeholders.resolve(owner, "online"));
        assertEquals("12 500", placeholders.resolve(owner, "balance"));
        assertEquals("Iban", placeholders.resolve(owner, "owner"));
        assertEquals(guild.getMember(owner.getUniqueId()).getRank().getName(), placeholders.resolve(owner, "RANK"));
        assertNull(placeholders.resolve(owner, "menu_rank"));
    }

    @Test
    void aPlayerInNoGuildGetsTheNoGuildText() {
        Player alone = f.player("Eve");
        assertEquals("messages.placeholders.no_guild", placeholders.resolve(alone, "name"));
        assertEquals("", placeholders.resolve(alone, "level"));
        assertNull(placeholders.resolve(null, "name"));
    }
}
