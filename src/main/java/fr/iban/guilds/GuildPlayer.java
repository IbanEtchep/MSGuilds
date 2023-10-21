package fr.iban.guilds;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.bukkitcore.manager.BukkitPlayerManager;
import fr.iban.guilds.enums.ChatMode;
import fr.iban.guilds.enums.Rank;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GuildPlayer {

    private final UUID uuid;
    private UUID guildId;
    private Rank rank;
    private ChatMode chatMode;
    private String name;

    public GuildPlayer(UUID uuid, UUID guildId, Rank rank, ChatMode chatMode) {
        this.uuid = uuid;
        this.guildId = guildId;
        this.rank = rank;
        this.chatMode = chatMode;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getGuildId() {
        return guildId;
    }

    public void setGuildId(UUID guildId) {
        this.guildId = guildId;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public ChatMode getChatMode() {
        return chatMode;
    }

    public void setChatMode(ChatMode chatMode) {
        this.chatMode = chatMode;
    }

    public String getName() {
        if (name == null) {
            name = CoreBukkitPlugin.getInstance().getPlayerManager().getName(uuid);
        }
        return name;
    }

    public boolean isGranted(Rank comparedRank) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.hasPermission("guilds.bypass")) {
            return true;
        }
        switch (rank) {
            case OWNER -> {
                return true;
            }
            case ADMIN -> {
                return comparedRank == Rank.ADMIN || comparedRank == Rank.MODERATOR || comparedRank == Rank.MEMBER;
            }
            case MODERATOR -> {
                return comparedRank == Rank.MODERATOR || comparedRank == Rank.MEMBER;
            }
            case MEMBER -> {
                return comparedRank == Rank.MEMBER;
            }
        }
        return false;
    }

    public void sendMessageIfOnline(String message, boolean raw) {
        Player player = Bukkit.getPlayer(uuid);
        BukkitPlayerManager playerManager = CoreBukkitPlugin.getInstance().getPlayerManager();

        if (!raw) {
            if (player != null) {
                player.sendMessage(message);
            } else {
                playerManager.sendMessageIfOnline(uuid, message);
            }
        } else {
            playerManager.sendMessageRawIfOnline(uuid, message);
        }
    }

    public void sendMessageIfOnline(String message) {
        sendMessageIfOnline(message, false);
    }
}
