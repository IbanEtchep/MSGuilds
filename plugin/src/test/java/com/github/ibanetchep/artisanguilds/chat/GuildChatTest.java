package com.github.ibanetchep.artisanguilds.chat;

import com.github.ibanetchep.artisanguilds.Fixture;
import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuildChatTest {

    private Fixture f;
    private GuildChat chat;

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture();
        chat = new GuildChat(f.registry, f.messages, Logger.getAnonymousLogger());
    }

    private GuildData guild(String owner, String name, String member) {
        Player founder = f.player(owner);
        f.guilds.createGuild(founder, name);
        GuildData guild = f.registry.byName(name);
        Player other = f.player(member);
        guild.invites().add(other.getUniqueId());
        f.guilds.joinGuild(other, guild);
        return guild;
    }

    private static Set<UUID> ids(GuildData... guilds) {
        Set<UUID> ids = new java.util.HashSet<>();
        for (GuildData g : guilds) g.members().forEach(m -> ids.add(m.getPlayerId()));
        return ids;
    }

    @Test
    void guildLinesReachTheGuildOnlyAndAllyLinesItsAlliesToo() {
        GuildData alpha = guild("Iban", "Alpha", "Ana");
        GuildData beta = guild("Bob", "Beta", "Bea");
        GuildData gamma = guild("Gus", "Gamma", "Gil");
        alpha.allianceIds().add(beta.getId());
        beta.allianceIds().add(alpha.getId());

        assertEquals(ids(alpha), chat.recipients(alpha, ChatMode.GUILD));
        assertEquals(ids(alpha, beta), chat.recipients(alpha, ChatMode.ALLY));
        assertEquals(ids(gamma), chat.recipients(gamma, ChatMode.ALLY));
    }

    @Test
    void anAllyThisServerDoesNotHoldIsSkipped() {
        GuildData alpha = guild("Iban", "Alpha", "Ana");
        alpha.allianceIds().add(UUID.randomUUID());

        assertEquals(ids(alpha), chat.recipients(alpha, ChatMode.ALLY));
    }
}
