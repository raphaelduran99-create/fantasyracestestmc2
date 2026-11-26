package com.fantasyraces;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class Gui {
    /** Dessine un texte avec retour à la ligne automatique, renvoie le y suivant. */
    public static int wrap(GuiGraphics g, Font f, String s, int x, int y, int w, int color) {
        for (FormattedCharSequence l : f.split(Component.literal(s), w)) {
            g.drawString(f, l, x, y, color, false);
            y += f.lineHeight + 1;
        }
        return y;
    }

    private Gui() {}
}
