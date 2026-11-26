package com.fantasyraces;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compétences actives. Aucune ne détruit de blocs ni ne met le feu au terrain. */
public final class Actives {
    @FunctionalInterface
    public interface Run { boolean run(ServerPlayer p, double power); }

    public record Active(String id, String name, int cd, String desc, Run run) {}

    public static final Map<String, Active> ALL = new LinkedHashMap<>();

    private static void reg(String id, String name, int cdSec, String desc, Run r) {
        ALL.put(id, new Active(id, name, cdSec * 20, desc + " (recharge " + cdSec + " s)", r));
    }

    // ---------- helpers ----------
    private static void eff(LivingEntity e, Holder<MobEffect> m, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(m, ticks, amp));
    }

    private static void snd(ServerPlayer p, SoundEvent s) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, 1f, 1f);
    }

    private static void part(ServerPlayer p, ParticleOptions o, double x, double y, double z, int n) {
        ((ServerLevel) p.level()).sendParticles(o, x, y, z, n, 0.3, 0.3, 0.3, 0.02);
    }

    private static void push(ServerPlayer p, Vec3 v) {
        p.setDeltaMovement(v);
        p.hurtMarked = true;
        p.fallDistance = 0;
    }

    private static List<LivingEntity> foes(ServerPlayer p, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r))) {
            if (e != p && e.isAlive() && e instanceof Enemy && e.distanceToSqr(p) <= r * r) out.add(e);
        }
        return out;
    }

    private static LivingEntity target(ServerPlayer p, double range) {
        Vec3 s = p.getEyePosition(), d = p.getLookAngle();
        Vec3 e = s.add(d.scale(range));
        BlockHitResult b = p.level().clip(new ClipContext(s, e, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double max = b.getType() == HitResult.Type.MISS ? range : b.getLocation().distanceTo(s);
        e = s.add(d.scale(max));
        AABB box = p.getBoundingBox().expandTowards(d.scale(max)).inflate(1.0);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(p.level(), p, s, e, box,
                x -> x instanceof LivingEntity && !x.isSpectator() && x != p);
        for (double t = 0.5; t < max; t += 0.7) {
            Vec3 q = s.add(d.scale(t));
            part(p, ParticleTypes.END_ROD, q.x, q.y, q.z, 1);
        }
        return r == null ? null : (LivingEntity) r.getEntity();
    }

    public static String nameOf(String id) { return ALL.get(id).name(); }

    // ---------- compétences ----------
    static {
        reg("dash", "Ruée", 8, "Charge rapidement vers l'avant.", (p, pw) -> {
            Vec3 l = p.getLookAngle();
            push(p, new Vec3(l.x, 0, l.z).normalize().scale(1.7).add(0, 0.2, 0));
            snd(p, SoundEvents.PLAYER_ATTACK_SWEEP);
            return true;
        });

        reg("leap", "Bond", 10, "Saute très haut et loin, atterrissage amorti.", (p, pw) -> {
            Vec3 l = p.getLookAngle();
            push(p, new Vec3(l.x * 0.9, 0.9, l.z * 0.9));
            eff(p, MobEffects.SLOW_FALLING, 60, 0);
            snd(p, SoundEvents.PLAYER_ATTACK_SWEEP);
            return true;
        });

        reg("blink", "Pas fantôme", 14, "Se téléporte jusqu'à 9 blocs dans la direction du regard.", (p, pw) -> {
            Vec3 s = p.getEyePosition(), d = p.getLookAngle();
            BlockHitResult b = p.level().clip(new ClipContext(s, s.add(d.scale(9)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            double max = b.getType() == HitResult.Type.MISS ? 9 : Math.max(0, b.getLocation().distanceTo(s) - 0.6);
            for (double t = max; t >= 1.0; t -= 0.5) {
                Vec3 c = s.add(d.scale(t));
                double fy = c.y - p.getEyeHeight();
                AABB box = p.getBoundingBox().move(c.x - p.getX(), fy - p.getY(), c.z - p.getZ());
                if (p.level().noCollision(p, box)) {
                    part(p, ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 25);
                    p.teleportTo(c.x, fy, c.z);
                    p.fallDistance = 0;
                    part(p, ParticleTypes.PORTAL, c.x, fy + 1, c.z, 25);
                    snd(p, SoundEvents.ENDERMAN_TELEPORT);
                    return true;
                }
            }
            Logic.msg(p, "Pas assez de place pour se téléporter.");
            return false;
        });

        reg("volley", "Salve", 14, "Tire 3 flèches en éventail, sans consommer de munition.", (p, pw) -> {
            for (int i = -1; i <= 1; i++) {
                Arrow a = EntityType.ARROW.create(p.level());
                if (a == null) return false;
                a.setOwner(p);
                a.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
                a.shootFromRotation(p, p.getXRot(), p.getYRot() + i * 8f, 0f, 2.8f, 1.0f);
                a.pickup = AbstractArrow.Pickup.DISALLOWED;
                a.setBaseDamage(2.0 * pw);
                p.level().addFreshEntity(a);
            }
            snd(p, SoundEvents.ARROW_SHOOT);
            return true;
        });

        reg("bolt", "Trait arcanique", 8, "Projette un trait magique (7 dégâts) sur la cible visée.", (p, pw) -> {
            LivingEntity t = target(p, 16);
            snd(p, SoundEvents.EVOKER_CAST_SPELL);
            if (t == null) return false;
            t.hurt(p.damageSources().indirectMagic(p, p), (float) (7 * pw));
            part(p, ParticleTypes.WITCH, t.getX(), t.getY() + 1, t.getZ(), 15);
            return true;
        });

        reg("drain", "Siphon de vie", 12, "Blesse la cible (5 dégâts) et te rend de la vie.", (p, pw) -> {
            LivingEntity t = target(p, 14);
            snd(p, SoundEvents.EVOKER_CAST_SPELL);
            if (t == null) return false;
            float dmg = (float) (5 * pw);
            t.hurt(p.damageSources().indirectMagic(p, p), dmg);
            p.heal(dmg * 0.8f);
            part(p, ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY() + 1, t.getZ(), 8);
            return true;
        });

        reg("heal", "Soin", 20, "Soigne 3 cœurs à toi et aux alliés proches.", (p, pw) -> {
            p.heal((float) (6 * pw));
            eff(p, MobEffects.REGENERATION, 60, 0);
            part(p, ParticleTypes.HEART, p.getX(), p.getY() + 1.5, p.getZ(), 8);
            for (Player o : p.level().players()) {
                if (o != p && !o.isSpectator() && o.distanceToSqr(p) < 64) {
                    o.heal((float) (4 * pw));
                    part(p, ParticleTypes.HEART, o.getX(), o.getY() + 1.5, o.getZ(), 6);
                }
            }
            snd(p, SoundEvents.PLAYER_LEVELUP);
            return true;
        });

        reg("shield", "Égide", 25, "Absorption (4 cœurs) et résistance 5 s.", (p, pw) -> {
            eff(p, MobEffects.ABSORPTION, 200, 1);
            eff(p, MobEffects.DAMAGE_RESISTANCE, 100, 0);
            snd(p, SoundEvents.EVOKER_CAST_SPELL);
            return true;
        });

        reg("cleanse", "Purification", 20, "Retire les effets négatifs, éteint le feu, régénère.", (p, pw) -> {
            for (MobEffectInstance i : new ArrayList<>(p.getActiveEffects())) {
                if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) p.removeEffect(i.getEffect());
            }
            p.clearFire();
            eff(p, MobEffects.REGENERATION, 100, 0);
            part(p, ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 15);
            return true;
        });

        reg("roar", "Rugissement", 20, "Repousse et affaiblit les monstres proches.", (p, pw) -> {
            for (LivingEntity e : foes(p, 7)) {
                eff(e, MobEffects.WEAKNESS, 160, 0);
                eff(e, MobEffects.MOVEMENT_SLOWDOWN, 80, 0);
                Vec3 dir = e.position().subtract(p.position()).normalize();
                e.push(dir.x * 0.9, 0.3, dir.z * 0.9);
            }
            snd(p, SoundEvents.RAVAGER_ROAR);
            return true;
        });

        reg("stomp", "Séisme", 16, "Frappe le sol : 4 dégâts et projection des monstres proches.", (p, pw) -> {
            for (LivingEntity e : foes(p, 4.5)) {
                e.hurt(p.damageSources().playerAttack(p), (float) (4 * pw));
                Vec3 dir = e.position().subtract(p.position()).normalize();
                e.push(dir.x * 0.6, 0.5, dir.z * 0.6);
                eff(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
            }
            part(p, ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 40);
            snd(p, SoundEvents.IRON_GOLEM_ATTACK);
            return true;
        });

        reg("frenzy", "Frénésie", 30, "Force I et Vitesse II pendant 8 s.", (p, pw) -> {
            eff(p, MobEffects.DAMAGE_BOOST, 160, 0);
            eff(p, MobEffects.MOVEMENT_SPEED, 160, 1);
            snd(p, SoundEvents.RAVAGER_ROAR);
            return true;
        });

        reg("stoneskin", "Peau de pierre", 25, "Résistance II pendant 8 s, mais ralenti.", (p, pw) -> {
            eff(p, MobEffects.DAMAGE_RESISTANCE, 160, 1);
            eff(p, MobEffects.MOVEMENT_SLOWDOWN, 160, 0);
            snd(p, SoundEvents.IRON_GOLEM_ATTACK);
            return true;
        });

        reg("smoke", "Voile d'ombre", 25, "Invisible 8 s, rapide, les monstres te perdent de vue.", (p, pw) -> {
            eff(p, MobEffects.INVISIBILITY, 160, 0);
            eff(p, MobEffects.MOVEMENT_SPEED, 160, 0);
            for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16))) {
                if (m.getTarget() == p) m.setTarget(null);
            }
            part(p, ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 30);
            return true;
        });

        reg("sprite", "Pas de plume", 20, "Chute lente 20 s, Vitesse II et Saut II 6 s.", (p, pw) -> {
            eff(p, MobEffects.SLOW_FALLING, 400, 0);
            eff(p, MobEffects.MOVEMENT_SPEED, 120, 1);
            eff(p, MobEffects.JUMP, 120, 1);
            return true;
        });

        reg("reveal", "Œil perçant", 30, "Fait briller les monstres à 24 blocs et donne la vision nocturne.", (p, pw) -> {
            for (LivingEntity e : foes(p, 24)) eff(e, MobEffects.GLOWING, 160, 0);
            eff(p, MobEffects.NIGHT_VISION, 400, 0);
            return true;
        });

        reg("entangle", "Racines", 18, "Immobilise presque les monstres proches 3,5 s.", (p, pw) -> {
            for (LivingEntity e : foes(p, 7)) {
                eff(e, MobEffects.MOVEMENT_SLOWDOWN, 70, 5);
                part(p, ParticleTypes.COMPOSTER, e.getX(), e.getY() + 0.5, e.getZ(), 10);
            }
            snd(p, SoundEvents.EVOKER_CAST_SPELL);
            return true;
        });

        reg("surge", "Marée", 25, "Grâce du dauphin, respiration aquatique 15 s, régénération.", (p, pw) -> {
            eff(p, MobEffects.DOLPHINS_GRACE, 300, 0);
            eff(p, MobEffects.WATER_BREATHING, 300, 0);
            eff(p, MobEffects.REGENERATION, 80, 1);
            part(p, ParticleTypes.SPLASH, p.getX(), p.getY() + 1, p.getZ(), 30);
            return true;
        });

        reg("firebreath", "Souffle ardent", 14, "Cône de flammes : 5 dégâts + brûlure (ne met pas le feu au terrain).", (p, pw) -> {
            Vec3 look = p.getLookAngle();
            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6))) {
                if (e == p || !e.isAlive() || !(e instanceof Enemy || e instanceof Player)) continue;
                Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(p.getEyePosition());
                if (to.length() <= 6 && to.normalize().dot(look) > 0.75) {
                    e.hurt(p.damageSources().indirectMagic(p, p), (float) (5 * pw));
                    e.setRemainingFireTicks(80);
                }
            }
            for (int i = 1; i <= 6; i++) {
                Vec3 q = p.getEyePosition().add(look.scale(i));
                part(p, ParticleTypes.FLAME, q.x, q.y, q.z, 6);
            }
            snd(p, SoundEvents.BLAZE_SHOOT);
            return true;
        });
    }

    private Actives() {}
}
