package com.github.ibanetchep.artisanguilds.menu;

import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.model.RankData;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The rows of the guild menus. Each fetcher runs on the viewer's thread at every render, so it
 * reads memory only. A null viewer is the editor's preview: it gets sample rows.
 *
 * <p>The ranks and their permissions are the viewer's own guild's, and only for a member with
 * {@link GuildPermission#MANAGE_RANKS}: Artisan does not check a menu's open requirements, so
 * the menu stays empty for anyone else. The actions check again ({@link GuildMenuActions}).
 */
public final class GuildMenuData {

    /** Placeholder of a rank permission's name: {@code messages.permissions.manage_ranks}. */
    static final String PERMISSION_KEY = "messages.permissions.";

    private final GuildRegistry registry;
    private final GuildMessages messages;

    public GuildMenuData(GuildRegistry registry, GuildMessages messages) {
        this.registry = registry;
        this.messages = messages;
    }

    /** One row: the viewer's guild and what they may do in it. */
    public List<Map<String, Object>> viewer(@Nullable Player viewer) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (viewer == null) {
            row.put("in_guild", true);
            row.put("guild_name", "Guild");
            row.put("rank", "Member");
            row.put("is_owner", true);
            row.put("can_manage_ranks", true);
            return List.of(row);
        }
        GuildData guild = registry.byPlayer(viewer.getUniqueId());
        MemberData member = guild != null ? guild.getMember(viewer.getUniqueId()) : null;
        row.put("in_guild", member != null);
        row.put("guild_name", member != null ? guild.getName() : "");
        row.put("rank", member != null ? member.getRank().getName() : "");
        row.put("is_owner", member != null && member.isOwner());
        row.put("can_manage_ranks", member != null && member.isGranted(GuildPermission.MANAGE_RANKS));
        return List.of(row);
    }

    /** The ranks of the viewer's guild, highest first. */
    public List<Map<String, Object>> ranks(@Nullable Player viewer) {
        if (viewer == null) {
            return List.of(rankRow("00000000-0000-0000-0000-000000000002", "<red>Officer", 1, 2, 1, 4),
                    rankRow("00000000-0000-0000-0000-000000000001", "<gray>Member", 2, 2, 5, 0));
        }
        GuildData guild = managed(viewer);
        if (guild == null) return List.of();
        List<RankData> ranks = new ArrayList<>(guild.getRanks());
        ranks.sort(Comparator.comparingInt(RankData::getOrder).reversed());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < ranks.size(); i++) {
            RankData rank = ranks.get(i);
            rows.add(rankRow(rank.getId().toString(), rank.getName(), i + 1, ranks.size(),
                    guild.membersWithRank(rank).size(), rank.getPermissions().size()));
        }
        return rows;
    }

    private static Map<String, Object> rankRow(String id, String name, int position, int count, int members, int permissions) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("position", position);
        row.put("members", members);
        row.put("permissions", permissions);
        row.put("top", position == 1);
        row.put("bottom", position == count);
        return row;
    }

    /** The permissions of the rank {@code params.rank} (its id) of the viewer's guild, granted or not. */
    public List<Map<String, Object>> rankPermissions(@Nullable Player viewer, Map<String, Object> params) {
        RankData rank = null;
        if (viewer != null) {
            GuildData guild = managed(viewer);
            UUID id = uuid(params.get("rank"));
            rank = guild != null && id != null ? guild.getRank(id) : null;
            if (rank == null) return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (GuildPermission permission : GuildPermission.values()) {
            String key = permission.name().toLowerCase(Locale.ROOT);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", key);
            row.put("name", messages.text(viewer, PERMISSION_KEY + key, permission.getName()));
            row.put("granted", rank != null && rank.hasPermission(permission));
            rows.add(row);
        }
        return rows;
    }

    /** The members of the viewer's guild, highest rank first, then by name. */
    public List<Map<String, Object>> members(@Nullable Player viewer) {
        if (viewer == null) return List.of(memberRow(UUID.randomUUID().toString(), "Steve", "<red>Officer", true, true));
        GuildData guild = registry.byPlayer(viewer.getUniqueId());
        if (guild == null) return List.of();
        return guild.members().stream()
                .sorted(Comparator.comparingInt((MemberData m) -> m.getRank().getOrder()).reversed()
                        .thenComparing(MemberData::getName, String.CASE_INSENSITIVE_ORDER))
                .map(m -> memberRow(m.getPlayerId().toString(), m.getName(), m.getRank().getName(), m.isOwner(),
                        GuildData.online.test(m.getPlayerId())))
                .toList();
    }

    private static Map<String, Object> memberRow(String id, String name, String rank, boolean owner, boolean online) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("rank", rank);
        row.put("owner", owner);
        row.put("online", online);
        return row;
    }

    private @Nullable GuildData managed(Player viewer) {
        GuildData guild = registry.byPlayer(viewer.getUniqueId());
        MemberData member = guild != null ? guild.getMember(viewer.getUniqueId()) : null;
        return member != null && member.isGranted(GuildPermission.MANAGE_RANKS) ? guild : null;
    }

    /** {@code kick_member}, ...: what {@code guilds:rank_permission} takes. */
    static List<String> permissionKeys() {
        return Arrays.stream(GuildPermission.values()).map(p -> p.name().toLowerCase(Locale.ROOT)).toList();
    }

    static @Nullable UUID uuid(@Nullable Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
