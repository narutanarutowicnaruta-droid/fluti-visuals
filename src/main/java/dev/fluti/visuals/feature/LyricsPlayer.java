package dev.fluti.visuals.feature;

import dev.fluti.visuals.FlutiClient;
import dev.fluti.visuals.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Твоя пісня: config/fluti/songs/назва.ogg + (необов'язково) назва.lrc з тайм-кодами.
 * Субтитри показуються над хотбаром у будь-якому світі/на будь-якому сервері.
 */
public final class LyricsPlayer {
    private record Line(long ms, String text) {}

    private static final Pattern TS = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]");
    private static List<Line> lines = List.of();
    private static long startNs;
    private static int src = -1;

    private LyricsPlayer() {}

    public static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("fluti/songs"); }

    public static List<String> songs() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir())) {
            s.map(f -> f.getFileName().toString())
                    .filter(n -> n.toLowerCase().endsWith(".ogg"))
                    .map(n -> n.substring(0, n.length() - 4))
                    .sorted().forEach(out::add);
        } catch (Exception ignored) { }
        return out;
    }

    public static boolean playing() { return src >= 0 && OggPlayer.isPlaying(src); }

    public static void toggle(String name) {
        if (playing()) stop(); else start(name);
    }

    public static void start(String name) {
        stop();
        if (name == null || name.isBlank()) return;
        var c = ConfigManager.get();
        lines = parse(dir().resolve(name + ".lrc"));
        startNs = System.nanoTime();
        src = OggPlayer.start(dir().resolve(name + ".ogg"), c.songVolume, 1f);
    }

    public static void stop() {
        if (src >= 0) OggPlayer.stop(src);
        src = -1;
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
        int cx = ctx.getScaledWindowWidth() / 2;
        int y = ctx.getScaledWindowHeight() - (int) c.lyricsY;
        int w = tr.getWidth(cur) + 14;
        ctx.fill(cx - w / 2, y - 4, cx + w / 2, y + 13, 0x90000000);
        ctx.drawCenteredTextWithShadow(tr, cur, cx, y, 0xFFFFFFFF);
        if (idx + 1 < lines.size() && !lines.get(idx + 1).text().isBlank())
            ctx.drawCenteredTextWithShadow(tr, lines.get(idx + 1).text(), cx, y + 16, 0xFF9999AA);
    }
}
