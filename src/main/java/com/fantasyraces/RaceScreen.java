package com.fantasyraces;

import com.fantasyraces.Defs.RaceDef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class RaceScreen extends Screen {
    private RaceDef sel;
    private Button ok;
    private int descY;

    public RaceScreen() { super(Component.literal("Choisis ta race (choix définitif)")); }

    @Override
    protected void init() {
        int cols = Math.max(3, Math.min(5, width / 100)), bw = 96, bh = 20, gap = 4;
        int x0 = (width - (cols * (bw + gap) - gap)) / 2, y0 = 24;
        int rows = (Races.RACES.size() + cols - 1) / cols;
        for (int i = 0; i < Races.RACES.size(); i++) {
            RaceDef r = Races.RACES.get(i);
            int x = x0 + (i % cols) * (bw + gap), y = y0 + (i / cols) * (bh + gap);
            addRenderableWidget(Button.builder(Component.literal(r.name()), b -> { sel = r; ok.active = true; })
                    .bounds(x, y, bw, bh).build());
        }
        descY = y0 + rows * (bh + gap) + 6;
        ok = Button.builder(Component.literal("Confirmer cette race"), b -> {
            if (sel != null) PacketDistributor.sendToServer(new Net.ChooseRace(sel.id()));
        }).bounds(width / 2 - 90, height - 26, 180, 20).build();
        ok.active = sel != null;
        addRenderableWidget(ok);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        if (sel == null) return;
        int x = width / 2 - 230, w = 460, y = descY;
        y = Gui.wrap(g, font, sel.name() + " — " + sel.lore(), x, y, w, 0xFFFF55);
        y = Gui.wrap(g, font, "+ " + sel.pros(), x, y, w, 0x55FF55);
        y = Gui.wrap(g, font, "- " + sel.cons(), x, y, w, 0xFF5555);
        Gui.wrap(g, font, "Stats : " + sel.fx().describe(), x, y, w, 0x55FFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
}
