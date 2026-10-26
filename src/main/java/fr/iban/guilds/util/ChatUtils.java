package fr.iban.guilds.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatUtils {

    private ChatUtils() {}

    /**
     * Converts a string with color codes (&) and MiniMessage into a Component
     * @param string The text to convert
     * @return The formatted Component
     */
    public static String translateColors(String string) {
        return componentToString(LegacyComponentSerializer.legacyAmpersand().deserialize(string));
    }

    /**
     * Converts a string containing both color codes (&) and MiniMessage syntax into a Component
     * Only formatting styles are allowed (colors, decorations, gradients)
     * @param message The message to convert
     * @return The Component formatted with both syntax types
     */
    public static Component parseAll(String message) {
        MiniMessage miniMessage = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.color()) // Couleurs basiques
                        .resolver(StandardTags.decorations()) // Decorations (bold, italic, etc.)
                        .resolver(StandardTags.gradient()) // Gradients
                        .resolver(StandardTags.rainbow()) // Rainbow
                        .build())
                .build();

        String miniMessageFormat = legacyToMiniMessage(message);
        return miniMessage.deserialize(miniMessageFormat);
    }
    /**
     * Converts a string with MiniMessage syntax into a Component
     * @param string The text to convert, in MiniMessage syntax
     * @return The formatted Component
     */
    public static Component parseMiniMessage(String string) {
        return MiniMessage.miniMessage().deserialize(string);
    }

    /**
     * Converts a Component into a string with color codes (&)
     * @param component The Component to convert
     * @return The formatted string
     */
    public static String componentToString(Component component) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    public static String legacyToMiniMessage(String legacy) {
        return MiniMessage.miniMessage().serialize(
                LegacyComponentSerializer.builder()
                        .character('&')
                        .hexColors()
                        .build()
                        .deserialize(legacy)
        );
    }

    public static String legacySectionToMiniMessage(String legacy) {
        return MiniMessage.miniMessage().serialize(
                LegacyComponentSerializer.builder()
                        .character('§')
                        .hexColors()
                        .build()
                        .deserialize(legacy)
        );
    }

    public static String toPlainText(Component newName) {
        return PlainTextComponentSerializer.plainText().serialize(newName);
    }

    public static String toMiniMessage(Component newName) {
        return MiniMessage.miniMessage().serialize(newName);
    }
}
