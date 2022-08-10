package fr.iban.guilds.util;

import fr.iban.guilds.Guild;
import fr.iban.guilds.GuildsPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GuildPlaceHolders extends PlaceholderExpansion {

    private final GuildsPlugin plugin;

    public GuildPlaceHolders(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "guilds";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {

        if (player == null) {
            return "";
        }

        if (identifier.equals("name")) {
            Guild guild = plugin.getGuildsManager().getGuildByPlayer(player);
            return guild == null ? "aucune" : guild.getName();
        }

        return null;
    }
}
