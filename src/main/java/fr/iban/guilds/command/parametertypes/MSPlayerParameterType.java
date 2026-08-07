package fr.iban.guilds.command.parametertypes;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.common.model.MSPlayer;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.parameter.PrioritySpec;
import revxrsal.commands.stream.MutableStringStream;

public class MSPlayerParameterType implements ParameterType<BukkitCommandActor, MSPlayer> {

    @Override
    public MSPlayer parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<BukkitCommandActor> executionContext) {
        String name = input.readString();
        MSPlayer player = CoreBukkitPlugin.getInstance().getPlayerManager().getOfflinePlayer(name);

        if (player == null) {
            throw new CommandErrorException("Le joueur " + name + " n'a jamais joué sur le serveur.");
        }

        return player;
    }

    @Override
    public @NotNull SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
        return (context) -> CoreBukkitPlugin.getInstance().getPlayerManager().getOnlinePlayerNames();
    }

    @Override
    public @NotNull PrioritySpec parsePriority() {
        return PrioritySpec.highest();
    }
}
