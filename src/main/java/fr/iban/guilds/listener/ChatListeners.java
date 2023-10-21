package fr.iban.guilds.listener;

import fr.iban.guilds.Guild;
import fr.iban.guilds.GuildPlayer;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.enums.ChatMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListeners implements Listener {

    private final GuildsPlugin plugin;

    public ChatListeners(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        Guild guild = plugin.getGuildsManager().getGuildByPlayer(player);
        if (guild == null) {
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(player.getUniqueId());

        if (guildPlayer.getChatMode() == ChatMode.GUILD && !e.getMessage().startsWith("!")) {
            guild.sendMessageToOnlineMembers("§7[Guilde] " + guildPlayer.getRank().getColor() + player.getName() + " §f➤ " + e.getMessage());
            plugin.getLogger().info("Chat (" + player.getName() + ") : " + e.getMessage());
            e.setCancelled(true);
        }

        if (guildPlayer.getChatMode() == ChatMode.PUBLIC && e.getMessage().startsWith("!")) {
            String msg = e.getMessage().replaceFirst("!", "");
            guild.sendMessageToOnlineMembers("§7[Guilde] " + guildPlayer.getRank().getColor() + player.getName() + " §f➤ " + msg);
            plugin.getLogger().info("§7[Guilde] Chat (" + player.getName() + ") : " + msg);
            e.setCancelled(true);
        }

        if (guildPlayer.getChatMode() == ChatMode.ALLY && !e.getMessage().startsWith("!")) {
            String msg = e.getMessage();
            guild.sendMessageToAllies("§7[Alliance] " + guildPlayer.getRank().getColor() + player.getName() + " §f➤ " + msg);
            plugin.getLogger().info("[Alliance] Chat (" + player.getName() + ") : " + msg);
            e.setCancelled(true);
        }

        if (guildPlayer.getChatMode() != ChatMode.PUBLIC && e.getMessage().startsWith("!")) {
            String msg = e.getMessage().replaceFirst("!", "");
            e.setMessage(msg);
        }
    }

}
