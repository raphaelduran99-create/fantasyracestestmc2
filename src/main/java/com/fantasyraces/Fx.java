package com.fantasyraces;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.*;

/** Ensemble d'effets : attributs Minecraft + stats custom, écrit sous forme "cle=val;cle=val". */
public final class Fx {
    public record Def(String label, Holder<Attribute> attr, AttributeModifier.Operation op, boolean pct) {}

    public static final Map<String, Def> ATTR = new LinkedHashMap<>();
    public static final Map<String, String> STAT = new LinkedHashMap<>();

    private static void a(String k, String label, Holder<Attribute> at, AttributeModifier.Operation op, boolean pct) {
        ATTR.put(k, new Def(label, at, op, pct));
    }

    static {
        var ADD = AttributeModifier.Operation.ADD_VALUE;
        var MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        a("health", "Vie max", Attributes.MAX_HEALTH, ADD, false);
        a("armor", "Armure", Attributes.ARMOR, ADD, false);
        a("tough", "Robustesse", Attributes.ARMOR_TOUGHNESS, ADD, false);
        a("atk", "Dégâts de mêlée", Attributes.ATTACK_DAMAGE, ADD, false);
        a("atkspeed", "Vitesse d'attaque", Attributes.ATTACK_SPEED, MUL, true);
        a("speed", "Vitesse", Attributes.MOVEMENT_SPEED, MUL, true);
        a("jump", "Saut", Attributes.JUMP_STRENGTH, MUL, true);
        a("safefall", "Chute sans dégâts", Attributes.SAFE_FALL_DISTANCE, ADD, false);
        a("scale", "Taille", Attributes.SCALE, MUL, true);
        a("step", "Hauteur de pas", Attributes.STEP_HEIGHT, ADD, false);
        a("mine", "Vitesse de minage", Attributes.BLOCK_BREAK_SPEED, MUL, true);
        a("reach", "Allonge (combat)", Attributes.ENTITY_INTERACTION_RANGE, ADD, false);
        a("breach", "Allonge (blocs)", Attributes.BLOCK_INTERACTION_RANGE, ADD, false);
        a("oxygen", "Respiration", Attributes.OXYGEN_BONUS, ADD, false);
        a("swim", "Nage", Attributes.WATER_MOVEMENT_EFFICIENCY, ADD, true);
        a("subm", "Minage sous l'eau", Attributes.SUBMERGED_MINING_SPEED, MUL, true);
        a("kb", "Résistance au recul", Attributes.KNOCKBACK_RESISTANCE, ADD, true);
        a("luck", "Chance", Attributes.LUCK, ADD, false);
        a("gravity", "Gravité", Attributes.GRAVITY, MUL, true);

        STAT.put("rdmg", "Dégâts à distance");
        STAT.put("rspeed", "Vitesse de tir");
        STAT.put("rvel", "Portée de tir");
        STAT.put("lifesteal", "Vol de vie");
        STAT.put("cdr", "Réduction de recharge");
        STAT.put("power", "Puissance des compétences");
        STAT.put("dmgred", "Réduction des dégâts");
        STAT.put("learn", "Gain de points");
    }

    public final Map<String, Double> v = new LinkedHashMap<>();

    public static Fx parse(String s) {
        Fx f = new Fx();
        if (s == null || s.isBlank()) return f;
        for (String p : s.split(";")) {
            String[] kv = p.split("=");
            if (kv.length != 2) continue;
            String k = kv[0].trim();
            if (!ATTR.containsKey(k) && !STAT.containsKey(k)) {
                FantasyRaces.LOGGER.warn("Effet inconnu ignoré : {}", k);
                continue;
            }
            f.v.put(k, Double.parseDouble(kv[1].trim()));
        }
        return f;
    }

    public Fx scaled(double k) {
        Fx f = new Fx();
        v.forEach((a, b) -> f.v.put(a, b * k));
        return f;
    }

    public void apply(ServerPlayer p, String src) {
        for (var e : v.entrySet()) {
            Def d = ATTR.get(e.getKey());
            if (d == null) continue;
            AttributeInstance in = p.getAttribute(d.attr());
            if (in == null) continue;
            in.addOrUpdateTransientModifier(new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(FantasyRaces.MODID, src + "/" + e.getKey()),
                    e.getValue(), d.op()));
        }
    }

    public void addStats(Map<String, Double> st) {
        for (var e : v.entrySet()) if (STAT.containsKey(e.getKey())) st.merge(e.getKey(), e.getValue(), Double::sum);
    }

    public static void clearAll(ServerPlayer p) {
        for (Def d : ATTR.values()) {
            AttributeInstance in = p.getAttribute(d.attr());
            if (in == null) continue;
            for (AttributeModifier m : List.copyOf(in.getModifiers()))
                if (m.id().getNamespace().equals(FantasyRaces.MODID)) in.removeModifier(m.id());
        }
    }

    public String describe() {
        StringJoiner j = new StringJoiner(", ");
        for (var e : v.entrySet()) {
            Def d = ATTR.get(e.getKey());
            String label = d != null ? d.label() : STAT.getOrDefault(e.getKey(), e.getKey());
            boolean pct = d == null || d.pct();
            double x = e.getValue();
            j.add(label + " " + (pct ? String.format(Locale.ROOT, "%+d%%", Math.round(x * 100)) : fmt(x)));
        }
        return j.toString();
    }

    private static String fmt(double x) {
        String s = x == Math.floor(x) ? String.valueOf((long) x) : String.format(Locale.ROOT, "%.1f", x);
        return x > 0 ? "+" + s : s;
    }
}
