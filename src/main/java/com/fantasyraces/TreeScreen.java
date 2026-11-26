package com.fantasyraces;

import com.fantasyraces.Defs.ClassDef;
import com.fantasyraces.Defs.Node;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeScreen extends Screen {
    public int version;
    private final Map<String, int[]> pos = new HashMap<>();
    private int tw, panelY, leftW;

    public TreeScreen() {
        super(Component.literal("Arbre de compétences"));
        version = ClientState.version;
    }

    public void refresh() {
        version = ClientState.version;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        pos.clear();
        ClassDef c = Races.cls(ClientState.cls);
        if (c == null) return;
        leftW = width >= 620 ? 140 : 0;
        int areaX = leftW > 0 ? leftW + 12 : 6, areaW = width - areaX - 6;
        tw = Math.min(118, (areaW - 12) / 4);
        panelY = 176;
        for (Node n : c.nodes()) {
            int x = areaX + (int) (n.col() / 3.0 * (areaW - tw)), y = 38 + n.row() * 46;
            pos.put(n.id(), new int[]{x, y});
            boolean un = ClientState.unlocked.contains(n.id());
            boolean can = !un && ClientState.points >= n.cost() && ClientState.unlocked.containsAll(n.parents());
            String sub = un ? "Débloqué" : n.cost() + " pt" + (n.slot() >= 0 ? " - compétence" : "");
            Tile t = new Tile(x, y, tw, 32, n.name(), sub, Gui.nodeIcon(n, c), () -> {
                if (can) PacketDistributor.sendToServer(new Net.Unlock(n.id()));
            });
            t.border = un ? 0xFF55FF55 : can ? 0xFFFFFF55 : 0xFF4A4F63;
            t.textColor = un ? 0xFF55FF55 : can ? 0xFFFFFFFF : 0xFF888888;
            t.dim = !un && !can;
            t.setTooltip(Tooltip.create(Component.literal(String.join("\n", tipLines(n, un, can)))));
            addRenderableWidget(t);
        }
    }

    private List<String> tipLines(Node n, boolean un, boolean can) {
        return List.of(n.name(), "Coût : " + n.cost() + " pt",
                n.fx().v.isEmpty() ? n.desc() : n.fx().describe(),
                un ? "[Débloqué]" : can ? "[Clic pour débloquer]" : "[Verrouillé : points ou prérequis manquants]");
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        ClassDef c = Races.cls(ClientState.cls);
        if (c == null) return;
        Gui.panel(g, 6, panelY, width - 12, height - panelY - 6);
        for (Node n : c.nodes()) {
            int[] cp = pos.get(n.id());
            if (cp == null) continue;
            for (String par : n.parents()) {
                int[] pp = pos.get(par);
                if (pp == null) continue;
                int x1 = pp[0] + tw / 2, y1 = pp[1] + 32, x2 = cp[0] + tw / 2, y2 = cp[1], m = (y1 + y2) / 2;
                int col = ClientState.unlocked.contains(par) ? 0xFF55FF55 : 0xFF777777;
                g.vLine(x1, y1, m, col);
                g.hLine(Math.min(x1, x2), Math.max(x1, x2), m, col);
                g.vLine(x2, m, y2, col);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        ClassDef c = Races.cls(ClientState.cls);
        if (c == null) return;
        String rn = Races.race(ClientState.race) != null ? Races.race(ClientState.race).name() : "";
        g.drawCenteredString(font, Component.literal(rn + " — " + c.name()).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), width / 2, 6, 0xFFFFFFFF);

        // points + barre de progression vers le prochain point
        g.renderItem(Gui.stack("experience_bottle"), width / 2 - 92, 15);
        g.drawString(font, "Points : " + ClientState.points, width / 2 - 72, 20, 0xFFFFFFFF, false);
        int bx = width / 2 + 0, bw = 90;
        g.fill(bx, 21, bx + bw, 27, 0xFF1C1E27);
        g.fill(bx, 21, bx + (int) (bw * Math.min(1.0, ClientState.xp / 60.0)), 27, 0xFF55FF55);
        g.renderOutline(bx, 21, bw, 6, 0xFF4A4F63);
        g.drawString(font, "prochain point", bx + bw + 4, 20, 0xFFAAAAAA, false);

        if (leftW > 0) {
            Gui.preview(g, 6, 38, leftW, 130, mx, my);
            Races.RACES.stream().filter(r -> r.id().equals(ClientState.race)).findFirst().ifPresent(r ->
                    Gui.ruler(g, font, 6, 38, leftW, 130, 1.8 * (1 + r.fx().v.getOrDefault("scale", 0.0))));
        }

        // panneau du bas : attributs totaux + compétences
        int px = 12, py = panelY + 5;
        g.drawString(font, "Attributs actuels", px, py, 0xFFFFFFFF, false);
        List<Fx.Line> lines = Gui.total().lines();
        int rows = Math.max(1, (height - py - 22) / 10);
        for (int i = 0; i < lines.size() && i / rows < 2; i++) {
            g.drawString(font, lines.get(i).text(), px + (i / rows) * 135, py + 12 + (i % rows) * 10, lines.get(i).color(), false);
        }
        int ax = width - 175;
        g.drawString(font, "Compétences actives", ax, py, 0xFFFFFFFF, false);
        for (int slot = 0; slot < 2; slot++) {
            Actives.Active a = Actives.ALL.get(c.acts()[slot]);
            boolean un = ClientState.unlocked.contains(c.id() + ".act" + (slot + 1));
            int ay = py + 12 + slot * 20;
            g.renderItem(Gui.skillIcon(a.id()), ax, ay);
            g.drawString(font, (slot == 0 ? "[R] " : "[V] ") + a.name() + (un ? "" : " (verrouillée)"), ax + 20, ay + 4,
                    un ? 0xFF55FF55 : 0xFF888888, false);
        }
        g.drawString(font, "/fantasyraces respec : réinitialiser", ax, py + 56, 0xFF777777, false);
    }

    @Override public boolean isPauseScreen() { return false; }
}
