package com.github.ibanetchep.artisanguilds.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The guild chest's rows: one per (guild, page) holding items, the page's contents as
 * {@code ItemStack.serializeItemsAsBytes} wrote them. An emptied page has no row. Blocking: the
 * writes run on the store's writer.
 */
public final class GuildChestRepository {

    private final DataSource dataSource;
    private final GuildTables t;

    public GuildChestRepository(DataSource dataSource, GuildTables tables) {
        this.dataSource = dataSource;
        this.t = tables;
    }

    /** Every stored page, by guild then page number. */
    public Map<UUID, Map<Integer, byte[]>> loadAll() throws SQLException {
        Map<UUID, Map<Integer, byte[]>> chests = new HashMap<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT guild_id, page, contents FROM " + t.chests());
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                chests.computeIfAbsent(UUID.fromString(r.getString("guild_id")), k -> new HashMap<>())
                        .put(r.getInt("page"), r.getBytes("contents"));
            }
        }
        return chests;
    }

    /** Whether the guild has a stored page: asked by a server where the chest is disabled. */
    public boolean hasItems(UUID guildId) throws SQLException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT 1 FROM " + t.chests() + " WHERE guild_id = ?")) {
            p.setString(1, guildId.toString());
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    /** Replaces the page; empty contents ({@code null}) delete it. One statement each, H2 and MySQL alike. */
    public void save(UUID guildId, int page, byte[] contents) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                delete(c, guildId, page);
                if (contents != null) {
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO " + t.chests() + " (guild_id, page, contents) VALUES (?, ?, ?)")) {
                        p.setString(1, guildId.toString());
                        p.setInt(2, page);
                        p.setBytes(3, contents);
                        p.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        }
    }

    private void delete(Connection c, UUID guildId, int page) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("DELETE FROM " + t.chests() + " WHERE guild_id = ? AND page = ?")) {
            p.setString(1, guildId.toString());
            p.setInt(2, page);
            p.executeUpdate();
        }
    }
}
