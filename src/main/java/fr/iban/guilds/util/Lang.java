package fr.iban.guilds.util;

import fr.iban.guilds.GuildsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public enum Lang {

    NO_PERM("&cVous n'avez pas la permission d'effectuer cette action."),
    NOT_GUILD_MEMBER("&cVous n'êtes pas membre d'une guilde !"),
    TARGET_NOT_GUILD_MEMBER("&cCe joueur n'est pas dans votre guilde !"),
    ALREADY_GUILD_MEMBER("&cVous êtes déjà membre d'une guilde !"),
    GUILD_ALREADY_EXISTS("&cUne guilde existe déjà au nom de {name} !"),
    NOT_GRANTED("&cVous n'avez pas la permission de {action} !"),
    GUILD_CREATED("&cVous avez crée une guilde au nom de {name}."),
    ALREADY_GUILD_OWNER("&cCette guilde vous appartient déjà !"),
    PLAYER_JOINED_GUILD("&7{name} a rejoint la guilde."),
    PLAYER_LEFT_GUILD("&7{name} a quitté la guilde."),
    ECONOMY_DISABLED("&cL'économie est désactivée."),
    BANK_NOT_ENOUGHT_MONEY("&cIl n'y a pas assez d'argent dans la banque."),
    BALANCE_NOT_ENOUGHT_MONEY("&cIl n'y a pas assez d'argent dans la banque."),
    RELOAD("&cReload effectué avec succès.");

    private final String def;

    private static File configFile;
    private static FileConfiguration config;

     Lang(String def) {
        this.def = def;
    }

    @Override
    public String toString() {
        return ChatColor.translateAlternateColorCodes('&', def);
    }

    public String replace(String find, String replace) {
        return toString().replace(find, replace);
    }

    public String getWithoutColor() {
        return ChatColor.stripColor(toString());
    }
}
