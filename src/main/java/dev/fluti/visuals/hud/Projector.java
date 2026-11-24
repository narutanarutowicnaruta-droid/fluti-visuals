package dev.fluti.visuals.hud;

import dev.fluti.visuals.feature.Freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

/** Проєкція світової точки на екран (камера, FOV). Без хуків у рендер світу, тому Sodium/Iris не заважають. */
public final class Projector {
    public final Vec3d eye;
    public final int w, h;
    private final double fx, fy, fz, rx, rz, ux, uy, uz, t, aspect, cx, cy, cz;

    private Projector(Vec3d eye, double yawDeg, double pitchDeg, double fov, int w, int h, boolean third) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        double sY = Math.sin(yaw), cY = Math.cos(yaw), sP = Math.sin(pitch), cP = Math.cos(pitch);
        fx = -sY * cP; fy = -sP; fz = cY * cP;
        rx = -cY; rz = -sY;
        ux = -sY * sP; uy = cP; uz = cY * sP;
        this.eye = eye;
        double back = third ? 4.0 : 0.0;                    // камера третьої особи стоїть на 4 блоки позаду
        cx = eye.x - fx * back; cy = eye.y - fy * back; cz = eye.z - fz * back;
        t = Math.tan(Math.toRadians(fov) / 2.0);
        this.w = w; this.h = h; aspect = (double) w / h;
    }

    public static Projector create(DrawContext ctx, float tp) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        Vec3d eye = p.getLerpedPos(tp).add(0, p.getEyeHeight(p.getPose()), 0);
        float yaw = p.getYaw(tp), pitch = p.getPitch(tp);
        if (Freelook.active) { yaw = Freelook.yaw; pitch = Freelook.pitch; }
        return new Projector(eye, yaw, pitch, mc.options.getFov().getValue(),
                ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), !mc.options.getPerspective().isFirstPerson());
    }

    /** {sx, sy, глибина} або null, якщо точка позаду камери. */
    public double[] project(double x, double y, double z) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth < 0.1) return null;
        double px = dx * rx + dz * rz;
        double py = dx * ux + dy * uy + dz * uz;
        return new double[]{w / 2.0 + px / (depth * t * aspect) * w / 2.0, h / 2.0 - py / (depth * t) * h / 2.0, depth};
    }
}
