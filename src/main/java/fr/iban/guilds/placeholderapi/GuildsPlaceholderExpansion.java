package fr.iban.guilds.placeholderapi;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.zmenu.data.GuildMenuData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GuildsPlaceholderExpansion extends PlaceholderExpansion {

    private final GuildsPlugin plugin;

    public GuildsPlaceholderExpansion(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "msguilds";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Iban";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {

        GuildMenuData guildMenuData = plugin.getMenuManager().getMenuData(player);
        if(guildMenuData != null) {
            if(params.equalsIgnoreCase("menu_rank") && guildMenuData.getCurrentRank() != null) {
                return guildMenuData.getCurrentRank().getName();
            }

            if(params.equalsIgnoreCase("menu_guild") && guildMenuData.getGuild() != null) {
                return guildMenuData.getGuild().getName();
            }
        }

        if (params.equalsIgnoreCase("name")) {
            Guild guild = plugin.getGuildManager().getGuildByPlayer(player);
            return guild == null ? "aucune" : guild.getName().replace("'", "’");
        }

        return null;
    }
}
