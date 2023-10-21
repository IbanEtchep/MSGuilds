package fr.iban.guilds.storage;

import com.google.gson.Gson;
import fr.iban.common.data.sql.DbAccess;
import fr.iban.common.teleport.SLocation;
import fr.iban.guilds.Guild;
import fr.iban.guilds.GuildPlayer;
import fr.iban.guilds.enums.ChatMode;
import fr.iban.guilds.enums.Rank;

import javax.sql.DataSource;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SqlStorage {

    private final DataSource ds = DbAccess.getDataSource();
    private final Gson gson = new Gson();

    public SqlStorage() {
        init();
    }

    private void init() {
        String[] createStatements = new String[]{
                "CREATE TABLE IF NOT EXISTS guilds(" +
                        "id INTEGER PRIMARY KEY AUTO_INCREMENT ," +
                        "guild_uuid VARCHAR(36) UNIQUE ," +
                        "name VARCHAR(255) UNIQUE ," +
                        "balance FLOAT DEFAULT 0," +
                        "exp BIGINT DEFAULT 0," +
                        "home VARCHAR(255)," +
                        "createdAt DATETIME DEFAULT NOW()" +
                        "); ",
                "CREATE TABLE IF NOT EXISTS guilds_ranks(" +
                        "id INTEGER PRIMARY KEY KEY AUTO_INCREMENT, " +
                        "label VARCHAR(255) UNIQUE" +
                        ");",
                "CREATE TABLE IF NOT EXISTS guilds_chatmode(" +
                        "id INTEGER PRIMARY KEY KEY AUTO_INCREMENT," +
                        "label VARCHAR(255) UNIQUE" +
                        ");",
                "CREATE TABLE IF NOT EXISTS guilds_logs(" +
                        "id INTEGER PRIMARY KEY AUTO_INCREMENT ," +
                        "guild_id INTEGER ," +
                        "log TEXT," +
                        "createdAt DATETIME DEFAULT NOW()," +
                        "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                        ");",
                "CREATE TABLE IF NOT EXISTS guilds_members(" +
                        "guild_id INTEGER ," +
                        "player_uuid VARCHAR(36) UNIQUE ," +
                        "rank_id INTEGER ," +
                        "chatmode_id INTEGER," +
                        "PRIMARY KEY (guild_id, player_uuid)," +
                        "FOREIGN KEY (guild_id) REFERENCES guilds(id)," +
                        "FOREIGN KEY (rank_id) REFERENCES guilds_ranks(id)," +
                        "FOREIGN KEY (chatmode_id) REFERENCES guilds_chatmode(id)" +
                        ");",
                "CREATE TABLE IF NOT EXISTS guilds_alliances(" +
                        "guild_a_id INTEGER ," +
                        "guild_b_id INTEGER ," +
                        "PRIMARY KEY (guild_a_id, guild_b_id)," +
                        "FOREIGN KEY (guild_a_id) REFERENCES guilds(id)," +
                        "FOREIGN KEY (guild_b_id) REFERENCES guilds(id)" +
                        ");",

        };

        String addRankStatement = "INSERT INTO guilds_ranks(label) VALUES (?) ON DUPLICATE KEY UPDATE label=VALUES(label)";
        String addChatModeStatement = "INSERT INTO guilds_chatmode(label) VALUES (?) ON DUPLICATE KEY UPDATE label=VALUES(label)";

        try (Connection connection = ds.getConnection()) {

            for (String createStatement : createStatements) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(createStatement)) {
                    preparedStatement.executeUpdate();
                }
            }

            for (Rank rank : Rank.values()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(addRankStatement)) {
                    preparedStatement.setString(1, rank.toString());
                    preparedStatement.executeUpdate();
                }
            }

            for (ChatMode chatMode : ChatMode.values()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(addChatModeStatement)) {
                    preparedStatement.setString(1, chatMode.toString());
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
                        UUID guildId = UUID.fromString(rs.getString("guild_uuid"));
                        String name = rs.getString("name");
                        double balance = rs.getDouble("balance");
                        long exp = rs.getLong("exp");
                        String sloc = rs.getString("home");
                        Date createdAt = rs.getTimestamp("createdAt");
                        Guild guild = new Guild(guildId, name, balance, exp, createdAt);
                        if (sloc != null) {
                            guild.setHome(gson.fromJson(sloc, SLocation.class));
                        }

                        guilds.add(guild);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return guilds;
    }

    public List<GuildPlayer> getGuildPlayers() {
        String sql = "SELECT player_uuid, guild_uuid, gc.label, gr.label  FROM guilds_members gm " +
                "JOIN guilds_ranks gr ON gr.id = gm.rank_id " +
                "JOIN guilds_chatmode gc ON gm.chatmode_id = gc.id " +
                "JOIN guilds g ON g.id = gm.guild_id;";
        List<GuildPlayer> guildPlayers = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                getGuildPlayersFromPreparedStatement(guildPlayers, ps);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return guildPlayers;
    }

    private void getGuildPlayersFromPreparedStatement(List<GuildPlayer> guildPlayers, PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                UUID guildUUid = UUID.fromString(rs.getString("guild_uuid"));
                ChatMode chatMode = ChatMode.valueOf(rs.getString("gc.label"));
                Rank rank = Rank.valueOf(rs.getString("gr.label"));

                GuildPlayer guildPlayer = new GuildPlayer(playerUuid, guildUUid, rank, chatMode);
                guildPlayers.add(guildPlayer);
            }
        }
    }

    public Guild getGuild(UUID guildId) {
        String sql = "SELECT * FROM guilds WHERE guild_uuid=?;";

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guildId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String name = rs.getString("name");
                        double balance = rs.getDouble("balance");
                        long exp = rs.getLong("exp");
                        String sloc = rs.getString("home");
                        Date createdAt = rs.getTimestamp("createdAt");
                        Guild guild = new Guild(guildId, name, balance, exp, createdAt);
                        if (sloc != null) {
                            guild.setHome(gson.fromJson(sloc, SLocation.class));
                        }

                        return guild;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<GuildPlayer> getGuildMembers(UUID guildId) {
        String sql = "SELECT player_uuid, guild_uuid, gc.label, gr.label  FROM guilds_members gm " +
                "JOIN guilds_ranks gr ON gr.id = gm.rank_id " +
                "JOIN guilds_chatmode gc ON gm.chatmode_id = gc.id " +
                "JOIN guilds g on g.id = gm.guild_id WHERE guild_uuid=?;";
        List<GuildPlayer> guildPlayers = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guildId.toString());
                getGuildPlayersFromPreparedStatement(guildPlayers, ps);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return guildPlayers;
    }


    public GuildPlayer getGuildPlayer(UUID uuid) {
        String sql = "SELECT player_uuid, guild_uuid, gc.label, gr.label  FROM guilds_members gm " +
                "JOIN guilds_ranks gr ON gr.id = gm.rank_id " +
                "JOIN guilds_chatmode gc ON gm.chatmode_id = gc.id " +
                "JOIN guilds g on g.id = gm.guild_id WHERE player_uuid=?;";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                        UUID guildUUid = UUID.fromString(rs.getString("guild_uuid"));
                        ChatMode chatMode = ChatMode.valueOf(rs.getString("gc.label"));
                        Rank rank = Rank.valueOf(rs.getString("gr.label"));

                        return new GuildPlayer(playerUuid, guildUUid, rank, chatMode);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveGuild(Guild guild) {
        String updateStatement = "INSERT INTO guilds(guild_uuid, balance, name, exp, home) VALUES (?, ?, ?, ?, ?) " +
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
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteGuild(UUID guildID) {
        String updateStatement = "DELETE FROM guilds WHERE guild_uuid=?";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateStatement)) {
                preparedStatement.setString(1, guildID.toString());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double getBalance(UUID guildId) {
        String sql = "SELECT balance FROM guilds WHERE guild_uuid=?";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guildId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setBalance(UUID guildId, double balance) {
        String updateStatement = "UPDATE guilds SET balance=? WHERE guild_uuid=?";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateStatement)) {
                preparedStatement.setDouble(1, balance);
                preparedStatement.setString(2, guildId.toString());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveGuildPlayer(GuildPlayer guildPlayer) {
        String updateStatement = "INSERT INTO guilds_members(guild_id, player_uuid, rank_id, chatmode_id) VALUES (" +
                "(SELECT id FROM guilds WHERE guild_uuid=?), ?," +
                " (SELECT id FROM guilds_ranks WHERE label=?)," +
                " (SELECT id FROM guilds_chatmode WHERE label=?)) " +
                "ON DUPLICATE KEY UPDATE guild_id=VALUES(guild_id), player_uuid=VALUES(player_uuid), rank_id=VALUES(rank_id), chatmode_id=VALUES(chatmode_id)";
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateStatement)) {
                preparedStatement.setString(1, guildPlayer.getGuildId().toString());
                preparedStatement.setString(2, guildPlayer.getUuid().toString());
                preparedStatement.setString(3, guildPlayer.getRank().toString());
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
                "(SELECT id FROM guilds WHERE guild_uuid=?), ?);";
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
        String sql = "SELECT * FROM guilds_logs WHERE guild_id=(SELECT id FROM guilds WHERE guild_uuid=?) ORDER BY createdAt DESC;";
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
        String insertStatement = "DELETE FROM guilds_logs WHERE guild_id=(SELECT id FROM guilds WHERE guild_uuid=?);";
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
        String sql = "SELECT guilds_a.guild_uuid AS guild_a_uuid, guilds_b.guild_uuid AS guild_b_uuid " +
                "FROM guilds_alliances " +
                "JOIN guilds AS guilds_a ON guilds_alliances.guild_a_id = guilds_a.id " +
                "JOIN guilds AS guilds_b ON guilds_alliances.guild_b_id = guilds_b.id " +
                "WHERE guilds_a.guild_uuid = ? OR guilds_b.guild_uuid = ?;";
        List<UUID> alliances = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guild.getId().toString());
                ps.setString(2, guild.getId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID guildAId = UUID.fromString(rs.getString("guild_a_uuid"));
                        UUID guildBId = UUID.fromString(rs.getString("guild_b_uuid"));

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

    public void addAlliance(Guild guild, Guild ally) {
        String sql = "INSERT INTO guilds_alliances(guild_a_id, guild_b_id) VALUES (" +
                "(SELECT id FROM guilds WHERE guild_uuid=?)," +
                "(SELECT id FROM guilds WHERE guild_uuid=?));";

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guild.getId().toString());
                ps.setString(2, ally.getId().toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeAlliance(Guild guild, Guild ally) {
        String sql = "DELETE FROM guilds_alliances WHERE guild_a_id=(SELECT id FROM guilds WHERE guild_uuid=?) AND guild_b_id=(SELECT id FROM guilds WHERE guild_uuid=?);";

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, guild.getId().toString());
                ps.setString(2, ally.getId().toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
