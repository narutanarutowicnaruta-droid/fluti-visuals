package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.concurrent.ThreadLocalRandom;

/** Клієнтські ефекти довкола тебе: шлейфи, аура, кроки, кола стрибка, тотем, погода. Лише частинки на твоєму екрані. */
public final class Ambient {
    private static boolean wasGround = true;
    private static double lastVy;
    private static int tickN, totemPrev = -1, popCheck;

    private Ambient() {}

    public static void init() { ClientTickEvents.END_CLIENT_TICK.register(Ambient::tick); }

    private static int totems(PlayerEntity p) {
        int n = 0;
        if (p.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) n += p.getMainHandStack().getCount();
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) n += p.getOffHandStack().getCount();
        return n;
    }

    private static ParticleEffect step(String n) {
        return switch (n) {
            case "SPARKS" -> ParticleTypes.ELECTRIC_SPARK;
            case "SNOW" -> ParticleTypes.SNOWFLAKE;
            case "SMOKE" -> ParticleTypes.SMOKE;
            case "FLAMES" -> ParticleTypes.FLAME;
            case "MAGIC" -> ParticleTypes.ENCHANTED_HIT;
            default -> null;
        };
    }

    private static void tick(MinecraftClient mc) {
        var p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) { totemPrev = -1; return; }
        FlutiConfig c = ConfigManager.get();
        var r = ThreadLocalRandom.current();
        tickN++;

        // 1) шлейф за стрілами, перлами, сніжками (лише те, що вже летить у повітрі)
        if (!c.trail.equals("OFF")) {
            ParticleEffect pe = KillFx.beamParticle(c.trail);
            for (Entity e : w.getEntities()) {
                if (e instanceof ProjectileEntity && !e.isOnGround() && e.getVelocity().lengthSquared() > 0.04)
                    w.addParticleClient(pe, e.getX(), e.getY(), e.getZ(), 0, 0, 0);
            }
        }

        // 2) аура-кільце під ногами (від третьої особи)
        if (!c.halo.equals("OFF") && !mc.options.getPerspective().isFirstPerson() && tickN % 2 == 0) {
            ParticleEffect pe = KillFx.beamParticle(c.halo);
            for (int i = 0; i < 10; i++) {
                double a = tickN * 0.15 + i * 0.628;
                w.addParticleClient(pe, p.getX() + Math.cos(a) * 0.7, p.getY() + 0.05, p.getZ() + Math.sin(a) * 0.7, 0, 0, 0);
            }
        }

        // 3) частинки кроків при бігу
        ParticleEffect sp = step(c.stepFx);
        if (sp != null && p.isOnGround() && p.isSprinting()) {
            w.addParticleClient(sp, p.getX() + (r.nextDouble() - 0.5) * 0.3, p.getY() + 0.05, p.getZ() + (r.nextDouble() - 0.5) * 0.3,
                    (r.nextDouble() - 0.5) * 0.05, 0.03, (r.nextDouble() - 0.5) * 0.05);
        }

        // 4) кола при стрибку та приземленні
        boolean g = p.isOnGround();
        if (!c.jumpRing.equals("OFF")) {
            if ((g && !wasGround && lastVy < -0.5) || (!g && wasGround && p.getVelocity().y > 0.2))
                KillFx.ring(w, p.getX(), p.getY() + 0.1, p.getZ(), KillFx.beamParticle(c.jumpRing), 0.2);
        }
        wasGround = g;
        lastVy = p.getVelocity().y;

        // 5) конфеті при спрацюванні тотема (по зникненню тотема + ефектах після попу)
        int t = totems(p);
        if (!c.totemFx.equals("OFF") && totemPrev >= 0 && t < totemPrev) popCheck = 4;
        totemPrev = t;
        if (popCheck > 0) {
            popCheck--;
            if (p.getAbsorptionAmount() > 0 && p.hasStatusEffect(StatusEffects.REGENERATION)) {
                popCheck = 0;
                confetti(w, p, c.totemFx);
            }
        }

        // 6) погода лише для тебе
        if (!c.weather.equals("OFF") && w.isSkyVisible(p.getBlockPos())) {
            ParticleEffect wp = switch (c.weather) {
                case "SNOW" -> ParticleTypes.SNOWFLAKE;
                case "RAIN" -> ParticleTypes.FALLING_WATER;
                case "ASH" -> ParticleTypes.ASH;
                case "PETALS" -> ParticleTypes.CHERRY_LEAVES;
                default -> null;
            };
            if (wp != null) {
                double vy = c.weather.equals("RAIN") ? -0.4 : -0.06;
                for (int i = 0; i < 8; i++) {
                    w.addParticleClient(wp, p.getX() + (r.nextDouble() - 0.5) * 24, p.getY() + 6 + r.nextDouble() * 5,
                            p.getZ() + (r.nextDouble() - 0.5) * 24, (r.nextDouble() - 0.5) * 0.04, vy, (r.nextDouble() - 0.5) * 0.04);
                }
            }
        }
    }

    private static void confetti(ClientWorld w, PlayerEntity p, String mode) {
        var r = ThreadLocalRandom.current();
        ParticleEffect[] set = mode.equals("BURST")
                ? new ParticleEffect[]{ParticleTypes.TOTEM_OF_UNDYING}
                : new ParticleEffect[]{ParticleTypes.FIREWORK, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.WITCH,
                        ParticleTypes.END_ROD, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.FLAME};
        for (int i = 0; i < 60; i++) {
            w.addParticleClient(set[r.nextInt(set.length)], p.getX(), p.getY() + 1.0, p.getZ(),
                    (r.nextDouble() - 0.5) * 0.9, r.nextDouble() * 0.6, (r.nextDouble() - 0.5) * 0.9);
        }
    }
}
