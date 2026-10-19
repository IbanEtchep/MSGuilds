package com.github.ibanetchep.artisanguilds.storage;

import net.artisanmc.modules.api.database.DatabaseDialect;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Creates the guild tables, or takes over the ones MSGuilds left, and upgrades them.
 *
 * <p>Three starting points:
 * <ul>
 *   <li>no {@code guilds} table: the tables are created;</li>
 *   <li>the tables of MSGuilds 1.1 (uuid ids, {@code owner_uuid}) without
 *       {@value #VERSION_TABLE}: they are <b>adopted</b> as they are. A column MSGuilds added
 *       later and a server may lack ({@code level}) is added; nothing is renamed, moved or
 *       deleted, so putting the MSGuilds jar back still works;</li>
 *   <li>the tables of MSGuilds before 1.1 (integer ids): refused, MSGuilds 1.1.1 converts them.</li>
 * </ul>
 *
 * <p>Plain JDBC, run once from {@code onEnable}, blocking.
 */
public final class GuildSchema {

    public static final String VERSION_TABLE = "guilds_schema_version";

    /** The version the adopted MSGuilds 1.1 tables are given: the same tables as step 1. */
    static final int ADOPTED_VERSION = 1;

    public enum Outcome { CREATED, ADOPTED, UPGRADED, UP_TO_DATE }

    public record Result(Outcome outcome, int fromVersion, int toVersion, List<String> addedColumns) {
    }

    /** Columns of MSGuilds 1.1 that ArtisanGuilds reads, by table. */
    private static final Map<String, List<String>> REQUIRED_COLUMNS = Map.of(
            "guilds", List.of("id", "name", "balance", "exp", "level", "home", "owner_uuid", "createdAt"),
            "guilds_ranks", List.of("id", "guild_id", "label", "rank_order", "permissions"),
            "guilds_members", List.of("guild_id", "player_uuid", "rank_id", "chat_mode"),
            "guilds_logs", List.of("id", "guild_id", "log", "createdAt"),
            "guilds_alliances", List.of("guild_a_id", "guild_b_id"));

    /** Columns MSGuilds added after its 1.1 conversion without an ALTER: added when missing. */
    private static final List<String[]> ADDABLE_COLUMNS = List.<String[]>of(
            new String[]{"guilds", "level", "INT DEFAULT 1 NOT NULL"});

    private final DataSource dataSource;
    private final DatabaseDialect dialect;

    public GuildSchema(DataSource dataSource, DatabaseDialect dialect) {
        this.dataSource = dataSource;
        this.dialect = dialect;
    }

    /** Forward only: append, never edit a released step. */
    private List<List<String>> migrations() {
        return List.of(createTables(false, "CHAR(36)"));
    }

    public Result migrate() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            Outcome outcome;
            int from;
            List<String> added = new ArrayList<>();
            if (tableExists(c, VERSION_TABLE)) {
                from = currentVersion(c);
                outcome = Outcome.UPGRADED;
            } else if (!tableExists(c, "guilds")) {
                from = 0;
                outcome = Outcome.CREATED;
                createVersionTable(c);
            } else {
                adopt(c, added);
                from = ADOPTED_VERSION;
                outcome = Outcome.ADOPTED;
            }

            List<List<String>> steps = migrations();
            for (int i = from; i < steps.size(); i++) {
                execute(c, steps.get(i));
                recordVersion(c, i + 1);
            }
            int to = Math.max(from, steps.size());
            if (outcome == Outcome.UPGRADED && to == from) outcome = Outcome.UP_TO_DATE;
            return new Result(outcome, from, to, List.copyOf(added));
        }
    }

    /** Takes over MSGuilds 1.1's tables: checks them, adds what is addable, records version 1. */
    private void adopt(Connection c, List<String> added) throws SQLException {
        if (!columnExists(c, "guilds", "owner_uuid")) {
            throw new LegacySchemaException("The guilds tables come from MSGuilds before 1.1 (integer ids). "
                    + "Start MSGuilds 1.1.1 once to convert them, then replace it with ArtisanGuilds.");
        }
        // A table missing on this server is created the way step 1 would, with ids of the type
        // guilds.id has (a MariaDB foreign key refuses CHAR(36) towards UUID).
        execute(c, createTables(true, columnType(c, "guilds", "id")));
        for (String[] column : ADDABLE_COLUMNS) {
            if (!columnExists(c, column[0], column[1])) {
                execute(c, List.of("ALTER TABLE " + column[0] + " ADD COLUMN " + column[1] + " " + column[2]));
                added.add(column[0] + "." + column[1]);
            }
        }
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, List<String>> table : REQUIRED_COLUMNS.entrySet()) {
            for (String column : table.getValue()) {
                if (!columnExists(c, table.getKey(), column)) missing.add(table.getKey() + "." + column);
            }
        }
        if (!missing.isEmpty()) {
            throw new LegacySchemaException("The guilds tables are not those of MSGuilds 1.1, missing columns: "
                    + String.join(", ", missing) + ".");
        }
        createVersionTable(c);
        recordVersion(c, ADOPTED_VERSION);
    }

    /**
     * The tables of step 1, written so that H2, MySQL and MariaDB run them the same. Ids are
     * {@code CHAR(36)}; an adopted MariaDB database keeps its native {@code UUID} columns, which
     * compare with the same strings, and gets {@code idType} in the tables it lacked.
     */
    private List<String> createTables(boolean ifNotExists, String idType) {
        String create = ifNotExists ? "CREATE TABLE IF NOT EXISTS " : "CREATE TABLE ";
        String autoId = dialect == DatabaseDialect.H2
                ? "BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY"
                : "BIGINT AUTO_INCREMENT PRIMARY KEY";
        String createdAt = (dialect == DatabaseDialect.H2 ? "TIMESTAMP" : "DATETIME") + " DEFAULT CURRENT_TIMESTAMP";
        return List.of(
                create + "guilds ("
                        + "id " + idType + " NOT NULL PRIMARY KEY, "
                        + "name VARCHAR(255) UNIQUE, "
                        + "balance DOUBLE DEFAULT 0 NOT NULL, "
                        + "exp BIGINT DEFAULT 0 NOT NULL, "
                        + "level INT DEFAULT 1 NOT NULL, "
                        + "home VARCHAR(255) NULL, "
                        + "owner_uuid " + idType + " NULL, "
                        + "createdAt " + createdAt + ")",
                create + "guilds_ranks ("
                        + "id " + idType + " NOT NULL PRIMARY KEY, "
                        + "guild_id " + idType + " NOT NULL, "
                        + "label VARCHAR(255) NOT NULL, "
                        + "rank_order INT DEFAULT 0 NOT NULL, "
                        + "permissions TEXT NULL, "
                        + "CONSTRAINT guilds_ranks_label UNIQUE (guild_id, label), "
                        + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE)",
                create + "guilds_logs ("
                        + "id " + autoId + ", "
                        + "guild_id " + idType + " NOT NULL, "
                        + "log TEXT NULL, "
                        + "createdAt " + createdAt + ", "
                        + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE)",
                create + "guilds_members ("
                        + "guild_id " + idType + " NOT NULL, "
                        + "player_uuid " + idType + " NOT NULL UNIQUE, "
                        + "rank_id " + idType + " NULL, "
                        + "chat_mode VARCHAR(50) NULL, "
                        + "PRIMARY KEY (guild_id, player_uuid), "
                        + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY (rank_id) REFERENCES guilds_ranks(id))",
                create + "guilds_alliances ("
                        + "guild_a_id " + idType + " NOT NULL, "
                        + "guild_b_id " + idType + " NOT NULL, "
                        + "PRIMARY KEY (guild_a_id, guild_b_id), "
                        + "FOREIGN KEY (guild_a_id) REFERENCES guilds(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY (guild_b_id) REFERENCES guilds(id) ON DELETE CASCADE)");
    }

    private static void createVersionTable(Connection c) throws SQLException {
        execute(c, List.of("CREATE TABLE IF NOT EXISTS " + VERSION_TABLE + " (version INT NOT NULL)"));
    }

    private static int currentVersion(Connection c) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT MAX(version) FROM " + VERSION_TABLE)) {
            return r.next() ? r.getInt(1) : 0;
        }
    }

    private static void recordVersion(Connection c, int version) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO " + VERSION_TABLE + " (version) VALUES (?)")) {
            p.setInt(1, version);
            p.executeUpdate();
        }
    }

    private static void execute(Connection c, List<String> statements) throws SQLException {
        for (String sql : statements) {
            try (Statement s = c.createStatement()) {
                s.execute(sql);
            }
        }
    }

    /**
     * Looked up in the connection's own database only: MySQL's metadata otherwise searches every
     * database of the server, and another plugin's {@code guilds} table would count. The name is
     * tried as written, then upper case (H2), then lower case.
     */
    static boolean tableExists(Connection c, String table) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        for (String name : spellings(table)) {
            try (ResultSet r = meta.getTables(c.getCatalog(), null, name, new String[]{"TABLE"})) {
                if (r.next()) return true;
            }
        }
        return false;
    }

    static boolean columnExists(Connection c, String table, String column) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        for (String tableName : spellings(table)) {
            try (ResultSet r = meta.getColumns(c.getCatalog(), null, tableName, null)) {
                while (r.next()) {
                    if (r.getString("COLUMN_NAME").equalsIgnoreCase(column)) return true;
                }
            }
        }
        return false;
    }

    /** The SQL type of a column, as {@code CREATE TABLE} takes it: {@code UUID}, {@code VARCHAR(36)}... */
    static String columnType(Connection c, String table, String column) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        for (String tableName : spellings(table)) {
            try (ResultSet r = meta.getColumns(c.getCatalog(), null, tableName, null)) {
                while (r.next()) {
                    if (!r.getString("COLUMN_NAME").equalsIgnoreCase(column)) continue;
                    String type = r.getString("TYPE_NAME").toUpperCase(Locale.ROOT);
                    return type.contains("CHAR") ? type + "(" + r.getInt("COLUMN_SIZE") + ")" : type;
                }
            }
        }
        throw new SQLException("No column " + table + "." + column);
    }

    private static List<String> spellings(String name) {
        return List.of(name, name.toUpperCase(Locale.ROOT), name.toLowerCase(Locale.ROOT)).stream()
                .distinct().toList();
    }

    /** The tables cannot be used as they are; the message says what to do. */
    public static final class LegacySchemaException extends SQLException {
        public LegacySchemaException(String message) {
            super(message);
        }
    }
}
