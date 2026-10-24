package com.github.ibanetchep.artisanguilds.storage;

import net.artisanmc.modules.api.database.DatabaseDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mariadb.jdbc.MariaDbDataSource;

import java.sql.SQLException;
import java.util.List;

import static com.github.ibanetchep.artisanguilds.storage.LegacyImportTest.GUILD;
import static com.github.ibanetchep.artisanguilds.storage.LegacyImportTest.PLAYER;
import static com.github.ibanetchep.artisanguilds.storage.LegacyImportTest.RANK;
import static com.github.ibanetchep.artisanguilds.storage.LegacyImportTest.intValue;
import static com.github.ibanetchep.artisanguilds.storage.LegacyImportTest.sql;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The import on a real MariaDB, from the exact tables MSGuilds created. Skipped unless
 * {@code GUILDS_MARIADB_URL} names a scratch database, e.g.
 * {@code jdbc:mariadb://localhost:3306/guilds?user=root&password=root}. Core's database and
 * Artisan's are the same one here, the case where both configs point at one database.
 */
@EnabledIfEnvironmentVariable(named = "GUILDS_MARIADB_URL", matches = ".+")
class LegacyImportMariaDbTest {

    private static final GuildTables TABLES = new GuildTables("artisan_guilds_");

    private MariaDbDataSource db;

    @BeforeEach
    void setUp() throws SQLException {
        db = new MariaDbDataSource(System.getenv("GUILDS_MARIADB_URL"));
        for (String table : List.of(TABLES.imports(), TABLES.alliances(), TABLES.logs(), TABLES.members(), TABLES.ranks(),
                TABLES.guilds(), TABLES.schemaVersion(), "guilds_alliances", "guilds_members", "guilds_logs", "guilds_ranks", "guilds")) {
            sql(db, "DROP TABLE IF EXISTS " + table);
        }
        new GuildSchema(db, DatabaseDialect.MARIADB, TABLES).migrate();
    }

    /** SqlStorage of MSGuilds 1.1.1: native UUID columns, level included, no alliance table yet. */
    @Test
    void importsTheTablesSqlStorageCreates() throws SQLException {
        sql(db, "CREATE TABLE IF NOT EXISTS guilds(id UUID PRIMARY KEY DEFAULT (UUID()),name VARCHAR(255) UNIQUE ,"
                + "balance FLOAT DEFAULT 0,exp BIGINT DEFAULT 0,level INT DEFAULT 1,home VARCHAR(255),"
                + "owner_uuid UUID,createdAt DATETIME DEFAULT NOW()); ");
        sql(db, "CREATE TABLE IF NOT EXISTS guilds_ranks(id UUID PRIMARY KEY DEFAULT (UUID()),guild_id UUID, "
                + "label VARCHAR(255), rank_order INTEGER DEFAULT 0, permissions JSON, "
                + "UNIQUE KEY unique_guild_rank (guild_id, label), "
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE);");
        sql(db, "CREATE TABLE IF NOT EXISTS guilds_logs(id INTEGER PRIMARY KEY AUTO_INCREMENT ,guild_id UUID ,"
                + "log TEXT,createdAt DATETIME DEFAULT NOW(),"
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE);");
        sql(db, "CREATE TABLE IF NOT EXISTS guilds_members(guild_id UUID ,player_uuid UUID UNIQUE ,rank_id UUID,"
                + "chat_mode VARCHAR(50),PRIMARY KEY (guild_id, player_uuid),"
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (rank_id) REFERENCES guilds_ranks(id));");
        seed();
        sql(db, "UPDATE guilds SET level = 4");

        LegacyImport.Result result = new LegacyImport(TABLES).run(db, "Core", db);

        assertEquals(LegacyImport.Outcome.IMPORTED, result.outcome());
        assertEquals(0, result.counts().get("alliances"));
        assertImported(4);
    }

    /** MigrationManager of MSGuilds 1.1 (the conversion from integer ids): VARCHAR ids, no level. */
    @Test
    void importsTheTablesTheConversionCreated() throws SQLException {
        sql(db, "CREATE TABLE guilds(id VARCHAR(36) PRIMARY KEY,name VARCHAR(255) UNIQUE,balance FLOAT DEFAULT 0,"
                + "exp BIGINT DEFAULT 0,home VARCHAR(255),owner_uuid VARCHAR(36),createdAt DATETIME DEFAULT NOW());");
        sql(db, "CREATE TABLE guilds_ranks(id VARCHAR(36) PRIMARY KEY,guild_id VARCHAR(36),label VARCHAR(255),"
                + "rank_order INTEGER DEFAULT 0,permissions JSON,UNIQUE KEY unique_guild_rank (guild_id, label),"
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE);");
        sql(db, "CREATE TABLE guilds_logs(id INTEGER PRIMARY KEY AUTO_INCREMENT,guild_id VARCHAR(36),log TEXT,"
                + "createdAt DATETIME DEFAULT NOW(),FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE);");
        sql(db, "CREATE TABLE guilds_members(guild_id VARCHAR(36),player_uuid VARCHAR(36) UNIQUE,rank_id VARCHAR(36),"
                + "chat_mode VARCHAR(50),PRIMARY KEY (guild_id, player_uuid),"
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (rank_id) REFERENCES guilds_ranks(id));");
        sql(db, "CREATE TABLE guilds_alliances(guild_a_id VARCHAR(36),guild_b_id VARCHAR(36),"
                + "PRIMARY KEY (guild_a_id, guild_b_id),"
                + "FOREIGN KEY (guild_a_id) REFERENCES guilds(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (guild_b_id) REFERENCES guilds(id) ON DELETE CASCADE);");
        seed();
        sql(db, "INSERT INTO guilds_alliances VALUES ('" + GUILD + "', '" + GUILD + "')");

        assertEquals(LegacyImport.Outcome.IMPORTED, new LegacyImport(TABLES).run(db, "Core", db).outcome());
        assertImported(1);
        assertEquals(1, intValue(db, "SELECT COUNT(*) FROM artisan_guilds_alliances"));
    }

    private void seed() throws SQLException {
        sql(db, "INSERT INTO guilds (id, name, balance, owner_uuid) VALUES ('" + GUILD + "', 'Alpha', 1250, '" + PLAYER + "')");
    }

    private void assertImported(int level) throws SQLException {
        sql(db, "INSERT INTO guilds_ranks (id, guild_id, label, rank_order, permissions) VALUES ('" + RANK + "', '"
                + GUILD + "', 'Chef', 4, '[\"KICK_MEMBER\"]')");
        // Rows written after the import stay in MSGuilds' tables only.
        assertEquals(0, intValue(db, "SELECT COUNT(*) FROM artisan_guilds_ranks"));
        assertEquals(1, intValue(db, "SELECT COUNT(*) FROM artisan_guilds_guilds"));
        assertEquals(level, intValue(db, "SELECT level FROM artisan_guilds_guilds"));
        assertEquals(1250, intValue(db, "SELECT balance FROM artisan_guilds_guilds"));
        assertEquals(PLAYER, stringOwner());
    }

    private String stringOwner() throws SQLException {
        try (var c = db.getConnection(); var s = c.createStatement();
             var r = s.executeQuery("SELECT owner_uuid FROM artisan_guilds_guilds")) {
            r.next();
            return r.getString(1);
        }
    }
}
