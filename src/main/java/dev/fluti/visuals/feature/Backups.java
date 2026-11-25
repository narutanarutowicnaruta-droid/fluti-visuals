package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/** Авто-бекап конфігу перед зміною профілю або імпортом. Зберігаємо останні 10. */
public final class Backups {
    private Backups() {}

    private static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("fluti/backups"); }

    public static void make() {
        try {
            ConfigManager.save();
            Path cfg = FabricLoader.getInstance().getConfigDir().resolve("fluti/config.json");
            if (!Files.isRegularFile(cfg)) return;
            Files.createDirectories(dir());
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Files.copy(cfg, dir().resolve("config-" + ts + ".json"));
            List<Path> all;
            try (Stream<Path> s = Files.list(dir())) { all = s.sorted().toList(); }
            for (int i = 0; i < all.size() - 10; i++) Files.deleteIfExists(all.get(i));
        } catch (Exception ignored) { }
    }

    public static boolean restoreLatest() {
        try (Stream<Path> s = Files.list(dir())) {
            Path last = s.sorted().reduce((a, b) -> b).orElse(null);
            return last != null && Profiles.importJson(Files.readString(last), false);
        } catch (Exception e) { return false; }
    }
}
