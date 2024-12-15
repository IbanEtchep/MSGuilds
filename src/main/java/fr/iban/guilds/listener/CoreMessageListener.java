package fr.iban.guilds.listener;

import com.google.gson.Gson;
import fr.iban.bukkitcore.event.CoreMessageEvent;
import fr.iban.common.messaging.Message;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.util.GuildRequestMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class CoreMessageListener implements Listener {

    private final GuildsPlugin plugin;
    private final Gson gson = new Gson();

    public CoreMessageListener(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMessage(CoreMessageEvent e) {
        Message message = e.getMessage();
        String channel = message.getChannel();

        switch (channel) {
            case GuildsPlugin.GUILD_SYNC_CHANNEL -> plugin.getGuildManager().reloadGuildFromDB(UUID.fromString(message.getMessage()));
            case GuildsPlugin.GUILD_PLAYER_SYNC_CHANNEL -> plugin.getGuildManager().reloadGuildPlayerFromDB(UUID.fromString(message.getMessage()));
            case GuildsPlugin.GUILD_INVITE_ADD -> consumeAddInviteMessage(message);
            case GuildsPlugin.GUILD_INVITE_REVOKE -> consumeRevokeInviteMessage(message);
            case GuildsPlugin.GUILD_ALLIANCE_REQUEST -> consumeAllianceRequestMessage(message);
        }
    }

    private void consumeAddInviteMessage(Message message) {
        GuildRequestMessage requestMessage = gson.fromJson(message.getMessage(), GuildRequestMessage.class);
        Guild guild = plugin.getGuildManager().getGuildById(requestMessage.senderID());
        if (guild != null) {
            if (!guild.getInvites().contains(requestMessage.targetID())) {
                guild.getInvites().add(requestMessage.targetID());
                plugin.getScheduler().runLater(
                        () -> guild.getInvites().remove(requestMessage.targetID()), 2400L);
            }
        }
    }

    private void consumeRevokeInviteMessage(Message message) {
        GuildRequestMessage requestMessage = gson.fromJson(message.getMessage(), GuildRequestMessage.class);
        Guild guild = plugin.getGuildManager().getGuildById(requestMessage.senderID());
        if (guild != null) {
            guild.getInvites().remove(requestMessage.targetID());
        }
    }

    private void consumeAllianceRequestMessage(Message message) {
        GuildRequestMessage requestMessage = gson.fromJson(message.getMessage(), GuildRequestMessage.class);
        Guild guild = plugin.getGuildManager().getGuildById(requestMessage.senderID());
        if (guild != null) {
            if (!guild.getAllianceInvites().contains(requestMessage.targetID())) {
                guild.getAllianceInvites().add(requestMessage.targetID());
                plugin.getScheduler().runLater(
                        () -> guild.getAllianceInvites().remove(requestMessage.targetID()), 2400L);
            }
        }
    }
}
