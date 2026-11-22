package dev.fluti.visuals.feature;

import com.mojang.blaze3d.platform.NativeImage;
import dev.fluti.visuals.FlutiClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Зміна кольору glint БЕЗ міксинів у рендер: генеруємо ресурспак з перефарбованими
 * enchanted_glint_item/entity.png (беремо ванільні з найнижчого пакета) і вмикаємо його.
 * Це працює однаково з Sodium, а Iris-шейдерпаки, що мають власний glint, можуть його перекривати.
 */
public final class GlintPack {
    public static final String[] COLORS = {"DEFAULT", "CYAN", "BLUE", "LAVA", "CRIMSON", "EMERALD"};
    private static final String PACK = "FlutiGlint";
    private static final String[] FILES = {"enchanted_glint_item", "enchanted_glint_entity"};

    private GlintPack() {}

    private static int[] tint(String name) {
        return switch (name) {
            case "CYAN" -> new int[]{40, 230, 255};
            case "BLUE" -> new int[]{50, 90, 255};
            case "LAVA" -> new int[]{255, 120, 20};
            case "CRIMSON" -> new int[]{230, 20, 70};
            case "EMERALD" -> new int[]{30, 230, 110};
            default -> null;
        };
    }

    public static void apply(String color) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int[] t = tint(color);
        Path dir = mc.getResourcePackDir().resolve(PACK);
        try {
            if (t != null) {
                Files.createDirectories(dir.resolve("assets/minecraft/textures/misc"));
                // pack_format для 1.21.11 перевір у wiki/Mojang changelog, якщо пак показується як "несумісний"
                Files.writeString(dir.resolve("pack.mcmeta"),
                        "{\"pack\":{\"pack_format\":75,\"description\":\"Fluti glint (auto-generated)\"}}");
                for (String f : FILES) {
                    Identifier id = Identifier.ofVanilla("textures/misc/" + f + ".png");
                    List<Resource> all = mc.getResourceManager().getAllResources(id);
                    if (all.isEmpty()) continue;
                    try (InputStream in = all.get(0).getInputStream(); // [0] = ванільний, щоб не фарбувати двічі
                         NativeImage img = NativeImage.read(in)) {
                        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
                            int argb = img.getColorArgb(x, y);
                            int a = argb >>> 24;
                            float l = (((argb >> 16) & 255) + ((argb >> 8) & 255) + (argb & 255)) / 765f;
                            l = Math.min(1f, l * 1.6f);
                            img.setColorArgb(x, y, (a << 24) | ((int) (t[0] * l) << 16) | ((int) (t[1] * l) << 8) | (int) (t[2] * l));
                        }
                        img.writeTo(dir.resolve("assets/minecraft/textures/misc/" + f + ".png"));
                    }
                }
            }
            ResourcePackManager m = mc.getResourcePackManager();
            m.scanPacks();
            String key = "file/" + PACK;
            if (t != null) m.enable(key); else m.disable(key);
            mc.options.refreshResourcePacks(m);  // зберігає список у options.txt і перезавантажує
        } catch (Throwable e) {
            FlutiClient.LOG.warn("Glint pack failed", e);
        }
    }
}
