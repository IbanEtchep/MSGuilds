package fr.iban.guilds.command.parametertypes;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.common.model.MSPlayerProfile;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.parameter.PrioritySpec;
import revxrsal.commands.stream.MutableStringStream;

public class MSPlayerProfileParameterType implements ParameterType<BukkitCommandActor, MSPlayerProfile> {

    @Override
    public MSPlayerProfile parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<BukkitCommandActor> executionContext) {
        String name = input.readString();
        MSPlayerProfile profile = CoreBukkitPlugin.getInstance().getPlayerManager().getProfile(name);

        if (profile == null) {
            throw new CommandErrorException("Le joueur " + name + " n'est pas en ligne.");
        }

        return profile;
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
