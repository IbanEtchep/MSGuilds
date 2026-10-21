package com.github.ibanetchep.artisanguilds.placeholder;

import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import net.artisanmc.modules.api.placeholder.PlaceholderResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * {@code %msguilds_<key>%}, MSGuilds' PlaceholderAPI expansion served by Artisan: in Artisan's
 * menus, and through PlaceholderAPI for scoreboards and chat. Same namespace and {@code name} key
 * as MSGuilds; {@code menu_rank} and {@code menu_guild} belonged to the zMenu menus and are gone.
 */
public final class GuildPlaceholders implements PlaceholderResolver {

    public static final String NAMESPACE = "msguilds";

    private final GuildRegistry registry;
    private final GuildMessages messages;

    public GuildPlaceholders(GuildRegistry registry, GuildMessages messages) {
        this.registry = registry;
        this.messages = messages;
    }

    @Override
    public @Nullable String resolve(@Nullable Player player, String key) {
        if (player == null) return null;
        GuildData guild = registry.byPlayer(player.getUniqueId());
        String lower = key.toLowerCase(Locale.ROOT);
        if (guild == null) {
            return switch (lower) {
                case "name" -> messages.text(player, "messages.placeholders.no_guild", "aucune");
                case "level", "members", "online", "balance", "rank", "owner" -> "";
                default -> null;
            };
        }
        return switch (lower) {
            // The apostrophe MSGuilds replaced: names end up inside quoted strings of other plugins.
            case "name" -> guild.getName().replace("'", "’");
            case "level" -> String.valueOf(guild.getLevel());
            case "members" -> String.valueOf(guild.members().size());
            case "online" -> String.valueOf(guild.getOnlineMembers().size());
            case "balance" -> String.format(Locale.ROOT, "%,d", (long) guild.getBalance()).replace(',', ' ');
            case "rank" -> {
                MemberData member = guild.getMember(player.getUniqueId());
                yield member == null ? "" : member.getRank().getName();
            }
            case "owner" -> {
                MemberData owner = guild.getOwner();
                yield owner == null || owner.getName() == null ? "" : owner.getName();
            }
            default -> null;
        };
    }
}
