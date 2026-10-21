package com.github.ibanetchep.artisanguilds.chat;

import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import com.github.ibanetchep.artisanguilds.service.Msg;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Guild and alliance chat across the network. The sender's server sends the line to each
 * recipient once, on whichever server they are (a player elsewhere through the network's
 * presence), so no other server re-broadcasts it.
 */
public final class GuildChat {

    private final GuildRegistry registry;
    private final GuildMessages messages;
    private final Logger logger;

    public GuildChat(GuildRegistry registry, GuildMessages messages, Logger logger) {
        this.registry = registry;
        this.messages = messages;
        this.logger = logger;
    }

    /** The members who read a line sent on {@code channel}: the guild, and its allies for {@link ChatMode#ALLY}. */
    public Set<UUID> recipients(GuildData guild, ChatMode channel) {
        Set<UUID> ids = new LinkedHashSet<>();
        guild.members().forEach(m -> ids.add(m.getPlayerId()));
        if (channel == ChatMode.ALLY) {
            for (UUID allyId : guild.allianceIds()) {
                GuildData ally = registry.get(allyId);
                if (ally != null) ally.members().forEach(m -> ids.add(m.getPlayerId()));
            }
        }
        return ids;
    }

    public void send(MemberData sender, ChatMode channel, String text) {
        Msg format = channel == ChatMode.ALLY ? Msg.CHAT_ALLY_FORMAT : Msg.CHAT_GUILD_FORMAT;
        // The rank is the guild's own MiniMessage (its colours); the player's line is escaped.
        Map<String, String> rich = Map.of("rank", sender.getRank().getName());
        for (UUID id : recipients(sender.getGuild(), channel)) {
            messages.sendRich(id, format, rich, "player", sender.getName(), "message", text);
        }
        logger.info("[" + (channel == ChatMode.ALLY ? "Alliance" : "Guilde") + "] Chat (" + sender.getName() + ") : " + text);
    }
}
