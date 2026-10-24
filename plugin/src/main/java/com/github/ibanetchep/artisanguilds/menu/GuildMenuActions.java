package com.github.ibanetchep.artisanguilds.menu;

import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.RankData;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import com.github.ibanetchep.artisanguilds.service.Msg;
import com.github.ibanetchep.artisanguilds.service.RankService;
import net.artisanmc.modules.api.action.ActionContext;
import net.artisanmc.modules.api.action.ActionRuntime;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The actions of the ranks menus. A rank is named by its id and looked up in the clicker's own
 * guild only, so an id from another guild finds nothing; {@link RankService} then checks
 * {@link GuildPermission#MANAGE_RANKS}, whatever the menu showed.
 */
public final class GuildMenuActions {

    private final GuildRegistry registry;
    private final RankService ranks;
    private final GuildMessages messages;

    public GuildMenuActions(GuildRegistry registry, RankService ranks, GuildMessages messages) {
        this.registry = registry;
        this.ranks = ranks;
        this.messages = messages;
    }

    /** {@code rank}, {@code direction}: {@code up} or {@code down}. */
    public ActionContext move(ActionContext ctx, Map<String, Object> args, ActionRuntime runtime) {
        RankData rank = rank(ctx, args, runtime);
        if (rank == null) return ctx;
        String direction = runtime.text(ctx, args.get("direction")).trim().toLowerCase(Locale.ROOT);
        switch (direction) {
            case "up" -> ranks.moveUp(ctx.getPlayer(), rank);
            case "down" -> ranks.moveDown(ctx.getPlayer(), rank);
            default -> runtime.warn(GuildsModule.RANK_MOVE + ": direction '" + direction + "' is neither up nor down");
        }
        return ctx;
    }

    /** {@code rank}: only a rank no member holds. */
    public ActionContext delete(ActionContext ctx, Map<String, Object> args, ActionRuntime runtime) {
        RankData rank = rank(ctx, args, runtime);
        if (rank != null) ranks.delete(ctx.getPlayer(), rank);
        return ctx;
    }

    /** {@code rank}, {@code name}: MiniMessage, as typed in the dialog. */
    public ActionContext rename(ActionContext ctx, Map<String, Object> args, ActionRuntime runtime) {
        RankData rank = rank(ctx, args, runtime);
        if (rank != null) ranks.rename(ctx.getPlayer(), rank, name(ctx, args, runtime));
        return ctx;
    }

    /** {@code name}: the new rank goes above the highest. */
    public ActionContext create(ActionContext ctx, Map<String, Object> args, ActionRuntime runtime) {
        ranks.create(ctx.getPlayer(), name(ctx, args, runtime));
        return ctx;
    }

    /** {@code rank}, {@code permission} (its key, {@code kick_member}), {@code granted}: true, false, or toggle when absent. */
    public ActionContext permission(ActionContext ctx, Map<String, Object> args, ActionRuntime runtime) {
        RankData rank = rank(ctx, args, runtime);
        if (rank == null) return ctx;
        String key = runtime.text(ctx, args.get("permission")).trim();
        GuildPermission permission;
        try {
            permission = GuildPermission.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            runtime.warn(GuildsModule.RANK_PERMISSION + ": unknown permission '" + key + "'");
            return ctx;
        }
        String granted = args.get("granted") == null ? "" : runtime.text(ctx, args.get("granted")).trim();
        boolean value = granted.isEmpty() || granted.equalsIgnoreCase("toggle")
                ? !rank.hasPermission(permission) : Boolean.parseBoolean(granted);
        ranks.setPermission(ctx.getPlayer(), rank, permission, value);
        return ctx;
    }

    /** The rank {@code args.rank} of the player's own guild; null, with a message, when there is none. */
    private @Nullable RankData rank(ActionContext ctx, Map<String, Object> args, ActionRuntime runtime) {
        Player player = ctx.getPlayer();
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return null;
        }
        UUID id = GuildMenuData.uuid(runtime.text(ctx, args.get("rank")));
        RankData rank = id != null ? guild.getRank(id) : null;
        if (rank == null) messages.send(player, Msg.ERROR_RANK_NOT_FOUND);
        return rank;
    }

    /** Checked by {@link RankService}: an empty name gets its length message. */
    private static String name(ActionContext ctx, Map<String, Object> args, ActionRuntime runtime) {
        return runtime.text(ctx, args.get("name")).trim();
    }
}
