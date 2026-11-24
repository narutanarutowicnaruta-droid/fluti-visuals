package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

/**
 * Плавний зум на клавішу через налаштування FOV (без міксинів у рендер, без читерських переваг:
 * просто «підзорна труба» для огляду). Викликається щокадру; при виході FOV повертається.
 */
public final class Zoom {
    private static float cur = -1f;
    private static int orig;

    private Zoom() {}

    public static void update() {
        var c = ConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean want = c.zoomEnabled && c.zoomKey > 0 && mc.player != null && mc.currentScreen == null
                && InputUtil.isKeyPressed(mc.getWindow(), c.zoomKey);
        var opt = mc.options.getFov();
        if (cur < 0) {
            if (!want) return;
            orig = opt.getValue();
            cur = orig;
        }
        float target = want ? Math.max(30f, orig / Math.max(1.2f, c.zoomFactor)) : orig;
        cur += (target - cur) * 0.3f;
        if (!want && Math.abs(cur - orig) < 0.6f) {
            opt.setValue(orig);
            cur = -1f;
            return;
        }
        opt.setValue(Math.max(30, Math.min(110, Math.round(cur))));
    }

    /** Викликається при закритті гри, щоб у options.txt не лишився «зумлений» FOV. */
    public static void restore() {
        if (cur >= 0) {
            MinecraftClient.getInstance().options.getFov().setValue(orig);
            cur = -1f;
        }
    }
}
