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
     * Convertit une chaîne avec des codes couleur (&)  et minimessage en Component
     * @param string Le texte à convertir
     * @return Le Component formaté
     */
    public static String translateColors(String string) {
        return componentToString(LegacyComponentSerializer.legacyAmpersand().deserialize(string));
    }

    /**
     * Convertit une chaîne contenant à la fois des codes couleur (&) et de la syntaxe MiniMessage en Component
     * Seuls les styles de formatage sont autorisés (couleurs, décorations, gradients)
     * @param message Le message à convertir
     * @return Le Component formaté avec les deux types de syntaxe
     */
    public static Component parseAll(String message) {
        MiniMessage miniMessage = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.color()) // Couleurs basiques
                        .resolver(StandardTags.decorations()) // Décorations (bold, italic, etc.)
                        .resolver(StandardTags.gradient()) // Gradients
                        .resolver(StandardTags.rainbow()) // Rainbow
                        .build())
                .build();

        String miniMessageFormat = legacyToMiniMessage(message);
        return miniMessage.deserialize(miniMessageFormat);
    }
    /**
     * Convertit une chaîne avec la syntaxe MiniMessage en Component
     * @param string Le texte à convertir avec syntaxe MiniMessage
     * @return Le Component formatté
     */
    public static Component parseMiniMessage(String string) {
        return MiniMessage.miniMessage().deserialize(string);
    }

    /**
     * Convertit un Component en chaîne avec des codes couleur (&)
     * @param component Le Component à convertir
     * @return La chaîne formatée
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

    public static String toPlainText(Component newName) {
        return PlainTextComponentSerializer.plainText().serialize(newName);
    }

    public static String toMiniMessage(Component newName) {
        return MiniMessage.miniMessage().serialize(newName);
    }
}
