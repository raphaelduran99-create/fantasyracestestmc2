package com.fantasyraces;

import com.fantasyraces.Defs.ClassDef;
import com.fantasyraces.Defs.Node;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ClassScreen extends Screen {
    private ClassDef sel;
    private Button ok;

    public ClassScreen() { super(Component.literal("Choisis ta classe (choix définitif)")); }

    @Override
    protected void init() {
        List<ClassDef> list = Races.classesOf(ClientState.race);
        int bw = 140, gap = 6, x0 = (width - (list.size() * (bw + gap) - gap)) / 2;
        for (int i = 0; i < list.size(); i++) {
            ClassDef c = list.get(i);
            addRenderableWidget(Button.builder(Component.literal(c.name()), b -> { sel = c; ok.active = true; })
                    .bounds(x0 + i * (bw + gap), 26, bw, 20).build());
        }
        ok = Button.builder(Component.literal("Confirmer cette classe"), b -> {
            if (sel != null) PacketDistributor.sendToServer(new Net.ChooseClass(sel.id()));
        }).bounds(width / 2 - 90, height - 26, 180, 20).build();
        ok.active = sel != null;
        addRenderableWidget(ok);
    }

    private String kitText(ClassDef c) {
        StringBuilder sb = new StringBuilder();
        for (String e : c.kit().split(",")) {
            String[] kv = e.split(":");
            ItemStack s = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(kv[0].trim())));
            if (sb.length() > 0) sb.append(", ");
            sb.append(kv[1].trim()).append("x ").append(s.getHoverName().getString());
        }
        return sb.toString();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        if (sel == null) return;
        int x = width / 2 - 230, w = 460, y = 56;
        y = Gui.wrap(g, font, sel.name() + " — " + sel.desc(), x, y, w, 0xFFFF55);
        y = Gui.wrap(g, font, "Kit de départ : " + kitText(sel), x, y + 2, w, 0xFFFFFF);
        y += 4;
        for (Node n : sel.nodes()) {
            String d = n.fx().v.isEmpty() ? n.desc() : n.fx().describe();
            y = Gui.wrap(g, font, "• " + n.name() + " (" + n.cost() + " pt) : " + d, x, y, w, n.slot() >= 0 ? 0x55FFFF : 0xAAAAAA);
        }
        Gui.wrap(g, font, "3 points de compétence offerts. Touches : R / V pour les compétences, K pour l'arbre.", x, y + 4, w, 0x55FF55);
    }

    @Override public boolean isPauseScreen() { return false; }
}
