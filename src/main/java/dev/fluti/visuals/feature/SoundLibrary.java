package dev.fluti.visuals.feature;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

/** Пресети звуків + твої .ogg з .minecraft/config/fluti/sounds/ (з'являються в меню автоматично). */
public final class SoundLibrary {
    public static final LinkedHashMap<String, String> PRESETS = new LinkedHashMap<>();

    static {
        p("BELL", "block.bell.use");
        p("CLICK", "ui.button.click");
        p("PLING", "block.note_block.pling");
        p("CHIME", "block.amethyst_block.chime");
        p("HARP", "block.note_block.harp");
        p("BASS", "block.note_block.bass");
        p("SNARE", "block.note_block.snare");
        p("HAT", "block.note_block.hat");
        p("XYLOPHONE", "block.note_block.xylophone");
        p("BIT", "block.note_block.bit");
        p("COWBELL", "block.note_block.cow_bell");
        p("FLUTE", "block.note_block.flute");
        p("GUITAR", "block.note_block.guitar");
        p("ORB", "entity.experience_orb.pickup");
        p("LEVELUP", "entity.player.levelup");
        p("ARROW_HIT", "entity.arrow.hit_player");
        p("ANVIL", "block.anvil.land");
        p("GLASS", "block.glass.break");
        p("TOTEM", "item.totem.use");
        p("THUNDER", "entity.lightning_bolt.thunder");
        p("BEACON", "block.beacon.activate");
        p("BEACON_POWER", "block.beacon.power_select");
        p("TRIDENT", "item.trident.thunder");
        p("CRIT", "entity.player.attack.crit");
        p("SWEEP", "entity.player.attack.sweep");
        p("STRONG", "entity.player.attack.strong");
        p("SHIELD_BREAK", "item.shield.break");
        p("ENDER_DEATH", "entity.ender_eye.death");
    }

    private SoundLibrary() {}

    private static void p(String key, String id) { PRESETS.put(key, id); }

    public static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("fluti/sounds"); }

    public static List<String> options() {
        List<String> out = new ArrayList<>();
        out.add("OFF");
        out.addAll(PRESETS.keySet());
        try (Stream<Path> s = Files.list(dir())) {
            s.map(f -> f.getFileName().toString())
                    .filter(n -> n.toLowerCase().endsWith(".ogg"))
                    .sorted()
                    .forEach(n -> out.add("FILE:" + n));
        } catch (Exception ignored) { }
        return out;
    }

    public static void play(String choice, float vol, float pitch, String fallbackFile) {
        if (choice == null || choice.equals("OFF")) return;
        float v = Math.max(0f, Math.min(1f, vol));
        if (choice.equals("CUSTOM")) choice = "FILE:" + fallbackFile;
        if (choice.startsWith("FILE:")) {
            OggPlayer.play(dir().resolve(choice.substring(5)), v, pitch);
            return;
        }
        String id = PRESETS.get(choice);
        if (id != null) ui(id, pitch, v);
    }

    /** Будь-який ванільний звук за id, напр. "ui.button.click". */
    public static void ui(String id, float pitch, float vol) {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvent.of(Identifier.ofVanilla(id)), pitch, vol));
    }
}
