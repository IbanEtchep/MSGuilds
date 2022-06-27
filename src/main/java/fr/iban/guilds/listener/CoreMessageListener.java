package fr.iban.guilds.listener;

import fr.iban.bukkitcore.event.CoreMessageEvent;
import fr.iban.common.messaging.Message;
import fr.iban.guilds.GuildsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class CoreMessageListener implements Listener {

    public GuildsPlugin plugin;

    public CoreMessageListener(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMessage(CoreMessageEvent e) {
        Message message = e.getMessage();
        String channel = message.getChannel();

        if(channel.equals(GuildsPlugin.GUILD_SYNC_CHANNEL)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getGuildsManager().reloadGuildFromDB(UUID.fromString(message.getMessage())));
        }else if(channel.equals(GuildsPlugin.GUILD_PLAYER_SYNC_CHANNEL)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getGuildsManager().reloadGuildPlayerFromDB(UUID.fromString(message.getMessage())));
        }
    }
}
