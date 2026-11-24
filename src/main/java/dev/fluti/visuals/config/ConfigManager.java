package dev.fluti.visuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.fluti.visuals.FlutiClient;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fluti/config.json");
    private static FlutiConfig cfg = new FlutiConfig();

    private ConfigManager() {}
    public static FlutiConfig get() { return cfg; }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                FlutiConfig c = GSON.fromJson(Files.readString(FILE), FlutiConfig.class);
                if (c != null) cfg = c;
            }
        } catch (Exception e) { FlutiClient.LOG.warn("Config load failed, using defaults", e); }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(cfg));
        } catch (Exception e) { FlutiClient.LOG.warn("Config save failed", e); }
    }
}
