package com.mogrul.lib.internal;

import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.nio.file.Path;

class Paths {
    static Path getModConfigPath(ServerStartingEvent event) {
           Path configPath = event.getServer().getServerDirectory().resolve("config");
           return configPath.resolve("Mogrul");
    }
}
