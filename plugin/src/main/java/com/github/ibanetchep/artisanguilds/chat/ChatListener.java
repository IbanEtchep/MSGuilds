package com.github.ibanetchep.artisanguilds.chat;

import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import com.github.ibanetchep.artisanguilds.service.Msg;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Paper's chat event routed by {@link ChatRoute}: guild and alliance lines never reach public chat. */
public final class ChatListener implements Listener {

    public static final String PERMISSION = "guilds.chat";

    private final GuildRegistry registry;
    private final GuildStore store;
    private final GuildChat chat;
    private final GuildMessages messages;

    public ChatListener(GuildRegistry registry, GuildStore store, GuildChat chat, GuildMessages messages) {
        this.registry = registry;
        this.store = store;
        this.chat = chat;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        MemberData member = registry.member(player.getUniqueId());
        if (member == null) return;

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        boolean allowed = player.hasPermission(PERMISSION);
        if (!allowed && member.getChatMode() != ChatMode.PUBLIC) {
            member.setChatMode(ChatMode.PUBLIC);
            store.saveMember(member);
        }

        ChatRoute route = ChatRoute.of(member.getChatMode(), message, allowed);
        if (route.denied()) {
            event.setCancelled(true);
            messages.send(player, Msg.ERROR_NO_PERMISSION);
        } else if (route.channel() != null) {
            event.setCancelled(true);
            chat.send(member, route.channel(), route.text());
        } else if (!route.text().equals(message)) {
            event.message(Component.text(route.text()));
        }
    }
}
