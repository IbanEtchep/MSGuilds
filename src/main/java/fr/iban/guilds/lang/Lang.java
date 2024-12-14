package fr.iban.guilds.lang;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public enum Lang {
    // Messages d'aide
    HELP_HEADER("messages.help.header"),
    HELP_NO_GUILD("messages.help.no_guild"),
    HELP_MEMBER("messages.help.member"),

    // Messages généraux
    RELOAD("messages.reload"),

    // Messages de guilde
    GUILD_CREATED("messages.guild.created"),
    GUILD_DELETED("messages.guild.deleted"),
    GUILD_DISBANDED("messages.guild.disbanded"),
    GUILD_DISBAND_CONFIRM("messages.guild.disband.confirm"),
    GUILD_ALREADY_EXISTS("messages.guild.already_exists"),
    GUILD_NAME_NO_SPACE("messages.guild.name.no_space"),
    GUILD_NAME_TOO_LONG("messages.guild.name.too_long"),
    LOG_GUILD_CREATED("messages.logs.guild_created"),

    // Messages de chat
    CHAT_ENABLED("messages.chat.enabled"),
    CHAT_DISABLED("messages.chat.disabled"),
    CHAT_MODE_CHANGED("messages.chat.mode_changed"),

    // Messages de membres
    MEMBER_INVITED("messages.members.invite.sent"),
    MEMBER_INVITE_REVOKED("messages.members.invite.revoked"),
    MEMBER_PROMOTED("messages.members.promote"),
    MEMBER_DEMOTED("messages.members.demote"),
    MEMBER_TRANSFERRED("messages.members.transfer"),
    MEMBER_KICKED("messages.members.kick"),
    MEMBER_JOINED("messages.members.join"),
    MEMBER_LEFT("messages.members.leave"),

    // Messages de banque
    BANK_BALANCE("messages.bank.balance"),
    BANK_DEPOSIT("messages.bank.deposit"),
    BANK_WITHDRAW("messages.bank.withdraw"),
    BANK_INSUFFICIENT_FUNDS("messages.bank.insufficient_funds"),
    BANK_ECONOMY_DISABLED("messages.bank.economy_disabled"),

    // Messages de home
    HOME_TELEPORT("messages.home.teleport"),
    HOME_SET("messages.home.set"),
    HOME_DELETE("messages.home.delete"),
    HOME_NOT_SET("messages.home.not_set"),

    // Messages d'info
    INFO_HEADER("messages.info.header"),
    INFO_CREATED("messages.info.created"),
    INFO_RANK_FORMAT("messages.info.rank_format"),
    INFO_ALLIANCES("messages.info.alliances"),

    // Messages de liste
    LIST_HEADER("messages.list.header"),
    LIST_FORMAT("messages.list.format"),
    LIST_FOOTER("messages.list.footer"),
    LIST_INVALID_PAGE("messages.list.invalid_page"),

    // Messages de logs
    LOGS_HEADER("messages.logs.header"),
    LOGS_ENTRY("messages.logs.entry"),
    LOGS_FOOTER("messages.logs.footer"),

    // Messages d'alliance
    ALLIANCE_INVITE_SENT("messages.alliance.invite.sent"),
    ALLIANCE_INVITE_RECEIVED("messages.alliance.invite.received"),
    ALLIANCE_ACCEPTED("messages.alliance.accept"),
    ALLIANCE_REMOVED("messages.alliance.remove"),

    // Messages d'erreur
    ERROR_NO_PERMISSION("messages.errors.no_permission"),
    ERROR_NOT_GUILD_MEMBER("messages.errors.not_guild_member"),
    ERROR_PLAYER_NOT_FOUND("messages.errors.player_not_found"),
    ERROR_ALREADY_IN_GUILD("messages.errors.already_in_guild"),
    ERROR_NOT_INVITED("messages.errors.not_invited"),
    ERROR_INSUFFICIENT_RANK("messages.errors.insufficient_rank"),
    ERROR_SPECIFY_GUILD("messages.errors.specify_guild"),
    ERROR_PLAYER_NOT_IN_GUILD("messages.errors.player_not_in_guild"),
    ERROR_ECONOMY_DISABLED("messages.errors.economy_disabled"),
    ERROR_ALREADY_GUILD_OWNER("messages.errors.already_guild_owner"),
    ERROR_ALREADY_ALLIED("messages.errors.already_allied"),
    ERROR_RANK_ALREADY_EXISTS("messages.errors.rank_already_exists"),
    ERROR_RANK_NAME_LENGTH("messages.errors.rank.name_length"),
    ERROR_RANK_NOT_EMPTY("messages.errors.rank_not_empty"),
    ERROR_RANK_ALREADY_LAST("messages.guild.rank.error.already_last"),
    ERROR_RANK_ALREADY_FIRST("messages.guild.rank.error.already_first"),

    PLAYER_JOINED_GUILD("messages.guild.player.joined"),
    PLAYER_LEFT_GUILD("messages.guild.player.left"),
    RANK_RENAMED("messages.guild.rank.renamed"),
    RANK_DELETED("messages.guild.rank.deleted"),
    RANK_MOVED_DOWN("messages.guild.rank.moved_down"),
    ;

    private final String key;
    private static YamlDocument messages;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    Lang(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static void setMessages(YamlDocument messagesFile) {
        messages = messagesFile;
    }

    private String getRaw() {
        return messages.getString(key, "Missing translation: " + key);
    }

    public Component component() {
        return MINI_MESSAGE.deserialize(getRaw());
    }

    public Component component(String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be paired (key, value)");
        }

        String message = getRaw();
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i + 1];
            message = message.replace("%" + placeholders[i] + "%", placeholder);
        }

        return MINI_MESSAGE.deserialize(message);
    }

    public String plainText() {
        return PLAIN_SERIALIZER.serialize(MINI_MESSAGE.deserialize(getRaw()));
    }

    public String plainText(String... placeholders) {
        return PLAIN_SERIALIZER.serialize(component(placeholders));
    }

    public String toString() {
        return LEGACY_SERIALIZER.serialize(MINI_MESSAGE.deserialize(getRaw()));
    }

    public String toString(String... placeholders) {
        return LEGACY_SERIALIZER.serialize(component(placeholders));
    }
}