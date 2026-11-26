package com.fantasyraces;

import com.fantasyraces.Defs.ClassDef;
import com.fantasyraces.Defs.Node;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class TreeScreen extends Screen {
    public int version;
    private final Map<String, int[]> pos = new HashMap<>();

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
        for (Node n : c.nodes()) {
            int x = (int) (width / 2 + (n.col() - 1.5) * 118) - 55, y = 50 + n.row() * 58;
            pos.put(n.id(), new int[]{x, y});
            boolean un = ClientState.unlocked.contains(n.id());
            boolean can = !un && ClientState.points >= n.cost() && ClientState.unlocked.containsAll(n.parents());
            ChatFormatting col = un ? ChatFormatting.GREEN : can ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY;
            Button b = Button.builder(Component.literal(n.name()).withStyle(col), bt -> {
                if (can) PacketDistributor.sendToServer(new Net.Unlock(n.id()));
            }).bounds(x, y, 110, 20).build();
            String eff = n.fx().v.isEmpty() ? n.desc() : n.fx().describe();
            b.setTooltip(Tooltip.create(Component.literal(n.name() + "\nCoût : " + n.cost() + " pt\n" + eff
                    + (un ? "\n[Débloqué]" : can ? "\n[Clic pour débloquer]" : "\n[Verrouillé]"))));
            addRenderableWidget(b);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        ClassDef c = Races.cls(ClientState.cls);
        if (c == null) return;
        for (Node n : c.nodes()) {
            int[] cp = pos.get(n.id());
            if (cp == null) continue;
            for (String par : n.parents()) {
                int[] pp = pos.get(par);
                if (pp == null) continue;
                int x1 = pp[0] + 55, y1 = pp[1] + 20, x2 = cp[0] + 55, y2 = cp[1], my2 = (y1 + y2) / 2;
                int col = ClientState.unlocked.contains(par) ? 0xFF55FF55 : 0xFF777777;
                g.vLine(x1, y1, my2, col);
                g.hLine(Math.min(x1, x2), Math.max(x1, x2), my2, col);
                g.vLine(x2, my2, y2, col);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        ClassDef c = Races.cls(ClientState.cls);
        String head = (Races.race(ClientState.race) != null ? Races.race(ClientState.race).name() : "") + " — "
                + (c != null ? c.name() : "") + "   |   Points : " + ClientState.points;
        g.drawCenteredString(font, head, width / 2, 10, 0xFFFF55);
        g.drawCenteredString(font, "Gagne de l'XP pour obtenir des points. R / V = compétences. /fantasyraces respec = réinitialiser",
                width / 2, 24, 0xAAAAAA);
    }

    @Override public boolean isPauseScreen() { return false; }
}
