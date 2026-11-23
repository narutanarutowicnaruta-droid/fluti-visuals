package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.FlutiConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Промінь/блискавка з неба на місці вбивства. Лише клієнтські частинки: нічого не спавниться на сервері,
 * жодних шейдерів і рендер-хуків, тому працює з Sodium та Iris.
 */
public final class KillFx {
    private static final List<Fx> LIST = new ArrayList<>();
    private static final int BOLT_N = 56;
    private static final double BOLT_STEP = 0.75;

    private static final class Fx {
        double x, y, z; int age; String style;
        double[] bx = new double[BOLT_N], bz = new double[BOLT_N];
    }

    private KillFx() {}

    public static void trigger(double x, double y, double z, FlutiConfig c) {
        ClientWorld w = MinecraftClient.getInstance().world;
        if (w == null) return;
        if (!"OFF".equals(c.killBeam)) {
            Fx f = new Fx();
            f.x = x; f.y = y; f.z = z; f.style = c.killBeam;
            if (f.style.equals("LIGHTNING")) {
                double cx = 0, cz = 0;
                var r = ThreadLocalRandom.current();
                for (int i = 0; i < BOLT_N; i++) {
                    double k = 1.0 - (double) i / BOLT_N;      // біля землі відхилення менше
                    cx += (r.nextDouble() - 0.5) * 0.9 * k;
                    cz += (r.nextDouble() - 0.5) * 0.9 * k;
                    f.bx[i] = cx; f.bz[i] = cz;
                }
                double ex = f.bx[BOLT_N - 1], ez = f.bz[BOLT_N - 1];   // зводимо кінець у точку удару
                for (int i = 0; i < BOLT_N; i++) { f.bx[i] -= ex * i / (BOLT_N - 1.0); f.bz[i] -= ez * i / (BOLT_N - 1.0); }
            }
            LIST.add(f);
        }
        burst(w, x, y + 1.0, z, c.killBurst);
    }

    private static ParticleEffect beamParticle(String s) {
        return switch (s) {
            case "BLUE" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "CYAN" -> ParticleTypes.ELECTRIC_SPARK;
            case "GOLD" -> ParticleTypes.FLAME;
            case "RED" -> ParticleTypes.CRIMSON_SPORE;
            case "GREEN" -> ParticleTypes.HAPPY_VILLAGER;
            case "PURPLE" -> ParticleTypes.WITCH;
            default -> ParticleTypes.END_ROD;
        };
    }

    private static void burst(ClientWorld w, double x, double y, double z, String mode) {
        ParticleEffect p = switch (mode) {
            case "TOTEM" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "FIREWORK" -> ParticleTypes.FIREWORK;
            case "HEARTS" -> ParticleTypes.HEART;
            case "SOUL" -> ParticleTypes.SOUL;
            case "EXPLOSION" -> ParticleTypes.EXPLOSION;
            case "SMOKE" -> ParticleTypes.SMOKE;
            case "STARS" -> ParticleTypes.END_ROD;
            default -> null;
        };
        if (p == null) return;
        var r = ThreadLocalRandom.current();
        int n = mode.equals("EXPLOSION") ? 3 : 30;
        for (int i = 0; i < n; i++) {
            w.addParticleClient(p, x, y, z, (r.nextDouble() - 0.5) * 0.7, r.nextDouble() * 0.5, (r.nextDouble() - 0.5) * 0.7);
        }
    }

    public static void tick() {
        ClientWorld w = MinecraftClient.getInstance().world;
        if (w == null) { LIST.clear(); return; }
        var r = ThreadLocalRandom.current();
        Iterator<Fx> it = LIST.iterator();
        while (it.hasNext()) {
            Fx f = it.next();
            f.age++;
            if (f.style.equals("LIGHTNING")) {
                if (f.age > 8) { it.remove(); continue; }
                if (f.age % 2 == 0) continue;   // мерехтіння
                for (int i = 0; i < BOLT_N; i++) {
                    double yy = f.y + (BOLT_N - i) * BOLT_STEP;
                    w.addParticleClient(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK,
                            f.x + f.bx[i], yy, f.z + f.bz[i], 0, 0, 0);
                }
                continue;
            }
            if (f.age > 22) { it.remove(); continue; }
            ParticleEffect p = beamParticle(f.style);
            double top = 48, reach = Math.min(top, f.age * 10.0);
            for (double d = top; d >= Math.max(0, top - reach); d -= 0.7) {
                w.addParticleClient(p, f.x + (r.nextDouble() - 0.5) * 0.25, f.y + d, f.z + (r.nextDouble() - 0.5) * 0.25, 0, 0, 0);
                if (r.nextInt(4) == 0) w.addParticleClient(ParticleTypes.END_ROD, f.x, f.y + d, f.z, 0, 0, 0);
            }
            if (f.age == 5 || f.age == 12) {   // хвилі при ударі об землю
                for (int a = 0; a < 24; a++) {
                    double ang = a * Math.PI / 12.0;
                    w.addParticleClient(p, f.x, f.y + 0.1, f.z, Math.cos(ang) * 0.3, 0.02, Math.sin(ang) * 0.3);
                }
            }
        }
    }
}
