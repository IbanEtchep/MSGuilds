package com.github.ibanetchep.artisanguilds.command;

import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import net.artisanmc.modules.api.network.NetworkPlayer;
import net.artisanmc.modules.api.playercommand.SuggestionProvider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * What {@code /guild} offers on Tab, from what this server holds in memory: Artisan calls these on
 * the completion thread, so nothing here reads the database. Artisan filters by what was typed.
 */
public final class GuildSuggestions {

    private final GuildRegistry registry;
    private final Supplier<Collection<NetworkPlayer>> online;
    /** A player's name from what this server knows, null when it knows none. */
    private final Function<UUID, String> nameOf;

    public GuildSuggestions(GuildRegistry registry, Supplier<Collection<NetworkPlayer>> online,
                            Function<UUID, String> nameOf) {
        this.registry = registry;
        this.online = online;
        this.nameOf = nameOf;
    }

    /** Every guild. */
    public SuggestionProvider guilds() {
        return ctx -> registry.all().stream().map(GuildData::getName).toList();
    }

    /** The other members of the sender's guild. */
    public SuggestionProvider members() {
        return ctx -> {
            UUID self = id(ctx.getSender());
            GuildData guild = guildOf(self);
            if (guild == null) return List.of();
            return guild.members().stream().filter(m -> !m.getPlayerId().equals(self)).map(MemberData::getName)
                    .filter(Objects::nonNull).toList();
        };
    }

    /** The players online on the network who are in no guild. */
    public SuggestionProvider invitable() {
        return ctx -> online.get().stream().filter(p -> registry.byPlayer(p.getId()) == null)
                .map(NetworkPlayer::getName).toList();
    }

    /** The players the sender's guild invited. */
    public SuggestionProvider invited() {
        return ctx -> {
            GuildData guild = guildOf(id(ctx.getSender()));
            if (guild == null) return List.of();
            return guild.invites().stream().map(nameOf).filter(Objects::nonNull).toList();
        };
    }

    /** Guilds the sender's guild may offer an alliance to: not itself, not an ally already. */
    public SuggestionProvider allianceTargets() {
        return ctx -> {
            GuildData guild = guildOf(id(ctx.getSender()));
            return registry.all().stream()
                    .filter(g -> guild == null || (!g.equals(guild) && !guild.allianceIds().contains(g.getId())))
                    .map(GuildData::getName).toList();
        };
    }

    /** Guilds that offered the sender's guild an alliance. */
    public SuggestionProvider allianceOffers() {
        return ctx -> {
            GuildData guild = guildOf(id(ctx.getSender()));
            if (guild == null) return List.of();
            return registry.all().stream().filter(g -> g.allianceInvites().contains(guild.getId()))
                    .map(GuildData::getName).toList();
        };
    }

    /** The sender's allies. */
    public SuggestionProvider allies() {
        return ctx -> {
            GuildData guild = guildOf(id(ctx.getSender()));
            if (guild == null) return List.of();
            return guild.allianceIds().stream().map(registry::get).filter(Objects::nonNull)
                    .map(GuildData::getName).toList();
        };
    }

    private @Nullable GuildData guildOf(@Nullable UUID playerId) {
        return playerId == null ? null : registry.byPlayer(playerId);
    }

    private static @Nullable UUID id(Object sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }
}
