package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/**
 * Косметика на гравці: китайський солом'яний капелюх, кільце над головою, крила, God mode.
 * Усе з клієнтських частинок: бачиш тільки ти (від третьої особи), без впливу на геймплей.
 */
public final class Cosmetics {
    private static int tickN;

    private Cosmetics() {}

    public static void init() { ClientTickEvents.END_CLIENT_TICK.register(Cosmetics::tick); }

    private static ParticleEffect pe(String n) {
        return n.equals("STRAW") ? ParticleTypes.CRIT : KillFx.beamParticle(n);
    }

    private static void tick(MinecraftClient mc) {
        var p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null || mc.options.getPerspective().isFirstPerson()) return;
        FlutiConfig c = ConfigManager.get();
        tickN++;
        String ring = c.headRing, wings = c.wings;
        if (c.god) { if (ring.equals("OFF")) ring = "GOLD"; if (wings.equals("OFF")) wings = "WHITE"; }
        double top = p.getY() + p.getEyeHeight(p.getPose()) + 0.22;

        // китайський капелюх: конус із кілець (поля, потім звуження до вершини)
        if (c.hat.equals("CHINESE")) {
            boolean straw = c.hatColor.equals("STRAW");
            if (straw || tickN % 6 == 0) {
                ParticleEffect e = pe(c.hatColor);
                double s = c.hatScale;
                for (int k = 0; k < 5; k++) {
                    double r = 0.95 * s * (1 - k * 0.19), y = top + k * 0.11 * s;
                    int n = Math.max(6, (int) (r * 16));
                    for (int i = 0; i < n; i++) {
                        double a = i * 2 * Math.PI / n;
                        w.addParticleClient(e, p.getX() + Math.cos(a) * r, y, p.getZ() + Math.sin(a) * r, 0, 0, 0);
                    }
                }
                w.addParticleClient(e, p.getX(), top + 0.55 * s, p.getZ(), 0, 0, 0);
            }
        }

        // кільце над головою (німб)
        if (!ring.equals("OFF") && tickN % 2 == 0) {
            ParticleEffect e = pe(ring);
            double rr = 0.5 * c.ringScale, y = top + 0.45 * c.ringScale;
            for (int i = 0; i < 14; i++) {
                double a = tickN * 0.05 + i * 2 * Math.PI / 14;
                w.addParticleClient(e, p.getX() + Math.cos(a) * rr, y, p.getZ() + Math.sin(a) * rr, 0, 0, 0);
            }
        }

        // крила зі світла на спині
        if (!wings.equals("OFF") && tickN % 2 == 0) {
            ParticleEffect e = pe(wings);
            double by = Math.toRadians(p.getBodyYaw());
            double fx = -Math.sin(by), fz = Math.cos(by), rx = -Math.cos(by), rz = -Math.sin(by);
            double flap = Math.sin(tickN * 0.25) * 0.12 + (p.getVelocity().horizontalLengthSquared() > 0.002 ? 0.12 : 0);
            for (int side = -1; side <= 1; side += 2) {
                for (int k = 0; k < 6; k++) {
                    double len = 0.45 + 0.2 * k, ang = Math.toRadians(75 - k * 16) + flap;
                    for (double t = 0.5; t <= 1.0; t += 0.5) {
                        double lat = Math.cos(ang) * len * t, up = Math.sin(ang) * len * t;
                        double back = -0.25 - 0.07 * k * t;
                        w.addParticleClient(e,
                                p.getX() + fx * back + rx * side * (0.15 + lat),
                                p.getY() + 1.35 + up,
                                p.getZ() + fz * back + rz * side * (0.15 + lat), 0, 0, 0);
                    }
                }
            }
        }
    }
}
