package fr.iban.guilds.service;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.api.GuildManager;
import fr.iban.guilds.api.service.GuildRankService;
import fr.iban.guilds.enums.GuildPermission;
import fr.iban.guilds.lang.Lang;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildRank;
import fr.iban.guilds.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GuildRankServiceImpl implements GuildRankService {

    private final GuildsPlugin plugin;
    private final GuildManager guildManager;

    public GuildRankServiceImpl(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.guildManager = plugin.getGuildManager();
    }

    @Override
    public void rankMoveUp(Player player, GuildRank rank) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.MANAGE_RANKS)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if(rank.getOrder() == guild.getRanks().size() - 1) {
            player.sendMessage(Lang.ERROR_RANK_ALREADY_FIRST.component());
            return;
        }

        guild.moveRankUp(rank);
        guildManager.saveGuild(guild);
        player.sendMessage(Lang.RANK_MOVED_UP.component("rank", rank.getName()));
    }

    @Override
    public void rankMoveDown(Player player, GuildRank rank) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.MANAGE_RANKS)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if(rank.getOrder() == 0) {
            player.sendMessage(Lang.ERROR_RANK_ALREADY_LAST.component());
            return;
        }

        guild.moveRankDown(rank);
        guildManager.saveGuild(guild);
        player.sendMessage(Lang.RANK_MOVED_DOWN.component("rank", rank.getName()));
    }

    @Override
    public void deleteRank(Player player, GuildRank rank) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.MANAGE_RANKS)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if(!guild.getMembersByRank(rank).isEmpty()) {
            player.sendMessage(Lang.ERROR_RANK_NOT_EMPTY.component());
            return;
        }

        player.sendMessage(Lang.RANK_DELETED.component("rank", rank.getName()));

        guild.removeRank(rank);
        guildManager.saveGuild(guild);
    }

    @Override
    public void renameRank(Player player, GuildRank rank, Component newName) {
        Guild guild = guildManager.getGuildByPlayer(player);
        String plainTextName = ChatUtils.toPlainText(newName);
        String miniMessageName = ChatUtils.toMiniMessage(newName);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.MANAGE_RANKS)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if(guild.getRank(plainTextName) != null) {
            player.sendMessage(Lang.ERROR_RANK_ALREADY_EXISTS.component());
            return;
        }

        if(plainTextName.length() > 24 || plainTextName.length() < 2) {
            player.sendMessage(Lang.ERROR_RANK_NAME_LENGTH.component());
            return;
        }


        player.sendMessage(Lang.RANK_RENAMED.component(
                "oldName", rank.getName(),
                "newName", miniMessageName
        ));

        rank.setName(miniMessageName);
        guildManager.saveGuild(guild);
    }

    @Override
    public void createRank(Player player, Component name) {
        Guild guild = guildManager.getGuildByPlayer(player);
        String plainTextName = ChatUtils.toPlainText(name);
        String miniMessageName = ChatUtils.toMiniMessage(name);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.MANAGE_RANKS)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if(guild.getRank(plainTextName) != null) {
            player.sendMessage(Lang.ERROR_RANK_ALREADY_EXISTS.component());
            return;
        }

        if(plainTextName.length() > 24 || plainTextName.length() < 2) {
            player.sendMessage(Lang.ERROR_RANK_NAME_LENGTH.component());
            return;
        }

        GuildRank rank = new GuildRank(UUID.randomUUID(), miniMessageName, guild.getRanks().size(), new HashSet<>());
        guild.addRank(rank);
        guildManager.saveGuild(guild);
        player.sendMessage(Lang.RANK_CREATED.component("rank", rank.getName()));
    }

}
