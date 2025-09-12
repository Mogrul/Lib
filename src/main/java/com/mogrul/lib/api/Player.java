package com.mogrul.lib.api;

import com.mogrul.lib.internal.Memory;
import com.mogrul.lib.internal.MogrulLib;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Player {
    public UUID uuid;
    public String username;
    public Instant firstJoined;
    public Instant lastJoined;
    public int currency;
    public int bounty;
    public String discordID;

    public boolean isConnected = false;

    public Player(
            UUID uuid,
            String username,
            Instant firstJoined,
            Instant lastJoined
    ) {
        this.uuid = uuid;
        this.username = username;
        this.firstJoined = firstJoined;
        this.lastJoined = lastJoined;
    }

    public static boolean commit(Player player) {
        return MogrulLib.commitPlayer(player);
    }

    public static boolean delete(Player player) {
        if (Memory.players.remove(player.uuid) == null) {
            MogrulLib.LOGGER.error("{} Unable to delete player {}, not found in Memory!",
                    MogrulLib.LOG_PREFIX,
                    player.uuid
            );
            return false;
        }

        return MogrulLib.deletePlayer(player);
    }

    public static Player get(UUID uuid) {
        return Memory.players.get(uuid);
    }

    public static Player get(String uuid) {
        return Memory.players.get(UUID.fromString(uuid));
    }

    public static Player getFromDiscord(String discordID) {
        return Memory.players.values().stream()
                .filter(player -> player.discordID.equals(discordID))
                .findFirst().orElse(null);
    }

    public static List<Player> getBountied() {
        return Memory.players.values().stream()
                .filter(player -> player.bounty > 0)
                .toList();
    }

    public static List<Player> getAll() {
        return (List<Player>) Memory.players.values();
    }

    public static List<Player> getOnline() {
        return Memory.players.values().stream()
                .filter(player -> player.isConnected)
                .toList();
    }
}
