package com.github.ibanetchep.artisanguilds.chat;

import com.github.ibanetchep.artisanguilds.api.ChatMode;
import org.jetbrains.annotations.Nullable;

/**
 * Where a guild member's chat line goes, as in MSGuilds: their chat mode, or a one-line prefix
 * ({@code !} for the guild, {@code ?} for the alliance) from public mode; in guild or alliance
 * mode the same prefix sends the line to public chat instead.
 *
 * @param channel null: public chat
 * @param text the line to send, prefix removed
 * @param denied the member lacks {@code guilds.chat} and used a prefix: the line is dropped
 */
public record ChatRoute(@Nullable ChatMode channel, String text, boolean denied) {

    public static ChatRoute of(ChatMode mode, String message, boolean allowed) {
        boolean guildPrefix = message.startsWith("!");
        boolean allyPrefix = message.startsWith("?");
        if (!allowed) {
            return new ChatRoute(null, message, guildPrefix || allyPrefix);
        }
        String stripped = guildPrefix || allyPrefix ? message.substring(1) : message;
        return switch (mode) {
            case PUBLIC -> guildPrefix ? new ChatRoute(ChatMode.GUILD, stripped, false)
                    : allyPrefix ? new ChatRoute(ChatMode.ALLY, stripped, false)
                    : new ChatRoute(null, message, false);
            case GUILD -> guildPrefix || allyPrefix ? new ChatRoute(null, stripped, false)
                    : new ChatRoute(ChatMode.GUILD, message, false);
            case ALLY -> guildPrefix || allyPrefix ? new ChatRoute(null, stripped, false)
                    : new ChatRoute(ChatMode.ALLY, message, false);
        };
    }

    /** The line still goes to public chat, possibly without its prefix. */
    public boolean isPublic() {
        return channel == null && !denied;
    }
}
