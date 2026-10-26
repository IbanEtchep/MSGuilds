package fr.iban.guilds.storage;

import com.google.gson.Gson;
import fr.iban.common.data.sql.DbAccess;
import fr.iban.guilds.enums.ChatMode;
import fr.iban.guilds.enums.DefaultRank;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class MigrationManager {

    private final DataSource ds = DbAccess.getDataSource();
    private final Gson gson = new Gson();

    /**
     * Checks whether a migration is needed
     */
    public boolean needsMigration() {
        try (Connection connection = ds.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Check whether the guilds table has an 'owner_uuid' column (new structure)
            try (ResultSet rs = metaData.getColumns(null, null, "guilds", "owner_uuid")) {
                if (rs.next()) {
                    // The new structure already exists, no need to migrate
                    return false;
                }
            }

            // Check whether the old table exists
            try (ResultSet rs = metaData.getTables(null, null, "guilds", null)) {
                if (rs.next()) {
                    // The old guilds table exists, migration needed
                    return true;
                }
            }

            // No table exists, no migration needed (first install)
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Runs the full migration
     */
    public void migrate() {
        System.out.println("========================================");
        System.out.println("START OF THE GUILDS MIGRATION");
        System.out.println("========================================");

        try (Connection connection = ds.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // Step 1: Create the new tables
                System.out.println("[1/7] Creating the new tables...");
                createNewTables(connection);

                // Step 2: Migrate the guilds
                System.out.println("[2/7] Migrating guilds...");
                Map<Integer, UUID> guildIdMapping = migrateGuilds(connection);
                System.out.println("    → " + guildIdMapping.size() + " guilds migrated");

                // Step 3: Create the default ranks for each guild
                System.out.println("[3/7] Creating default ranks...");
                Map<String, Map<String, UUID>> rankMappings = createDefaultRanks(connection, guildIdMapping);
                System.out.println("    → " + (rankMappings.size() * DefaultRank.values().length) + " ranks created");

                // Step 4: Migrate the members
                System.out.println("[4/7] Migrating members...");
                int memberCount = migrateMembers(connection, guildIdMapping, rankMappings);
                System.out.println("    → " + memberCount + " members migrated");

                // Step 5: Migrate the logs
                System.out.println("[5/7] Migrating logs...");
                int logCount = migrateLogs(connection, guildIdMapping);
                System.out.println("    → " + logCount + " logs migrated");

                // Step 6: Migrate the alliances
                System.out.println("[6/7] Migrating alliances...");
                int allianceCount = migrateAlliances(connection, guildIdMapping);
                System.out.println("    → " + allianceCount + " alliances migrated");

                // Step 7: Drop the old tables
                System.out.println("[7/7] Cleaning up the old tables...");
                dropOldTables(connection);

                connection.commit();

                System.out.println("========================================");
                System.out.println("MIGRATION COMPLETED SUCCESSFULLY!");
                System.out.println("========================================");

            } catch (SQLException e) {
                connection.rollback();
                System.err.println("========================================");
                System.err.println("ERROR DURING THE MIGRATION - ROLLBACK");
                System.err.println("========================================");
                e.printStackTrace();
                throw new RuntimeException("Migration failed", e);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database connection error", e);
        }
    }

    private void createNewTables(Connection connection) throws SQLException {
        // First, back up the old reference tables before dropping them
        System.out.println("    Backing up reference tables...");

        try (Statement stmt = connection.createStatement()) {
            // Create temporary copies of the reference tables
            stmt.executeUpdate("CREATE TEMPORARY TABLE guilds_ranks_old AS SELECT * FROM guilds_ranks;");
            stmt.executeUpdate("CREATE TEMPORARY TABLE guilds_chatmode_old AS SELECT * FROM guilds_chatmode;");
            stmt.executeUpdate("CREATE TEMPORARY TABLE guilds_members_old AS SELECT * FROM guilds_members;");
            stmt.executeUpdate("CREATE TEMPORARY TABLE guilds_logs_old AS SELECT * FROM guilds_logs;");
            stmt.executeUpdate("CREATE TEMPORARY TABLE guilds_alliances_old AS SELECT * FROM guilds_alliances;");
            System.out.println("    Reference tables backed up");
        } catch (SQLException e) {
            System.out.println("    Error during backup: " + e.getMessage());
            throw e;
        }

        // Drop all the old tables
        String[] dropOldStructure = {
                "DROP TABLE IF EXISTS guilds_members;",
                "DROP TABLE IF EXISTS guilds_logs;",
                "DROP TABLE IF EXISTS guilds_alliances;",
                "DROP TABLE IF EXISTS guilds_ranks;",
                "DROP TABLE IF EXISTS guilds_chatmode;"
        };

        for (String sql : dropOldStructure) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("    Dropping: " + sql);
            } catch (SQLException e) {
                System.out.println("    Info: " + e.getMessage());
            }
        }

        // Rename the guilds table
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("ALTER TABLE guilds RENAME TO guilds_old;");
            System.out.println("    Table guilds renamed to guilds_old");
        } catch (SQLException e) {
            System.out.println("    Info: " + e.getMessage());
        }

        // Create the new tables
        String[] createStatements = {
                "CREATE TABLE guilds(" +
                        "id VARCHAR(36) PRIMARY KEY," +
                        "name VARCHAR(255) UNIQUE," +
                        "balance FLOAT DEFAULT 0," +
                        "exp BIGINT DEFAULT 0," +
                        "home VARCHAR(255)," +
                        "owner_uuid VARCHAR(36)," +
                        "createdAt DATETIME DEFAULT NOW()" +
                        ");",

                "CREATE TABLE guilds_ranks(" +
                        "id VARCHAR(36) PRIMARY KEY," +
                        "guild_id VARCHAR(36)," +
                        "label VARCHAR(255)," +
                        "rank_order INTEGER DEFAULT 0," +
                        "permissions JSON," +
                        "UNIQUE KEY unique_guild_rank (guild_id, label)," +
                        "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                        ");",

                "CREATE TABLE guilds_logs(" +
                        "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                        "guild_id VARCHAR(36)," +
                        "log TEXT," +
                        "createdAt DATETIME DEFAULT NOW()," +
                        "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                        ");",

                "CREATE TABLE guilds_members(" +
                        "guild_id VARCHAR(36)," +
                        "player_uuid VARCHAR(36) UNIQUE," +
                        "rank_id VARCHAR(36)," +
                        "chat_mode VARCHAR(50)," +
                        "PRIMARY KEY (guild_id, player_uuid)," +
                        "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                        "FOREIGN KEY (rank_id) REFERENCES guilds_ranks(id)" +
                        ");",

                "CREATE TABLE guilds_alliances(" +
                        "guild_a_id VARCHAR(36)," +
                        "guild_b_id VARCHAR(36)," +
                        "PRIMARY KEY (guild_a_id, guild_b_id)," +
                        "FOREIGN KEY (guild_a_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                        "FOREIGN KEY (guild_b_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                        ");"
        };

        for (String sql : createStatements) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            }
        }
    }

    private Map<Integer, UUID> migrateGuilds(Connection connection) throws SQLException {
        Map<Integer, UUID> idMapping = new HashMap<>();

        String selectSql = "SELECT * FROM guilds_old;";
        String insertSql = "INSERT INTO guilds(id, name, balance, exp, home, owner_uuid, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?);";

        try (PreparedStatement selectPs = connection.prepareStatement(selectSql);
             PreparedStatement insertPs = connection.prepareStatement(insertSql);
             ResultSet rs = selectPs.executeQuery()) {

            while (rs.next()) {
                int oldId = rs.getInt("id");
                UUID newId = UUID.fromString(rs.getString("guild_uuid"));

                // Check whether the guild has members
                UUID ownerUuid = findGuildOwner(connection, oldId);
                if (ownerUuid == null) {
                    System.out.println("    WARNING: Guild " + oldId + " (" + rs.getString("name") + ") has no members - skipped");
                    continue; // Skip this guild
                }

                insertPs.setString(1, newId.toString());
                insertPs.setString(2, rs.getString("name"));
                insertPs.setDouble(3, rs.getDouble("balance"));
                insertPs.setLong(4, rs.getLong("exp"));
                insertPs.setString(5, rs.getString("home"));
                insertPs.setString(6, ownerUuid.toString());
                insertPs.setTimestamp(7, rs.getTimestamp("createdAt"));

                insertPs.executeUpdate();
                idMapping.put(oldId, newId);
            }
        }

        return idMapping;
    }

    private UUID findGuildOwner(Connection connection, int guildOldId) throws SQLException {
        String sql = "SELECT player_uuid FROM guilds_members_old " +
                "JOIN guilds_ranks_old gr ON gr.id = guilds_members_old.rank_id " +
                "WHERE guild_id = ? AND gr.label = 'OWNER' LIMIT 1;";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, guildOldId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("player_uuid"));
                }
            }
        }

        // Fallback: take the first member
        String fallbackSql = "SELECT player_uuid FROM guilds_members_old WHERE guild_id = ? LIMIT 1;";
        try (PreparedStatement ps = connection.prepareStatement(fallbackSql)) {
            ps.setInt(1, guildOldId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("player_uuid"));
                }
            }
        }

        // No member found, return null instead of throwing
        return null;
    }

    private Map<String, Map<String, UUID>> createDefaultRanks(Connection connection, Map<Integer, UUID> guildIdMapping) throws SQLException {
        Map<String, Map<String, UUID>> rankMappings = new HashMap<>();

        String insertSql = "INSERT INTO guilds_ranks(id, guild_id, label, rank_order, permissions) VALUES (?, ?, ?, ?, ?);";

        Map<String, Integer> rankOrder = Map.of(
                "OWNER", 4,
                "ADMIN", 3,
                "MODERATOR", 2,
                "MEMBER", 1
        );

        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            for (UUID guildId : guildIdMapping.values()) {
                Map<String, UUID> guildRanks = new HashMap<>();

                for (DefaultRank defaultRank : DefaultRank.values()) {
                    UUID rankId = UUID.randomUUID();
                    String rankName = defaultRank.name();

                    ps.setString(1, rankId.toString());
                    ps.setString(2, guildId.toString());
                    ps.setString(3, defaultRank.getName());
                    ps.setInt(4, rankOrder.getOrDefault(rankName, 0));
                    ps.setString(5, gson.toJson(defaultRank.getPermissions()));

                    ps.executeUpdate();
                    guildRanks.put(rankName, rankId);
                }

                rankMappings.put(guildId.toString(), guildRanks);
            }
        }

        return rankMappings;
    }

    private int migrateMembers(Connection connection, Map<Integer, UUID> guildIdMapping, Map<String, Map<String, UUID>> rankMappings) throws SQLException {
        // First, create a temporary copy of the old data
        String createTempSql = "CREATE TEMPORARY TABLE temp_members AS " +
                "SELECT gm.*, gr.label as rank_label, gc.label as chatmode_label " +
                "FROM guilds_members_old gm " +
                "JOIN guilds_ranks_old gr ON gr.id = gm.rank_id " +
                "JOIN guilds_chatmode_old gc ON gc.id = gm.chatmode_id;";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createTempSql);
        }

        String selectSql = "SELECT * FROM temp_members;";
        String insertSql = "INSERT INTO guilds_members(guild_id, player_uuid, rank_id, chat_mode) VALUES (?, ?, ?, ?);";

        int count = 0;
        try (PreparedStatement selectPs = connection.prepareStatement(selectSql);
             PreparedStatement insertPs = connection.prepareStatement(insertSql);
             ResultSet rs = selectPs.executeQuery()) {

            while (rs.next()) {
                int oldGuildId = rs.getInt("guild_id");
                UUID newGuildId = guildIdMapping.get(oldGuildId);
                String playerUuid = rs.getString("player_uuid");
                String rankLabel = rs.getString("rank_label");
                String chatModeLabel = rs.getString("chatmode_label");

                UUID rankId = rankMappings.get(newGuildId.toString()).get(rankLabel);

                insertPs.setString(1, newGuildId.toString());
                insertPs.setString(2, playerUuid);
                insertPs.setString(3, rankId.toString());
                insertPs.setString(4, chatModeLabel);

                insertPs.executeUpdate();
                count++;
            }
        }

        return count;
    }

    private int migrateLogs(Connection connection, Map<Integer, UUID> guildIdMapping) throws SQLException {
        String selectSql = "SELECT * FROM guilds_logs_old;";
        String insertSql = "INSERT INTO guilds_logs(guild_id, log, createdAt) VALUES (?, ?, ?);";

        int count = 0;
        try (PreparedStatement selectPs = connection.prepareStatement(selectSql);
             PreparedStatement insertPs = connection.prepareStatement(insertSql);
             ResultSet rs = selectPs.executeQuery()) {

            while (rs.next()) {
                int oldGuildId = rs.getInt("guild_id");
                UUID newGuildId = guildIdMapping.get(oldGuildId);

                if (newGuildId != null) {
                    insertPs.setString(1, newGuildId.toString());
                    insertPs.setString(2, rs.getString("log"));
                    insertPs.setTimestamp(3, rs.getTimestamp("createdAt"));

                    insertPs.executeUpdate();
                    count++;
                }
            }
        }

        return count;
    }

    private int migrateAlliances(Connection connection, Map<Integer, UUID> guildIdMapping) throws SQLException {
        String selectSql = "SELECT * FROM guilds_alliances_old;";
        String insertSql = "INSERT INTO guilds_alliances(guild_a_id, guild_b_id) VALUES (?, ?);";

        int count = 0;
        try (PreparedStatement selectPs = connection.prepareStatement(selectSql);
             PreparedStatement insertPs = connection.prepareStatement(insertSql);
             ResultSet rs = selectPs.executeQuery()) {

            while (rs.next()) {
                int oldGuildAId = rs.getInt("guild_a_id");
                int oldGuildBId = rs.getInt("guild_b_id");

                UUID newGuildAId = guildIdMapping.get(oldGuildAId);
                UUID newGuildBId = guildIdMapping.get(oldGuildBId);

                if (newGuildAId != null && newGuildBId != null) {
                    insertPs.setString(1, newGuildAId.toString());
                    insertPs.setString(2, newGuildBId.toString());

                    insertPs.executeUpdate();
                    count++;
                }
            }
        }

        return count;
    }

    private void dropOldTables(Connection connection) throws SQLException {
        String[] dropStatements = {
                "DROP TEMPORARY TABLE IF EXISTS guilds_members_old;",
                "DROP TEMPORARY TABLE IF EXISTS guilds_logs_old;",
                "DROP TEMPORARY TABLE IF EXISTS guilds_alliances_old;",
                "DROP TEMPORARY TABLE IF EXISTS guilds_ranks_old;",
                "DROP TEMPORARY TABLE IF EXISTS guilds_chatmode_old;",
                "DROP TABLE IF EXISTS guilds_old;"
        };

        for (String sql : dropStatements) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                System.out.println("    Info: " + e.getMessage());
            }
        }
    }
}