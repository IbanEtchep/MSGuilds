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
    GUILD_NAME_INVALID_LENGTH("messages.guild.name.invalid_length"),
    GUILD_RANK_CHANGE("messages.guild.rank.change"),

    // Messages de chat
    CHAT_MODE_CHANGED("messages.chat.mode_changed"),

    // Messages de membres
    MEMBER_INVITED("messages.members.invite.sent"),
    MEMBER_INVITE_RECEIVED("messages.members.invite.received"),
    MEMBER_INVITE_REVOKED("messages.members.invite.revoked"),
    MEMBER_JOINED("messages.members.join"),
    MEMBER_LEFT("messages.members.leave"),

    // Messages de banque
    BANK_BALANCE("messages.bank.balance"),
    BANK_DEPOSIT_SUCCESS("messages.bank.deposit.success"),
    BANK_WITHDRAW_SUCCESS("messages.bank.withdraw.success"),
    BANK_INSUFFICIENT_FUNDS("messages.bank.insufficient_funds"),
    BANK_ECONOMY_DISABLED("messages.bank.economy_disabled"),
    PLAYER_INSUFFICIENT_FUNDS("messages.bank.player_insufficient_funds"),

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
    LOG_GUILD_CREATED("messages.logs.guild_created"),
    LOG_GUILD_DISBANDED("messages.logs.guild_disbanded"),
    LOG_BANK_DEPOSIT("messages.logs.bank_deposit"),
    LOG_BANK_WITHDRAW("messages.logs.bank_withdraw"),
    LOG_RANK_CHANGE("messages.logs.rank.change"),
    LOG_HOME_SET("messages.logs.home.set"),
    LOG_HOME_DELETE("messages.logs.home.delete"),

    ALLIANCE_ACCEPTED("messages.alliance.accept"),
    ALLIANCE_REMOVED("messages.alliance.remove"),
    ALLIANCE_REQUEST_RECEIVED("messages.alliance.request_received"),
    ALLIANCE_ENDED("messages.alliance.ended"),
    ALLIANCE_REQUEST_SENT("messages.alliance.request.sent"),
    ALLIANCE_FORMED("messages.alliance.formed"),

    // Messages de transfert
    ERROR_TRANSFER_OWNER("messages.guild.transfer.error.owner"),
    ERROR_TRANSFER_SELF("messages.guild.transfer.error.self"),
    TRANSFER_SUCCESS("messages.guild.transfer.success"),

    // Messages de kick
    ERROR_KICK_SELF("messages.guild.kick.error.self"),
    ERROR_KICK_RANK("messages.guild.kick.error.rank"),
    KICK_SUCCESS("messages.guild.kick.success"),
    KICK_TARGET("messages.guild.kick.target"),

    // Messages de leave
    ERROR_LEAVE_OWNER("messages.guild.leave.error.owner"),
    LEAVE_SUCCESS("messages.guild.leave.success"),
    JOIN_SUCCESS("messages.guild.join.success"),

    // Messages de promotion/rétrogradation
    PROMOTE_SUCCESS("messages.guild.promote.success"),
    DEMOTE_SUCCESS("messages.guild.demote.success"),

    // Messages de rang
    RANK_RENAMED("messages.guild.rank.renamed"),
    RANK_DELETED("messages.guild.rank.deleted"),
    RANK_MOVED_UP("messages.guild.rank.moved_up"),
    RANK_MOVED_DOWN("messages.guild.rank.moved_down"),

    // Messages d'erreur (à garder)
    ERROR_NO_PERMISSION("messages.errors.no_permission"),
    ERROR_NOT_GUILD_MEMBER("messages.errors.not_guild_member"),
    ERROR_PLAYER_NOT_FOUND("messages.errors.player_not_found"),
    ERROR_ALREADY_IN_GUILD("messages.errors.already_in_guild"),
    ERROR_SELF_ALREADY_IN_GUILD("messages.errors.self_already_in_guild"),
    ERROR_NOT_INVITED("messages.errors.not_invited"),
    ERROR_INSUFFICIENT_RANK("messages.errors.insufficient_rank"),
    ERROR_SPECIFY_GUILD("messages.errors.specify_guild"),
    ERROR_PLAYER_NOT_IN_GUILD("messages.errors.player_not_in_guild"),
    ERROR_ECONOMY_DISABLED("messages.errors.economy_disabled"),
    ERROR_ALREADY_GUILD_OWNER("messages.errors.already_guild_owner"),
    ERROR_ALREADY_ALLIED("messages.errors.already_allied"),
    ERROR_RANK_ALREADY_EXISTS("messages.errors.rank_already_exists"),
    ERROR_RANK_NAME_LENGTH("messages.errors.rank_name_length"),
    ERROR_RANK_NOT_EMPTY("messages.errors.rank_not_empty"),
    ERROR_RANK_ALREADY_LAST("messages.guild.rank.error.already_last"),
    ERROR_RANK_ALREADY_FIRST("messages.guild.rank.error.already_first"),
    ERROR_RANK_TOO_HIGH("messages.errors.rank_too_high"),
    ERROR_TARGET_RANK_TOO_HIGH("messages.errors.target_rank_too_high"),
    ERROR_NEED_POSITIVE_AMOUNT("messages.errors.need_positive_amount"),
    ERROR_CANNOT_SELF_ALLY("messages.errors.cannot_self_ally"),
    ERROR_ALLIANCE_INVITE_ALREADY_SENT("messages.errors.alliance_invite_already_sent"),
    ERROR_NOT_GUILD_OWNER("messages.errors.not_guild_owner"),
    ERROR_NOT_ALLIED("messages.errors.not_allied"),
    ERROR_PLAYER_NOT_INVITED("messages.errors.player_not_invited"),
    ERROR_ALREADY_LOWEST_RANK("messages.errors.already_lowest_rank"),
    ERROR_ALREADY_HIGHEST_RANK("messages.errors.already_highest_rank");


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