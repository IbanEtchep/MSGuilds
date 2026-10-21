package com.github.ibanetchep.artisanguilds.chat;

import com.github.ibanetchep.artisanguilds.api.ChatMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRouteTest {

    @Test
    void publicModeSendsPrefixedLinesToTheGuildOrTheAlliance() {
        assertEquals(new ChatRoute(null, "hello", false), ChatRoute.of(ChatMode.PUBLIC, "hello", true));
        assertEquals(new ChatRoute(ChatMode.GUILD, "rdv au spawn", false), ChatRoute.of(ChatMode.PUBLIC, "!rdv au spawn", true));
        assertEquals(new ChatRoute(ChatMode.ALLY, "aide", false), ChatRoute.of(ChatMode.PUBLIC, "?aide", true));
    }

    @Test
    void guildAndAllyModesKeepTheLineAndAPrefixSendsItToPublicChat() {
        assertEquals(new ChatRoute(ChatMode.GUILD, "salut", false), ChatRoute.of(ChatMode.GUILD, "salut", true));
        assertEquals(new ChatRoute(ChatMode.ALLY, "salut", false), ChatRoute.of(ChatMode.ALLY, "salut", true));
        ChatRoute out = ChatRoute.of(ChatMode.GUILD, "!à tous", true);
        assertTrue(out.isPublic());
        assertEquals("à tous", out.text());
        assertEquals("à tous", ChatRoute.of(ChatMode.ALLY, "?à tous", true).text());
    }

    @Test
    void withoutThePermissionPrefixedLinesAreDroppedAndOthersStayPublic() {
        assertTrue(ChatRoute.of(ChatMode.PUBLIC, "!secret", false).denied());
        assertTrue(ChatRoute.of(ChatMode.PUBLIC, "?secret", false).denied());
        assertEquals(new ChatRoute(null, "hello", false), ChatRoute.of(ChatMode.GUILD, "hello", false));
    }
}
