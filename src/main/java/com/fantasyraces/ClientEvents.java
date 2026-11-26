package com.fantasyraces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = FantasyRaces.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean first;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (Keys.MENU.consumeClick()) {
            if (!ClientState.synced) continue;
            Screen s = ClientState.race.isEmpty() ? new RaceScreen()
                    : ClientState.cls.isEmpty() ? new ClassScreen() : new TreeScreen();
            mc.setScreen(s);
        }
        while (Keys.SKILL1.consumeClick()) PacketDistributor.sendToServer(new Net.UseSkill(0));
        while (Keys.SKILL2.consumeClick()) PacketDistributor.sendToServer(new Net.UseSkill(1));

        if (ClientState.changed) {
            Screen cur = mc.screen;
            boolean done = true;
            if (ClientState.race.isEmpty()) {
                if (!first) {
                    if (cur == null) { mc.setScreen(new RaceScreen()); first = true; }
                    else done = false; // un autre écran est ouvert : on réessaie au prochain tick
                }
            } else if (ClientState.cls.isEmpty()) {
                if (cur instanceof RaceScreen) { mc.setScreen(new ClassScreen()); first = true; }
                else if (!first) {
                    if (cur == null) { mc.setScreen(new ClassScreen()); first = true; }
                    else done = false;
                }
            } else if (cur instanceof ClassScreen) {
                mc.setScreen(new TreeScreen());
            }
            if (done) ClientState.changed = false;
        }
        if (mc.screen instanceof TreeScreen ts && ts.version != ClientState.version) ts.refresh();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        first = false;
        ClientState.synced = false;
    }

    private ClientEvents() {}
}
