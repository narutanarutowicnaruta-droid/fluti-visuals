package dev.fluti.visuals.feature;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Профілі налаштувань: вбудовані пресети, власні файли та обмін через буфер обміну. */
public final class Profiles {
    public static final List<String> BUILT_IN = List.of("Default", "PvP", "Creator", "Cinema", "Beauty");
    // Це профіль НЕ перезаписує: мітки, макроси, мова, тема, клавіші, колір glint (щоб не перезавантажувати ресурси).
    private static final Set<String> SKIP = Set.of("waypoints", "macros", "lang", "theme", "profile",
            "freelookKey", "clipKey", "zoomKey", "glintColor");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Profiles() {}

    public static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("fluti/profiles"); }

    public static List<String> names() {
        List<String> out = new ArrayList<>(BUILT_IN);
        try (Stream<Path> s = Files.list(dir())) {
            s.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5)).sorted()
                    .filter(n -> !out.contains(n)).forEach(out::add);
        } catch (Exception ignored) { }
        return out;
    }

    private static FlutiConfig builtin(String name) {
        FlutiConfig c = new FlutiConfig();
        switch (name) {
            case "PvP" -> {
                c.hitmarkers = true; c.hitSound = "BELL"; c.killSound = "LEVELUP"; c.critParticles = "CRIT";
                c.stats.enabled = true; c.markFx = "OFF"; c.killBeam = "OFF";
            }
            case "Creator" -> {
                c.cinematic = true; c.cinematicSmooth = 0.12f; c.maskPasswords = true; c.hitmarkers = false;
                c.armor.enabled = false; c.potions.enabled = false; c.ping.enabled = false;
            }
            case "Cinema" -> {
                c.cinematic = true; c.cinematicSmooth = 0.08f; c.hitmarkers = false; c.killBeam = "LIGHTNING";
                c.markFx = "ALL"; c.armor.enabled = false; c.potions.enabled = false; c.ping.enabled = false;
            }
            case "Beauty" -> {
                c.critParticles = "STARS"; c.hitParticles = "HEARTS"; c.killBeam = "BLUE"; c.killBurst = "TOTEM";
                c.markFx = "ALL"; c.hitmarkers = false;
            }
            default -> { }
        }
        return c;
    }

    private static void copyInto(FlutiConfig dst, FlutiConfig src, boolean positions) {
        for (Field f : FlutiConfig.class.getFields()) {
            int m = f.getModifiers();
            if (Modifier.isStatic(m) || Modifier.isTransient(m) || SKIP.contains(f.getName())) continue;
            try {
                Object v = f.get(src);
                if (v == null) continue;
                if (v instanceof FlutiConfig.HudCfg h) {
                    FlutiConfig.HudCfg d = (FlutiConfig.HudCfg) f.get(dst);
                    d.enabled = h.enabled;
                    if (positions) { d.x = h.x; d.y = h.y; }
                } else f.set(dst, v);
            } catch (Exception ignored) { }
        }
    }

    private static void finish(FlutiConfig cur, boolean wasGreen) {
        if (wasGreen != cur.greenScreen) GreenScreen.apply(cur.greenScreen);
        ConfigManager.save();
    }

    public static boolean apply(String name) {
        FlutiConfig cur = ConfigManager.get();
        boolean wasGreen = cur.greenScreen;
        try {
            if (BUILT_IN.contains(name)) copyInto(cur, builtin(name), false);
            else {
                Path f = dir().resolve(name + ".json");
                if (!Files.isRegularFile(f)) return false;
                FlutiConfig src = GSON.fromJson(Files.readString(f), FlutiConfig.class);
                if (src == null) return false;
                copyInto(cur, src, true);
            }
        } catch (Exception e) { return false; }
        cur.profile = name;
        finish(cur, wasGreen);
        return true;
    }

    public static boolean saveAs(String name) {
        if (name == null) return false;
        String n = name.trim().replaceAll("[^\\p{L}\\p{N} _-]", "");
        if (n.isEmpty() || BUILT_IN.contains(n)) return false;
        try {
            Files.createDirectories(dir());
            Files.writeString(dir().resolve(n + ".json"), GSON.toJson(ConfigManager.get()));
            ConfigManager.get().profile = n;
            ConfigManager.save();
            return true;
        } catch (Exception e) { return false; }
    }

    public static String exportJson() { return GSON.toJson(ConfigManager.get()); }

    public static boolean importJson(String json) {
        if (json == null || json.isBlank()) return false;
        FlutiConfig cur = ConfigManager.get();
        boolean wasGreen = cur.greenScreen;
        try {
            FlutiConfig src = GSON.fromJson(json, FlutiConfig.class);
            if (src == null) return false;
            copyInto(cur, src, true);
        } catch (Exception e) { return false; }
        finish(cur, wasGreen);
        return true;
    }
}
