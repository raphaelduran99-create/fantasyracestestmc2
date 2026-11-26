package com.fantasyraces;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

public class PlayerData {
    public String race = "", cls = "";
    public Set<String> unlocked = new HashSet<>();
    public int points = 0;
    public double xpBank = 0;
    // non sauvegardé
    public transient Map<String, Double> stats;
    public transient long[] cd = new long[2];
    public transient double drawAcc;

    public static final MapCodec<PlayerData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("race", "").forGetter(d -> d.race),
            Codec.STRING.optionalFieldOf("cls", "").forGetter(d -> d.cls),
            Codec.STRING.listOf().optionalFieldOf("unlocked", List.of()).forGetter(d -> new ArrayList<>(d.unlocked)),
            Codec.INT.optionalFieldOf("points", 0).forGetter(d -> d.points),
            Codec.DOUBLE.optionalFieldOf("xp", 0.0).forGetter(d -> d.xpBank)
    ).apply(i, (r, c, u, p, x) -> {
        PlayerData d = new PlayerData();
        d.race = r; d.cls = c; d.unlocked.addAll(u); d.points = p; d.xpBank = x;
        return d;
    }));
}
