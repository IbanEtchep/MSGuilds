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
            player.sendMessage("§cVous devez être au moins administrateur pour inviter une guilde à vous rejoindre.");
            return;
        }

        if (targetGuild.getId() == guild.getId()) {
            player.sendMessage("§cVous ne pouvez pas vous allier avec votre propre guilde.");
            return;
        }

        if (guild.getAlliances().contains(targetGuild)) {
            player.sendMessage("§cVous êtes déjà allié avec cette guilde.");
            return;
        }

        if (guild.getAllianceInvites().contains(targetGuild.getId())) {
            player.sendMessage("§cVous avez déjà envoyé une invitation à cette guilde.");
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
            player.sendMessage("§cVous devez être au moins administrateur pour accepter une invitation à rejoindre une alliance.");
            return;
        }

        if (!targetGuild.getAllianceInvites().contains(guild.getId())) {
            player.sendMessage("§cVous n'avez pas reçu d'invitation de cette guilde.");
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
            sender.sendMessage("§cVous devez être au moins administrateur pour révoquer une alliance.");
            return;
        }

        if (!guild.getAlliances().contains(target)) {
            sender.sendMessage("§cVous n'êtes pas allié avec cette guilde.");
            return;
        }

        guild.getAlliances().remove(target);
        target.getAlliances().remove(guild);
        guild.sendMessageToOnlineMembers("§cVous n'êtes plus allié avec la guilde " + target.getName() + ".");
        target.sendMessageToOnlineMembers("§cVous n'êtes plus allié avec la guilde " + guild.getName() + ".");
        guildManager.addLog(guild, "Alliance avec la guilde " + target.getName() + " révoquée.");
        guildManager.saveGuild(guild);
        guildManager.saveGuild(target);
    }

}
