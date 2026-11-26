package com.fantasyraces;

import com.fantasyraces.Defs.ClassDef;
import com.fantasyraces.Defs.Node;
import com.fantasyraces.Defs.RaceDef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class Gui {
    private static final Map<String, String> RACE_ICON = new HashMap<>();
    private static final Map<String, String> SKILL_ICON = new HashMap<>();

    static {
        String[] r = {"human", "player_head", "elf", "bow", "dwarf", "iron_pickaxe", "fairy", "glowstone_dust",
                "dragonkin", "dragon_head", "orc", "iron_axe", "goblin", "gold_nugget", "halfling", "bread",
                "tiefling", "nether_wart", "gnome", "redstone", "triton", "trident", "undead", "skeleton_skull",
                "lycan", "bone", "dryad", "oak_sapling", "goliath", "iron_block"};
        for (int i = 0; i < r.length; i += 2) RACE_ICON.put(r[i], r[i + 1]);
        String[] s = {"dash", "sugar", "leap", "rabbit_foot", "blink", "ender_pearl", "volley", "arrow",
                "bolt", "amethyst_shard", "drain", "spider_eye", "heal", "glistering_melon_slice", "shield", "shield",
                "cleanse", "milk_bucket", "roar", "bell", "stomp", "iron_boots", "frenzy", "blaze_powder",
                "stoneskin", "cobblestone", "smoke", "ink_sac", "sprite", "feather", "reveal", "spyglass",
                "entangle", "vine", "surge", "heart_of_the_sea", "firebreath", "fire_charge"};
        for (int i = 0; i < s.length; i += 2) SKILL_ICON.put(s[i], s[i + 1]);
    }

    public static ItemStack stack(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(path)));
    }

    public static ItemStack raceIcon(String id) { return stack(RACE_ICON.getOrDefault(id, "player_head")); }

    public static ItemStack skillIcon(String id) { return stack(SKILL_ICON.getOrDefault(id, "nether_star")); }

    private static boolean has(Set<String> k, String... keys) {
        for (String s : keys) if (k.contains(s)) return true;
        return false;
    }

    public static ItemStack nodeIcon(Node n, ClassDef c) {
        if (n.slot() >= 0) return skillIcon(c.acts()[n.slot()]);
        if (n.id().endsWith(".cap")) return stack("nether_star");
        Set<String> k = n.fx().v.keySet();
        if (has(k, "rdmg", "rspeed", "rvel")) return stack("bow");
        if (has(k, "atk", "atkspeed", "lifesteal")) return stack("iron_sword");
        if (has(k, "armor", "tough", "dmgred", "kb", "health")) return stack("iron_chestplate");
        if (has(k, "speed", "jump", "safefall")) return stack("leather_boots");
        if (has(k, "power", "cdr")) return stack("enchanted_book");
        if (has(k, "mine", "breach")) return stack("iron_pickaxe");
        if (has(k, "luck")) return stack("emerald");
        return stack("experience_bottle");
    }

    public static List<ItemStack> kit(ClassDef c) {
        List<ItemStack> out = new ArrayList<>();
        for (String e : c.kit().split(",")) {
            String[] kv = e.split(":");
            ItemStack s = stack(kv[0].trim());
            s.setCount(Integer.parseInt(kv[1].trim()));
            out.add(s);
        }
        return out;
    }

    public static String firstItem(ClassDef c) { return c.kit().split(",")[0].split(":")[0].trim(); }

    public static Fx total() {
        Fx t = new Fx();
        RaceDef r = Races.race(ClientState.race);
        if (r != null) t.addAll(r.fx());
        ClassDef c = Races.cls(ClientState.cls);
        if (c != null) for (Node n : c.nodes()) if (ClientState.unlocked.contains(n.id())) t.addAll(n.fx());
        return t;
    }

    // ---------- dessin ----------
    public static int wrap(GuiGraphics g, Font f, String s, int x, int y, int w, int color) {
        for (FormattedCharSequence l : f.split(Component.literal(s), w)) {
            g.drawString(f, l, x, y, color, false);
            y += f.lineHeight + 1;
        }
        return y;
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xCC10121A);
        g.renderOutline(x, y, w, h, 0xFF3C4256);
    }

    /** Prévisualisation du personnage (suit la souris). */
    public static void preview(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        g.fillGradient(x, y, x + w, y + h, 0xFF2E3446, 0xFF12141B);
        g.renderOutline(x, y, w, h, 0xFF3C4256);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x, y, x + w, y + h, (int) (h * 0.36), 0.0625f,
                    (float) mx, (float) my, mc.player);
        }
    }

    /** Règle de taille : trait blanc = humain standard (1,8), barre cyan = la race. */
    public static void ruler(GuiGraphics g, Font f, int x, int y, int w, int h, double blocks) {
        int bx = x + w - 10, base = y + h - 14;
        double max = 2.6;
        int usable = h - 22;
        int refY = base - (int) (usable * 1.8 / max);
        g.hLine(bx - 3, bx + 8, refY, 0x99FFFFFF);
        int bh = (int) (usable * blocks / max);
        g.fill(bx, base - bh, bx + 5, base, 0xFF55FFFF);
        g.drawString(f, String.format(Locale.ROOT, "Taille : %.2f bloc", blocks), x + 4, y + h - 11, 0xFF55FFFF, false);
    }

    /** Arbre en icônes. Renvoie le nœud survolé. */
    public static Node drawTreeIcons(GuiGraphics g, ClassDef c, int ox, int oy, int w, Set<String> unlocked, int mx, int my) {
        Map<String, int[]> pos = new HashMap<>();
        for (Node n : c.nodes()) pos.put(n.id(), new int[]{ox + 11 + (int) (n.col() / 3.0 * (w - 22)), oy + n.row() * 30 + 11});
        for (Node n : c.nodes()) {
            int[] cp = pos.get(n.id());
            for (String par : n.parents()) {
                int[] pp = pos.get(par);
                int y1 = pp[1] + 11, y2 = cp[1] - 11, mid = (y1 + y2) / 2;
                g.vLine(pp[0], y1, mid, 0xFF777777);
                g.hLine(Math.min(pp[0], cp[0]), Math.max(pp[0], cp[0]), mid, 0xFF777777);
                g.vLine(cp[0], mid, y2, 0xFF777777);
            }
        }
        Node hov = null;
        for (Node n : c.nodes()) {
            int[] p = pos.get(n.id());
            boolean un = unlocked.contains(n.id());
            g.fill(p[0] - 11, p[1] - 11, p[0] + 11, p[1] + 11, 0xFF1C1E27);
            g.renderOutline(p[0] - 11, p[1] - 11, 22, 22, un ? 0xFF55FF55 : n.slot() >= 0 ? 0xFF55FFFF : 0xFF4A4F63);
            g.renderItem(nodeIcon(n, c), p[0] - 8, p[1] - 8);
            if (mx >= p[0] - 11 && mx < p[0] + 11 && my >= p[1] - 11 && my < p[1] + 11) hov = n;
        }
        return hov;
    }

    public static List<Component> nodeTip(Node n) {
        List<Component> l = new ArrayList<>();
        l.add(Component.literal(n.name()).withStyle(net.minecraft.ChatFormatting.YELLOW));
        l.add(Component.literal("Coût : " + n.cost() + " pt").withStyle(net.minecraft.ChatFormatting.GRAY));
        l.add(Component.literal(n.fx().v.isEmpty() ? n.desc() : n.fx().describe()));
        return l;
    }

    private Gui() {}
}
