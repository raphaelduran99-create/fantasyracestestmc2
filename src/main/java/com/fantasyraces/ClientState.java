package com.fantasyraces;

import java.util.HashSet;
import java.util.Set;

/** État reçu du serveur (classe neutre, sans référence au code client). */
public final class ClientState {
    public static String race = "", cls = "";
    public static Set<String> unlocked = new HashSet<>();
    public static int points, xp, version;
    public static boolean synced, changed;

    public static void set(Net.Sync s) {
        race = s.race();
        cls = s.cls();
        unlocked = new HashSet<>(s.unlocked());
        points = s.points();
        xp = s.xp();
        version++;
        synced = true;
        changed = true;
    }

    private ClientState() {}
}
