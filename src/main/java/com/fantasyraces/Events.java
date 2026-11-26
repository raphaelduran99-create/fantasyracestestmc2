package com.fantasyraces;

import com.fantasyraces.Defs.*;
import com.mojang.brigadier.Command;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;

@EventBusSubscriber(modid = FantasyRaces.MODID)
public final class Events {
    static final Set<String> MEAT = Set.of("beef", "cooked_beef", "porkchop", "cooked_porkchop", "chicken",
            "cooked_chicken", "mutton", "cooked_mutton", "rabbit", "cooked_rabbit", "rabbit_stew", "cod",
            "cooked_cod", "salmon", "cooked_salmon", "tropical_fish", "pufferfish", "rotten_flesh");

    static String path(ItemStack s) { return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath(); }

    /** Applique un effet sans clignotement (rafraîchi uniquement quand il faiblit). */
    static void fx(ServerPlayer p, Holder<MobEffect> e, int dur, int amp) {
        MobEffectInstance cur = p.getEffect(e);
        if (cur == null || cur.getAmplifier() < amp || cur.getDuration() < dur - 30)
            p.addEffect(new MobEffectInstance(e, dur, amp, true, false, false));
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { Logic.recompute(p); Logic.sync(p); }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { Logic.recompute(p); Logic.sync(p); }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerData d = Logic.data(p);
        RaceDef r = Races.race(d.race);
        if (r == null) return;
        if (d.stats == null) Logic.recompute(p);
        Set<Trait> t = r.traits();
        ServerLevel lv = (ServerLevel) p.level();
        int tc = p.tickCount;

        if (t.contains(Trait.HUNGER_DRAIN)) p.causeFoodExhaustion(0.005f);
        if (t.contains(Trait.SLOW_FALL) && p.fallDistance > 2.5f) fx(p, MobEffects.SLOW_FALLING, 60, 0);
        if (t.contains(Trait.LAVA_SWIM)) {
            if (p.isInLava()) fx(p, MobEffects.FIRE_RESISTANCE, 60, 0);
            if (p.isOnFire()) p.clearFire();
        }
        if (tc % 10 == 0 && t.contains(Trait.UNDEAD_PEACE)) {
            for (Mob m : lv.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16))) {
                if (m.getTarget() == p && m.getType().is(EntityTypeTags.UNDEAD) && m.getLastHurtByMob() != p) m.setTarget(null);
            }
        }
        if (tc % 20 != 0) return;

        BlockPos pos = p.blockPosition();
        boolean sun = lv.isDay() && lv.canSeeSky(pos.above()) && !lv.isRainingAt(pos);
        boolean helmet = !p.getItemBySlot(EquipmentSlot.HEAD).isEmpty();

        if (t.contains(Trait.SUN_BLIND) && sun && !helmet) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false, false));
            Logic.msg(p, "Le soleil vous aveugle... (un casque aide)");
        }
        if (t.contains(Trait.UNDERGROUND_SIGHT) && !lv.canSeeSky(pos.above()) && p.getY() < 56) fx(p, MobEffects.NIGHT_VISION, 300, 0);
        if (t.contains(Trait.DARK_SIGHT) && lv.getMaxLocalRawBrightness(pos) <= 6) fx(p, MobEffects.NIGHT_VISION, 300, 0);
        if (t.contains(Trait.WATER_BREATH) && p.isInWater()) fx(p, MobEffects.WATER_BREATHING, 300, 0);
        if (t.contains(Trait.NIGHT_POWER) && lv.isNight()) {
            fx(p, MobEffects.DAMAGE_BOOST, 300, 0);
            fx(p, MobEffects.MOVEMENT_SPEED, 300, 0);
        }
        if (t.contains(Trait.NO_POISON)) p.removeEffect(MobEffects.POISON);
        if (t.contains(Trait.SUN_BURN) && sun && !helmet && !p.isInWaterOrRain() && !p.hasEffect(MobEffects.FIRE_RESISTANCE)
                && p.getRemainingFireTicks() < 40) p.setRemainingFireTicks(80);
        if (t.contains(Trait.WATER_HURT) && p.isInWater() && tc % 60 == 0) p.hurt(p.damageSources().generic(), 1f);
        if (t.contains(Trait.SUN_FEED) && sun && tc % 100 == 0 && p.getFoodData().needsFood()) p.getFoodData().eat(1, 0.3f);
    }

    @SubscribeEvent
    public static void eat(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack s = e.getItem();
        if (!s.has(DataComponents.FOOD)) return;
        RaceDef r = Races.race(Logic.data(p).race);
        if (r == null) return;
        boolean meat = MEAT.contains(path(s));
        if (r.has(Trait.VEGETARIAN) && meat) {
            e.setCanceled(true);
            Logic.msg(p, "Un elfe ne mange pas de viande.");
        } else if (r.has(Trait.CARNIVORE) && !meat) {
            e.setCanceled(true);
            Logic.msg(p, "Un loup-garou ne mange que de la viande.");
        }
    }

    @SubscribeEvent
    public static void drawTick(LivingEntityUseItemEvent.Tick e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack s = e.getItem();
        if (!(s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem)) return;
        PlayerData d = Logic.data(p);
        double sp = Math.min(1, Logic.stat(d, "rspeed"));
        if (sp <= 0) return;
        d.drawAcc += sp;
        if (d.drawAcc >= 1) { d.drawAcc -= 1; e.setDuration(e.getDuration() - 1); }
    }

    @SubscribeEvent
    public static void arrowJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (e.getEntity() instanceof AbstractArrow a && a.getOwner() instanceof ServerPlayer p && !a.getTags().contains("fr_s")) {
            a.addTag("fr_s");
            double rv = Logic.stat(Logic.data(p), "rvel");
            if (rv > 0) a.setDeltaMovement(a.getDeltaMovement().scale(1 + rv));
        }
    }

    @SubscribeEvent
    public static void hurt(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (e.getEntity() instanceof ServerPlayer p) {
            PlayerData d = Logic.data(p);
            RaceDef r = Races.race(d.race);
            if (r != null) {
                float a = e.getAmount();
                boolean fire = src.is(DamageTypeTags.IS_FIRE);
                if (r.has(Trait.LAVA_SWIM) && fire) { e.setCanceled(true); return; }
                if (r.has(Trait.FIRE_HALF) && fire) a *= 0.5f;
                if (r.has(Trait.FIRE_WEAK) && fire) a *= 1.5f;
                if (r.has(Trait.MAGIC_WEAK) && (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC))) a *= 1.25f;
                Entity atk = src.getEntity();
                if (atk instanceof LivingEntity le) {
                    String it = path(le.getMainHandItem());
                    if (r.has(Trait.IRON_WEAK) && (it.startsWith("iron_") || atk instanceof IronGolem)) a *= 1.5f;
                    if (r.has(Trait.GOLD_WEAK) && it.startsWith("golden_")) a *= 1.5f;
                }
                a *= (float) (1 - Math.max(0, Math.min(0.6, Logic.stat(d, "dmgred"))));
                e.setAmount(a);
            }
        }
        if (src.getEntity() instanceof ServerPlayer att) {
            PlayerData d = Logic.data(att);
            if (src.is(DamageTypeTags.IS_PROJECTILE)) {
                e.setAmount((float) (e.getAmount() * (1 + Logic.stat(d, "rdmg"))));
            } else if (src.getDirectEntity() == att) {
                double ls = Logic.stat(d, "lifesteal");
                if (ls > 0) att.heal((float) (e.getAmount() * ls));
            }
        }
    }

    @SubscribeEvent
    public static void xp(PlayerXpEvent.PickupXp e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerData d = Logic.data(p);
        if (d.race.isEmpty()) return;
        d.xpBank += e.getOrb().getValue() * (1 + Logic.stat(d, "learn"));
        int gain = 0;
        while (d.xpBank >= 60) { d.xpBank -= 60; d.points++; gain++; }
        if (gain > 0) {
            Logic.msg(p, "+" + gain + " point(s) de compétence (touche K)");
            Logic.sync(p);
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("fantasyraces")
                .then(Commands.literal("respec").executes(c -> {
                    Logic.respec(c.getSource().getPlayerOrException());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(c -> {
                    Logic.reset(c.getSource().getPlayerOrException());
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private Events() {}
}
