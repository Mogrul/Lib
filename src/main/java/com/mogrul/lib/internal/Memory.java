package com.mogrul.lib.internal;

import com.mogrul.lib.api.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Memory {
    public static Map<UUID, Player> players = new HashMap<UUID, Player>();
}
