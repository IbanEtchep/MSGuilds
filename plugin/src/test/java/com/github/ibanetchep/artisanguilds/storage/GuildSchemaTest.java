package com.github.ibanetchep.artisanguilds.storage;

import net.artisanmc.modules.api.database.DatabaseDialect;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildSchemaTest {

    private static final String GUILD = "6f1c1b2e-3f0a-4c55-9d7e-0a1b2c3d4e5f";
    private static final String OTHER_GUILD = "0b9e7f3c-1d2a-4e8b-a6c5-112233445566";
    private static final String RANK = "a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5";
    private static final String PLAYER = "11111111-2222-3333-4444-555555555555";

    private JdbcDataSource dataSource;
    private GuildSchema schema;

    @BeforeEach
    void setUp() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:guilds-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        schema = new GuildSchema(dataSource, DatabaseDialect.H2);
    }

    @Test
    void createsTheTablesOnAnEmptyDatabase() throws SQLException {
        GuildSchema.Result result = schema.migrate();

        assertEquals(GuildSchema.Outcome.CREATED, result.outcome());
        assertEquals(0, result.fromVersion());
        assertEquals(1, result.toVersion());
        sql("INSERT INTO guilds (id, name, owner_uuid) VALUES ('" + GUILD + "', 'Alpha', '" + PLAYER + "')");
        sql("INSERT INTO guilds_logs (guild_id, log) VALUES ('" + GUILD + "', 'created')");
        sql("INSERT INTO guilds_logs (guild_id, log) VALUES ('" + GUILD + "', 'renamed')");
        assertEquals(1, intValue("SELECT level FROM guilds"));
        assertEquals(2, intValue("SELECT COUNT(DISTINCT id) FROM guilds_logs"));
    }

    @Test
    void aSecondStartChangesNothing() throws SQLException {
        schema.migrate();

        GuildSchema.Result again = schema.migrate();

        assertEquals(GuildSchema.Outcome.UP_TO_DATE, again.outcome());
        assertEquals(1, again.toVersion());
        assertEquals(1, intValue("SELECT COUNT(*) FROM " + GuildSchema.VERSION_TABLE));
    }

    @Test
    void adoptsTheMsGuildsTablesAndAddsTheMissingLevel() throws SQLException {
        createMsGuilds11Tables(false);
        seedMsGuilds11Rows();

        GuildSchema.Result result = schema.migrate();

        assertEquals(GuildSchema.Outcome.ADOPTED, result.outcome());
        assertEquals(List.of("guilds.level"), result.addedColumns());
        assertEquals(1, result.toVersion());
        assertEquals(2, intValue("SELECT COUNT(*) FROM guilds"));
        assertEquals(1, intValue("SELECT COUNT(*) FROM guilds_ranks"));
        assertEquals(1, intValue("SELECT COUNT(*) FROM guilds_members"));
        assertEquals(1, intValue("SELECT COUNT(*) FROM guilds_logs"));
        assertEquals(1, intValue("SELECT COUNT(*) FROM guilds_alliances"));
        assertEquals(2, intValue("SELECT COUNT(*) FROM guilds WHERE level = 1"));
        assertEquals(1250, intValue("SELECT CAST(balance AS INT) FROM guilds WHERE name = 'Alpha'"));

        assertEquals(GuildSchema.Outcome.UP_TO_DATE, schema.migrate().outcome());
    }

    @Test
    void keepsTheLevelsOfADatabaseThatAlreadyHasThem() throws SQLException {
        createMsGuilds11Tables(true);
        sql("INSERT INTO guilds (id, name, level, owner_uuid) VALUES ('" + GUILD + "', 'Alpha', 4, '" + PLAYER + "')");

        GuildSchema.Result result = schema.migrate();

        assertEquals(GuildSchema.Outcome.ADOPTED, result.outcome());
        assertTrue(result.addedColumns().isEmpty());
        assertEquals(4, intValue("SELECT level FROM guilds"));
    }

    @Test
    void createsATableMsGuildsNeverCreated() throws SQLException {
        createMsGuilds11Tables(true);
        sql("DROP TABLE guilds_alliances");

        schema.migrate();

        sql("INSERT INTO guilds (id, name) VALUES ('" + GUILD + "', 'Alpha')");
        sql("INSERT INTO guilds (id, name) VALUES ('" + OTHER_GUILD + "', 'Beta')");
        sql("INSERT INTO guilds_alliances VALUES ('" + GUILD + "', '" + OTHER_GUILD + "')");
        assertEquals(1, intValue("SELECT COUNT(*) FROM guilds_alliances"));
    }

    @Test
    void refusesTheTablesOfMsGuildsBefore11AndTouchesNothing() throws SQLException {
        sql("CREATE TABLE guilds (id INT AUTO_INCREMENT PRIMARY KEY, guild_uuid VARCHAR(36), name VARCHAR(255))");
        sql("INSERT INTO guilds (guild_uuid, name) VALUES ('" + GUILD + "', 'Alpha')");

        GuildSchema.LegacySchemaException error =
                assertThrows(GuildSchema.LegacySchemaException.class, () -> schema.migrate());

        assertTrue(error.getMessage().contains("MSGuilds 1.1.1"));
        try (Connection c = dataSource.getConnection()) {
            assertFalse(GuildSchema.tableExists(c, GuildSchema.VERSION_TABLE));
            assertFalse(GuildSchema.tableExists(c, "guilds_ranks"));
        }
        assertEquals(1, intValue("SELECT COUNT(*) FROM guilds"));
    }

    /** What MSGuilds 1.1's SqlStorage creates on MariaDB, in H2's words (native UUID and JSON). */
    private void createMsGuilds11Tables(boolean withLevel) throws SQLException {
        sql("CREATE TABLE guilds (id UUID DEFAULT RANDOM_UUID() PRIMARY KEY, name VARCHAR(255) UNIQUE, "
                + "balance FLOAT DEFAULT 0, exp BIGINT DEFAULT 0, " + (withLevel ? "level INT DEFAULT 1, " : "")
                + "home VARCHAR(255), owner_uuid UUID, createdAt TIMESTAMP DEFAULT NOW())");
        sql("CREATE TABLE guilds_ranks (id UUID DEFAULT RANDOM_UUID() PRIMARY KEY, guild_id UUID, "
                + "label VARCHAR(255), rank_order INTEGER DEFAULT 0, permissions JSON, "
                + "CONSTRAINT unique_guild_rank UNIQUE (guild_id, label), "
                + "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE)");
        sql("CREATE TABLE guilds_logs (id INTEGER AUTO_INCREMENT PRIMARY KEY, guild_id UUID, log TEXT, "
                + "createdAt TIMESTAMP DEFAULT NOW(), FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE)");
        sql("CREATE TABLE guilds_members (guild_id UUID, player_uuid UUID UNIQUE, rank_id UUID, chat_mode VARCHAR(50), "
                + "PRIMARY KEY (guild_id, player_uuid), FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (rank_id) REFERENCES guilds_ranks(id))");
        sql("CREATE TABLE guilds_alliances (guild_a_id UUID, guild_b_id UUID, PRIMARY KEY (guild_a_id, guild_b_id), "
                + "FOREIGN KEY (guild_a_id) REFERENCES guilds(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (guild_b_id) REFERENCES guilds(id) ON DELETE CASCADE)");
    }

    private void seedMsGuilds11Rows() throws SQLException {
        sql("INSERT INTO guilds (id, name, balance, owner_uuid) VALUES ('" + GUILD + "', 'Alpha', 1250, '" + PLAYER + "')");
        sql("INSERT INTO guilds (id, name) VALUES ('" + OTHER_GUILD + "', 'Beta')");
        sql("INSERT INTO guilds_ranks (id, guild_id, label, rank_order, permissions) "
                + "VALUES ('" + RANK + "', '" + GUILD + "', 'Chef', 4, '[\"KICK_MEMBER\"]' FORMAT JSON)");
        sql("INSERT INTO guilds_members VALUES ('" + GUILD + "', '" + PLAYER + "', '" + RANK + "', 'PUBLIC')");
        sql("INSERT INTO guilds_logs (guild_id, log) VALUES ('" + GUILD + "', 'created')");
        sql("INSERT INTO guilds_alliances VALUES ('" + GUILD + "', '" + OTHER_GUILD + "')");
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
