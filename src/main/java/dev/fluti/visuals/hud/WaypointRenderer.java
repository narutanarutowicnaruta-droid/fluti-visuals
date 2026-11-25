package dev.fluti.visuals.hud;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import dev.fluti.visuals.feature.Opts;
import dev.fluti.visuals.feature.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/**
 * Мітки: 3D-кільце на землі навколо точки й вертикальна лінія, що йде вгору з центру кільця.
 * Усе малюється на HUD за власною проєкцією, видно до 1 000 000 блоків. Ефекти: світіння, пульс-хвилі, іскри, відблиск.
 */
public final class WaypointRenderer {
    private static final int N = 40;

    private WaypointRenderer() {}

    static void line(DrawContext c, double[] a, double[] b, int argb, int th) {
        if (a == null || b == null) return;
        int x0 = (int) a[0], y0 = (int) a[1], x1 = (int) b[0], y1 = (int) b[1];
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps > 1800) return;
        if (steps == 0) { c.fill(x0, y0, x0 + th, y0 + th, argb); return; }
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / steps, y = y0 + (y1 - y0) * i / steps;
            c.fill(x, y, x + th, y + th, argb);
        }
    }

    private static void ring(DrawContext c, Projector pr, double cx, double y, double cz, double r, int argb, int th) {
        double[] prev = null, first = null;
        for (int i = 0; i <= N; i++) {
            double a = i * 2 * Math.PI / N;
            double[] cur = pr.project(cx + Math.cos(a) * r, y, cz + Math.sin(a) * r);
            if (i == 0) first = cur; else line(c, prev, cur, argb, th);
            prev = cur;
        }
    }

    public static void render(DrawContext ctx, float tp) {
        FlutiConfig c = ConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!c.showMarks || mc.player == null || mc.world == null || mc.options.hudHidden) return;
        Projector pr = Projector.create(ctx, tp);
        TextRenderer tr = mc.textRenderer;
        long now = System.currentTimeMillis();
        String fx = c.markFx;
        boolean glow = fx.equals("GLOW") || fx.equals("ALL");
        boolean pulse = fx.equals("PULSE") || fx.equals("ALL");
        boolean orbit = fx.equals("ORBIT") || fx.equals("ALL");

        for (FlutiConfig.Waypoint m : WaypointManager.here()) {
            double dx = m.x - pr.eye.x, dy = m.y - pr.eye.y, dz = m.z - pr.eye.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 1_000_000) continue;
            double R = Math.min(60, 0.9 + dist * 0.02), H = Math.min(400, 3 + dist * 0.06);   // розмір росте з відстанню
            double gy = m.y + 0.05;
            int rgb = Opts.markRgb(c.markColor, m.color), col = 0xFF000000 | rgb;
            double[] base = pr.project(m.x, gy, m.z), top = pr.project(m.x, m.y + H, m.z);
            if (base == null && top == null) continue;

            if (glow) {                                       // м'яке світіння: товстіша напівпрозора копія
                double br = 0.75 + 0.25 * Math.sin(now / 400.0);
                int a = (int) (70 * br);
                ring(ctx, pr, m.x, gy, m.z, R, (a << 24) | rgb, 3);
                line(ctx, base, top, (a << 24) | rgb, 4);
            }
            if (pulse) {                                      // дві хвилі розходяться від кільця
                for (int k = 0; k < 2; k++) {
                    double ph = ((now + k * 900L) % 1800L) / 1800.0;
                    ring(ctx, pr, m.x, gy, m.z, R * (1 + ph * 0.9), ((int) (200 * (1 - ph)) << 24) | rgb, 1);
                }
            }
            ring(ctx, pr, m.x, gy, m.z, R, col, 2);          // 3D-кільце
            line(ctx, base, top, col, 2);                     // лінія вгору з центру кільця
            if (orbit) {                                      // іскри біжать по кільцю
                for (int i = 0; i < 3; i++) {
                    double a = now / 900.0 * Math.PI + i * 2.094;
                    double[] s = pr.project(m.x + Math.cos(a) * R, gy, m.z + Math.sin(a) * R);
                    if (s != null) ctx.fill((int) s[0] - 1, (int) s[1] - 1, (int) s[0] + 2, (int) s[1] + 2, 0xFFFFFFFF);
                }
            }
            if (glow) {                                       // відблиск біжить вгору по лінії
                double u = (now % 1600L) / 1600.0;
                double[] s = pr.project(m.x, m.y + H * u, m.z);
                if (s != null) ctx.fill((int) s[0] - 1, (int) s[1] - 3, (int) s[0] + 3, (int) s[1] + 3, 0xC0FFFFFF);
            }

            double[] lab = top != null ? top : base;
            int lx = (int) lab[0], ly = Math.max(24, (int) lab[1] - 26);
            String coords = (int) Math.floor(m.x) + " " + (int) Math.floor(m.y) + " " + (int) Math.floor(m.z);
            if (c.markDist) coords += "  " + fmt(dist);
            ctx.drawCenteredTextWithShadow(tr, m.name, lx, ly, col);
            ctx.drawCenteredTextWithShadow(tr, coords, lx, ly + 10, 0xFFFFFFFF);
        }
    }

    private static String fmt(double d) {
        return d < 1000 ? (int) d + " m" : String.format(Locale.ROOT, "%.1f km", d / 1000.0);
    }
}
