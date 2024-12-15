package fr.iban.guilds.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.iban.common.data.sql.DbAccess;
import fr.iban.common.teleport.SLocation;
import fr.iban.guilds.enums.ChatMode;
import fr.iban.guilds.enums.GuildPermission;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildPlayer;
import fr.iban.guilds.model.GuildRank;
import fr.iban.guilds.model.dto.GuildPlayerDTO;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

public class SqlStorage {

    private final DataSource ds = DbAccess.getDataSource();
    private final Gson gson = new Gson();

    public SqlStorage() {
        init();
    }

    private void init() {
        String[] createStatements = new String[]{
                "CREATE TABLE IF NOT EXISTS guilds(" +
                        "id UUID PRIMARY KEY DEFAULT (UUID())," +
                        "name VARCHAR(255) UNIQUE ," +
                        "balance FLOAT DEFAULT 0," +
                        "exp BIGINT DEFAULT 0," +
                        "home VARCHAR(255)," +
                        "owner_uuid UUID," +
                        "createdAt DATETIME DEFAULT NOW()" +
                        "); ",
                "CREATE TABLE IF NOT EXISTS guilds_ranks(" +
                        "id UUID PRIMARY KEY DEFAULT (UUID())," +
                        "guild_id UUID, " +
                        "label VARCHAR(255), " +
                        "rank_order INTEGER DEFAULT 0, " +
                        "permissions JSON, " +
                        "UNIQUE KEY unique_guild_rank (guild_id, label), " +
                        "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                        ");",
                "CREATE TABLE IF NOT EXISTS guilds_logs(" +
                        "id INTEGER PRIMARY KEY AUTO_INCREMENT ," +
                        "guild_id UUID ," +
                        "log TEXT," +
                        "createdAt DATETIME DEFAULT NOW()," +
                        "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                        ");",
                "CREATE TABLE IF NOT EXISTS guilds_members(" +
                        "guild_id UUID ," +
                        "player_uuid UUID UNIQUE ," +
                        "rank_id UUID," +
                        "chat_mode VARCHAR(50)," +
                        "PRIMARY KEY (guild_id, player_uuid)," +
                        "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                        "FOREIGN KEY (rank_id) REFERENCES guilds_ranks(id)" +
                        ");",
                "CREATE TABLE IF NOT EXISTS guilds_alliances(" +
                        "guild_a_id UUID," +
                        "guild_b_id UUID," +
                        "PRIMARY KEY (guild_a_id, guild_b_id)," +
                        "FOREIGN KEY (guild_a_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                        "FOREIGN KEY (guild_b_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                        ");",
        };


        try (Connection connection = ds.getConnection()) {

            for (String createStatement : createStatements) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(createStatement)) {
                    preparedStatement.executeUpdate();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Guild> getGuilds() {
        String sql = "SELECT * FROM guilds;";
        List<Guild> guilds = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        guilds.add(getGuildFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return guilds;
    }

    public List<GuildPlayerDTO> getGuildPlayerDTOs() {
        String sql = "SELECT player_uuid, gm.guild_id, chat_mode, gr.label  FROM guilds_members gm " +
                "JOIN guilds_ranks gr ON gr.id = gm.rank_id;";
        List<GuildPlayerDTO> guildPlayers = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        guildPlayers.add(getGuildPlayerDTOFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return guildPlayers;
    }

    private GuildPlayerDTO getGuildPlayerDTOFromResultSet(ResultSet resultSet) throws SQLException {
        UUID playerUuid = UUID.fromString(resultSet.getString("player_uuid"));
        UUID guildUniqueId = UUID.fromString(resultSet.getString("gm.guild_id"));
        ChatMode chatMode = ChatMode.valueOf(resultSet.getString("chat_mode"));
        String rank = resultSet.getString("gr.label");

        return new GuildPlayerDTO(playerUuid, guildUniqueId, rank, chatMode);
    }


    public Guild getGuild(UUID guildId) {
        String sql = "SELECT * FROM guilds WHERE id=?;";

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guildId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return getGuildFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Guild getGuildFromResultSet(ResultSet resultSet) throws SQLException {
        UUID guildId = UUID.fromString(resultSet.getString("id"));
        String name = resultSet.getString("name");
        double balance = resultSet.getDouble("balance");
        long exp = resultSet.getLong("exp");
        String sloc = resultSet.getString("home");
        Date createdAt = resultSet.getTimestamp("createdAt");
        UUID owner = UUID.fromString(resultSet.getString("owner_uuid"));
        Guild guild = new Guild(guildId, name, balance, exp, createdAt);

        guild.setOwnerUUID(owner);

        if (sloc != null) {
            guild.setHome(gson.fromJson(sloc, SLocation.class));
        }

        List<GuildRank> ranks = getGuildRanks(guild);
        guild.setRanks(ranks);

        return guild;
    }

    public List<GuildPlayerDTO> getGuildPlayerDTOs(UUID guildId) {
        String sql = "SELECT player_uuid, gm.guild_id, chat_mode, gr.label  FROM guilds_members gm " +
                "JOIN guilds_ranks gr ON gr.id = gm.rank_id WHERE gm.guild_id=?;";
        List<GuildPlayerDTO> guildPlayers = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guildId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        guildPlayers.add(getGuildPlayerDTOFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return guildPlayers;
    }


    public GuildPlayerDTO getGuildPlayerDto(UUID uuid) {
        String sql = "SELECT player_uuid, gm.guild_id, chat_mode, gr.label  FROM guilds_members gm " +
                "JOIN guilds_ranks gr ON gr.id = gm.rank_id WHERE player_uuid=?;";

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return getGuildPlayerDTOFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveGuild(Guild guild) {
        String updateStatement = "INSERT INTO guilds(id, balance, name, exp, home, owner_uuid) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE balance=VALUES(balance), exp=VALUES(exp), balance=VALUES(balance), home=VALUES(home)";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(updateStatement)) {
                ps.setString(1, guild.getId().toString());
                ps.setDouble(2, guild.getBalance());
                ps.setString(3, guild.getName());
                ps.setLong(4, guild.getExp());
                if (guild.getHome() == null) {
                    ps.setNull(5, Types.VARCHAR);
                } else {
                    ps.setString(5, gson.toJson(guild.getHome()));
                }
                ps.setString(6, guild.getOwnerUUID().toString());

                ps.executeUpdate();
                saveRanks(guild);
                saveAlliances(guild);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteGuild(UUID guildID) {
        String updateStatement = "DELETE FROM guilds WHERE id=?";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateStatement)) {
                preparedStatement.setString(1, guildID.toString());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveGuildPlayer(GuildPlayer guildPlayer) {
        String updateStatement = "INSERT INTO guilds_members(guild_id, player_uuid, rank_id, chat_mode) VALUES (?, ?,?, ?) "
                + "ON DUPLICATE KEY UPDATE guild_id=VALUES(guild_id), player_uuid=VALUES(player_uuid), rank_id=VALUES(rank_id), chat_mode=VALUES(chat_mode)";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateStatement)) {
                preparedStatement.setString(1, guildPlayer.getGuild().getId().toString());
                preparedStatement.setString(2, guildPlayer.getUuid().toString());
                preparedStatement.setString(3, guildPlayer.getRank().getId().toString());
                preparedStatement.setString(4, guildPlayer.getChatMode().toString());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteGuildPlayer(UUID uuid) {
        String updateStatement = "DELETE FROM guilds_members WHERE player_uuid=?";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateStatement)) {
                preparedStatement.setString(1, uuid.toString());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addLog(Guild guild, String text) {
        String insertStatement = "INSERT INTO guilds_logs(guild_id, log) VALUES (" +
                "?, ?);";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertStatement)) {
                preparedStatement.setString(1, guild.getId().toString());
                preparedStatement.setString(2, text);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getLogs(UUID guildId) {
        List<String> logs = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        String sql = "SELECT * FROM guilds_logs WHERE guild_id=? ORDER BY createdAt DESC;";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guildId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date createdAt = rs.getTimestamp("createdAt");
                        String log = rs.getString("log");
                        logs.add(dateFormat.format(createdAt) + " : " + log);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public void deleteLogs(Guild guild) {
        String insertStatement = "DELETE FROM guilds_logs WHERE guild_id=?;";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertStatement)) {
                preparedStatement.setString(1, guild.getId().toString());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<UUID> getAlliances(Guild guild) {
        String sql = "SELECT * FROM guilds_alliances " +
                "WHERE guild_a_id = ? OR guild_b_id = ?;";
        List<UUID> alliances = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guild.getId().toString());
                ps.setString(2, guild.getId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID guildAId = UUID.fromString(rs.getString("guild_a_id"));
                        UUID guildBId = UUID.fromString(rs.getString("guild_b_id"));

                        if(alliances.contains(guildAId) || alliances.contains(guildBId)) {
                            continue;
                        }

                        if (guildAId.equals(guild.getId())) {
                            alliances.add(guildBId);
                        } else {
                            alliances.add(guildAId);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return alliances;
    }

    public void saveAlliances(Guild guild) {
        String insertStatement = """
        INSERT INTO guilds_alliances(guild_a_id, guild_b_id) 
        VALUES (?, ?)
        ON DUPLICATE KEY UPDATE guild_a_id=guild_a_id
    """;

        String deleteStatement = """
        DELETE FROM guilds_alliances 
        WHERE (guild_a_id = ? AND guild_b_id NOT IN (?))
        OR (guild_b_id = ? AND guild_a_id NOT IN (?))
    """;

        try (Connection connection = ds.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // Insert/Update des alliances
                try (PreparedStatement insertPs = connection.prepareStatement(insertStatement)) {
                    for (Guild ally : guild.getAlliances()) {
                        insertPs.setString(1, guild.getId().toString());
                        insertPs.setString(2, ally.getId().toString());
                        insertPs.addBatch();
                    }
                    insertPs.executeBatch();
                }

                // Suppression des anciennes alliances
                if (!guild.getAlliances().isEmpty()) {
                    try (PreparedStatement deletePs = connection.prepareStatement(deleteStatement)) {
                        String allyIds = guild.getAlliances().stream()
                                .map(ally -> ally.getId().toString())
                                .collect(Collectors.joining("','", "'", "'"));

                        deletePs.setString(1, guild.getId().toString());
                        deletePs.setString(2, allyIds);
                        deletePs.setString(3, guild.getId().toString());
                        deletePs.setString(4, allyIds);
                        deletePs.executeUpdate();
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveRanks(Guild guild) {
        String insertStatement = "INSERT INTO guilds_ranks(id, guild_id, label, rank_order, permissions) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE guild_id=VALUES(guild_id), label=VALUES(label), rank_order=VALUES(rank_order), permissions=VALUES(permissions)";
        String deleteStatement = "DELETE FROM guilds_ranks WHERE guild_id=? AND id NOT IN (?)";

        try (Connection connection = ds.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // Insert/Update ranks
                try (PreparedStatement insertPs = connection.prepareStatement(insertStatement)) {
                    for (GuildRank rank : guild.getRanks()) {
                        insertPs.setString(1, rank.getId().toString());
                        insertPs.setString(2, guild.getId().toString());
                        insertPs.setString(3, rank.getName());
                        insertPs.setInt(4, rank.getOrder());
                        insertPs.setString(5, gson.toJson(rank.getPermissions()));
                        insertPs.addBatch();
                    }
                    insertPs.executeBatch();
                }

                // Delete ranks that no longer exist
                if (!guild.getRanks().isEmpty()) {
                    try (PreparedStatement deletePs = connection.prepareStatement(deleteStatement)) {
                        deletePs.setString(1, guild.getId().toString());
                        String rankIds = guild.getRanks().stream()
                                .map(rank -> rank.getId().toString())
                                .collect(Collectors.joining("','", "'", "'"));
                        deletePs.setString(2, rankIds);
                        deletePs.executeUpdate();
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<GuildRank> getGuildRanks(Guild guild) {
        String sql = "SELECT * FROM guilds_ranks WHERE guild_id=? ORDER BY rank_order;";
        List<GuildRank> ranks = new ArrayList<>();
        Type type = new TypeToken<Set<GuildPermission>>() {}.getType();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guild.getId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ranks.add(new GuildRank(
                                UUID.fromString(rs.getString("id")),
                                rs.getString("label"),
                                rs.getInt("rank_order"),
                                gson.fromJson(rs.getString("permissions"), type)
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ranks;
    }

}
