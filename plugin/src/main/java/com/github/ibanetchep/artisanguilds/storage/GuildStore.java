package com.github.ibanetchep.artisanguilds.storage;

import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.sync.GuildSync;
import net.artisanmc.modules.api.network.NetworkApi;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes on one thread, in the order they were asked: memory is already changed, the database
 * follows. Once a change is written the other servers are told to read it again
 * ({@link GuildSync}), like MSGuilds did through MSCore.
 */
public final class GuildStore {

    private final GuildRepository repository;
    private final Executor writer;
    private final NetworkApi network;
    private final Logger logger;

    public GuildStore(GuildRepository repository, Executor writer, NetworkApi network, Logger logger) {
        this.repository = repository;
        this.writer = writer;
        this.network = network;
        this.logger = logger;
    }

    public GuildRepository repository() {
        return repository;
    }

    public Executor writer() {
        return writer;
    }

    public void saveGuild(GuildData guild) {
        run("save guild " + guild.getName(), () -> repository.saveGuild(guild),
                () -> network.publish(GuildSync.GUILD, guild.getId().toString()));
    }

    public void deleteGuild(UUID guildId) {
        run("delete guild " + guildId, () -> repository.deleteGuild(guildId),
                () -> network.publish(GuildSync.GUILD, guildId.toString()));
    }

    public void saveMember(MemberData member) {
        run("save member " + member.getPlayerId(), () -> repository.saveMember(member),
                () -> network.publish(GuildSync.PLAYER, GuildSync.player(member.getPlayerId(), null)));
    }

    /** {@code reason} tells the other servers why ({@code GuildMemberLeaveEvent.Reason}). */
    public void deleteMember(UUID playerId, String reason) {
        run("delete member " + playerId, () -> repository.deleteMember(playerId),
                () -> network.publish(GuildSync.PLAYER, GuildSync.player(playerId, reason)));
    }

    public void addLog(GuildData guild, String log) {
        run("log of " + guild.getName(), () -> repository.addLog(guild.getId(), log), () -> { });
    }

    public CompletableFuture<List<String>> logs(UUID guildId) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        writer.execute(() -> {
            try {
                future.complete(repository.logs(guildId));
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @FunctionalInterface
    private interface Write {
        void run() throws SQLException;
    }

    private void run(String what, Write write, Runnable then) {
        writer.execute(() -> {
            try {
                write.run();
                then.run();
            } catch (SQLException | RuntimeException e) {
                logger.log(Level.SEVERE, "[ArtisanGuilds] Could not " + what, e);
            }
        });
    }
}
