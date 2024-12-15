package fr.iban.guilds.listener;

import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildPlayer;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.enums.ChatMode;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListeners implements Listener {

    private final GuildsPlugin plugin;

    public ChatListeners(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player player = e.getPlayer();
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player);
        if (guild == null) {
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(player.getUniqueId());
        String message = PlainTextComponentSerializer.plainText().serialize(e.message());

        if (guildPlayer.getChatMode() == ChatMode.GUILD && !message.startsWith("!")) {
            Component guildMessage = Component.text("[Guilde] ", NamedTextColor.GRAY)
                    .append(Component.text(guildPlayer.getRank().getName()))
                    .append(Component.text(player.getName()))
                    .append(Component.text(" ➤ ", NamedTextColor.WHITE))
                    .append(e.message());

            guild.sendMessageToOnlineMembers(guildMessage);
            plugin.getLogger().info("Chat (" + player.getName() + ") : " + message);
            e.setCancelled(true);
        }

        if (guildPlayer.getChatMode() == ChatMode.PUBLIC && message.startsWith("!")) {
            String msg = message.substring(1);
            Component guildMessage = Component.text("[Guilde] ", NamedTextColor.GRAY)
                    .append(Component.text(guildPlayer.getRank().getName()))
                    .append(Component.text(player.getName()))
                    .append(Component.text(" ➤ ", NamedTextColor.WHITE))
                    .append(Component.text(msg));

            guild.sendMessageToOnlineMembers(guildMessage);
            plugin.getLogger().info("[Guilde] Chat (" + player.getName() + ") : " + msg);
            e.setCancelled(true);
        }

        if (guildPlayer.getChatMode() == ChatMode.ALLY && !message.startsWith("!")) {
            Component allyMessage = Component.text("[Alliance] ", NamedTextColor.GRAY)
                    .append(Component.text(guildPlayer.getRank().getName()))
                    .append(Component.text(player.getName()))
                    .append(Component.text(" ➤ ", NamedTextColor.WHITE))
                    .append(e.message());

            guild.sendMessageToAllies(allyMessage);
            plugin.getLogger().info("[Alliance] Chat (" + player.getName() + ") : " + message);
            e.setCancelled(true);
        }

        if (guildPlayer.getChatMode() != ChatMode.PUBLIC && message.startsWith("!")) {
            String msg = message.substring(1);
            e.message(Component.text(msg));
        }
    }
}