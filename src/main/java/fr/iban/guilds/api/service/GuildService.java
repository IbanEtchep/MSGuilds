package fr.iban.guilds.api.service;

import fr.iban.guilds.enums.ChatMode;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildRank;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface GuildService {

    void createGuild(Player player, String name);

    void toggleChatMode(Player player);

    void setChatMode(Player player, ChatMode chatMode);

    void disbandGuild(Player player);

    void joinGuild(Player player, Guild guild);

    void quitGuild(Player player);

    void invite(Player player, OfflinePlayer target);

    void revokeInvite(Player player, OfflinePlayer target);

    void teleportHome(Player player);

    void setHome(Player player);

    void delHome(Player player);

    void kick(Player player, OfflinePlayer target);

    void demote(Player player, OfflinePlayer target);

    void promote(Player player, OfflinePlayer target);

    void transfer(Player player, OfflinePlayer target);
}
