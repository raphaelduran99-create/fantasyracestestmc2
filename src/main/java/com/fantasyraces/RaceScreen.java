package com.fantasyraces;

import com.fantasyraces.Defs.RaceDef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class RaceScreen extends Screen {
    private RaceDef sel;
    private Button ok;
    private final List<Tile> tiles = new ArrayList<>();
    private int top, ph, cx, cw, rx, rw;

    public RaceScreen() { super(Component.literal("Choisis ta race")); }

    @Override
    protected void init() {
        tiles.clear();
        top = 24;
        ph = height - top - 34;
        int tw = 86, th = 20, gap = 1;
        for (int i = 0; i < Races.RACES.size(); i++) {
            RaceDef r = Races.RACES.get(i);
            Tile t = new Tile(6 + (i % 2) * (tw + gap), top + (i / 2) * (th + gap), tw, th, r.name(), "", Gui.raceIcon(r.id()), () -> select(r));
            t.setTooltip(Tooltip.create(Component.literal(r.lore())));
            tiles.add(t);
            addRenderableWidget(t);
        }
        cx = 6 + 2 * tw + gap + 8;
        cw = 112;
        rx = cx + cw + 8;
        rw = Math.max(110, width - rx - 6);
        ok = Button.builder(Component.literal("Confirmer cette race (définitif)"), b -> {
            if (sel != null) PacketDistributor.sendToServer(new Net.ChooseRace(sel.id()));
        }).bounds(width / 2 - 100, height - 26, 200, 20).build();
        ok.active = sel != null;
        addRenderableWidget(ok);
        if (sel != null) select(sel);
    }

    private void select(RaceDef r) {
        sel = r;
        ok.active = true;
        for (int i = 0; i < tiles.size(); i++) tiles.get(i).selected = Races.RACES.get(i) == r;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        Gui.panel(g, rx, top, rw, ph);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, Component.literal("Choisis ta race").withStyle(ChatFormatting.BOLD), width / 2, 9, 0xFFFFFFFF);

        Gui.preview(g, cx, top, cw, ph, mx, my);
        double scale = sel == null ? 0 : sel.fx().v.getOrDefault("scale", 0.0);
        Gui.ruler(g, font, cx, top, cw, ph, 1.8 * (1 + scale));
        if (sel == null) {
            Gui.wrap(g, font, "Clique sur une race à gauche pour voir ses avantages, ses défauts et ses attributs.", rx + 6, top + 8, rw - 12, 0xFFAAAAAA);
            return;
        }
        int x = rx + 6, w = rw - 12, y = top + 6;
        g.drawString(font, Component.literal(sel.name()).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), x, y, 0xFFFFFFFF, false);
        g.renderItem(Gui.raceIcon(sel.id()), rx + rw - 22, top + 4);
        y += 13;
        y = Gui.wrap(g, font, sel.lore(), x, y, w, 0xFFAAAAAA) + 3;
        y = Gui.wrap(g, font, "+ " + sel.pros(), x, y, w, 0xFF55FF55) + 2;
        y = Gui.wrap(g, font, "- " + sel.cons(), x, y, w, 0xFFFF5555) + 5;
        g.drawString(font, "Attributs", x, y, 0xFFFFFFFF, false);
        y += 11;
        for (Fx.Line l : sel.fx().lines()) {
            if (y > top + ph - 10) break;
            g.drawString(font, l.text(), x, y, l.color(), false);
            y += 10;
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
