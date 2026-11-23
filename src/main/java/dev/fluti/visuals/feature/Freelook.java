package dev.fluti.visuals.feature;

import dev.fluti.visuals.FlutiClient;
import dev.fluti.visuals.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

/**
 * Огляд камерою: поки тримаєш клавішу, миша крутить ТІЛЬКИ камеру, а гравець (тіло, приціл, удари) лишається як був.
 * Захист: якщо камера не підхопила кут (хук не спрацював), мишу НЕ блокуємо, і гравець крутиться як завжди.
 */
public final class Freelook {
    public static boolean active;
    public static float yaw, pitch;
    public static volatile long lastCamNs;          // коли камера востаннє взяла наш кут
    private static long activatedNs;
    private static boolean warned;

    private Freelook() {}

    /** Викликається щокадру з обробника миші. */
    public static void update() {
        var c = ConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean want = c.freelook && c.freelookKey > 0 && mc.player != null && mc.currentScreen == null
                && InputUtil.isKeyPressed(mc.getWindow(), c.freelookKey);
        if (want && !active) {
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
            activatedNs = System.nanoTime();
        }
        active = want;
    }

    /** true, якщо можна віддавати мишу камері (хук працює або ще йде коротка пауза після натискання). */
    public static boolean hookWorks() {
        long now = System.nanoTime();
        if (lastCamNs >= activatedNs) return true;           // камера вже підхопила кут
        if (now - activatedNs < 250_000_000L) return true;   // дамо камері кілька кадрів
        if (!warned) {
            warned = true;
            FlutiClient.LOG.warn("Free look: camera hook did not fire (Camera.update/setRotation). Free look disabled for safety.");
            var mc = MinecraftClient.getInstance();
            if (mc.player != null)
                mc.player.sendMessage(Text.literal("Free look: camera hook not working, see latest.log"), true);
        }
        return false;
    }

    public static void rotate(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
    }
}
