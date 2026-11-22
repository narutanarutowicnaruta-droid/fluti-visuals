package dev.fluti.visuals.gui;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import dev.fluti.visuals.feature.GlintPack;
import dev.fluti.visuals.feature.GreenScreen;
import dev.fluti.visuals.feature.HeldPresets;
import dev.fluti.visuals.feature.LyricsPlayer;
import dev.fluti.visuals.feature.Opts;
import dev.fluti.visuals.feature.SoundLibrary;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Компактний ClickGUI зі скролом (влазить в екран ноутбука) і звуками. */
public class ClickGuiScreen extends Screen {
    private static final String[] TABS = {"Visuals", "Audio", "Creator", "Utilities"};
    private static final int SIDE = 78, ROW_H = 18, GAP = 2, MAX_MACROS = 5;
    private static final int BG = 0xF00E0E14, SIDEBG = 0xFF15151F, ACCENT = 0xFF9B7CFF, ROW = 0xFF1C1C28,
            ROW_HOV = 0xFF26263A, OFF = 0xFF3A3A4A, OUTLINE = 0xFF3B2F6B;

    private final FlutiConfig cfg = ConfigManager.get();
    private int tab, px, py, pw, ph, contentH;
    private final int[] scroll = new int[4];
    private Row slider;
    private FlutiConfig.Macro listening;
    private TextFieldWidget cmdField;
    private ButtonWidget addBtn;

    public ClickGuiScreen() {
        super(Text.literal("Fluti"));
        if (cfg.guiSounds) SoundLibrary.ui("block.amethyst_block.chime", 1.6f, 0.5f);
    }

    // ---------------- рядки ----------------
    private interface Row {
        int h();
        void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov);
        /** &lt;0 — не оброблено, 0 — оброблено без звуку, &gt;0 — висота тону звуку кліку. */
        float click(double mx, int x, int w, int button);
        default boolean isSlider() { return false; }
    }

    private static void base(DrawContext c, TextRenderer tr, int x, int y, int w, String label, boolean hov) {
        c.fill(x, y, x + w, y + ROW_H, hov ? ROW_HOV : ROW);
        c.drawText(tr, label, x + 6, y + 5, 0xFFE8E8F0, false);
    }

    private static Row header(String text) {
        return new Row() {
            public int h() { return 14; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                c.drawText(tr, text, x + 2, y + 4, ACCENT, false);
                c.fill(x + tr.getWidth(text) + 8, y + 8, x + w, y + 9, 0xFF2A2347);
            }
            public float click(double mx, int x, int w, int b) { return -1; }
        };
    }

    private static Row info(String text) {
        return new Row() {
            public int h() { return 12; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                c.drawText(tr, tr.trimToWidth(text, w), x + 2, y + 2, 0xFF8888A0, false);
            }
            public float click(double mx, int x, int w, int b) { return -1; }
        };
    }

    private static Row toggle(String label, Supplier<Boolean> g, Consumer<Boolean> s) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                base(c, tr, x, y, w, label, hov);
                boolean on = g.get();
                c.fill(x + w - 28, y + 4, x + w - 6, y + 14, on ? ACCENT : OFF);
                int k = on ? x + w - 16 : x + w - 26;
                c.fill(k, y + 6, k + 8, y + 12, 0xFFFFFFFF);
            }
            public float click(double mx, int x, int w, int b) {
                boolean n = !g.get(); s.accept(n); return n ? 1.3f : 0.8f;
            }
        };
    }

    private static Supplier<List<String>> fixed(List<String> l) { return () -> l; }

    private static String shown(String v) { return v.startsWith("FILE:") ? v.substring(5) : v; }

    private static Row cycle(String label, Supplier<List<String>> opts, Supplier<String> g, Consumer<String> s) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                base(c, tr, x, y, w, label, hov);
                int maxW = w - tr.getWidth(label) - 40;
                String v = "< " + tr.trimToWidth(shown(g.get()), Math.max(20, maxW)) + " >";
                c.drawText(tr, v, x + w - 6 - tr.getWidth(v), y + 5, ACCENT, false);
            }
            public float click(double mx, int x, int w, int b) {
                List<String> o = opts.get();
                if (o.isEmpty()) return 0f;
                int i = o.indexOf(g.get());
                i = (i + (b == 1 ? o.size() - 1 : 1) + o.size()) % o.size();
                s.accept(o.get(i));
                return 1.1f;
            }
        };
    }

    private static Row slider(String label, float min, float max, Supplier<Float> g, Consumer<Float> s) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                base(c, tr, x, y, w, label, hov);
                int bx = x + w - 86, bw = 80;
                float t = (g.get() - min) / (max - min);
                String val = String.format(Locale.ROOT, "%.2f", g.get());
                c.drawText(tr, val, bx - 6 - tr.getWidth(val), y + 5, 0xFF9999AA, false);
                c.fill(bx, y + 8, bx + bw, y + 10, OFF);
                c.fill(bx, y + 8, bx + (int) (bw * t), y + 10, ACCENT);
                c.fill(bx + (int) (bw * t) - 2, y + 4, bx + (int) (bw * t) + 2, y + 14, 0xFFFFFFFF);
            }
            public float click(double mx, int x, int w, int b) {
                int bx = x + w - 86;
                float t = (float) Math.max(0, Math.min(1, (mx - bx) / 80.0));
                s.accept(min + t * (max - min));
                return 0f;
            }
            public boolean isSlider() { return true; }
        };
    }

    private static Row action(String label, Runnable r) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                c.fill(x, y, x + w, y + ROW_H, hov ? 0xFF38305E : 0xFF2A2347);
                c.drawText(tr, label, x + 6, y + 5, ACCENT, false);
            }
            public float click(double mx, int x, int w, int b) { r.run(); return 1.0f; }
        };
    }

    private Row macroRow(FlutiConfig.Macro m) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                base(c, tr, x, y, w, m.command, hov);
                String k = listening == m ? "[ press key ]" : "[" + keyName(m.key) + "]";
                c.drawText(tr, k, x + w - 22 - tr.getWidth(k), y + 5, ACCENT, false);
                c.drawText(tr, "x", x + w - 12, y + 5, 0xFFFF6060, false);
            }
            public float click(double mx, int x, int w, int b) {
                if (mx >= x + w - 18) { cfg.macros.remove(m); if (listening == m) listening = null; return 0.7f; }
                listening = m; return 1.2f;
            }
        };
    }

    private static String keyName(int k) {
        return k <= 0 ? "none" : InputUtil.Type.KEYSYM.createFromCode(k).getLocalizedText().getString();
    }

    private Consumer<Float> held(Consumer<Float> set) { return v -> { set.accept(v); cfg.heldPreset = "CUSTOM"; }; }

    private List<Row> rows() {
        List<Row> r = new ArrayList<>();
        switch (tab) {
            case 0 -> {
                r.add(header("Enchantment glint"));
                r.add(cycle("Glint colour", fixed(List.of(GlintPack.COLORS)), () -> cfg.glintColor, v -> { cfg.glintColor = v; GlintPack.apply(v); }));
                r.add(info("Змінюється ресурспаком, ресурси перезавантажаться."));
                r.add(header("Hit effects"));
                r.add(toggle("Hitmarkers", () -> cfg.hitmarkers, v -> cfg.hitmarkers = v));
                r.add(cycle("Hitmarker colour", fixed(Opts.HM_COLOR), () -> cfg.hitmarkerColor, v -> cfg.hitmarkerColor = v));
                r.add(cycle("Hitmarker style", fixed(Opts.HM_STYLE), () -> cfg.hitmarkerStyle, v -> cfg.hitmarkerStyle = v));
                r.add(cycle("Hit particles", fixed(Opts.PARTICLES), () -> cfg.hitParticles, v -> cfg.hitParticles = v));
                r.add(cycle("Crit particles", fixed(Opts.PARTICLES), () -> cfg.critParticles, v -> cfg.critParticles = v));
                r.add(header("Kill effects"));
                r.add(cycle("Sky beam", fixed(Opts.KILL_BEAM), () -> cfg.killBeam, v -> cfg.killBeam = v));
                r.add(cycle("Kill burst", fixed(Opts.KILL_BURST), () -> cfg.killBurst, v -> cfg.killBurst = v));
                r.add(header("Hand and item (not blocks)"));
                r.add(toggle("Custom item position", () -> cfg.heldEnabled, v -> cfg.heldEnabled = v));
                r.add(cycle("Preset", fixed(HeldPresets.names()), () -> cfg.heldPreset, v -> HeldPresets.apply(cfg, v)));
                r.add(slider("Offset X", -0.6f, 0.6f, () -> cfg.hx, held(v -> cfg.hx = v)));
                r.add(slider("Offset Y", -0.6f, 0.6f, () -> cfg.hy, held(v -> cfg.hy = v)));
                r.add(slider("Offset Z", -0.6f, 0.6f, () -> cfg.hz, held(v -> cfg.hz = v)));
                r.add(slider("Rotate X", -90f, 90f, () -> cfg.hrx, held(v -> cfg.hrx = v)));
                r.add(slider("Rotate Y", -90f, 90f, () -> cfg.hry, held(v -> cfg.hry = v)));
                r.add(slider("Rotate Z", -90f, 90f, () -> cfg.hrz, held(v -> cfg.hrz = v)));
                r.add(slider("Scale", 0.4f, 1.6f, () -> cfg.hscale, held(v -> cfg.hscale = v)));
                r.add(action("Reset item position", () -> HeldPresets.apply(cfg, "DEFAULT")));
            }
            case 1 -> {
                r.add(header("Hit sound"));
                r.add(cycle("Hit sound", SoundLibrary::options, () -> cfg.hitSound,
                        v -> { cfg.hitSound = v; SoundLibrary.play(v, cfg.soundVolume, cfg.hitPitch, "hit.ogg"); }));
                r.add(slider("Hit pitch", 0.5f, 2f, () -> cfg.hitPitch, v -> cfg.hitPitch = v));
                r.add(header("Kill sound"));
                r.add(cycle("Kill sound", SoundLibrary::options, () -> cfg.killSound,
                        v -> { cfg.killSound = v; SoundLibrary.play(v, cfg.soundVolume, cfg.killPitch, "kill.ogg"); }));
                r.add(slider("Kill pitch", 0.5f, 2f, () -> cfg.killPitch, v -> cfg.killPitch = v));
                r.add(header("General"));
                r.add(slider("Volume", 0f, 1f, () -> cfg.soundVolume, v -> cfg.soundVolume = v));
                r.add(toggle("Menu sounds", () -> cfg.guiSounds, v -> cfg.guiSounds = v));
                r.add(info("Свої звуки: .minecraft/config/fluti/sounds/*.ogg"));
                r.add(info("Вони з'являться в списку як FILE:назва.ogg"));
            }
            case 2 -> {
                r.add(header("Camera"));
                r.add(toggle("Safe cinematic camera", () -> cfg.cinematic, v -> cfg.cinematic = v));
                r.add(slider("Smoothing (less = smoother)", 0.05f, 0.5f, () -> cfg.cinematicSmooth, v -> cfg.cinematicSmooth = v));
                r.add(header("Privacy"));
                r.add(toggle("Password masking", () -> cfg.maskPasswords, v -> cfg.maskPasswords = v));
                r.add(header("Green screen"));
                r.add(toggle("Green screen mode", () -> cfg.greenScreen, v -> { cfg.greenScreen = v; GreenScreen.apply(v); }));
                r.add(header("Song and lyrics"));
                r.add(cycle("Song", () -> { List<String> l = new ArrayList<>(LyricsPlayer.songs()); if (l.isEmpty()) l.add(""); return l; },
                        () -> cfg.song.isEmpty() ? "(none)" : cfg.song, v -> cfg.song = v));
                r.add(action("Play / stop song", () -> LyricsPlayer.toggle(cfg.song)));
                r.add(toggle("Show lyrics", () -> cfg.lyricsOn, v -> cfg.lyricsOn = v));
                r.add(slider("Song volume", 0f, 1f, () -> cfg.songVolume, v -> cfg.songVolume = v));
                r.add(slider("Lyrics height", 20f, 160f, () -> cfg.lyricsY, v -> cfg.lyricsY = v));
                r.add(info("Файли: config/fluti/songs/назва.ogg + назва.lrc"));
            }
            default -> {
                r.add(header("HUD widgets"));
                r.add(toggle("Armor durability", () -> cfg.armor.enabled, v -> cfg.armor.enabled = v));
                r.add(toggle("Potion timers", () -> cfg.potions.enabled, v -> cfg.potions.enabled = v));
                r.add(toggle("Ping", () -> cfg.ping.enabled, v -> cfg.ping.enabled = v));
                r.add(toggle("Kills and combo", () -> cfg.stats.enabled, v -> cfg.stats.enabled = v));
                r.add(toggle("FPS", () -> cfg.fps.enabled, v -> cfg.fps.enabled = v));
                r.add(action("Edit HUD positions >", () -> client.setScreen(new HudEditScreen(this))));
                r.add(header("Macros (" + cfg.macros.size() + "/" + MAX_MACROS + ")"));
                for (FlutiConfig.Macro m : cfg.macros) r.add(macroRow(m));
            }
        }
        return r;
    }

    // ---------------- екран ----------------
    private int viewH() { return ph - 16 - (tab == 3 ? 28 : 0); }
    private int cx() { return px + SIDE + 8; }
    private int cw() { return pw - SIDE - 16; }

    @Override protected void init() {
        pw = Math.min(390, width - 16);
        ph = Math.min(270, height - 16);
        px = (width - pw) / 2; py = (height - ph) / 2;
        int fy = py + ph - 26;
        cmdField = new TextFieldWidget(textRenderer, cx(), fy, cw() - 56, 20, Text.literal("cmd"));
        cmdField.setPlaceholder(Text.literal("/home, /ah, /spawn ..."));
        cmdField.setMaxLength(100);
        addBtn = ButtonWidget.builder(Text.literal("Add"), b -> {
            String t = cmdField.getText().trim();
            if (t.isEmpty() || cfg.macros.size() >= MAX_MACROS) return;
            if (!t.startsWith("/")) t = "/" + t;
            FlutiConfig.Macro m = new FlutiConfig.Macro(t, -1);
            cfg.macros.add(m); listening = m; cmdField.setText("");
        }).dimensions(cx() + cw() - 52, fy, 52, 20).build();
        addDrawableChild(cmdField);
        addDrawableChild(addBtn);
    }

    @Override public void renderBackground(DrawContext ctx, int mx, int my, float d) { ctx.fill(0, 0, width, height, 0x90000000); }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, OUTLINE);
        ctx.fill(px, py, px + pw, py + ph, BG);
        ctx.fill(px, py, px + SIDE, py + ph, SIDEBG);
        ctx.drawText(textRenderer, "Fluti", px + 10, py + 10, ACCENT, true);
        ctx.drawText(textRenderer, "v0.2", px + 10, py + 21, 0xFF66667A, false);
        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 40 + i * 24;
            boolean hov = mx >= px + 6 && mx <= px + SIDE - 6 && my >= ty && my <= ty + 20;
            ctx.fill(px + 6, ty, px + SIDE - 6, ty + 20, i == tab ? 0xFF2A2347 : hov ? 0xFF1E1E2C : 0x00000000);
            if (i == tab) ctx.fill(px + 6, ty, px + 8, ty + 20, ACCENT);
            ctx.drawText(textRenderer, TABS[i], px + 16, ty + 6, i == tab ? 0xFFFFFFFF : 0xFF9999AA, false);
        }

        int top = py + 8, vh = viewH(), x = cx(), w = cw();
        int y = top - scroll[tab];
        ctx.enableScissor(x - 2, top, x + w + 2, top + vh);
        for (Row r : rows()) {
            int h = r.h();
            boolean hov = mx >= x && mx <= x + w && my >= Math.max(y, top) && my <= Math.min(y + h, top + vh);
            if (y + h > top && y < top + vh) r.render(ctx, textRenderer, x, y, w, hov);
            y += h + GAP;
        }
        ctx.disableScissor();
        contentH = y - (top - scroll[tab]);
        int max = Math.max(0, contentH - vh);
        scroll[tab] = Math.max(0, Math.min(max, scroll[tab]));
        if (max > 0) {
            int barH = Math.max(14, vh * vh / contentH);
            int barY = top + (int) ((vh - barH) * (scroll[tab] / (float) max));
            ctx.fill(px + pw - 4, top, px + pw - 2, top + vh, 0xFF1C1C28);
            ctx.fill(px + pw - 4, barY, px + pw - 2, barY + barH, ACCENT);
        }
        cmdField.visible = tab == 3; addBtn.visible = tab == 3;
        super.render(ctx, mx, my, d);
    }

    private void uiSound(float pitch) {
        if (cfg.guiSounds && pitch > 0) SoundLibrary.ui("ui.button.click", pitch, 0.5f);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 40 + i * 24;
            if (mx >= px + 6 && mx <= px + SIDE - 6 && my >= ty && my <= ty + 20) {
                if (tab != i) uiSound(1.4f);
                tab = i; listening = null; return true;
            }
        }
        if (super.mouseClicked(click, doubled)) return true;
        int top = py + 8, vh = viewH(), x = cx(), w = cw();
        if (my < top || my > top + vh) return false;
        int y = top - scroll[tab];
        for (Row r : rows()) {
            int h = r.h();
            if (mx >= x && mx <= x + w && my >= y && my < y + h) {
                float p = r.click(mx, x, w, click.button());
                if (p >= 0) { if (r.isSlider()) slider = r; uiSound(p); return true; }
            }
            y += h + GAP;
        }
        return false;
    }

    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (slider != null) { slider.click(click.x(), cx(), cw(), 0); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override public boolean mouseReleased(Click click) { slider = null; return super.mouseReleased(click); }

    @Override public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scroll[tab] -= (int) (vAmount * 18);
        return true;
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (listening != null) {
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) listening.key = input.key();
            listening = null; return true;
        }
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void removed() {
        ConfigManager.save();
        if (cfg.guiSounds) SoundLibrary.ui("block.amethyst_block.chime", 0.8f, 0.4f);
    }
}
