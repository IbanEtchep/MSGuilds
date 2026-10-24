package com.github.ibanetchep.artisanguilds.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copies the guilds of MSGuilds 1.1 ({@code guilds}, {@code guilds_ranks}, {@code guilds_members},
 * {@code guilds_logs}, {@code guilds_alliances}, and {@code guilds_chests} where it exists; usually
 * in the database of the Core plugin) into ArtisanGuilds' tables, once.
 *
 * <p>Runs only when ArtisanGuilds' tables are empty and no import is recorded. The copy is one
 * transaction on the target: on any error nothing is kept and the next start tries again. Each
 * table's row count is checked before the commit. The source is only read: putting MSGuilds back
 * finds its data as it was.
 */
public final class LegacyImport {

    public enum Outcome { IMPORTED, ALREADY_IMPORTED, NO_SOURCE, TARGET_NOT_EMPTY }

    public record Result(Outcome outcome, Map<String, Integer> counts) {
        static Result of(Outcome outcome) {
            return new Result(outcome, Map.of());
        }
    }

    static final String GUILDS = "guilds";
    static final String RANKS = "guilds_ranks";
    static final String MEMBERS = "guilds_members";
    static final String LOGS = "guilds_logs";
    static final String ALLIANCES = "guilds_alliances";
    /** The guild chest of MSGuilds' guild-levels build, when a server ran it. */
    static final String CHESTS = "guilds_chests";

    private final GuildTables t;

    public LegacyImport(GuildTables tables) {
        this.t = tables;
    }

    /**
     * @param source      where MSGuilds' tables are; may be the target's own data source
     * @param sourceLabel what the log and the import record say about it, e.g. {@code Core database}
     * @throws LegacySchemaException when the source holds MSGuilds' tables from before 1.1
     */
    public Result run(DataSource source, String sourceLabel, DataSource target) throws SQLException {
        try (Connection to = target.getConnection()) {
            if (count(to, t.imports()) > 0) return Result.of(Outcome.ALREADY_IMPORTED);
            try (Connection from = source.getConnection()) {
                if (!GuildSchema.tableExists(from, GUILDS)) return Result.of(Outcome.NO_SOURCE);
                if (!GuildSchema.columnExists(from, GUILDS, "owner_uuid")) {
                    throw new LegacySchemaException("The guilds tables of " + sourceLabel + " come from MSGuilds before 1.1 "
                            + "(integer ids). Start MSGuilds 1.1.1 once to convert them, then ArtisanGuilds.");
                }
                if (count(to, t.guilds()) > 0) return Result.of(Outcome.TARGET_NOT_EMPTY);

                boolean autoCommit = to.getAutoCommit();
                to.setAutoCommit(false);
                try {
                    Map<String, Integer> counts = copy(from, to);
                    try (PreparedStatement p = to.prepareStatement(
                            "INSERT INTO " + t.imports() + " (source, imported_at, counts) VALUES (?, ?, ?)")) {
                        p.setString(1, truncate(sourceLabel, 255));
                        p.setTimestamp(2, Timestamp.from(Instant.now()));
                        p.setString(3, truncate(counts.entrySet().stream()
                                .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")), 255));
                        p.executeUpdate();
                    }
                    to.commit();
                    return new Result(Outcome.IMPORTED, counts);
                } catch (SQLException | RuntimeException e) {
                    to.rollback();
                    throw e;
                } finally {
                    to.setAutoCommit(autoCommit);
                }
            }
        }
    }

    /** Parents first, for the foreign keys. */
    private Map<String, Integer> copy(Connection from, Connection to) throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        boolean hasLevel = GuildSchema.columnExists(from, GUILDS, "level");

        counts.put("guilds", copyRows(from, to,
                "SELECT id, name, balance, exp, " + (hasLevel ? "level" : "1 AS level") + ", home, owner_uuid, createdAt FROM " + GUILDS,
                "INSERT INTO " + t.guilds() + " (id, name, balance, exp, level, home, owner_uuid, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                (r, p) -> {
                    p.setString(1, r.getString("id"));
                    p.setString(2, r.getString("name"));
                    p.setDouble(3, r.getDouble("balance"));
                    p.setLong(4, r.getLong("exp"));
                    int level = r.getInt("level");
                    // Levels start at 0 since the guild-levels build; 1.1 had 1 at least.
                    p.setInt(5, r.wasNull() || level < 0 ? 1 : level);
                    p.setString(6, r.getString("home"));
                    p.setString(7, r.getString("owner_uuid"));
                    p.setTimestamp(8, createdAt(r));
                }));
        counts.put("ranks", copyRows(from, to,
                "SELECT id, guild_id, label, rank_order, permissions FROM " + RANKS,
                "INSERT INTO " + t.ranks() + " (id, guild_id, label, rank_order, permissions) VALUES (?, ?, ?, ?, ?)",
                (r, p) -> {
                    p.setString(1, r.getString("id"));
                    p.setString(2, r.getString("guild_id"));
                    p.setString(3, r.getString("label"));
                    p.setInt(4, r.getInt("rank_order"));
                    p.setString(5, r.getString("permissions"));
                }));
        counts.put("members", copyRows(from, to,
                "SELECT player_uuid, guild_id, rank_id, chat_mode FROM " + MEMBERS,
                "INSERT INTO " + t.members() + " (player_uuid, guild_id, rank_id, chat_mode) VALUES (?, ?, ?, ?)",
                (r, p) -> {
                    p.setString(1, r.getString("player_uuid"));
                    p.setString(2, r.getString("guild_id"));
                    p.setString(3, r.getString("rank_id"));
                    p.setString(4, r.getString("chat_mode"));
                }));
        counts.put("logs", copyRows(from, to,
                "SELECT guild_id, log, createdAt FROM " + LOGS + " ORDER BY id",
                "INSERT INTO " + t.logs() + " (guild_id, log, created_at) VALUES (?, ?, ?)",
                (r, p) -> {
                    p.setString(1, r.getString("guild_id"));
                    p.setString(2, r.getString("log"));
                    p.setTimestamp(3, createdAt(r));
                }));
        counts.put("alliances", copyRows(from, to,
                "SELECT guild_a_id, guild_b_id FROM " + ALLIANCES,
                "INSERT INTO " + t.alliances() + " (guild_a_id, guild_b_id) VALUES (?, ?)",
                (r, p) -> {
                    p.setString(1, r.getString("guild_a_id"));
                    p.setString(2, r.getString("guild_b_id"));
                }));

        counts.put("chests", copyRows(from, to,
                "SELECT guild_id, page, contents FROM " + CHESTS,
                "INSERT INTO " + t.chests() + " (guild_id, page, contents) VALUES (?, ?, ?)",
                (r, p) -> {
                    p.setString(1, r.getString("guild_id"));
                    p.setInt(2, r.getInt("page"));
                    p.setBytes(3, r.getBytes("contents"));
                }));

        // The copy must be whole: a row lost on the way rolls everything back.
        check(from, to, GUILDS, t.guilds());
        check(from, to, RANKS, t.ranks());
        check(from, to, MEMBERS, t.members());
        check(from, to, LOGS, t.logs());
        check(from, to, ALLIANCES, t.alliances());
        check(from, to, CHESTS, t.chests());
        return counts;
    }

    @FunctionalInterface
    private interface RowCopier {
        void bind(ResultSet row, PreparedStatement insert) throws SQLException;
    }

    /** A source table MSGuilds never created on this server counts as empty. */
    private static int copyRows(Connection from, Connection to, String select, String insert, RowCopier copier) throws SQLException {
        String table = select.substring(select.indexOf(" FROM ") + 6).split(" ")[0];
        if (!GuildSchema.tableExists(from, table)) return 0;
        int rows = 0;
        try (Statement s = from.createStatement();
             ResultSet r = s.executeQuery(select);
             PreparedStatement p = to.prepareStatement(insert)) {
            while (r.next()) {
                copier.bind(r, p);
                p.addBatch();
                if (++rows % 500 == 0) p.executeBatch();
            }
            p.executeBatch();
        }
        return rows;
    }

    private static void check(Connection from, Connection to, String source, String target) throws SQLException {
        int expected = GuildSchema.tableExists(from, source) ? count(from, source) : 0;
        int copied = count(to, target);
        if (expected != copied) {
            throw new SQLException("Import of " + source + " incomplete: " + copied + " rows copied out of " + expected
                    + ". Nothing was kept; the next start tries again.");
        }
    }

    private static Timestamp createdAt(ResultSet r) throws SQLException {
        Timestamp at = r.getTimestamp("createdAt");
        return at != null ? at : Timestamp.from(Instant.now());
    }

    private static int count(Connection c, String table) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return r.next() ? r.getInt(1) : 0;
        }
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** The source cannot be imported as it is; the message says what to do. */
    public static final class LegacySchemaException extends SQLException {
        public LegacySchemaException(String message) {
            super(message);
        }
    }
}
