package fr.iban.guilds.command.parametertypes;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.lang.Lang;
import fr.iban.guilds.model.Guild;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.parameter.PrioritySpec;
import revxrsal.commands.stream.MutableStringStream;

public class GuildParameterType implements ParameterType<BukkitCommandActor, Guild> {

    private final GuildsPlugin plugin;

    public GuildParameterType(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Guild parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<BukkitCommandActor> executionContext) {
        String value = input.readString();
        Guild guild = plugin.getGuildManager().getGuildByName(value);

        if (guild == null) {
            throw new CommandErrorException(Lang.ERROR_GUILD_NOT_FOUND.plainText("guild", value));
        }

        return guild;
    }

    @Override
    public @NotNull SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
        return (context) -> plugin.getGuildManager().getGuildNames();
    }

    @Override
    public @NotNull PrioritySpec parsePriority() {
        return PrioritySpec.highest();
    }
}