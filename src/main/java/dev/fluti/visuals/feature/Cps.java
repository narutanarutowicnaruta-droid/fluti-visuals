package dev.fluti.visuals.feature;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/** Лічильник кліків за секунду (тільки показ, нічого не клікає). */
public final class Cps {
    private static final ArrayDeque<Long> L = new ArrayDeque<>(), R = new ArrayDeque<>();
    private static boolean ld, rd;

    private Cps() {}

    public static void update() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) { ld = false; rd = false; return; }
        long h = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (l && !ld) L.add(now);
        if (r && !rd) R.add(now);
        ld = l; rd = r;
    }

    private static int count(ArrayDeque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    public static int left() { return count(L); }
    public static int right() { return count(R); }
}
