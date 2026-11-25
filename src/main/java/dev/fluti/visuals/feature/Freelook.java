package dev.fluti.visuals.feature;

import dev.fluti.visuals.FlutiClient;
import dev.fluti.visuals.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

/**
 * Огляд навколо гравця: поки тримаєш клавішу, камера вилітає з першої особи в третю й крутиться навколо тебе.
 * Тіло, приціл і удари лишаються як були. Камеру, як у ваніли, не пускає крізь блоки.
 * Захист: якщо камера не підхопила кут, мишу НЕ блокуємо.
 */
public final class Freelook {
    public static boolean active;
    public static float yaw, pitch;
    public static volatile long lastCamNs;
    private static long activatedNs;
    private static boolean warned;
    private static Perspective saved;

    private Freelook() {}

    public static void update() {
        var c = ConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean want = c.freelook && c.freelookKey > 0 && mc.player != null && mc.currentScreen == null
                && InputUtil.isKeyPressed(mc.getWindow(), c.freelookKey);
        if (want && !active) {
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
            activatedNs = System.nanoTime();
            saved = mc.options.getPerspective();
            if (saved.isFirstPerson()) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);   // камера вилітає назад
        } else if (!want && active) {
            if (saved != null && saved.isFirstPerson()) mc.options.setPerspective(saved);           // і повертається
            saved = null;
        }
        active = want;
    }

    public static boolean hookWorks() {
        long now = System.nanoTime();
        if (lastCamNs >= activatedNs) return true;
        if (now - activatedNs < 250_000_000L) return true;
        if (!warned) {
            warned = true;
            FlutiClient.LOG.warn("Free look: camera hook did not fire. Mouse is not captured.");
            var mc = MinecraftClient.getInstance();
            if (mc.player != null) mc.player.sendMessage(Text.literal("Free look: camera hook not working, see latest.log"), true);
        }
        return false;
    }

    public static void rotate(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
    }
}
