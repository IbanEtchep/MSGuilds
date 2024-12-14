package fr.iban.guilds.service;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.api.GuildManager;
import fr.iban.guilds.api.service.GuildAllianceService;
import fr.iban.guilds.enums.GuildPermission;
import fr.iban.guilds.lang.Lang;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildPlayer;
import fr.iban.guilds.util.GuildRequestMessage;
import org.bukkit.entity.Player;

public class GuildAllianceServiceImpl implements GuildAllianceService {

    private final GuildsPlugin plugin;
    private final GuildManager guildManager;

    public GuildAllianceServiceImpl(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.guildManager = plugin.getGuildManager();
    }

    @Override
    public void sendAllianceRequest(Player sender, Guild targetGuild) {
        Guild guild = guildManager.getGuildByPlayer(sender);

        if (guild == null) {
            sender.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMembers().get(sender.getUniqueId());

        if (!guildPlayer.isGranted(GuildPermission.MANAGE_ALLIANCES)) {
            sender.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if (targetGuild.getId() == guild.getId()) {
            sender.sendMessage(Lang.ERROR_CANNOT_SELF_ALLY.component());
            return;
        }

        if (guild.getAlliances().contains(targetGuild)) {
            sender.sendMessage(Lang.ERROR_ALREADY_ALLIED.component());
            return;
        }

        if (guild.getAllianceInvites().contains(targetGuild.getId())) {
            sender.sendMessage(Lang.ERROR_ALLIANCE_INVITE_ALREADY_SENT.component());
            return;
        }

        guild.getAllianceInvites().add(targetGuild.getId());
        plugin.getMessagingManager().sendMessage(GuildsPlugin.GUILD_ALLIANCE_REQUEST, new GuildRequestMessage(guild.getId(), targetGuild.getId()));
        targetGuild.sendMessageToOnlineMembers(Lang.ALLIANCE_REQUEST_RECEIVED.component("guild", guild.getName()));
        sender.sendMessage(Lang.ALLIANCE_REQUEST_SENT.component("guild", targetGuild.getName()));
    }

    @Override
    public void acceptAllianceRequest(Player sender, Guild targetGuild) {
        Guild guild = guildManager.getGuildByPlayer(sender);

        if (guild == null) {
            sender.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMembers().get(sender.getUniqueId());

        if (!guildPlayer.isGranted(GuildPermission.MANAGE_ALLIANCES)) {
            sender.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if (!targetGuild.getAllianceInvites().contains(guild.getId())) {
            sender.sendMessage(Lang.ERROR_NOT_INVITED.component());
            return;
        }

        if (guild.getAlliances().contains(targetGuild)) {
            sender.sendMessage(Lang.ERROR_ALREADY_ALLIED.component());
            return;
        }

        guild.getAllianceInvites().remove(targetGuild.getId());
        guild.getAlliances().add(targetGuild);
        targetGuild.getAlliances().add(guild);
        guild.sendMessageToOnlineMembers(Lang.ALLIANCE_FORMED.component("guild", targetGuild.getName()));
        targetGuild.sendMessageToOnlineMembers(Lang.ALLIANCE_FORMED.component("guild", guild.getName()));
        guildManager.addLog(guild, "Alliance avec la guilde " + targetGuild.getName() + " acceptée.");
        guildManager.saveGuild(guild);
        guildManager.saveGuild(targetGuild);
    }

    @Override
    public void revokeAlliance(Player sender, Guild target) {
        Guild guild = guildManager.getGuildByPlayer(sender);

        if (guild == null) {
            sender.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMembers().get(sender.getUniqueId());

        if (!guildPlayer.isGranted(GuildPermission.MANAGE_ALLIANCES)) {
            sender.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if (!guild.getAlliances().contains(target)) {
            sender.sendMessage(Lang.ERROR_NOT_ALLIED.component());
            return;
        }

        guild.getAlliances().remove(target);
        target.getAlliances().remove(guild);
        guild.sendMessageToOnlineMembers(Lang.ALLIANCE_ENDED.component("guild", target.getName()));
        target.sendMessageToOnlineMembers(Lang.ALLIANCE_ENDED.component("guild", guild.getName()));
        guildManager.addLog(guild, Lang.ALLIANCE_ENDED.plainText("guild", target.getName()));
        guildManager.saveGuild(guild);
        guildManager.saveGuild(target);
    }

}
