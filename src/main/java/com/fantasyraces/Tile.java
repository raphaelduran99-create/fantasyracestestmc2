package com.fantasyraces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Bouton visuel : icône d'objet + titre + sous-titre. */
public class Tile extends AbstractWidget {
    public interface Click { void run(); }

    private final ItemStack icon;
    private final String sub;
    private final Click click;
    public boolean selected, dim;
    public int border = 0xFF4A4F63, textColor = 0xFFFFFFFF;

    public Tile(int x, int y, int w, int h, String label, String sub, ItemStack icon, Click click) {
        super(x, y, w, h, Component.literal(label));
        this.sub = sub;
        this.icon = icon;
        this.click = click;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        Font f = Minecraft.getInstance().font;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int bg = selected ? 0xFF2F4A2A : isHoveredOrFocused() ? 0xFF38384A : 0xFF1C1E27;
        g.fill(x, y, x + w, y + h, bg);
        g.renderOutline(x, y, w, h, selected ? 0xFFFFFF55 : border);
        g.renderItem(icon, x + 4, y + (h - 16) / 2);
        int tw = w - 28;
        String name = f.plainSubstrByWidth(getMessage().getString(), tw);
        if (sub == null || sub.isEmpty()) {
            g.drawString(f, name, x + 24, y + (h - 8) / 2, textColor, false);
        } else {
            g.drawString(f, name, x + 24, y + h / 2 - 9, textColor, false);
            g.drawString(f, f.plainSubstrByWidth(sub, tw), x + 24, y + h / 2 + 2, 0xFFAAAAAA, false);
        }
        if (dim) g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x88000000);
    }

    @Override
    public void onClick(double mx, double my) { click.run(); }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput o) { defaultButtonNarrationText(o); }
}
