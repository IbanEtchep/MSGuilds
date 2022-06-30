package fr.iban.guilds.listener;

import com.google.gson.Gson;
import fr.iban.bukkitcore.event.CoreMessageEvent;
import fr.iban.common.messaging.Message;
import fr.iban.guilds.Guild;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.util.GuildRequestMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.Messenger;

import java.util.UUID;

public class CoreMessageListener implements Listener {

    private GuildsPlugin plugin;
    private Gson gson = new Gson();

    public CoreMessageListener(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMessage(CoreMessageEvent e) {
        Message message = e.getMessage();
        String channel = message.getChannel();

        switch (channel) {
            case GuildsPlugin.GUILD_SYNC_CHANNEL -> Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getGuildsManager().reloadGuildFromDB(UUID.fromString(message.getMessage())));
            case GuildsPlugin.GUILD_PLAYER_SYNC_CHANNEL -> Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getGuildsManager().reloadGuildPlayerFromDB(UUID.fromString(message.getMessage())));
            case GuildsPlugin.GUILD_INVITE_ADD -> consumeAddInviteMessage(message);
            case GuildsPlugin.GUILD_INVITE_REVOKE -> consumeRevokeInviteMessage(message);
        }
    }

    private void consumeAddInviteMessage(Message message) {
        GuildRequestMessage requestMessage = gson.fromJson(message.getMessage(), GuildRequestMessage.class);
        Guild guild = plugin.getGuildsManager().getGuildByPlayerId(requestMessage.getGuildID());
        if (guild != null) {
            if (!guild.getInvites().contains(requestMessage.getPlayerID())) {
                guild.getInvites().add(requestMessage.getPlayerID());
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> guild.getInvites().remove(requestMessage.getPlayerID()), 2400L);
            }
        }
    }

    private void consumeRevokeInviteMessage(Message message) {
        GuildRequestMessage requestMessage = gson.fromJson(message.getMessage(), GuildRequestMessage.class);
        Guild guild = plugin.getGuildsManager().getGuildByPlayerId(requestMessage.getGuildID());
        if (guild != null) {
            guild.getInvites().remove(requestMessage.getPlayerID());
        }
    }
}
