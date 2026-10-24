package com.github.ibanetchep.artisanguilds;

import com.github.ibanetchep.artisanguilds.api.ArtisanGuildsApi;
import com.github.ibanetchep.artisanguilds.api.Guild;
import com.github.ibanetchep.artisanguilds.api.GuildLevelInfo;
import com.github.ibanetchep.artisanguilds.api.GuildMember;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.service.BankService;
import com.github.ibanetchep.artisanguilds.service.GuildLeveling;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** {@link ArtisanGuildsApi} over the registry and the services. */
final class GuildsApiImpl implements ArtisanGuildsApi {

    private final GuildRegistry registry;
    private final GuildStore store;
    private final BankService bank;
    private final GuildLeveling leveling;

    GuildsApiImpl(GuildRegistry registry, GuildStore store, BankService bank, GuildLeveling leveling) {
        this.registry = registry;
        this.store = store;
        this.bank = bank;
        this.leveling = leveling;
    }

    @Override
    public Collection<Guild> getGuilds() {
        return List.copyOf(registry.all());
    }

    @Override
    public @Nullable Guild getGuild(UUID guildId) {
        return registry.get(guildId);
    }

    @Override
    public @Nullable Guild getGuildByName(String name) {
        return registry.byName(name);
    }

    @Override
    public @Nullable Guild getGuildByPlayer(UUID playerId) {
        return registry.byPlayer(playerId);
    }

    @Override
    public @Nullable GuildMember getMember(UUID playerId) {
        return registry.member(playerId);
    }

    @Override
    public boolean deposit(Guild guild, double amount) {
        return bank.deposit(data(guild), amount);
    }

    @Override
    public boolean withdraw(Guild guild, double amount, String reason) {
        return bank.withdraw(data(guild), amount, reason);
    }

    @Override
    public GuildLevelInfo getLevelInfo(Guild guild) {
        return leveling.info(data(guild));
    }

    @Override
    public @Nullable GuildLevelInfo getLevelInfo(int level) {
        return leveling.info(level);
    }

    @Override
    public void addExp(Guild guild, long exp) {
        leveling.addExp(data(guild), exp);
    }

    @Override
    public void setLevel(Guild guild, int level) {
        leveling.setLevel(data(guild), level);
    }

    @Override
    public void addLog(Guild guild, String log) {
        store.addLog(data(guild), log);
    }

    @Override
    public CompletableFuture<List<String>> getLogs(Guild guild) {
        return store.logs(guild.getId());
    }

    /** The guild as this server holds it now: a stale instance after a disband is refused. */
    private GuildData data(Guild guild) {
        GuildData data = registry.get(guild.getId());
        if (data == null) throw new IllegalArgumentException("Guild " + guild.getName() + " no longer exists");
        return data;
    }
}
