package com.fantasyraces;

import com.fantasyraces.Defs.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class Logic {
    public static PlayerData data(Player p) { return p.getData(FantasyRaces.DATA); }

    public static double stat(PlayerData d, String k) {
        return d.stats == null ? 0 : d.stats.getOrDefault(k, 0.0);
    }

    public static void msg(ServerPlayer p, String s) {
        p.displayClientMessage(Component.literal(s).withStyle(ChatFormatting.AQUA), true);
    }

    public static void recompute(ServerPlayer p) {
        PlayerData d = data(p);
        Fx.clearAll(p);
        Map<String, Double> st = new HashMap<>();
        RaceDef r = Races.race(d.race);
        if (r != null) { r.fx().apply(p, "race"); r.fx().addStats(st); }
        ClassDef c = Races.cls(d.cls);
        if (c != null) {
            for (Node n : c.nodes()) {
                if (d.unlocked.contains(n.id())) { n.fx().apply(p, n.id()); n.fx().addStats(st); }
            }
        }
        d.stats = st;
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    public static void sync(ServerPlayer p) {
        PlayerData d = data(p);
        PacketDistributor.sendToPlayer(p, new Net.Sync(d.race, d.cls, new ArrayList<>(d.unlocked), d.points, (int) d.xpBank));
    }

    public static void chooseRace(ServerPlayer p, String id) {
        PlayerData d = data(p);
        if (!d.race.isEmpty() || Races.race(id) == null) return;
        d.race = id;
        recompute(p);
        sync(p);
        msg(p, "Race choisie : " + Races.race(id).name());
    }

    public static void chooseClass(ServerPlayer p, String id) {
        PlayerData d = data(p);
        ClassDef c = Races.cls(id);
        if (c == null || d.race.isEmpty() || !d.cls.isEmpty() || !c.race().equals(d.race)) return;
        d.cls = id;
        d.points += 3;
        d.unlocked.clear();
        giveKit(p, c);
        recompute(p);
        sync(p);
        msg(p, "Classe : " + c.name() + " — touche K pour l'arbre de compétences");
    }

    public static void unlock(ServerPlayer p, String nodeId) {
        PlayerData d = data(p);
        ClassDef c = Races.cls(d.cls);
        if (c == null || d.unlocked.contains(nodeId)) return;
        for (Node n : c.nodes()) {
            if (!n.id().equals(nodeId)) continue;
            if (d.points < n.cost() || !d.unlocked.containsAll(n.parents())) return;
            d.points -= n.cost();
            d.unlocked.add(nodeId);
            recompute(p);
            sync(p);
            return;
        }
    }

    public static void respec(ServerPlayer p) {
        PlayerData d = data(p);
        ClassDef c = Races.cls(d.cls);
        if (c == null) return;
        for (Node n : c.nodes()) if (d.unlocked.contains(n.id())) d.points += n.cost();
        d.unlocked.clear();
        recompute(p);
        sync(p);
        msg(p, "Compétences réinitialisées, points rendus.");
    }

    public static void reset(ServerPlayer p) {
        PlayerData d = data(p);
        d.race = ""; d.cls = ""; d.points = 0; d.xpBank = 0; d.unlocked.clear();
        recompute(p);
        sync(p);
        msg(p, "Race et classe réinitialisées : rouvre le menu (K).");
    }

    public static void useSkill(ServerPlayer p, int slot) {
        PlayerData d = data(p);
        ClassDef c = Races.cls(d.cls);
        if (c == null || slot < 0 || slot > 1) return;
        if (!d.unlocked.contains(c.id() + ".act" + (slot + 1))) { msg(p, "Compétence non débloquée (touche K)."); return; }
        Actives.Active a = Actives.ALL.get(c.acts()[slot]);
        long now = p.level().getGameTime();
        if (now < d.cd[slot]) { msg(p, a.name() + " : encore " + ((d.cd[slot] - now) / 20 + 1) + " s"); return; }
        double pw = 1 + stat(d, "power");
        if (a.run().run(p, pw)) {
            double f = Math.max(0.4, 1 - stat(d, "cdr"));
            d.cd[slot] = now + (long) (a.cd() * f);
            msg(p, a.name() + " !");
        }
    }

    private static void giveKit(ServerPlayer p, ClassDef c) {
        for (String e : c.kit().split(",")) {
            String[] kv = e.split(":");
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(kv[0].trim()));
            ItemStack s = new ItemStack(item, Integer.parseInt(kv[1].trim()));
            if (item instanceof ArmorItem a && p.getItemBySlot(a.getEquipmentSlot()).isEmpty()) {
                p.setItemSlot(a.getEquipmentSlot(), s);
            } else if (item instanceof ShieldItem && p.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
                p.setItemSlot(EquipmentSlot.OFFHAND, s);
            } else if (!p.getInventory().add(s)) {
                p.drop(s, false);
            }
        }
    }

    private Logic() {}
}
