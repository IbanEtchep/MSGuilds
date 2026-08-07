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
     * Vérifie si une migration est nécessaire
     */
    public boolean needsMigration() {
        try (Connection connection = ds.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Vérifier si la table guilds a une colonne 'owner_uuid' (nouvelle structure)
            try (ResultSet rs = metaData.getColumns(null, null, "guilds", "owner_uuid")) {
                if (rs.next()) {
                    // La nouvelle structure existe déjà, pas besoin de migrer
                    return false;
                }
            }

            // Vérifier si l'ancienne table existe
            try (ResultSet rs = metaData.getTables(null, null, "guilds", null)) {
                if (rs.next()) {
                    // L'ancienne table guilds existe, migration nécessaire
                    return true;
                }
            }

            // Aucune table n'existe, pas de migration nécessaire (première installation)
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Exécute la migration complète
     */
    public void migrate() {
        System.out.println("========================================");
        System.out.println("DEBUT DE LA MIGRATION DES GUILDES");
        System.out.println("========================================");

        try (Connection connection = ds.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // Étape 1: Créer les nouvelles tables
                System.out.println("[1/7] Création des nouvelles tables...");
                createNewTables(connection);

                // Étape 2: Migrer les guildes
                System.out.println("[2/7] Migration des guildes...");
                Map<Integer, UUID> guildIdMapping = migrateGuilds(connection);
                System.out.println("    → " + guildIdMapping.size() + " guildes migrées");

                // Étape 3: Créer les rangs par défaut pour chaque guilde
                System.out.println("[3/7] Création des rangs par défaut...");
                Map<String, Map<String, UUID>> rankMappings = createDefaultRanks(connection, guildIdMapping);
                System.out.println("    → " + (rankMappings.size() * DefaultRank.values().length) + " rangs créés");

                // Étape 4: Migrer les membres
                System.out.println("[4/7] Migration des membres...");
                int memberCount = migrateMembers(connection, guildIdMapping, rankMappings);
                System.out.println("    → " + memberCount + " membres migrés");

                // Étape 5: Migrer les logs
                System.out.println("[5/7] Migration des logs...");
                int logCount = migrateLogs(connection, guildIdMapping);
                System.out.println("    → " + logCount + " logs migrés");

                // Étape 6: Migrer les alliances
                System.out.println("[6/7] Migration des alliances...");
                int allianceCount = migrateAlliances(connection, guildIdMapping);
                System.out.println("    → " + allianceCount + " alliances migrées");

                // Étape 7: Supprimer les anciennes tables
                System.out.println("[7/7] Nettoyage des anciennes tables...");
                dropOldTables(connection);

                connection.commit();

                System.out.println("========================================");
                System.out.println("MIGRATION TERMINEE AVEC SUCCES !");
                System.out.println("========================================");

            } catch (SQLException e) {
                connection.rollback();
                System.err.println("========================================");
                System.err.println("ERREUR LORS DE LA MIGRATION - ROLLBACK");
                System.err.println("========================================");
                e.printStackTrace();
                throw new RuntimeException("Migration échouée", e);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur de connexion à la base de données", e);
        }
    }

    private void createNewTables(Connection connection) throws SQLException {
        // D'abord, sauvegarder les anciennes tables de référence avant de les supprimer
        System.out.println("    Sauvegarde des tables de référence...");

        try (Statement stmt = connection.createStatement()) {
            // Créer des copies temporaires des tables de référence
            stmt.executeUpdate("CREATE TEMPORARY TABLE guilds_ranks_old AS SELECT * FROM guilds_ranks;");
            stmt.executeUpdate("CREATE TEMPORARY TABLE guilds_chatmode_old AS SELECT * FROM guilds_chatmode;");
            stmt.executeUpdate("CREATE TEMPORARY TABLE guilds_members_old AS SELECT * FROM guilds_members;");
            stmt.executeUpdate("CREATE TEMPORARY TABLE guilds_logs_old AS SELECT * FROM guilds_logs;");
            stmt.executeUpdate("CREATE TEMPORARY TABLE guilds_alliances_old AS SELECT * FROM guilds_alliances;");
            System.out.println("    Tables de référence sauvegardées");
        } catch (SQLException e) {
            System.out.println("    Erreur lors de la sauvegarde: " + e.getMessage());
            throw e;
        }

        // Supprimer toutes les anciennes tables
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
                System.out.println("    Suppression: " + sql);
            } catch (SQLException e) {
                System.out.println("    Info: " + e.getMessage());
            }
        }

        // Renommer la table guilds
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("ALTER TABLE guilds RENAME TO guilds_old;");
            System.out.println("    Table guilds renommée en guilds_old");
        } catch (SQLException e) {
            System.out.println("    Info: " + e.getMessage());
        }

        // Créer les nouvelles tables
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

                // Vérifier si la guilde a des membres
                UUID ownerUuid = findGuildOwner(connection, oldId);
                if (ownerUuid == null) {
                    System.out.println("    ATTENTION: Guilde " + oldId + " (" + rs.getString("name") + ") sans membres - ignorée");
                    continue; // Sauter cette guilde
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

        // Fallback: prendre le premier membre
        String fallbackSql = "SELECT player_uuid FROM guilds_members_old WHERE guild_id = ? LIMIT 1;";
        try (PreparedStatement ps = connection.prepareStatement(fallbackSql)) {
            ps.setInt(1, guildOldId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("player_uuid"));
                }
            }
        }

        // Aucun membre trouvé, retourner null au lieu de throw
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
        // D'abord, créer une copie temporaire des anciennes données
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