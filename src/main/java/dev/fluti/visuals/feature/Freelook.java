package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

/**
 * Огляд камерою: поки тримаєш клавішу, миша крутить ТІЛЬКИ камеру, а гравець (тіло, приціл, удари) лишається як був.
 * У третій особі камера обертається навколо гравця, і її, як у ваніли, не пускають крізь блоки.
 * У першій особі камера лишається в очах гравця й просто повертається.
 */
public final class Freelook {
    public static boolean active;
    public static float yaw, pitch;

    private Freelook() {}

    /** Викликається щокадру (з обробника миші). */
    public static void update() {
        var c = ConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean want = c.freelook && c.freelookKey > 0 && mc.player != null && mc.currentScreen == null
                && InputUtil.isKeyPressed(mc.getWindow(), c.freelookKey);
        if (want && !active) { yaw = mc.player.getYaw(); pitch = mc.player.getPitch(); }
        active = want;
    }

    public static void rotate(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
    }
}
