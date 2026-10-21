package com.github.ibanetchep.artisanguilds.storage;

import net.artisanmc.modules.api.database.DatabaseDialect;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Core's database and Artisan's are two H2 databases here, MSGuilds' tables in H2's words. */
class LegacyImportTest {

    static final String GUILD = "6f1c1b2e-3f0a-4c55-9d7e-0a1b2c3d4e5f";
    static final String OTHER_GUILD = "0b9e7f3c-1d2a-4e8b-a6c5-112233445566";
    static final String RANK = "a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5";
    static final String PLAYER = "11111111-2222-3333-4444-555555555555";

    private static final GuildTables TABLES = new GuildTables("artisan_guilds_");

    private DataSource core;
    private DataSource artisan;
    private final LegacyImport legacyImport = new LegacyImport(TABLES);

    @BeforeEach
    void setUp() throws SQLException {
        core = h2();
        artisan = h2();
        new GuildSchema(artisan, DatabaseDialect.H2, TABLES).migrate();
    }

    @Test
    void createsTheTablesOnceThenChangesNothing() throws SQLException {
        DataSource empty = h2();
        GuildSchema schema = new GuildSchema(empty, DatabaseDialect.H2, TABLES);

        assertTrue(schema.migrate().created());
        GuildSchema.Result again = schema.migrate();

        assertEquals(1, again.fromVersion());
        assertEquals(1, again.toVersion());
        sql(empty, "INSERT INTO artisan_guilds_guilds (id, name) VALUES ('" + GUILD + "', 'Alpha')");
        sql(empty, "INSERT INTO artisan_guilds_logs (guild_id, log) VALUES ('" + GUILD + "', 'a')");
        sql(empty, "INSERT INTO artisan_guilds_logs (guild_id, log) VALUES ('" + GUILD + "', 'b')");
        assertEquals(1, intValue(empty, "SELECT level FROM artisan_guilds_guilds"));
        assertEquals(2, intValue(empty, "SELECT COUNT(DISTINCT id) FROM artisan_guilds_logs"));
    }

    @Test
    void importsEveryTableAndAddsTheMissingLevel() throws SQLException {
        createMsGuildsTables(core, false);
        seed(core);

        LegacyImport.Result result = legacyImport.run(core, "Core", artisan);

        assertEquals(LegacyImport.Outcome.IMPORTED, result.outcome());
        assertEquals(2, result.counts().get("guilds"));
        assertEquals(2, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_guilds"));
        assertEquals(2, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_guilds WHERE level = 1"));
        assertEquals(1250, intValue(artisan, "SELECT CAST(balance AS INT) FROM artisan_guilds_guilds WHERE name = 'Alpha'"));
        assertEquals(1, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_members m "
                + "JOIN artisan_guilds_ranks r ON r.id = m.rank_id WHERE r.label = 'Chef' AND m.guild_id = '" + GUILD + "'"));
        assertEquals(2, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_logs"));
        assertEquals(1, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_alliances"));
        assertEquals(1, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_imports"));
        // The source is only read.
        assertEquals(2, intValue(core, "SELECT COUNT(*) FROM guilds"));
    }

    @Test
    void keepsTheLevels() throws SQLException {
        createMsGuildsTables(core, true);
        sql(core, "INSERT INTO guilds (id, name, level) VALUES ('" + GUILD + "', 'Alpha', 4)");

        legacyImport.run(core, "Core", artisan);

        assertEquals(4, intValue(artisan, "SELECT level FROM artisan_guilds_guilds"));
    }

    @Test
    void neverRunsTwice() throws SQLException {
        createMsGuildsTables(core, true);
        seed(core);
        legacyImport.run(core, "Core", artisan);
        sql(artisan, "DELETE FROM artisan_guilds_alliances");

        LegacyImport.Result again = legacyImport.run(core, "Core", artisan);

        assertEquals(LegacyImport.Outcome.ALREADY_IMPORTED, again.outcome());
        assertEquals(0, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_alliances"));
    }

    @Test
    void leavesGuildsCreatedSinceAlone() throws SQLException {
        createMsGuildsTables(core, true);
        seed(core);
        sql(artisan, "INSERT INTO artisan_guilds_guilds (id, name) VALUES ('" + UUID.randomUUID() + "', 'New')");

        assertEquals(LegacyImport.Outcome.TARGET_NOT_EMPTY, legacyImport.run(core, "Core", artisan).outcome());
        assertEquals(1, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_guilds"));
    }

    @Test
    void nothingToImportWithoutMsGuildsTables() throws SQLException {
        assertEquals(LegacyImport.Outcome.NO_SOURCE, legacyImport.run(core, "Core", artisan).outcome());
    }

    @Test
    void importsFromTheSameDatabase() throws SQLException {
        createMsGuildsTables(artisan, true);
        seed(artisan);

        assertEquals(LegacyImport.Outcome.IMPORTED, legacyImport.run(artisan, "Artisan", artisan).outcome());
        assertEquals(2, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_guilds"));
    }

    @Test
    void aTableMsGuildsNeverCreatedCountsAsEmpty() throws SQLException {
        createMsGuildsTables(core, true);
        sql(core, "DROP TABLE guilds_alliances");
        sql(core, "INSERT INTO guilds (id, name) VALUES ('" + GUILD + "', 'Alpha')");

        LegacyImport.Result result = legacyImport.run(core, "Core", artisan);

        assertEquals(LegacyImport.Outcome.IMPORTED, result.outcome());
        assertEquals(0, result.counts().get("alliances"));
    }

    @Test
    void aFailedCopyKeepsNothingAndIsTriedAgain() throws SQLException {
        createMsGuildsTables(core, true);
        seed(core);
        // A name too long for ArtisanGuilds' column makes the copy of the second guild fail.
        sql(core, "ALTER TABLE guilds ALTER COLUMN name VARCHAR(400)");
        sql(core, "UPDATE guilds SET name = '" + "x".repeat(300) + "' WHERE id = '" + OTHER_GUILD + "'");

        assertThrows(SQLException.class, () -> legacyImport.run(core, "Core", artisan));
        assertEquals(0, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_guilds"));
        assertEquals(0, intValue(artisan, "SELECT COUNT(*) FROM artisan_guilds_imports"));

        sql(core, "UPDATE guilds SET name = 'Beta' WHERE id = '" + OTHER_GUILD + "'");
        assertEquals(LegacyImport.Outcome.IMPORTED, legacyImport.run(core, "Core", artisan).outcome());
    }

    @Test
    void refusesTheTablesOfMsGuildsBefore11() throws SQLException {
        sql(core, "CREATE TABLE guilds (id INT AUTO_INCREMENT PRIMARY KEY, guild_uuid VARCHAR(36), name VARCHAR(255))");

        LegacyImport.LegacySchemaException error =
                assertThrows(LegacyImport.LegacySchemaException.class, () -> legacyImport.run(core, "Core", artisan));

        assertTrue(error.getMessage().contains("MSGuilds 1.1.1"));
    }

    /** What MSGuilds 1.1's SqlStorage creates on MariaDB, in H2's words (native UUID and JSON). */
    static void createMsGuildsTables(DataSource ds, boolean withLevel) throws SQLException {
        sql(ds, "CREATE TABLE guilds (id UUID DEFAULT RANDOM_UUID() PRIMARY KEY, name VARCHAR(255) UNIQUE, "
                + "balance FLOAT DEFAULT 0, exp BIGINT DEFAULT 0, " + (withLevel ? "level INT DEFAULT 1, " : "")
                + "home VARCHAR(255), owner_uuid UUID, createdAt TIMESTAMP DEFAULT NOW())");
        sql(ds, "CREATE TABLE guilds_ranks (id UUID DEFAULT RANDOM_UUID() PRIMARY KEY, guild_id UUID, "
                + "label VARCHAR(255), rank_order INTEGER DEFAULT 0, permissions JSON, "
                + "CONSTRAINT unique_guild_rank UNIQUE (guild_id, label), "
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE)");
        sql(ds, "CREATE TABLE guilds_logs (id INTEGER AUTO_INCREMENT PRIMARY KEY, guild_id UUID, log TEXT, "
                + "createdAt TIMESTAMP DEFAULT NOW(), FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE)");
        sql(ds, "CREATE TABLE guilds_members (guild_id UUID, player_uuid UUID UNIQUE, rank_id UUID, chat_mode VARCHAR(50), "
                + "PRIMARY KEY (guild_id, player_uuid), FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (rank_id) REFERENCES guilds_ranks(id))");
        sql(ds, "CREATE TABLE guilds_alliances (guild_a_id UUID, guild_b_id UUID, PRIMARY KEY (guild_a_id, guild_b_id), "
                + "FOREIGN KEY (guild_a_id) REFERENCES guilds(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (guild_b_id) REFERENCES guilds(id) ON DELETE CASCADE)");
    }

    static void seed(DataSource ds) throws SQLException {
        sql(ds, "INSERT INTO guilds (id, name, balance, owner_uuid) VALUES ('" + GUILD + "', 'Alpha', 1250, '" + PLAYER + "')");
        sql(ds, "INSERT INTO guilds (id, name) VALUES ('" + OTHER_GUILD + "', 'Beta')");
        sql(ds, "INSERT INTO guilds_ranks (id, guild_id, label, rank_order, permissions) "
                + "VALUES ('" + RANK + "', '" + GUILD + "', 'Chef', 4, '[\"KICK_MEMBER\"]' FORMAT JSON)");
        sql(ds, "INSERT INTO guilds_members VALUES ('" + GUILD + "', '" + PLAYER + "', '" + RANK + "', 'PUBLIC')");
        sql(ds, "INSERT INTO guilds_logs (guild_id, log) VALUES ('" + GUILD + "', 'created')");
        sql(ds, "INSERT INTO guilds_logs (guild_id, log) VALUES ('" + GUILD + "', 'renamed')");
        sql(ds, "INSERT INTO guilds_alliances VALUES ('" + GUILD + "', '" + OTHER_GUILD + "')");
    }

    private static DataSource h2() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:guilds-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        return ds;
    }

    static void sql(DataSource ds, String statement) throws SQLException {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute(statement);
        }
    }

    static int intValue(DataSource ds, String query) throws SQLException {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(query)) {
            assertTrue(r.next());
            return r.getInt(1);
        }
    }
}
