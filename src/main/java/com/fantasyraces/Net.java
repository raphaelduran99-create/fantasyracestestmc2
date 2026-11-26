package com.fantasyraces;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

@EventBusSubscriber(modid = FantasyRaces.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Net {
    static ResourceLocation loc(String s) { return ResourceLocation.fromNamespaceAndPath(FantasyRaces.MODID, s); }

    public record Sync(String race, String cls, List<String> unlocked, int points, int xp) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(loc("sync"));
        public static final StreamCodec<ByteBuf, Sync> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Sync::race,
                ByteBufCodecs.STRING_UTF8, Sync::cls,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Sync::unlocked,
                ByteBufCodecs.VAR_INT, Sync::points,
                ByteBufCodecs.VAR_INT, Sync::xp,
                Sync::new);
        @Override public Type<Sync> type() { return TYPE; }
    }

    public record ChooseRace(String value) implements CustomPacketPayload {
        public static final Type<ChooseRace> TYPE = new Type<>(loc("choose_race"));
        public static final StreamCodec<ByteBuf, ChooseRace> CODEC = ByteBufCodecs.STRING_UTF8.map(ChooseRace::new, ChooseRace::value);
        @Override public Type<ChooseRace> type() { return TYPE; }
    }

    public record ChooseClass(String value) implements CustomPacketPayload {
        public static final Type<ChooseClass> TYPE = new Type<>(loc("choose_class"));
        public static final StreamCodec<ByteBuf, ChooseClass> CODEC = ByteBufCodecs.STRING_UTF8.map(ChooseClass::new, ChooseClass::value);
        @Override public Type<ChooseClass> type() { return TYPE; }
    }

    public record Unlock(String value) implements CustomPacketPayload {
        public static final Type<Unlock> TYPE = new Type<>(loc("unlock"));
        public static final StreamCodec<ByteBuf, Unlock> CODEC = ByteBufCodecs.STRING_UTF8.map(Unlock::new, Unlock::value);
        @Override public Type<Unlock> type() { return TYPE; }
    }

    public record UseSkill(int slot) implements CustomPacketPayload {
        public static final Type<UseSkill> TYPE = new Type<>(loc("use_skill"));
        public static final StreamCodec<ByteBuf, UseSkill> CODEC = ByteBufCodecs.VAR_INT.map(UseSkill::new, UseSkill::slot);
        @Override public Type<UseSkill> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToClient(Sync.TYPE, Sync.CODEC, (pl, ctx) -> ClientState.set(pl));
        r.playToServer(ChooseRace.TYPE, ChooseRace.CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) Logic.chooseRace(sp, pl.value());
        });
        r.playToServer(ChooseClass.TYPE, ChooseClass.CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) Logic.chooseClass(sp, pl.value());
        });
        r.playToServer(Unlock.TYPE, Unlock.CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) Logic.unlock(sp, pl.value());
        });
        r.playToServer(UseSkill.TYPE, UseSkill.CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) Logic.useSkill(sp, pl.slot());
        });
    }

    private Net() {}
}
