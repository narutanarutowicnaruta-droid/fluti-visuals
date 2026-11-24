package dev.fluti.visuals.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/** Пресети ванільних звуків + твої .ogg (через ресурспак FlutiSounds). Довгі звуки обрізаються до заданих секунд. */
public final class SoundLibrary {
    public static final LinkedHashMap<String, String> PRESETS = new LinkedHashMap<>();
    private static final List<Object[]> STOPS = new ArrayList<>();   // {SoundInstance, Integer ticksLeft}

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

    public static List<String> options() {
        List<String> out = new ArrayList<>();
        out.add("OFF");
        out.addAll(PRESETS.keySet());
        for (String n : SoundPack.names()) out.add("FILE:" + n + ".ogg");
        return out;
    }

    public static void play(String choice, float vol, float pitch, String fallbackFile) {
        play(choice, vol, pitch, fallbackFile, 0f);
    }

    /** maxSec больше 0: звук автоматично обрізається до цієї кількості секунд. */
    public static void play(String choice, float vol, float pitch, String fallbackFile, float maxSec) {
        if (choice == null || choice.equals("OFF") || choice.equals("CUSTOM")) return;
        float v = Math.max(0f, Math.min(1f, vol));
        if (choice.startsWith("FILE:")) {
            playPack("h." + SoundPack.sanitize(choice.substring(5)), v, pitch, maxSec);
            return;
        }
        String id = PRESETS.get(choice);
        if (id != null) ui(id, pitch, v);
    }

    /** Звук із ресурспаку FlutiSounds, напр. "h.hit" або "s.song". */
    public static SoundInstance playPack(String event, float vol, float pitch, float maxSec) {
        var sm = MinecraftClient.getInstance().getSoundManager();
        PositionedSoundInstance inst = PositionedSoundInstance.master(SoundEvent.of(Identifier.of("fluti", event)), pitch, vol);
        sm.play(inst);
        if (maxSec > 0f) STOPS.add(new Object[]{inst, (int) (maxSec * 20)});
        return inst;
    }

    public static void tick() {
        if (STOPS.isEmpty()) return;
        var sm = MinecraftClient.getInstance().getSoundManager();
        Iterator<Object[]> it = STOPS.iterator();
        while (it.hasNext()) {
            Object[] s = it.next();
            int left = (Integer) s[1] - 1;
            if (left <= 0) { sm.stop((SoundInstance) s[0]); it.remove(); } else s[1] = left;
        }
    }

    public static void ui(String id, float pitch, float vol) {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvent.of(Identifier.ofVanilla(id)), pitch, vol));
    }
}
