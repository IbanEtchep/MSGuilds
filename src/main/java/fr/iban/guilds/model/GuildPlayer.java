package fr.iban.guilds.model;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.guilds.enums.ChatMode;
import fr.iban.guilds.enums.GuildPermission;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GuildPlayer {

    private final UUID uuid;
    private Guild guild;
    private ChatMode chatMode;
    private String name;
    private GuildRank rank;

    public GuildPlayer(UUID uuid, Guild guild, GuildRank rank, ChatMode chatMode) {
        this.uuid = uuid;
        this.guild = guild;
        this.rank = rank;
        this.chatMode = chatMode;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public GuildRank getRank() {
        return rank;
    }

    public void setRank(GuildRank rank) {
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
            name = CoreBukkitPlugin.getInstance().getPlayerManager().getOfflinePlayer(uuid).getName();
        }
        return name;
    }

    public boolean isGranted(GuildPermission permission) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.hasPermission("guilds.bypass")) {
            return true;
        }

        return rank.hasPermission(permission) || isOwner();
    }

    public boolean isOwner() {
        return guild.getOwnerUUID().equals(uuid);
    }

    public void sendMessageIfOnline(Component message) {
        CoreBukkitPlugin.getInstance().getPlayerManager().sendMessageIfOnline(uuid, message);
    }
}
