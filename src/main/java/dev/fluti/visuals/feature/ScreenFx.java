package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Кольорокорекція та сяйво країв екрана. Малюється як звичайний HUD-шар, сумісно з Sodium та Iris. */
public final class ScreenFx {
    private ScreenFx() {}

    public static void render(DrawContext ctx) {
        FlutiConfig c = ConfigManager.get();
        var p = MinecraftClient.getInstance().player;
        if (p == null) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();

        int tint = switch (c.tint) {
            case "WARM" -> 0xFFA040;
            case "COLD" -> 0x4090FF;
            case "PINK" -> 0xFF70C0;
            case "GREEN" -> 0x40D080;
            case "DARK" -> 0x000000;
            default -> -1;
        };
        if (tint >= 0) ctx.fill(0, 0, w, h, (0x24 << 24) | tint);

        int rgb = -1, maxA = 0x60;
        switch (c.vignette) {
            case "HEALTH" -> {
                if (p.getAbsorptionAmount() > 0) rgb = 0xFFC940;
                else if (p.getHealth() <= 6) { rgb = 0xFF2020; maxA = (int) (0x50 + 0x40 * Math.abs(Math.sin(System.currentTimeMillis() / 300.0))); }
                else if (p.getHealth() >= p.getMaxHealth()) rgb = 0x4D9FFF;
            }
            case "DARK" -> { rgb = 0x000000; maxA = 0xA0; }
            case "BLUE" -> rgb = 0x4D9FFF;
            case "GOLD" -> rgb = 0xFFC940;
            default -> { }
        }
        if (rgb < 0) return;
        int band = Math.max(w, h) / 9;
        for (int i = 0; i < band; i += 2) {
            double k = 1.0 - (double) i / band;
            int col = ((int) (maxA * k * k) << 24) | rgb;
            ctx.fill(i, i, w - i, i + 2, col);
            ctx.fill(i, h - i - 2, w - i, h - i, col);
            ctx.fill(i, i + 2, i + 2, h - i - 2, col);
            ctx.fill(w - i - 2, i + 2, w - i, h - i - 2, col);
        }
    }
}
