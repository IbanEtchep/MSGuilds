package fr.iban.guilds.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ChatUtils {

    private ChatUtils() {}

    /**
     * Convertit une chaîne avec des codes couleur (&) en Component
     * @param string Le texte à convertir
     * @return Le Component formaté
     */
    public static String translateColors(String string) {
        return componentToString(LegacyComponentSerializer.legacyAmpersand().deserialize(string));
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
}
