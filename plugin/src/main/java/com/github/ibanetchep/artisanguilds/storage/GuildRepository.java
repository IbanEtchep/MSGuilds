package com.github.ibanetchep.artisanguilds.storage;

import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.api.GuildHome;
import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.model.RankData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The guilds in {@code artisan_guilds_*}, with plain JDBC that H2, MySQL and MariaDB run the same
 * (an update, then an insert when nothing was updated). Every method blocks: {@code GuildStore}
 * calls them on its own thread.
 */
public final class GuildRepository {

    public record RankRow(UUID id, String label, int order, Set<GuildPermission> permissions) {}

    public record MemberRow(UUID playerId, UUID guildId, UUID rankId, ChatMode chatMode) {}

    public record GuildRow(UUID id, String name, UUID ownerId, double balance, long exp, int level,
                           @Nullable GuildHome home, Instant createdAt,
                           List<RankRow> ranks, List<MemberRow> members, Set<UUID> allies) {}

    private final DataSource dataSource;
    private final GuildTables t;
    private final Gson gson = new Gson();

    public GuildRepository(DataSource dataSource, GuildTables tables) {
        this.dataSource = dataSource;
        this.t = tables;
    }

    public List<GuildRow> loadAll() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            Map<UUID, List<RankRow>> ranks = new HashMap<>();
            try (PreparedStatement p = c.prepareStatement("SELECT id, guild_id, label, rank_order, permissions FROM " + t.ranks());
                 ResultSet r = p.executeQuery()) {
                while (r.next()) ranks.computeIfAbsent(uuid(r, "guild_id"), k -> new ArrayList<>()).add(rank(r));
            }
            Map<UUID, List<MemberRow>> members = new HashMap<>();
            try (PreparedStatement p = c.prepareStatement("SELECT player_uuid, guild_id, rank_id, chat_mode FROM " + t.members());
                 ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    MemberRow m = member(r);
                    members.computeIfAbsent(m.guildId(), k -> new ArrayList<>()).add(m);
                }
            }
            Map<UUID, Set<UUID>> allies = new HashMap<>();
            try (PreparedStatement p = c.prepareStatement("SELECT guild_a_id, guild_b_id FROM " + t.alliances());
                 ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    UUID a = uuid(r, "guild_a_id");
                    UUID b = uuid(r, "guild_b_id");
                    allies.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                    allies.computeIfAbsent(b, k -> new HashSet<>()).add(a);
                }
            }
            List<GuildRow> guilds = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement(GUILD_COLUMNS + t.guilds()); ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    UUID id = uuid(r, "id");
                    guilds.add(guild(r, ranks.getOrDefault(id, List.of()), members.getOrDefault(id, List.of()),
                            allies.getOrDefault(id, Set.of())));
                }
            }
            return guilds;
        }
    }

    /** Null when the guild no longer exists. */
    public @Nullable GuildRow load(UUID guildId) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            List<RankRow> ranks = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement("SELECT id, label, rank_order, permissions FROM " + t.ranks() + " WHERE guild_id = ?")) {
                p.setString(1, guildId.toString());
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) ranks.add(rank(r));
                }
            }
            List<MemberRow> members = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement("SELECT player_uuid, guild_id, rank_id, chat_mode FROM " + t.members() + " WHERE guild_id = ?")) {
                p.setString(1, guildId.toString());
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) members.add(member(r));
                }
            }
            Set<UUID> allies = new HashSet<>();
            try (PreparedStatement p = c.prepareStatement("SELECT guild_a_id, guild_b_id FROM " + t.alliances() + " WHERE guild_a_id = ? OR guild_b_id = ?")) {
                p.setString(1, guildId.toString());
                p.setString(2, guildId.toString());
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        UUID a = uuid(r, "guild_a_id");
                        allies.add(a.equals(guildId) ? uuid(r, "guild_b_id") : a);
                    }
                }
            }
            try (PreparedStatement p = c.prepareStatement(GUILD_COLUMNS + t.guilds() + " WHERE id = ?")) {
                p.setString(1, guildId.toString());
                try (ResultSet r = p.executeQuery()) {
                    return r.next() ? guild(r, ranks, members, allies) : null;
                }
            }
        }
    }

    /** Null when the player is in no guild. */
    public @Nullable MemberRow loadMember(UUID playerId) throws SQLException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT player_uuid, guild_id, rank_id, chat_mode FROM " + t.members() + " WHERE player_uuid = ?")) {
            p.setString(1, playerId.toString());
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? member(r) : null;
            }
        }
    }

    /** The guild, its ranks (those it no longer has are deleted) and its alliances, in one transaction. */
    public void saveGuild(GuildData guild) throws SQLException {
        String id = guild.getId().toString();
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                int updated;
                try (PreparedStatement p = c.prepareStatement("UPDATE " + t.guilds()
                        + " SET name = ?, balance = ?, exp = ?, level = ?, home = ?, owner_uuid = ? WHERE id = ?")) {
                    bindGuild(p, guild);
                    p.setString(7, id);
                    updated = p.executeUpdate();
                }
                if (updated == 0) {
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO " + t.guilds()
                            + " (name, balance, exp, level, home, owner_uuid, id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                        bindGuild(p, guild);
                        p.setString(7, id);
                        p.setTimestamp(8, Timestamp.from(guild.getCreatedAt()));
                        p.executeUpdate();
                    }
                }
                List<RankData> ranks = guild.getRanks();
                for (RankData rank : ranks) {
                    try (PreparedStatement p = c.prepareStatement("UPDATE " + t.ranks()
                            + " SET label = ?, rank_order = ?, permissions = ? WHERE id = ?")) {
                        bindRank(p, rank);
                        if (p.executeUpdate() > 0) continue;
                    }
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO " + t.ranks()
                            + " (label, rank_order, permissions, id, guild_id) VALUES (?, ?, ?, ?, ?)")) {
                        bindRank(p, rank);
                        p.setString(5, id);
                        p.executeUpdate();
                    }
                }
                String kept = ranks.stream().map(r -> "?").collect(Collectors.joining(","));
                try (PreparedStatement p = c.prepareStatement("DELETE FROM " + t.ranks() + " WHERE guild_id = ?"
                        + (ranks.isEmpty() ? "" : " AND id NOT IN (" + kept + ")"))) {
                    p.setString(1, id);
                    for (int i = 0; i < ranks.size(); i++) p.setString(i + 2, ranks.get(i).getId().toString());
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("DELETE FROM " + t.alliances() + " WHERE guild_a_id = ? OR guild_b_id = ?")) {
                    p.setString(1, id);
                    p.setString(2, id);
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO " + t.alliances() + " (guild_a_id, guild_b_id) VALUES (?, ?)")) {
                    for (UUID ally : guild.allianceIds()) {
                        p.setString(1, id);
                        p.setString(2, ally.toString());
                        p.addBatch();
                    }
                    p.executeBatch();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void saveMember(MemberData member) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            int updated;
            try (PreparedStatement p = c.prepareStatement("UPDATE " + t.members()
                    + " SET guild_id = ?, rank_id = ?, chat_mode = ? WHERE player_uuid = ?")) {
                bindMember(p, member);
                updated = p.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement p = c.prepareStatement("INSERT INTO " + t.members()
                        + " (guild_id, rank_id, chat_mode, player_uuid) VALUES (?, ?, ?, ?)")) {
                    bindMember(p, member);
                    p.executeUpdate();
                }
            }
        }
    }

    public void deleteMember(UUID playerId) throws SQLException {
        update("DELETE FROM " + t.members() + " WHERE player_uuid = ?", playerId.toString());
    }

    /** Its ranks, members, logs and alliances go with it (foreign keys). */
    public void deleteGuild(UUID guildId) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Members first: their rank's key does not cascade.
                for (String table : List.of(t.members(), t.logs(), t.ranks())) {
                    try (PreparedStatement p = c.prepareStatement("DELETE FROM " + table + " WHERE guild_id = ?")) {
                        p.setString(1, guildId.toString());
                        p.executeUpdate();
                    }
                }
                try (PreparedStatement p = c.prepareStatement("DELETE FROM " + t.alliances() + " WHERE guild_a_id = ? OR guild_b_id = ?")) {
                    p.setString(1, guildId.toString());
                    p.setString(2, guildId.toString());
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("DELETE FROM " + t.guilds() + " WHERE id = ?")) {
                    p.setString(1, guildId.toString());
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void addLog(UUID guildId, String log) throws SQLException {
        update("INSERT INTO " + t.logs() + " (guild_id, log) VALUES (?, ?)", guildId.toString(), log);
    }

    /** Newest first, as {@code dd/MM/yy HH:mm:ss : log}. */
    public List<String> logs(UUID guildId) throws SQLException {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        List<String> logs = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT log, created_at FROM " + t.logs()
                     + " WHERE guild_id = ? ORDER BY created_at DESC, id DESC")) {
            p.setString(1, guildId.toString());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Timestamp at = r.getTimestamp("created_at");
                    String log = r.getString("log");
                    logs.add((at != null ? format.format(at) : "??/??/?? ??:??:??") + " : " + (log != null ? log : ""));
                }
            }
        }
        return logs;
    }

    private static final String GUILD_COLUMNS = "SELECT id, name, owner_uuid, balance, exp, level, home, created_at FROM ";

    private GuildRow guild(ResultSet r, List<RankRow> ranks, List<MemberRow> members, Set<UUID> allies) throws SQLException {
        Timestamp createdAt = r.getTimestamp("created_at");
        String owner = r.getString("owner_uuid");
        return new GuildRow(uuid(r, "id"), r.getString("name"), owner != null ? UUID.fromString(owner) : null,
                r.getDouble("balance"), r.getLong("exp"), r.getInt("level"), home(r.getString("home")),
                createdAt != null ? createdAt.toInstant() : Instant.now(), ranks, members, allies);
    }

    private RankRow rank(ResultSet r) throws SQLException {
        return new RankRow(uuid(r, "id"), r.getString("label"), r.getInt("rank_order"), permissions(r.getString("permissions")));
    }

    private static MemberRow member(ResultSet r) throws SQLException {
        String rank = r.getString("rank_id");
        String mode = r.getString("chat_mode");
        ChatMode chatMode;
        try {
            chatMode = mode != null ? ChatMode.valueOf(mode) : ChatMode.PUBLIC;
        } catch (IllegalArgumentException e) {
            chatMode = ChatMode.PUBLIC;
        }
        return new MemberRow(uuid(r, "player_uuid"), uuid(r, "guild_id"), rank != null ? UUID.fromString(rank) : null, chatMode);
    }

    private void bindGuild(PreparedStatement p, GuildData guild) throws SQLException {
        p.setString(1, guild.getName());
        p.setDouble(2, guild.getBalance());
        p.setLong(3, guild.getExp());
        p.setInt(4, guild.getLevel());
        if (guild.getHome() == null) p.setNull(5, Types.VARCHAR);
        else p.setString(5, homeJson(guild.getHome()));
        p.setString(6, guild.getOwnerId() != null ? guild.getOwnerId().toString() : null);
    }

    private void bindRank(PreparedStatement p, RankData rank) throws SQLException {
        p.setString(1, rank.getName());
        p.setInt(2, rank.getOrder());
        JsonArray permissions = new JsonArray();
        rank.getPermissions().stream().map(Enum::name).sorted().forEach(permissions::add);
        p.setString(3, gson.toJson(permissions));
        p.setString(4, rank.getId().toString());
    }

    private static void bindMember(PreparedStatement p, MemberData member) throws SQLException {
        p.setString(1, member.getGuild().getId().toString());
        p.setString(2, member.getRank().getId().toString());
        p.setString(3, member.getChatMode().name());
        p.setString(4, member.getPlayerId().toString());
    }

    /** MSGuilds' JSON arrays; names this version does not know are dropped. */
    static Set<GuildPermission> permissions(@Nullable String json) {
        Set<GuildPermission> permissions = EnumSet.noneOf(GuildPermission.class);
        if (json == null || json.isBlank()) return permissions;
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonArray()) return permissions;
        for (JsonElement e : parsed.getAsJsonArray()) {
            try {
                permissions.add(GuildPermission.valueOf(e.getAsString()));
            } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
            }
        }
        return permissions;
    }

    /** MSCore's {@code SLocation} JSON, kept as is: {@code {x, y, z, pitch, yaw, world, server}}. */
    static @Nullable GuildHome home(@Nullable String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            return new GuildHome(o.has("server") ? o.get("server").getAsString() : "", o.get("world").getAsString(),
                    o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble(),
                    o.has("yaw") ? o.get("yaw").getAsFloat() : 0f, o.has("pitch") ? o.get("pitch").getAsFloat() : 0f);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static String homeJson(GuildHome home) {
        JsonObject o = new JsonObject();
        o.addProperty("x", home.x());
        o.addProperty("y", home.y());
        o.addProperty("z", home.z());
        o.addProperty("pitch", home.pitch());
        o.addProperty("yaw", home.yaw());
        o.addProperty("world", home.world());
        o.addProperty("server", home.server());
        return o.toString();
    }

    private void update(String sql, String... values) throws SQLException {
        try (Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) p.setString(i + 1, values[i]);
            p.executeUpdate();
        }
    }

    private static UUID uuid(ResultSet r, String column) throws SQLException {
        return UUID.fromString(r.getString(column));
    }
}
