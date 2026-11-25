package dev.fluti.visuals.feature;

import dev.fluti.visuals.FlutiClient;
import dev.fluti.visuals.config.ConfigManager;
import net.minecraft.client.MinecraftClient;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Косметика через згенерований ресурспак FlutiCosmetics: модель меча Metka підміняє вигляд твоїх мечів.
 * Це лише картинка на твоєму клієнті; сервер і інші гравці бачать звичайний меч.
 */
public final class CosmeticsPack {
    public static final String PACK = "FlutiCosmetics";
    public static final List<String> MODELS = List.of("METKA", "CATANA");
    public static final List<String> TARGETS = List.of("DIAMOND", "NETHERITE", "IRON", "GOLDEN", "STONE", "WOODEN", "ALL");
    private static final List<String> ALL = List.of("diamond_sword", "netherite_sword", "iron_sword", "golden_sword", "stone_sword", "wooden_sword");

    private CosmeticsPack() {}

    public static void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var c = ConfigManager.get();
        Path root = mc.getResourcePackDir().resolve(PACK);
        try {
            if (c.swordSkin) {
                Files.createDirectories(root.resolve("assets/fluti/models/item"));
                Files.createDirectories(root.resolve("assets/fluti/textures/item"));
                Path items = root.resolve("assets/minecraft/items");
                Files.createDirectories(items);
                try (var s = Files.list(items)) { for (Path f : s.toList()) Files.deleteIfExists(f); }
                Files.writeString(root.resolve("pack.mcmeta"),
                        "{\"pack\":{\"pack_format\":75,\"description\":\"Fluti cosmetics (auto-generated)\"}}");
                for (String mdl : List.of("metka", "catana")) {
                    copy("/assets/fluti/cosmetics/" + mdl + "_model.json", root.resolve("assets/fluti/models/item/" + mdl + ".json"));
                    copy("/assets/fluti/cosmetics/" + mdl + "_texture.png", root.resolve("assets/fluti/textures/item/" + mdl + ".png"));
                }
                String chosen = c.swordModel.equals("CATANA") ? "catana" : "metka";
                List<String> ids = c.swordTarget.equals("ALL") ? ALL : List.of(c.swordTarget.toLowerCase() + "_sword");
                for (String id : ids)
                    Files.writeString(items.resolve(id + ".json"), "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"fluti:item/" + chosen + "\"}}");
            }
            var m = mc.getResourcePackManager();
            m.scanPacks();
            String key = "file/" + PACK;
            boolean enabled = m.getEnabledIds().contains(key);
            if (c.swordSkin) {
                if (enabled) mc.reloadResources();
                else { m.enable(key); mc.options.refreshResourcePacks(m); }
            } else if (enabled) {
                m.disable(key);
                mc.options.refreshResourcePacks(m);
            }
        } catch (Exception e) {
            FlutiClient.LOG.warn("Cosmetics pack failed", e);
        }
    }

    private static void copy(String res, Path dest) throws Exception {
        try (InputStream in = CosmeticsPack.class.getResourceAsStream(res)) {
            if (in == null) throw new IllegalStateException("missing " + res);
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
