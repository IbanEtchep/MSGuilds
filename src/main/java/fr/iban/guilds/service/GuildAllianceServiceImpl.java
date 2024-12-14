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
    public void sendAllianceRequest(Player player, Guild targetGuild) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMembers().get(player.getUniqueId());

        if (!guildPlayer.isGranted(GuildPermission.MANAGE_ALLIANCES)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if (targetGuild.getId() == guild.getId()) {
            player.sendMessage(Lang.ERROR_CANNOT_SELF_ALLY.component());
            return;
        }

        if (guild.getAlliances().contains(targetGuild)) {
            player.sendMessage(Lang.ERROR_ALREADY_ALLIED.component());
            return;
        }

        if (guild.getAllianceInvites().contains(targetGuild.getId())) {
            player.sendMessage(Lang.ERROR_ALLIANCE_INVITE_ALREADY_SENT.component());
            return;
        }

        guild.getAllianceInvites().add(targetGuild.getId());
        plugin.getMessagingManager().sendMessage(GuildsPlugin.GUILD_ALLIANCE_REQUEST, new GuildRequestMessage(guild.getId(), targetGuild.getId()));
        targetGuild.sendMessageToOnlineMembers("[\"\",{\"text\":\"Votre guilde a reçu une invitation d'alliance à \",\"color\":\"green\"},{\"text\":\" " + guild.getName() + "\",\"color\":\"dark_green\"},{\"text\":\". Tapez \",\"color\":\"green\"},{\"text\":\"/guild alliance accept " + guild.getName() + "\",\"bold\":true,\"color\":\"white\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/guild alliance accept " + guild.getName() + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Clic pour accepter\"}},{\"text\":\" ou cliquez\",\"color\":\"green\"},{\"text\":\" pour accepter.\",\"color\":\"green\"}]", true);
        player.sendMessage("§aVous avez envoyé une invitation d'alliance à " + targetGuild.getName() + ".");
    }

    @Override
    public void acceptAllianceRequest(Player player, Guild targetGuild) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMembers().get(player.getUniqueId());

        if (!guildPlayer.isGranted(GuildPermission.MANAGE_ALLIANCES)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if (!targetGuild.getAllianceInvites().contains(guild.getId())) {
            player.sendMessage(Lang.ERROR_NOT_INVITED.component());
            return;
        }

        if (guild.getAlliances().contains(targetGuild)) {
            player.sendMessage(Lang.ERROR_ALREADY_ALLIED.component());
            return;
        }

        guild.getAllianceInvites().remove(targetGuild.getId());
        guild.getAlliances().add(targetGuild);
        targetGuild.getAlliances().add(guild);
        guild.sendMessageToOnlineMembers("§aVous êtes désormais allié avec la guilde " + targetGuild.getName() + ".");
        targetGuild.sendMessageToOnlineMembers("§aVous êtes désormais allié avec la guilde " + guild.getName() + ".");
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
        guild.sendMessageToOnlineMembers(Lang.ALLIANCE_ENDED.toString("guild", target.getName()));
        target.sendMessageToOnlineMembers(Lang.ALLIANCE_ENDED.toString("guild", guild.getName()));
        guildManager.addLog(guild, Lang.ALLIANCE_ENDED.plainText("guild", target.getName()));
        guildManager.saveGuild(guild);
        guildManager.saveGuild(target);
    }

}
