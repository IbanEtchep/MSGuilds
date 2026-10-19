package com.github.ibanetchep.artisanguilds.storage;

import net.artisanmc.modules.api.database.DatabaseDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mariadb.jdbc.MariaDbDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The adoption on a real MariaDB, with the exact statements MSGuilds ran. Skipped unless
 * {@code GUILDS_MARIADB_URL} names an empty database, e.g.
 * {@code jdbc:mariadb://localhost:3306/guilds?user=root&password=root}.
 */
@EnabledIfEnvironmentVariable(named = "GUILDS_MARIADB_URL", matches = ".+")
class GuildSchemaMariaDbTest {

    private static final String GUILD = "6f1c1b2e-3f0a-4c55-9d7e-0a1b2c3d4e5f";
    private static final String RANK = "a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5";
    private static final String PLAYER = "11111111-2222-3333-4444-555555555555";

    private MariaDbDataSource dataSource;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = new MariaDbDataSource(System.getenv("GUILDS_MARIADB_URL"));
        for (String table : List.of(GuildSchema.VERSION_TABLE, "guilds_alliances", "guilds_members",
                "guilds_logs", "guilds_ranks", "guilds")) {
            sql("DROP TABLE IF EXISTS " + table);
        }
    }

    /** SqlStorage of MSGuilds 1.1.1 on a new server: native UUID columns, level included. */
    @Test
    void adoptsTheTablesSqlStorageCreates() throws SQLException {
        sql("CREATE TABLE IF NOT EXISTS guilds(id UUID PRIMARY KEY DEFAULT (UUID()),name VARCHAR(255) UNIQUE ,"
                + "balance FLOAT DEFAULT 0,exp BIGINT DEFAULT 0,level INT DEFAULT 1,home VARCHAR(255),"
                + "owner_uuid UUID,createdAt DATETIME DEFAULT NOW()); ");
        sql("CREATE TABLE IF NOT EXISTS guilds_ranks(id UUID PRIMARY KEY DEFAULT (UUID()),guild_id UUID, "
                + "label VARCHAR(255), rank_order INTEGER DEFAULT 0, permissions JSON, "
                + "UNIQUE KEY unique_guild_rank (guild_id, label), "
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE);");
        sql("CREATE TABLE IF NOT EXISTS guilds_logs(id INTEGER PRIMARY KEY AUTO_INCREMENT ,guild_id UUID ,"
                + "log TEXT,createdAt DATETIME DEFAULT NOW(),"
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE);");
        sql("CREATE TABLE IF NOT EXISTS guilds_members(guild_id UUID ,player_uuid UUID UNIQUE ,rank_id UUID,"
                + "chat_mode VARCHAR(50),PRIMARY KEY (guild_id, player_uuid),"
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (rank_id) REFERENCES guilds_ranks(id));");
        seed();

        GuildSchema.Result result = new GuildSchema(dataSource, DatabaseDialect.MARIADB).migrate();

        assertEquals(GuildSchema.Outcome.ADOPTED, result.outcome());
        assertTrue(result.addedColumns().isEmpty());
        assertRowsKept();
        // guilds_alliances did not exist: created with CHAR(36) columns referencing UUID ones.
        sql("INSERT INTO guilds_alliances VALUES ('" + GUILD + "', '" + GUILD + "')");
        assertEquals(GuildSchema.Outcome.UP_TO_DATE, new GuildSchema(dataSource, DatabaseDialect.MARIADB).migrate().outcome());
    }

    /** MigrationManager of MSGuilds 1.1 (the conversion from integer ids): VARCHAR ids, no level. */
    @Test
    void adoptsTheTablesTheConversionCreatedAndAddsLevel() throws SQLException {
        sql("CREATE TABLE guilds(id VARCHAR(36) PRIMARY KEY,name VARCHAR(255) UNIQUE,balance FLOAT DEFAULT 0,"
                + "exp BIGINT DEFAULT 0,home VARCHAR(255),owner_uuid VARCHAR(36),createdAt DATETIME DEFAULT NOW());");
        sql("CREATE TABLE guilds_ranks(id VARCHAR(36) PRIMARY KEY,guild_id VARCHAR(36),label VARCHAR(255),"
                + "rank_order INTEGER DEFAULT 0,permissions JSON,UNIQUE KEY unique_guild_rank (guild_id, label),"
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE);");
        sql("CREATE TABLE guilds_logs(id INTEGER PRIMARY KEY AUTO_INCREMENT,guild_id VARCHAR(36),log TEXT,"
                + "createdAt DATETIME DEFAULT NOW(),FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE);");
        sql("CREATE TABLE guilds_members(guild_id VARCHAR(36),player_uuid VARCHAR(36) UNIQUE,rank_id VARCHAR(36),"
                + "chat_mode VARCHAR(50),PRIMARY KEY (guild_id, player_uuid),"
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (rank_id) REFERENCES guilds_ranks(id));");
        sql("CREATE TABLE guilds_alliances(guild_a_id VARCHAR(36),guild_b_id VARCHAR(36),"
                + "PRIMARY KEY (guild_a_id, guild_b_id),"
                + "FOREIGN KEY (guild_a_id) REFERENCES guilds(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (guild_b_id) REFERENCES guilds(id) ON DELETE CASCADE);");
        seed();

        GuildSchema.Result result = new GuildSchema(dataSource, DatabaseDialect.MARIADB).migrate();

        assertEquals(GuildSchema.Outcome.ADOPTED, result.outcome());
        assertEquals(List.of("guilds.level"), result.addedColumns());
        assertRowsKept();
        assertEquals(1, intValue("SELECT level FROM guilds"));
    }

    @Test
    void createsTheTablesOnAnEmptyDatabase() throws SQLException {
        GuildSchema.Result result = new GuildSchema(dataSource, DatabaseDialect.MARIADB).migrate();

        assertEquals(GuildSchema.Outcome.CREATED, result.outcome());
        seed();
        assertRowsKept();
    }

    private void seed() throws SQLException {
        sql("INSERT INTO guilds (id, name, balance, owner_uuid) VALUES ('" + GUILD + "', 'Alpha', 1250, '" + PLAYER + "')");
        sql("INSERT INTO guilds_ranks (id, guild_id, label, rank_order, permissions) "
                + "VALUES ('" + RANK + "', '" + GUILD + "', 'Chef', 4, '[\"KICK_MEMBER\"]')");
        sql("INSERT INTO guilds_members VALUES ('" + GUILD + "', '" + PLAYER + "', '" + RANK + "', 'PUBLIC')");
        sql("INSERT INTO guilds_logs (guild_id, log) VALUES ('" + GUILD + "', 'created')");
    }

    private void assertRowsKept() throws SQLException {
        assertEquals(1, intValue("SELECT COUNT(*) FROM guilds"));
        assertEquals(1, intValue("SELECT COUNT(*) FROM guilds_members m JOIN guilds_ranks r ON r.id = m.rank_id "
                + "WHERE m.guild_id = '" + GUILD + "' AND r.label = 'Chef'"));
        assertEquals(1, intValue("SELECT COUNT(*) FROM guilds_logs"));
        assertEquals(1250, intValue("SELECT balance FROM guilds"));
    }

    private void sql(String statement) throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute(statement);
        }
    }

    private int intValue(String query) throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery(query)) {
            assertTrue(r.next());
            return r.getInt(1);
        }
    }
}
