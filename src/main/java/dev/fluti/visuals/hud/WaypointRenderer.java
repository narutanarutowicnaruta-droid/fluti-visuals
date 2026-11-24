package dev.fluti.visuals.hud;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import dev.fluti.visuals.feature.Opts;
import dev.fluti.visuals.feature.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/**
 * Мітки на HUD: власна проєкція світової точки на екран (камера, FOV), без хуків у рендер світу,
 * тому Sodium та Iris не заважають. Видно до 1 000 000 блоків. Ефекти: світіння, пульс, іскри навколо кола.
 */
public final class WaypointRenderer {
    private static final int[] GLOW_A = {60, 38, 20};

    private WaypointRenderer() {}

    public static void render(DrawContext ctx, float tp) {
        FlutiConfig c = ConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!c.showMarks || mc.player == null || mc.world == null || mc.options.hudHidden) return;
        var p = mc.player;
        Vec3d eye = p.getLerpedPos(tp).add(0, p.getEyeHeight(p.getPose()), 0);
        float yawDeg = p.getYaw(tp), pitchDeg = p.getPitch(tp);
        if (dev.fluti.visuals.feature.Freelook.active) { yawDeg = dev.fluti.visuals.feature.Freelook.yaw; pitchDeg = dev.fluti.visuals.feature.Freelook.pitch; }
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        double sY = Math.sin(yaw), cY = Math.cos(yaw), sP = Math.sin(pitch), cP = Math.cos(pitch);
        double fx = -sY * cP, fy = -sP, fz = cY * cP;       // вперед
        double rx = -cY, rz = -sY;                          // вправо
        double ux = -sY * sP, uy = cP, uz = cY * sP;        // вгору
        double t = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        double aspect = (double) w / h;
        TextRenderer tr = mc.textRenderer;
        long now = System.currentTimeMillis();
        String fxMode = c.markFx;
        boolean glow = fxMode.equals("GLOW") || fxMode.equals("ALL");
        boolean pulse = fxMode.equals("PULSE") || fxMode.equals("ALL");
        boolean orbit = fxMode.equals("ORBIT") || fxMode.equals("ALL");

        for (FlutiConfig.Waypoint m : WaypointManager.here()) {
            double dx = m.x - eye.x, dy = m.y - eye.y, dz = m.z - eye.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 1_000_000) continue;
            double z = dx * fx + dy * fy + dz * fz;
            if (z < 0.1) continue;                           // позаду камери
            double x = dx * rx + dz * rz;
            double y = dx * ux + dy * uy + dz * uz;
            int sx = (int) (w / 2.0 + x / (z * t * aspect) * w / 2.0);
            int sy = (int) (h / 2.0 - y / (z * t) * h / 2.0);
            if (sx < -40 || sx > w + 40 || sy < -40 || sy > h + 40) continue;

            int rgb = Opts.markRgb(c.markColor, m.color);
            int col = 0xFF000000 | rgb;

            if (glow) {                                       // м'яке світіння навколо лінії й кола
                double br = 0.75 + 0.25 * Math.sin(now / 400.0);
                for (int g = 3; g >= 1; g--) {
                    int a = (int) (24 * (4 - g) * br);
                    ctx.fill(sx - g, sy - 18, sx + 1 + g, sy + 19, (a << 24) | rgb);
                }
                for (int i = 0; i < 3; i++) ring(ctx, sx, sy, 7 + i, (GLOW_A[i] << 24) | rgb);
            }
            if (pulse) {                                      // дві хвилі, що розходяться від кола
                for (int k = 0; k < 2; k++) {
                    double ph = ((now + k * 800L) % 1600L) / 1600.0;
                    ring(ctx, sx, sy, (int) (5 + ph * 16), ((int) (190 * (1 - ph)) << 24) | rgb);
                }
            }
            ctx.fill(sx, sy - 18, sx + 1, sy + 19, col);            // лінія крізь коло
            ring(ctx, sx, sy, 5, col);                               // коло
            ctx.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xFFFFFFFF);
            if (glow) {                                        // яскравий відблиск, що біжить по лінії
                int yy = sy - 18 + (int) (((now % 1500L) / 1500.0) * 33);
                ctx.fill(sx - 1, yy, sx + 2, yy + 5, 0xB0FFFFFF);
            }
            if (orbit) {                                       // три іскри крутяться навколо кола зі шлейфом
                for (int i = 0; i < 3; i++) {
                    for (int tr2 = 0; tr2 < 3; tr2++) {
                        double a = now / 700.0 * Math.PI + i * 2.094 - tr2 * 0.28;
                        int ox = (int) Math.round(sx + Math.cos(a) * 10), oy = (int) Math.round(sy + Math.sin(a) * 10);
                        int al = tr2 == 0 ? 0xFF : tr2 == 1 ? 0x99 : 0x44;
                        ctx.fill(ox, oy, ox + 2, oy + 2, (al << 24) | (tr2 == 0 ? 0xFFFFFF : rgb));
                    }
                }
            }

            String coords = (int) Math.floor(m.x) + " " + (int) Math.floor(m.y) + " " + (int) Math.floor(m.z);
            if (c.markDist) coords += "  " + fmt(dist);
            int ty = sy - 18 - 22;
            ctx.drawCenteredTextWithShadow(tr, m.name, sx, ty, col);
            ctx.drawCenteredTextWithShadow(tr, coords, sx, ty + 10, 0xFFFFFFFF);
        }
    }

    private static void ring(DrawContext ctx, int cx, int cy, int r, int argb) {
        int n = Math.max(24, r * 5);
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n;
            int px = (int) Math.round(cx + Math.cos(a) * r), py = (int) Math.round(cy + Math.sin(a) * r);
            ctx.fill(px, py, px + 1, py + 1, argb);
        }
    }

    private static String fmt(double d) {
        return d < 1000 ? (int) d + " m" : String.format(Locale.ROOT, "%.1f km", d / 1000.0);
    }
}
