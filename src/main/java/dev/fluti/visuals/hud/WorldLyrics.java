package dev.fluti.visuals.hud;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.feature.Opts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** World Lyrics: слова поточного рядка пісні висять у повітрі навколо тебе й змінюються з музикою. */
public final class WorldLyrics {
    private static int lastIdx = -1;
    private static float baseYaw;

    private WorldLyrics() {}

    public static void draw(DrawContext ctx, float tp, String text, long age, long len, int idx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p == null) return;
        var c = ConfigManager.get();
        if (idx != lastIdx) { lastIdx = idx; baseYaw = p.getYaw(tp); }
        float fade = Math.min(Math.min(1f, age / 350f), Math.min(1f, (len - age) / 450f));
        if (fade <= 0.02f) return;
        int alpha = (int) (255 * fade);
        Projector pr = Projector.create(ctx, tp);
        TextRenderer tr = mc.textRenderer;
        String[] words = text.trim().split("\\s+");
        Random rnd = new Random(idx * 7919L + 13);
        int glowRgb = Opts.accent(c.theme) & 0xFFFFFF;
        var m = ctx.getMatrices();
        for (int i = 0; i < words.length; i++) {
            double ang = Math.toRadians(baseYaw + (i - (words.length - 1) / 2.0) * 26.0 + (rnd.nextDouble() - 0.5) * 60 * c.lyricsScatter);
            double dist = c.lyricsDist * (0.8 + 0.4 * rnd.nextDouble());
            double y = pr.eye.y + (rnd.nextDouble() - 0.3) * 2.4 * c.lyricsScatter + 0.3;
            double[] s = pr.project(pr.eye.x - Math.sin(ang) * dist, y, pr.eye.z + Math.cos(ang) * dist);
            if (s == null) continue;
            float sc = (float) Math.max(0.5, Math.min(9, c.lyricsWSize * 18.0 / s[2]));
            int tw = tr.getWidth(words[i]);
            m.pushMatrix();
            m.translate((float) s[0], (float) s[1]);
            m.scale(sc, sc);
            if (c.lyricsGlow) {
                int gc = ((int) (alpha * 0.35 * c.lyricsGlowStr) << 24) | glowRgb;
                ctx.drawText(tr, words[i], -tw / 2 - 1, -4, gc, false);
                ctx.drawText(tr, words[i], -tw / 2 + 1, -4, gc, false);
                ctx.drawText(tr, words[i], -tw / 2, -5, gc, false);
                ctx.drawText(tr, words[i], -tw / 2, -3, gc, false);
            }
            ctx.drawText(tr, words[i], -tw / 2, -4, (alpha << 24) | 0xFFFFFF, c.lyricsShadow);
            m.popMatrix();
        }
    }
}
