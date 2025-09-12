package com.mogrul.lib.internal;

import com.mogrul.lib.api.Player;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(MogrulLib.MODID)
public class MogrulLib {
    public static final String MODID = "mogrullib";
    public static final String LOG_PREFIX = "[Mogrul Lib]";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Path configModPath;
    public static Path SQLFilePath;

    public MogrulLib() {
        if (FMLEnvironment.dist.isClient()) {
            LOGGER.error("{} {} is a server-side mod, disabling functionality.", LOG_PREFIX, MODID);
        }

        NeoForge.EVENT_BUS.register(this);
    }

    public static boolean commitPlayer(Player player) {
        return SQL.updatePlayer(player);
    }

    public static boolean deletePlayer(Player player) {
        return SQL.deletePlayer(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerStarting(ServerStartingEvent event) {
        configModPath = Paths.getModConfigPath(event);
        SQLFilePath = configModPath.resolve("mogrul.db");

        if (Files.notExists(configModPath)) {
            try {
                Files.createDirectories(configModPath);
                MogrulLib.SQLFilePath = configModPath.resolve("mogrul.db");

                if (Files.notExists(SQLFilePath)) {
                    Files.createFile(SQLFilePath);
                }

            } catch (IOException e) {
                LOGGER.error("{} Unable to create Mogrul directory in config path.", LOG_PREFIX);
                configModPath = null;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerStarted(ServerStartedEvent event) {
        SQL.createConnection();
        Memory.players = SQL.getAllPlayers();
        LOGGER.info("{} Loaded {} player/s from db!", LOG_PREFIX, Memory.players.size());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerStopping(ServerStoppingEvent event) {
        SQL.closeConnection();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        Player player = Memory.players.get(event.getEntity().getUUID());
        if (player != null) {
            player.isConnected = true;
        } else {
            Player newPlayer = SQL.addPlayer(serverPlayer);
            if (newPlayer != null) {
                newPlayer.isConnected = true;
                Memory.players.put(newPlayer.uuid, newPlayer);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        Player player = Memory.players.get(serverPlayer.getUUID());
        if (player != null) {
            player.isConnected = false;
        }
    }
}
