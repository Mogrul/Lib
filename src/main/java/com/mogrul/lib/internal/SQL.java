package com.mogrul.lib.internal;

import com.mogrul.lib.api.Player;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.level.PistonEvent;

import java.sql.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


class SQL {
    private static Connection connection;

    static void createConnection() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + MogrulLib.SQLFilePath.toAbsolutePath()
            );

            MogrulLib.LOGGER.info("{} SQL connection created for {}",
                    MogrulLib.LOG_PREFIX,
                    MogrulLib.SQLFilePath.toAbsolutePath()
            );

            enableForeignKeys();
            createDefaultTables();

        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Failed to create SQL connection for {}\n{}",
                    MogrulLib.LOG_PREFIX,
                    MogrulLib.SQLFilePath.toAbsolutePath(),
                    e.getMessage()
            );
        }
    }

    static void closeConnection() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Unable to close SQL connection for {}\n{}",
                    MogrulLib.LOG_PREFIX,
                    MogrulLib.SQLFilePath.toAbsolutePath(),
                    e.getMessage()
            );
        }
    }

    static Map<UUID, Player> getAllPlayers() {
        Map<UUID, Player> players = new HashMap<>();
        String sql = "SELECT p.*, c.*, d.* " +
                "FROM players p " +
                "LEFT JOIN currency c ON p.uuid = c.uuid " +
                "LEFT JOIN discord d ON p.uuid = d.uuid;";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String username = rs.getString("username");
                Instant firstJoined = Instant.ofEpochMilli(rs.getLong("first_joined"));
                Instant lastJoined = Instant.ofEpochMilli(rs.getLong("last_joined"));

                String discordID = rs.getString("discord_id");

                int currency = rs.getInt("currency");
                int bounty = rs.getInt("bounty");

                Player player = new Player(
                        uuid,
                        username,
                        firstJoined,
                        lastJoined
                );
                player.currency = currency;
                player.bounty = bounty;
                player.discordID = discordID;

                players.put(uuid, player);
            }
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Failed to fetch all players in SQL connection for {}\n{}",
                    MogrulLib.LOG_PREFIX,
                    MogrulLib.SQLFilePath.toAbsolutePath(),
                    e.getMessage()
            );
        }

        return players;
    }

    static Player addPlayer(ServerPlayer serverPlayer) {
        UUID uuid = serverPlayer.getUUID();
        String username = serverPlayer.getScoreboardName();
        Instant firstJoined = Instant.now();
        Instant lastJoined = Instant.now();

        boolean success = false;

        String sql = "INSERT INTO players (uuid, username, first_joined, last_joined) " +
                "VALUES (?, ?, ?, ?);";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setLong(3, firstJoined.toEpochMilli());
            ps.setLong(4, lastJoined.toEpochMilli());

            ps.executeUpdate();
            success = true;

        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Failed to add player {} to db!\n{}",
                    MogrulLib.LOG_PREFIX,
                    username,
                    e.getMessage()
            );
        }

        if (success) {
            Player player = new Player(
                    uuid,
                    username,
                    firstJoined,
                    lastJoined
            );
            player.currency = 0;
            player.bounty = 0;

            return player;
        } else {
            return null;
        }
    }

    static boolean updatePlayer(Player player) {
        if (!isConnected()) return false;

        String playersSQL = "UPDATE players SET username = ?, first_joined = ?, last_joined = ? WHERE uuid = ?;";
        String currencySQL = "UPDATE currency SET currency = ?, bounty = ? WHERE uuid = ?;";
        String discordSQL = "UPDATE discord SET discord_id = ? WHERE uuid = ?;";

        int rowsPlayers = 0;
        int rowsCurrency = 0;
        int rowsDiscord = 0;

        try (PreparedStatement ps = connection.prepareStatement(playersSQL)) {
            ps.setString(1, player.username);
            ps.setLong(2, player.firstJoined.toEpochMilli());
            ps.setLong(3, player.lastJoined.toEpochMilli());

            rowsPlayers = ps.executeUpdate();
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Failed to update player in players {}\n{}",
                    MogrulLib.LOG_PREFIX,
                    player.uuid,
                    e.getMessage()
            );
        }

        try (PreparedStatement ps = connection.prepareStatement(currencySQL)) {
            ps.setInt(1, player.currency);
            ps.setInt(2, player.bounty);

            rowsCurrency = ps.executeUpdate();
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Failed to update currency in currency {}\n{}",
                    MogrulLib.LOG_PREFIX,
                    player.uuid,
                    e.getMessage()
            );
        }

        try (PreparedStatement ps = connection.prepareStatement(discordSQL)) {
            ps.setString(1, player.discordID);

            rowsDiscord = ps.executeUpdate();
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Failed to update discord in discod {}\n{}",
                    MogrulLib.LOG_PREFIX,
                    player.uuid,
                    e.getMessage()
            );
        }

        return (rowsPlayers > 0 || rowsCurrency > 0 || rowsDiscord > 0);
    }

    static boolean deletePlayer(Player player) {
        String sql = "DELETE FROM players WHERE uuid = ?;";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.uuid.toString());

            if (ps.executeUpdate() > 0) return true;
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Failed to delete player in players {}\n{}",
                    MogrulLib.LOG_PREFIX,
                    player.uuid,
                    e.getMessage()
            );
        }

        return false;
    }

    private static void enableForeignKeys() {
        if (!isConnected()) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("[{}] Failed to enable foreign keys on SQL connection!\n{}",
                    MogrulLib.LOG_PREFIX,
                    e.getMessage()
            );
        }
    }

    private static void createDefaultTables() {
        if (!isConnected()) return;
        createPlayerTable();
        createCurrencyTable();
        createDiscordTable();
        createDefaultInsertTrigger();
    }

    private static void createPlayerTable() {
        if (!isConnected()) return;
        String sql = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid TEXT PRIMARY KEY, " +
                "username TEXT NOT NULL, " +
                "first_joined INTEGER NOT NULL, " +
                "last_joined INTEGER NOT NULL" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Failed to create player table!\n{}", MogrulLib.LOG_PREFIX, e.getMessage());
        }
    }

    private static void createCurrencyTable() {
        if (!isConnected()) return;
        String sql = "CREATE TABLE IF NOT EXISTS currency (" +
                "uuid TEXT PRIMARY KEY, " +
                "currency INTEGER DEFAULT 0, " +
                "bounty INTEGER DEFAULT 0, " +
                "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Failed to create currency table!\n{}", MogrulLib.LOG_PREFIX, e.getMessage());
        }
    }

    private static void createDiscordTable() {
        if (!isConnected()) return;
        String sql = "CREATE TABLE IF NOT EXISTS discord (" +
                "uuid TEXT PRIMARY KEY, " +
                "discord_id TEXT, " +
                "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Failed to create discord table!\n{}", MogrulLib.LOG_PREFIX, e.getMessage());
        }
    }

    private static void createDefaultInsertTrigger() {
        if (!isConnected()) return;
        String sql = "CREATE TRIGGER IF NOT EXISTS insert_defaults_after_player " +
                "AFTER INSERT ON players " +
                "FOR EACH ROW " +
                "BEGIN " +
                    "INSERT INTO currency (uuid) VALUES (NEW.uuid); " +
                    "INSERT INTO discord (uuid) VALUES (NEW.uuid); " +
                "END;";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            MogrulLib.LOGGER.error("{} Couldn't create default insert trigger!\n{}", MogrulLib.LOG_PREFIX, e.getMessage());
        }
    }

    private static boolean isConnected() {
        boolean connected = (connection != null);

        if (!connected) {
            MogrulLib.LOGGER.error("{} Failed to connect to SQL, connection is null!", MogrulLib.LOG_PREFIX);
        }

        return connected;
    }
}
