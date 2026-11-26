package com.fantasyraces;

import com.fantasyraces.Defs.ClassDef;
import com.fantasyraces.Defs.Node;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClassScreen extends Screen {
    private ClassDef sel;
    private Button ok;
    private final List<Tile> tiles = new ArrayList<>();
    private List<ClassDef> list = new ArrayList<>();
    private int top, ph, cx, cw, rx, rw;

    public ClassScreen() { super(Component.literal("Choisis ta classe")); }

    @Override
    protected void init() {
        tiles.clear();
        list = Races.classesOf(ClientState.race);
        top = 24;
        ph = height - top - 34;
        for (int i = 0; i < list.size(); i++) {
            ClassDef c = list.get(i);
            Tile t = new Tile(6, top + i * 44, 150, 40, c.name(), c.desc(), Gui.stack(Gui.firstItem(c)), () -> select(c));
            t.setTooltip(Tooltip.create(Component.literal(c.name() + "\n" + c.desc())));
            tiles.add(t);
            addRenderableWidget(t);
        }
        cx = 6 + 150 + 8;
        cw = 112;
        rx = cx + cw + 8;
        rw = Math.max(110, width - rx - 6);
        ok = Button.builder(Component.literal("Confirmer cette classe (définitif)"), b -> {
            if (sel != null) PacketDistributor.sendToServer(new Net.ChooseClass(sel.id()));
        }).bounds(width / 2 - 100, height - 26, 200, 20).build();
        ok.active = sel != null;
        addRenderableWidget(ok);
        if (sel != null) select(sel);
    }

    private void select(ClassDef c) {
        sel = c;
        ok.active = true;
        for (int i = 0; i < tiles.size(); i++) tiles.get(i).selected = list.get(i) == c;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        Gui.panel(g, rx, top, rw, ph);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        String rn = Races.race(ClientState.race) != null ? Races.race(ClientState.race).name() : "";
        g.drawCenteredString(font, Component.literal(rn + " — choisis ta classe").withStyle(ChatFormatting.BOLD), width / 2, 9, 0xFFFFFFFF);

        int previewH = Math.max(60, ph - 98);
        Gui.preview(g, cx, top, cw, previewH, mx, my);
        if (sel == null) {
            Gui.wrap(g, font, "Clique sur une classe pour voir son kit, ses compétences et son arbre.", rx + 6, top + 8, rw - 12, 0xFFAAAAAA);
            return;
        }
        Node hov = Gui.drawTreeIcons(g, sel, cx + 4, top + previewH + 6, cw - 8, Set.<String>of(), mx, my);

        int x = rx + 6, w = rw - 12, y = top + 6;
        g.drawString(font, Component.literal(sel.name()).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), x, y, 0xFFFFFFFF, false);
        y += 12;
        y = Gui.wrap(g, font, sel.desc(), x, y, w, 0xFFAAAAAA) + 4;

        g.drawString(font, "Kit de départ", x, y, 0xFFFFFFFF, false);
        y += 11;
        ItemStack hovItem = null;
        int perRow = Math.max(1, w / 20), i = 0;
        for (ItemStack s : Gui.kit(sel)) {
            int sx = x + (i % perRow) * 20, sy = y + (i / perRow) * 20;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF1C1E27);
            g.renderOutline(sx, sy, 18, 18, 0xFF4A4F63);
            g.renderItem(s, sx + 1, sy + 1);
            g.renderItemDecorations(font, s, sx + 1, sy + 1);
            if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) hovItem = s;
            i++;
        }
        y += ((i + perRow - 1) / perRow) * 20 + 4;

        g.drawString(font, "Compétences actives", x, y, 0xFFFFFFFF, false);
        y += 11;
        for (int slot = 0; slot < 2; slot++) {
            Actives.Active a = Actives.ALL.get(sel.acts()[slot]);
            g.renderItem(Gui.skillIcon(a.id()), x, y);
            g.drawString(font, (slot == 0 ? "[R] " : "[V] ") + a.name(), x + 20, y + 4, 0xFF55FFFF, false);
            y += 18;
        }
        Gui.wrap(g, font, "3 points offerts pour débloquer l'arbre.", x, Math.min(y + 2, top + ph - 20), w, 0xFF55FF55);

        if (hov != null) g.renderComponentTooltip(font, Gui.nodeTip(hov), mx, my);
        else if (hovItem != null) g.renderTooltip(font, hovItem, mx, my);
    }

    @Override public boolean isPauseScreen() { return false; }
}
