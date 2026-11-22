package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;

public final class GreenScreen {
    public static final int COLOR = 0x00B140; // стандартний chroma-green
    private static CloudRenderMode saved;

    private GreenScreen() {}

    public static boolean on() { return ConfigManager.get().greenScreen; }

    /** Хмари вимикаємо через налаштування, а оригінал відновлюємо при вимкненні. */
    public static void apply(boolean enable) {
        var opt = MinecraftClient.getInstance().options.getCloudRenderMode();
        if (enable) { saved = opt.getValue(); opt.setValue(CloudRenderMode.OFF); }
        else if (saved != null) { opt.setValue(saved); saved = null; }
    }
}
