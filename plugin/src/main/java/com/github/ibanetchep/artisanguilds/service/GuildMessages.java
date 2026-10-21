package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.GuildPlatform;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import net.artisanmc.modules.api.text.TextApi;
import net.artisanmc.modules.api.translation.MessageBundle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Messages of {@code lang/<code>.yml} ({@link Msg}), placeholders as {@code key, value} pairs.
 * A player on another server gets the message in the bundle's default language: their own
 * language is only known where they are.
 */
public final class GuildMessages {

    private final MessageBundle bundle;
    private final TextApi text;
    private final GuildPlatform platform;

    public GuildMessages(MessageBundle bundle, TextApi text, GuildPlatform platform) {
        this.bundle = bundle;
        this.text = text;
        this.platform = platform;
    }

    public void send(CommandSender to, Msg msg, Object... placeholders) {
        bundle.send(to, msg.key(), map(placeholders));
    }

    /** To the player wherever they are on the network; nothing when offline. */
    public void sendTo(UUID playerId, Msg msg, Object... placeholders) {
        Player player = platform.player(playerId);
        if (player != null) {
            send(player, msg, placeholders);
        } else if (platform.isOnline(playerId)) {
            platform.sendRemote(playerId, miniMessage(msg, placeholders));
        }
    }

    /**
     * Like {@link #sendTo}, with {@code rich} values inserted as MiniMessage before the escaped
     * {@code placeholders}: a rank's colours in a line a player typed.
     */
    public void sendRich(UUID playerId, Msg msg, Map<String, String> rich, Object... placeholders) {
        Player player = platform.player(playerId);
        if (player != null) {
            text.send(player, bundle.format(insert(bundle.raw(player, msg.key()), rich), map(placeholders)));
        } else if (platform.isOnline(playerId)) {
            platform.sendRemote(playerId, bundle.format(insert(bundle.raw(bundle.getDefaultLang(), msg.key()), rich), map(placeholders)));
        }
    }

    /** Like {@link #send}, with {@code rich} values inserted as MiniMessage: a rank's name, a log line. */
    public void sendRich(CommandSender to, Msg msg, Map<String, String> rich, Object... placeholders) {
        text.send(to, bundle.format(insert(bundle.raw(to, msg.key()), rich), map(placeholders)));
    }

    /**
     * The raw text of {@code key} in the viewer's language (the default one without a viewer,
     * as in the editor's preview), else {@code fallback}: for the rows of the menus. A server's
     * lang file copied from MSGuilds lacks the keys added since, hence the fallback.
     */
    public String text(@Nullable CommandSender viewer, String key, String fallback) {
        String lang = viewer != null ? bundle.langOf(viewer) : bundle.getDefaultLang();
        String found = bundle.find(lang, key);
        if (found == null) found = bundle.find(bundle.getDefaultLang(), key);
        return found != null ? found : fallback;
    }

    /** {@code lang/*.yml} read again (edits of the server's files). */
    public void reload() {
        bundle.reload();
    }

    private static String insert(String template, Map<String, String> values) {
        for (Map.Entry<String, String> e : values.entrySet()) {
            String k = e.getKey();
            template = template.replace("%" + k + "%", e.getValue()).replace("<" + k + ">", e.getValue()).replace("{" + k + "}", e.getValue());
        }
        return template;
    }

    public void sendToMembers(GuildData guild, Msg msg, Object... placeholders) {
        guild.members().forEach(m -> sendTo(m.getPlayerId(), msg, placeholders));
    }

    /** Without formatting, in the default language: for the guild's log. */
    public String plain(Msg msg, Object... placeholders) {
        return text.plain(miniMessage(msg, placeholders));
    }

    private String miniMessage(Msg msg, Object... placeholders) {
        return bundle.format(bundle.raw(bundle.getDefaultLang(), msg.key()), map(placeholders));
    }

    private static Map<String, Object> map(Object... placeholders) {
        if (placeholders.length % 2 != 0) throw new IllegalArgumentException("Placeholders go in pairs (key, value)");
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < placeholders.length; i += 2) map.put((String) placeholders[i], String.valueOf(placeholders[i + 1]));
        return map;
    }
}
