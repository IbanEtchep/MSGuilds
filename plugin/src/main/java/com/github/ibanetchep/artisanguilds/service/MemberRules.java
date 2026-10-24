package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.model.RankData;
import org.jetbrains.annotations.Nullable;

/**
 * Who may promote, demote or kick whom, and manage which rank. The only copy of these rules:
 * {@link GuildService} sends the refusal, the menu rows turn it into {@code can_promote},
 * {@code can_demote}, {@code can_kick} and {@code can_manage}, so a menu never offers what the
 * command refuses.
 *
 * <p>Each method returns null when allowed, else the message saying why not.
 */
public final class MemberRules {

    private MemberRules() {}

    public static @Nullable Msg promoteRefusal(MemberData actor, MemberData target) {
        if (!actor.getGuild().equals(target.getGuild())) return Msg.ERROR_PLAYER_NOT_IN_GUILD;
        if (!above(actor, target)) return Msg.ERROR_TARGET_RANK_TOO_HIGH;
        if (!actor.isGranted(GuildPermission.PROMOTE_MEMBER)) return Msg.ERROR_INSUFFICIENT_RANK;
        if (actor.getGuild().rankAbove(target.getRank()) == null) return Msg.ERROR_ALREADY_HIGHEST_RANK;
        return null;
    }

    public static @Nullable Msg demoteRefusal(MemberData actor, MemberData target) {
        if (!actor.getGuild().equals(target.getGuild())) return Msg.ERROR_PLAYER_NOT_IN_GUILD;
        // The founder changes through /guild transfer, never by losing rank.
        if (target.isOwner()) return Msg.ERROR_CANNOT_DEMOTE_OWNER;
        if (!above(actor, target)) return Msg.ERROR_RANK_TOO_HIGH;
        if (!actor.isGranted(GuildPermission.DEMOTE_MEMBER)) return Msg.ERROR_INSUFFICIENT_RANK;
        if (actor.getGuild().rankBelow(target.getRank()) == null) return Msg.ERROR_ALREADY_LOWEST_RANK;
        return null;
    }

    public static @Nullable Msg kickRefusal(MemberData actor, MemberData target) {
        if (actor.getPlayerId().equals(target.getPlayerId())) return Msg.ERROR_KICK_SELF;
        if (!actor.isGranted(GuildPermission.KICK_MEMBER)) return Msg.ERROR_INSUFFICIENT_RANK;
        if (!actor.getGuild().equals(target.getGuild())) return Msg.ERROR_PLAYER_NOT_IN_GUILD;
        if (target.isOwner() || !above(actor, target)) return Msg.ERROR_KICK_RANK;
        return null;
    }

    /**
     * A rank's permissions change from above only: by the owner, or a member with
     * {@link GuildPermission#MANAGE_RANKS} whose rank is strictly higher. Nobody grants
     * permissions to their own rank.
     */
    public static @Nullable Msg rankRefusal(MemberData actor, RankData rank) {
        if (!actor.isGranted(GuildPermission.MANAGE_RANKS)) return Msg.ERROR_INSUFFICIENT_RANK;
        if (!actor.isOwner() && actor.getRank().getOrder() <= rank.getOrder()) return Msg.ERROR_RANK_TOO_HIGH;
        return null;
    }

    /** A strictly higher rank, or the owner. */
    private static boolean above(MemberData actor, MemberData target) {
        return actor.isOwner() || actor.getRank().getOrder() > target.getRank().getOrder();
    }
}
