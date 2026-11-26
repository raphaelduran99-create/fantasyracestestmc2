package com.fantasyraces;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = FantasyRaces.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Keys {
    public static final KeyMapping MENU = new KeyMapping("key.fantasyraces.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.fantasyraces");
    public static final KeyMapping SKILL1 = new KeyMapping("key.fantasyraces.skill1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.fantasyraces");
    public static final KeyMapping SKILL2 = new KeyMapping("key.fantasyraces.skill2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.fantasyraces");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent e) {
        e.register(MENU);
        e.register(SKILL1);
        e.register(SKILL2);
    }

    private Keys() {}
}
