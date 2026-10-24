package com.github.ibanetchep.artisanguilds.model;

import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.api.GuildMember;
import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Function;

public final class MemberData implements GuildMember {

    /** Names of players, resolved once per member: Bukkit's offline players unless a test sets another. */
    public static volatile Function<UUID, String> names = id -> {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString();
    };

    private final UUID playerId;
    private final GuildData guild;
    private volatile RankData rank;
    private volatile ChatMode chatMode;
    private volatile String name;

    public MemberData(UUID playerId, GuildData guild, RankData rank, ChatMode chatMode) {
        this.playerId = playerId;
        this.guild = guild;
        this.rank = rank;
        this.chatMode = chatMode;
    }

    @Override
    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public String getName() {
        if (name == null) name = names.apply(playerId);
        return name;
    }

    /** A name this server just saw (the player is online here). */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public GuildData getGuild() {
        return guild;
    }

    @Override
    public RankData getRank() {
        return rank;
    }

    public void setRank(RankData rank) {
        this.rank = rank;
    }

    @Override
    public ChatMode getChatMode() {
        return chatMode;
    }

    public void setChatMode(ChatMode chatMode) {
        this.chatMode = chatMode;
    }

    @Override
    public boolean isOwner() {
        return playerId.equals(guild.getOwnerId());
    }

    @Override
    public boolean isGranted(GuildPermission permission) {
        Player player = Bukkit.getServer() == null ? null : Bukkit.getPlayer(playerId);
        if (player != null && player.hasPermission("guilds.bypass")) return true;
        return isOwner() || rank.hasPermission(permission);
    }
}
