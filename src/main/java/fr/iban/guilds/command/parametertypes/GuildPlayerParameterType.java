package fr.iban.guilds.command.parametertypes;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.lang.Lang;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.parameter.PrioritySpec;
import revxrsal.commands.stream.MutableStringStream;

import java.util.List;

public class GuildPlayerParameterType implements ParameterType<BukkitCommandActor, GuildPlayer> {

    private final GuildsPlugin plugin;

    public GuildPlayerParameterType(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public GuildPlayer parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<@NotNull BukkitCommandActor> executionContext) {
        String value = input.readString();
        BukkitCommandActor actor = executionContext.actor();
        Player player = actor.asPlayer();

        if(player == null) {
            throw new CommandErrorException("You need to be a player to execute this command.");
        }

        Guild guild = plugin.getGuildManager().getGuildByPlayer(player);

        if(guild == null) {
            throw new CommandErrorException(Lang.ERROR_NOT_GUILD_MEMBER.plainText());
        }

        GuildPlayer guildPlayer = guild.getMembers().values().stream().filter(gp -> gp.getName().equalsIgnoreCase(value)).findFirst().orElse(null);

        if(guildPlayer == null) {
            throw new CommandErrorException(Lang.ERROR_PLAYER_NOT_IN_GUILD.plainText());
        }

        return guildPlayer;
    }

    @Override
    public @NotNull SuggestionProvider<@NotNull BukkitCommandActor> defaultSuggestions() {
        return (context) -> {
            Player player = context.actor().asPlayer();

            if (player == null) return List.of();

            Guild guild = plugin.getGuildManager().getGuildByPlayer(player);

            if(guild == null) {
                return List.of();
            }

            return guild.getMembers().values().stream().map(GuildPlayer::getName).toList();
        };
    }

    @Override
    public @NotNull PrioritySpec parsePriority() {
        return PrioritySpec.highest();
    }
}