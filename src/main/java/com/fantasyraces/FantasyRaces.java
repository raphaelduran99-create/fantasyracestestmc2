package com.fantasyraces;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(FantasyRaces.MODID)
public class FantasyRaces {
    public static final String MODID = "fantasyraces";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<AttachmentType<?>> ATT =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    public static final Supplier<AttachmentType<PlayerData>> DATA = ATT.register("data",
            () -> AttachmentType.builder(PlayerData::new).serialize(PlayerData.CODEC).copyOnDeath().build());

    public FantasyRaces(IEventBus bus) {
        ATT.register(bus);
    }
}
