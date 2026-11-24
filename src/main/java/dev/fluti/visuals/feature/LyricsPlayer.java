package dev.fluti.visuals.feature;

import dev.fluti.visuals.FlutiClient;
import dev.fluti.visuals.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.SoundInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Пісня з ресурспаку FlutiSounds (грає ванільний рушій) + великі субтитри з .lrc (config/fluti/songs/назва.lrc). */
public final class LyricsPlayer {
    private record Line(long ms, String text) {}

    private static final Pattern TS = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]");
    private static List<Line> lines = List.of();
    private static long startNs;
    private static SoundInstance inst;

    private LyricsPlayer() {}

    public static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("fluti/songs"); }

    public static List<String> songs() { return SoundPack.names(); }

    public static boolean playing() {
        if (inst == null) return false;
        if (System.nanoTime() - startNs < 800_000_000L) return true;      // дамо рушію час запустити
        return MinecraftClient.getInstance().getSoundManager().isPlaying(inst);
    }

    public static void toggle(String name) { if (playing()) stop(); else start(name); }

    public static void start(String name) {
        stop();
        if (name == null || name.isBlank()) return;
        lines = parse(dir().resolve(name + ".lrc"));
        startNs = System.nanoTime();
        inst = SoundLibrary.playPack("s." + name, ConfigManager.get().songVolume, 1f, 0f);
    }

    public static void stop() {
        if (inst != null) MinecraftClient.getInstance().getSoundManager().stop(inst);
        inst = null;
        lines = List.of();
    }

    private static List<Line> parse(Path lrc) {
        List<Line> out = new ArrayList<>();
        if (!Files.isRegularFile(lrc)) return out;
        try {
            for (String raw : Files.readAllLines(lrc)) {
                Matcher m = TS.matcher(raw);
                List<Long> ts = new ArrayList<>();
                int end = 0;
                while (m.find()) {
                    ts.add((long) ((Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2))) * 1000));
                    end = m.end();
                }
                if (ts.isEmpty()) continue;
                String text = raw.substring(end).trim();
                for (long t : ts) out.add(new Line(t, text));
            }
            out.sort(Comparator.comparingLong(Line::ms));
        } catch (Exception e) { FlutiClient.LOG.warn("Bad lrc {}", lrc, e); }
        return out;
    }

    /** Великі субтитри по центру екрана (розмір у меню). */
    public static void render(DrawContext ctx) {
        var c = ConfigManager.get();
        if (!c.lyricsOn || !playing() || lines.isEmpty()) return;
        long el = (System.nanoTime() - startNs) / 1_000_000L;
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) { if (lines.get(i).ms() <= el) idx = i; else break; }
        if (idx < 0) return;
        String cur = lines.get(idx).text();
        if (cur.isBlank()) return;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        float s = Math.max(1f, c.lyricsScale);
        int cx = ctx.getScaledWindowWidth() / 2;
        int y = ctx.getScaledWindowHeight() - (int) c.lyricsY - (int) (10 * s);
        int bw = (int) (tr.getWidth(cur) * s) + 16;
        ctx.fill(cx - bw / 2, y - 4, cx + bw / 2, y + (int) (9 * s) + 4, 0x90000000);
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) cx, (float) y);
        m.scale(s, s);
        ctx.drawCenteredTextWithShadow(tr, cur, 0, 0, 0xFFFFFFFF);
        m.popMatrix();
        if (idx + 1 < lines.size() && !lines.get(idx + 1).text().isBlank())
            ctx.drawCenteredTextWithShadow(tr, lines.get(idx + 1).text(), cx, y + (int) (9 * s) + 8, 0xFF9999AA);
    }
}
