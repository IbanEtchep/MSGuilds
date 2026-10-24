package com.github.ibanetchep.artisanguildquests;

import com.github.ibanetchep.artisanguilds.api.Guild;
import com.github.ibanetchep.artisanguilds.api.GuildMember;
import com.github.ibanetchep.msquests.core.lang.Translator;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildQuestActorTest {

    static Guild guild(UUID id, String name, Map<UUID, GuildMember> members) {
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn(id);
        when(guild.getName()).thenReturn(name);
        when(guild.getMembers()).thenReturn(members);
        return guild;
    }

    @Test
    void membershipIsReadOffTheLiveGuild() {
        Map<UUID, GuildMember> members = new ConcurrentHashMap<>();
        UUID player = UUID.randomUUID();
        GuildQuestActor actor = new GuildQuestActor(guild(UUID.randomUUID(), "Alpha", members));

        assertFalse(actor.isMember(player));
        members.put(player, mock(GuildMember.class));
        assertTrue(actor.isMember(player));
    }

    @Test
    void nameAndPlaceholdersFollowTheGuild() {
        UUID id = UUID.randomUUID();
        Guild guild = guild(id, "Alpha", Map.of(UUID.randomUUID(), mock(GuildMember.class)));
        when(guild.getLevel()).thenReturn(4);
        GuildQuestActor actor = new GuildQuestActor(guild);
        when(guild.getName()).thenReturn("Beta");

        assertEquals("guild", actor.getActorType());
        assertEquals(id, actor.getId());
        assertEquals("Beta", actor.getName());
        Map<String, String> placeholders = actor.getPlaceholders(mock(Translator.class));
        assertEquals("Beta", placeholders.get("actor_name"));
        assertEquals("Beta", placeholders.get("guild_name"));
        assertEquals("4", placeholders.get("guild_level"));
        assertEquals("1", placeholders.get("guild_members"));
    }
}
