package dev.fluti.visuals.feature;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.fluti.visuals.FlutiClient;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Твої .ogg потрапляють у згенерований ресурспак FlutiSounds. Звуки грає ВАНІЛЬНИЙ звуковий рушій:
 * h.<назва> для ударів/вбивств (короткі), s.<назва> для пісень (стрімінг). Після додавання файлу ресурси перезавантажуються.
 */
public final class SoundPack {
    public static final String PACK = "FlutiSounds";

    private SoundPack() {}

    public static Path root() { return MinecraftClient.getInstance().getResourcePackDir().resolve(PACK); }
    public static Path soundsDir() { return root().resolve("assets/fluti/sounds/custom"); }

    public static String sanitize(String s) {
        String n = s.toLowerCase();
        if (n.endsWith(".ogg")) n = n.substring(0, n.length() - 4);
        n = n.replaceAll("[^a-z0-9_]", "_");
        return n.isEmpty() ? "sound" : n;
    }

    public static List<String> names() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(soundsDir())) {
            s.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".ogg"))
                    .map(n -> n.substring(0, n.length() - 4)).sorted().forEach(out::add);
        } catch (Exception ignored) { }
        return out;
    }

    /** Копіює файл у пак, перебудовує sounds.json і перезавантажує ресурси. Повертає назву або null. */
    public static String add(Path src) {
        try {
            String name = sanitize(src.getFileName().toString());
            Files.createDirectories(soundsDir());
            Files.copy(src, soundsDir().resolve(name + ".ogg"), StandardCopyOption.REPLACE_EXISTING);
            rebuild();
            reload();
            return name;
        } catch (Exception e) {
            FlutiClient.LOG.warn("Cannot add sound {}", src, e);
            return null;
        }
    }

    public static void rebuild() throws Exception {
        Files.createDirectories(root());
        Files.writeString(root().resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":75,\"description\":\"Fluti sounds (auto-generated)\"}}");
        JsonObject all = new JsonObject();
        for (String n : names()) {
            all.add("h." + n, entry(n, false));
            all.add("s." + n, entry(n, true));
        }
        Files.createDirectories(root().resolve("assets/fluti"));
        Files.writeString(root().resolve("assets/fluti/sounds.json"), new Gson().toJson(all));
    }

    private static JsonObject entry(String n, boolean stream) {
        JsonObject snd = new JsonObject();
        snd.addProperty("name", "fluti:custom/" + n);
        if (stream) snd.addProperty("stream", true);
        JsonArray arr = new JsonArray();
        arr.add(snd);
        JsonObject o = new JsonObject();
        o.add("sounds", arr);
        return o;
    }

    public static void reload() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var m = mc.getResourcePackManager();
        m.scanPacks();
        String key = "file/" + PACK;
        if (m.getEnabledIds().contains(key)) mc.reloadResources();
        else { m.enable(key); mc.options.refreshResourcePacks(m); }
    }
}
