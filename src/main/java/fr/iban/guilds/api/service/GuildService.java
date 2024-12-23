package fr.iban.guilds.api.service;

import fr.iban.common.model.MSPlayer;
import fr.iban.common.model.MSPlayerProfile;
import fr.iban.guilds.enums.ChatMode;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildPlayer;
import fr.iban.guilds.model.GuildRank;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface GuildService {

    void createGuild(Player player, String name);

    void toggleChatMode(Player player);

    void setChatMode(Player player, ChatMode chatMode);

    void disbandGuild(Player player);

    void joinGuild(Player player, Guild guild);

    void quitGuild(Player player);

    void invite(Player player, MSPlayerProfile target);

    void revokeInvite(Player player, MSPlayer target);

    void teleportHome(Player player);

    void setHome(Player player);

    void delHome(Player player);

    void kick(Player player, GuildPlayer target);

    void demote(Player player, GuildPlayer target);

    void promote(Player player, GuildPlayer target);

    void transfer(Player player, GuildPlayer target);
}
