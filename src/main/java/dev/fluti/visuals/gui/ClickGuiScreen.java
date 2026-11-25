package dev.fluti.visuals.gui;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import dev.fluti.visuals.config.Lang;
import dev.fluti.visuals.feature.Backups;
import dev.fluti.visuals.feature.CosmeticsPack;
import dev.fluti.visuals.feature.GlintPack;
import dev.fluti.visuals.feature.GreenScreen;
import dev.fluti.visuals.feature.HeldPresets;
import dev.fluti.visuals.feature.LyricsPlayer;
import dev.fluti.visuals.feature.OggPlayer;
import dev.fluti.visuals.feature.Opts;
import dev.fluti.visuals.feature.Profiles;
import dev.fluti.visuals.feature.SoundLibrary;
import dev.fluti.visuals.feature.WaypointManager;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** ClickGUI 0.6: бічні вкладки + під-вкладки, кнопки з зеленою/червоною обводкою, опис кожної функції, меню можна рухати. */
public class ClickGuiScreen extends Screen {
    private static final String[] TAB_KEYS = {"tab.visuals", "tab.audio", "tab.creator", "tab.utilities", "tab.marks", "tab.profiles", "tab.language", "tab.cosmetics"};
    private static final String[][] SUBS = {
            {"sub.combat", "sub.trails", "sub.screen", "sub.hand"},
            {"sub.sounds", "sub.music"},
            {"sub.camera", "sub.privacy"},
            {"sub.hud", "sub.bar", "sub.macros", "sub.tools"},
            {}, {}, {}, {}};
    private static final int TOP = 26, SIDE = 84, ROW_H = 26, GAP = 3, STRIP_H = 22, MAX_MACROS = 5;
    private static final int NONE = -1, SILENT = 0, CLICK = 1, ON = 2, OFF = 3, DEL = 4, TAB = 5;
    private static final int GREEN = 0xFF3DDC84, RED = 0xFFE5484D;
    private static final int BG = 0xF00E0E14, SIDEBG = 0xB0140A28, ROW = 0xB0221638, ROW_HOV = 0xC02E1E4A, EDGE = 0xFF4A3380;
    private static int memX = Integer.MIN_VALUE, memY;

    private final FlutiConfig cfg = ConfigManager.get();
    private int tab, px, py, pw, ph, contentH;
    private final int[] sub = new int[TAB_KEYS.length];
    private final int[] scroll = new int[TAB_KEYS.length * 8];
    private Row slider;
    private double lastTickX;
    private boolean dragging;
    private double dragDX, dragDY, dragAcc;
    private FlutiConfig.Macro listening;
    private Consumer<Integer> pickKey;
    private String pickId;
    private TextFieldWidget cmdField, nameField;
    private ButtonWidget addBtn, markBtn;

    public ClickGuiScreen() {
        super(Text.literal("Fluti"));
        if (cfg.guiSounds) SoundLibrary.ui("block.amethyst_block.chime", 1.6f, 0.5f);
    }

    private static int accent() { return Opts.accent(ConfigManager.get().theme); }

    // ---------------- рядки ----------------
    private interface Row {
        int h();
        void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov);
        int click(double mx, int x, int w, int button);
        default boolean isSlider() { return false; }
    }

    private static void outline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 1, y, x + w - 1, y + 1, col); c.fill(x + 1, y + h - 1, x + w - 1, y + h, col);   // зрізані кути
        c.fill(x, y + 1, x + 1, y + h - 1, col); c.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    private static void card(DrawContext c, TextRenderer tr, int x, int y, int w, String name, String desc,
                             int bg, int edge, int reserve) {
        c.fill(x + 1, y, x + w - 1, y + ROW_H, bg);
        c.fill(x, y + 1, x + w, y + ROW_H - 1, bg);
        outline(c, x, y, w, ROW_H, edge);
        c.drawText(tr, tr.trimToWidth(name, Math.max(20, w - reserve - 14)), x + 7, y + 4, 0xFFF0F0F8, false);
        if (desc != null && !desc.isEmpty())
            c.drawText(tr, tr.trimToWidth(desc, w - 14), x + 7, y + 15, 0xFF8A8AA3, false);
    }

    private static Row header(String text) {
        return new Row() {
            public int h() { return 14; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                c.drawText(tr, text, x + 2, y + 4, accent(), false);
                c.fill(x + tr.getWidth(text) + 8, y + 8, x + w, y + 9, Opts.dim(accent(), 0.35f));
            }
            public int click(double mx, int x, int w, int b) { return NONE; }
        };
    }

    private static Row info(String text) {
        return new Row() {
            public int h() { return 12; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                c.drawText(tr, tr.trimToWidth(text, w), x + 2, y + 2, 0xFF8888A0, false);
            }
            public int click(double mx, int x, int w, int b) { return NONE; }
        };
    }

    private static Row toggle(String id, Supplier<Boolean> g, Consumer<Boolean> s) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                boolean on = g.get();
                int bg = on ? (hov ? 0xFF1B3528 : 0xFF14261D) : (hov ? 0xFF3A2025 : 0xFF2A171B);
                card(c, tr, x, y, w, Lang.t(id), Lang.d(id), bg, on ? GREEN : RED, 44);
                String tag = Lang.get(on ? "on" : "off");
                c.drawText(tr, tag, x + w - 8 - tr.getWidth(tag), y + 9, on ? GREEN : RED, false);
            }
            public int click(double mx, int x, int w, int b) { boolean n = !g.get(); s.accept(n); return n ? ON : OFF; }
        };
    }

    private static Supplier<List<String>> fixed(List<String> l) { return () -> l; }

    private static String shown(String v) { return v.startsWith("FILE:") ? v.substring(5) : v; }

    private static Row cycle(String id, Supplier<List<String>> opts, Supplier<String> g, Consumer<String> s) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                String v = "< " + tr.trimToWidth(shown(g.get()), 100) + " >";
                card(c, tr, x, y, w, Lang.t(id), Lang.d(id), hov ? ROW_HOV : ROW, EDGE, tr.getWidth(v) + 8);
                c.drawText(tr, v, x + w - 7 - tr.getWidth(v), y + 9, accent(), false);
            }
            public int click(double mx, int x, int w, int b) {
                List<String> o = opts.get();
                if (o.isEmpty()) return SILENT;
                int i = o.indexOf(g.get());
                i = (i + (b == 1 ? o.size() - 1 : 1) + o.size()) % o.size();
                s.accept(o.get(i));
                return CLICK;
            }
        };
    }

    private static Row slider(String id, float min, float max, Supplier<Float> g, Consumer<Float> s) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                card(c, tr, x, y, w, Lang.t(id), Lang.d(id), hov ? ROW_HOV : ROW, EDGE, 100);
                int bx = x + w - 92, bw = 82;
                float t = (g.get() - min) / (max - min);
                String val = String.format(Locale.ROOT, "%.2f", g.get());
                c.drawText(tr, val, bx + bw - tr.getWidth(val), y + 3, 0xFF9999AA, false);
                c.fill(bx, y + 14, bx + bw, y + 16, 0xFF3A3A4A);
                c.fill(bx, y + 14, bx + (int) (bw * t), y + 16, accent());
                c.fill(bx + (int) (bw * t) - 2, y + 10, bx + (int) (bw * t) + 2, y + 20, 0xFFFFFFFF);
            }
            public int click(double mx, int x, int w, int b) {
                int bx = x + w - 92;
                float t = (float) Math.max(0, Math.min(1, (mx - bx) / 82.0));
                s.accept(min + t * (max - min));
                return SILENT;
            }
            public boolean isSlider() { return true; }
        };
    }

    private static Row action(String id, Runnable r) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                card(c, tr, x, y, w, Lang.t(id), Lang.d(id), hov ? 0xFF38305E : 0xFF2A2347, accent(), 10);
            }
            public int click(double mx, int x, int w, int b) { r.run(); return CLICK; }
        };
    }

    private Row langRow(String code) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                boolean sel = cfg.lang.equals(code);
                card(c, tr, x, y, w, Lang.NAMES.get(code), code.toUpperCase(Locale.ROOT),
                        sel ? 0xFF14261D : (hov ? ROW_HOV : ROW), sel ? GREEN : EDGE, 20);
                if (sel) c.drawText(tr, "OK", x + w - 8 - tr.getWidth("OK"), y + 9, GREEN, false);
            }
            public int click(double mx, int x, int w, int b) { cfg.lang = code; Lang.load(code); return ON; }
        };
    }

    private Row macroRow(FlutiConfig.Macro m) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                String k = (m.onJoin ? "J " : "") + (listening == m ? "[ ... ]" : "[" + keyName(m.key) + "]");
                card(c, tr, x, y, w, m.command, "", hov ? ROW_HOV : ROW, listening == m ? accent() : EDGE, tr.getWidth(k) + 30);
                c.drawText(tr, k, x + w - 24 - tr.getWidth(k), y + 9, accent(), false);
                c.drawText(tr, "x", x + w - 12, y + 9, 0xFFFF6060, false);
            }
            public int click(double mx, int x, int w, int b) {
                if (mx >= x + w - 20) { cfg.macros.remove(m); if (listening == m) listening = null; return DEL; }
                if (b == 1) { m.onJoin = !m.onJoin; return m.onJoin ? ON : OFF; }
                listening = m; return CLICK;
            }
        };
    }

    private Row markRow(FlutiConfig.Waypoint wp) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                card(c, tr, x, y, w, wp.name, (int) wp.x + " " + (int) wp.y + " " + (int) wp.z,
                        hov ? ROW_HOV : ROW, 0xFF000000 | wp.color, 24);
                c.drawText(tr, "x", x + w - 12, y + 9, 0xFFFF6060, false);
            }
            public int click(double mx, int x, int w, int b) {
                if (mx >= x + w - 20) { cfg.waypoints.remove(wp); ConfigManager.save(); return DEL; }
                return SILENT;
            }
        };
    }

    private Row keyRow(String id, Supplier<Integer> g, Consumer<Integer> set) {
        return new Row() {
            public int h() { return ROW_H; }
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hov) {
                String k = id.equals(pickId) ? "[ ... ]" : "[" + keyName(g.get()) + "]";
                card(c, tr, x, y, w, Lang.t(id), Lang.d(id), hov ? ROW_HOV : ROW, id.equals(pickId) ? accent() : EDGE, tr.getWidth(k) + 10);
                c.drawText(tr, k, x + w - 7 - tr.getWidth(k), y + 9, accent(), false);
            }
            public int click(double mx, int x, int w, int b) { pickId = id; pickKey = set; return CLICK; }
        };
    }

    private void msg(String key) { msgRaw(Lang.get(key)); }

    private void msgRaw(String text) {
        if (client != null && client.player != null) client.player.sendMessage(Text.literal(text), true);
    }

    private static String keyName(int k) {
        return k <= 0 ? "none" : InputUtil.Type.KEYSYM.createFromCode(k).getLocalizedText().getString();
    }

    private Consumer<Float> held(Consumer<Float> set) { return v -> { set.accept(v); cfg.heldPreset = "CUSTOM"; }; }

    private static void openDir(Path d) {
        try { Files.createDirectories(d); } catch (Exception ignored) { }
        net.minecraft.util.Util.getOperatingSystem().open(d);
    }

    private List<Row> rows() {
        List<Row> r = new ArrayList<>();
        int s = sub[tab];
        switch (tab) {
            case 0 -> {
                switch (s) {
                    case 0 -> {
                        r.add(header(Lang.h("glint")));
                        r.add(cycle("glint", fixed(List.of(GlintPack.COLORS)), () -> cfg.glintColor, v -> { cfg.glintColor = v; GlintPack.apply(v); }));
                        r.add(header(Lang.h("hit")));
                        r.add(toggle("hitmarkers", () -> cfg.hitmarkers, v -> cfg.hitmarkers = v));
                        r.add(cycle("hmcolor", fixed(Opts.HM_COLOR), () -> cfg.hitmarkerColor, v -> cfg.hitmarkerColor = v));
                        r.add(cycle("hmstyle", fixed(Opts.HM_STYLE), () -> cfg.hitmarkerStyle, v -> cfg.hitmarkerStyle = v));
                        r.add(cycle("hitpart", fixed(Opts.PARTICLES), () -> cfg.hitParticles, v -> cfg.hitParticles = v));
                        r.add(cycle("critpart", fixed(Opts.PARTICLES), () -> cfg.critParticles, v -> cfg.critParticles = v));
                        r.add(header(Lang.h("kill")));
                        r.add(cycle("beam", fixed(Opts.KILL_BEAM), () -> cfg.killBeam, v -> cfg.killBeam = v));
                        r.add(cycle("burst", fixed(Opts.KILL_BURST), () -> cfg.killBurst, v -> cfg.killBurst = v));
                    }
                    case 1 -> {
                        r.add(header(Lang.h("trails")));
                        r.add(cycle("trail", fixed(Opts.TRAIL), () -> cfg.trail, v -> cfg.trail = v));
                        r.add(cycle("halo", fixed(Opts.TRAIL), () -> cfg.halo, v -> cfg.halo = v));
                        r.add(cycle("stepfx", fixed(Opts.STEP), () -> cfg.stepFx, v -> cfg.stepFx = v));
                        r.add(cycle("jumpring", fixed(Opts.TRAIL), () -> cfg.jumpRing, v -> cfg.jumpRing = v));
                        r.add(cycle("totemfx", fixed(Opts.TOTEM), () -> cfg.totemFx, v -> cfg.totemFx = v));
                        r.add(header(Lang.h("world")));
                        r.add(cycle("weather", fixed(Opts.WEATHER), () -> cfg.weather, v -> cfg.weather = v));
                    }
                    case 2 -> {
                        r.add(header(Lang.h("screen")));
                        r.add(cycle("vignette", fixed(Opts.VIGNETTE), () -> cfg.vignette, v -> cfg.vignette = v));
                        r.add(cycle("tint", fixed(Opts.TINT), () -> cfg.tint, v -> cfg.tint = v));
                    }
                    default -> {
                        r.add(header(Lang.h("held")));
                        r.add(toggle("held", () -> cfg.heldEnabled, v -> cfg.heldEnabled = v));
                        r.add(cycle("preset", fixed(HeldPresets.names()), () -> cfg.heldPreset, v -> HeldPresets.apply(cfg, v)));
                        r.add(slider("hx", -0.6f, 0.6f, () -> cfg.hx, held(v -> cfg.hx = v)));
                        r.add(slider("hy", -0.6f, 0.6f, () -> cfg.hy, held(v -> cfg.hy = v)));
                        r.add(slider("hz", -0.6f, 0.6f, () -> cfg.hz, held(v -> cfg.hz = v)));
                        r.add(slider("hrx", -90f, 90f, () -> cfg.hrx, held(v -> cfg.hrx = v)));
                        r.add(slider("hry", -90f, 90f, () -> cfg.hry, held(v -> cfg.hry = v)));
                        r.add(slider("hrz", -90f, 90f, () -> cfg.hrz, held(v -> cfg.hrz = v)));
                        r.add(slider("hscale", 0.4f, 1.6f, () -> cfg.hscale, held(v -> cfg.hscale = v)));
                        r.add(action("hreset", () -> HeldPresets.apply(cfg, "DEFAULT")));
                    }
                }
            }
            case 1 -> {
                if (s == 0) {
                    r.add(header(Lang.h("hitsnd")));
                    r.add(cycle("hitsnd", SoundLibrary::options, () -> cfg.hitSound,
                            v -> { cfg.hitSound = v; SoundLibrary.play(v, cfg.soundVolume, cfg.hitPitch, "hit.ogg", cfg.hitMaxSec); }));
                    r.add(slider("hitpitch", 0.5f, 2f, () -> cfg.hitPitch, v -> cfg.hitPitch = v));
                    r.add(slider("hitmax", 0.2f, 3f, () -> cfg.hitMaxSec, v -> cfg.hitMaxSec = v));
                    r.add(header(Lang.h("killsnd")));
                    r.add(cycle("killsnd", SoundLibrary::options, () -> cfg.killSound,
                            v -> { cfg.killSound = v; SoundLibrary.play(v, cfg.soundVolume, cfg.killPitch, "kill.ogg", cfg.killMaxSec); }));
                    r.add(slider("killpitch", 0.5f, 2f, () -> cfg.killPitch, v -> cfg.killPitch = v));
                    r.add(slider("killmax", 0.2f, 4f, () -> cfg.killMaxSec, v -> cfg.killMaxSec = v));
                    r.add(header(Lang.h("general")));
                    r.add(slider("volume", 0f, 1f, () -> cfg.soundVolume, v -> cfg.soundVolume = v));
                    r.add(toggle("guisnd", () -> cfg.guiSounds, v -> cfg.guiSounds = v));
                    r.add(cycle("armorwarn", SoundLibrary::options, () -> cfg.armorWarn,
                            v -> { cfg.armorWarn = v; SoundLibrary.play(v, cfg.soundVolume, 1f, "hit.ogg"); }));
                    r.add(header(Lang.h("custom")));
                    r.add(action("opensounds", () -> openDir(SoundLibrary.dir())));
                    r.add(info(Lang.i("dropsounds")));
                } else {
                    r.add(header(Lang.h("song")));
                    r.add(cycle("song", () -> { List<String> l = new ArrayList<>(LyricsPlayer.songs()); if (l.isEmpty()) l.add(""); return l; },
                            () -> cfg.song.isEmpty() ? "(none)" : cfg.song, v -> cfg.song = v));
                    r.add(action("playsong", () -> LyricsPlayer.toggle(cfg.song)));
                    r.add(slider("songvol", 0f, 1f, () -> cfg.songVolume, v -> cfg.songVolume = v));
                    r.add(header(Lang.h("lyrics")));
                    r.add(toggle("lyrics", () -> cfg.lyricsOn, v -> cfg.lyricsOn = v));
                    r.add(cycle("lyricsmode", fixed(List.of("WORLD", "SCREEN")), () -> cfg.lyricsMode, v -> cfg.lyricsMode = v));
                    r.add(slider("lyricsdist", 4f, 16f, () -> cfg.lyricsDist, v -> cfg.lyricsDist = v));
                    r.add(slider("lyricswsize", 0.5f, 3f, () -> cfg.lyricsWSize, v -> cfg.lyricsWSize = v));
                    r.add(slider("lyricsscatter", 0f, 1.5f, () -> cfg.lyricsScatter, v -> cfg.lyricsScatter = v));
                    r.add(toggle("lyricsglow", () -> cfg.lyricsGlow, v -> cfg.lyricsGlow = v));
                    r.add(slider("lyricsglowstr", 0f, 1f, () -> cfg.lyricsGlowStr, v -> cfg.lyricsGlowStr = v));
                    r.add(toggle("lyricsshadow", () -> cfg.lyricsShadow, v -> cfg.lyricsShadow = v));
                    r.add(slider("lyricsoffset", -2000f, 2000f, () -> cfg.lyricsOffset, v -> cfg.lyricsOffset = v));
                    r.add(slider("lyricsscale", 1f, 6f, () -> cfg.lyricsScale, v -> cfg.lyricsScale = v));
                    r.add(slider("lyricsy", 20f, 160f, () -> cfg.lyricsY, v -> cfg.lyricsY = v));
                    r.add(header(Lang.h("custom")));
                    r.add(action("opensongs", () -> openDir(LyricsPlayer.dir())));
                    r.add(info(Lang.i("dropsongs")));
                }
            }
            case 2 -> {
                if (s == 0) {
                    r.add(header(Lang.h("camera")));
                    r.add(toggle("cinematic", () -> cfg.cinematic, v -> cfg.cinematic = v));
                    r.add(slider("smooth", 0.05f, 0.5f, () -> cfg.cinematicSmooth, v -> cfg.cinematicSmooth = v));
                    r.add(header(Lang.h("zoom")));
                    r.add(toggle("zoom", () -> cfg.zoomEnabled, v -> cfg.zoomEnabled = v));
                    r.add(keyRow("zoomkey", () -> cfg.zoomKey, v -> cfg.zoomKey = v));
                    r.add(slider("zoomfactor", 1.5f, 4f, () -> cfg.zoomFactor, v -> cfg.zoomFactor = v));
                    r.add(header(Lang.h("freelook")));
                    r.add(toggle("freelook", () -> cfg.freelook, v -> cfg.freelook = v));
                    r.add(keyRow("freelookkey", () -> cfg.freelookKey, v -> cfg.freelookKey = v));
                } else {
                    r.add(header(Lang.h("privacy")));
                    r.add(toggle("mask", () -> cfg.maskPasswords, v -> cfg.maskPasswords = v));
                    r.add(header(Lang.h("green")));
                    r.add(toggle("green", () -> cfg.greenScreen, v -> { cfg.greenScreen = v; GreenScreen.apply(v); }));
                }
            }
            case 3 -> {
                switch (s) {
                    case 0 -> {
                        r.add(header(Lang.h("hud")));
                        r.add(toggle("armor", () -> cfg.armor.enabled, v -> cfg.armor.enabled = v));
                        r.add(toggle("potions", () -> cfg.potions.enabled, v -> cfg.potions.enabled = v));
                        r.add(toggle("ping", () -> cfg.ping.enabled, v -> cfg.ping.enabled = v));
                        r.add(toggle("stats", () -> cfg.stats.enabled, v -> cfg.stats.enabled = v));
                        r.add(toggle("fps", () -> cfg.fps.enabled, v -> cfg.fps.enabled = v));
                        r.add(toggle("coords", () -> cfg.coords.enabled, v -> cfg.coords.enabled = v));
                        r.add(toggle("cps", () -> cfg.cps.enabled, v -> cfg.cps.enabled = v));
                        r.add(toggle("biome", () -> cfg.biome.enabled, v -> cfg.biome.enabled = v));
                        r.add(toggle("cooldown", () -> cfg.cooldown.enabled, v -> cfg.cooldown.enabled = v));
                        r.add(toggle("target", () -> cfg.target.enabled, v -> cfg.target.enabled = v));
                        r.add(slider("hudopacity", 0.1f, 1f, () -> cfg.hudOpacity, v -> cfg.hudOpacity = v));
                        r.add(toggle("hudsnap", () -> cfg.hudSnap, v -> cfg.hudSnap = v));
                        r.add(action("hudedit", () -> client.setScreen(new HudEditScreen(this))));
                    }
                    case 1 -> {
                        r.add(header(Lang.h("info")));
                        r.add(toggle("info", () -> cfg.info.enabled, v -> cfg.info.enabled = v));
                        r.add(toggle("wmfps", () -> cfg.wmFps, v -> cfg.wmFps = v));
                        r.add(toggle("wmping", () -> cfg.wmPing, v -> cfg.wmPing = v));
                        r.add(toggle("wmcoords", () -> cfg.wmCoords, v -> cfg.wmCoords = v));
                        r.add(toggle("wmtime", () -> cfg.wmTime, v -> cfg.wmTime = v));
                    }
                    case 2 -> {
                        r.add(header(Lang.h("macros") + " (" + cfg.macros.size() + "/" + MAX_MACROS + ")"));
                        r.add(info(Lang.i("macrohint")));
                        for (FlutiConfig.Macro m : cfg.macros) r.add(macroRow(m));
                    }
                    default -> {
                        r.add(header(Lang.h("tools")));
                        r.add(keyRow("clipkey", () -> cfg.clipKey, v -> cfg.clipKey = v));
                        r.add(action("reloadpacks", () -> client.reloadResources()));
                        r.add(action("openpacks", () -> openDir(client.getResourcePackDir())));
                        r.add(action("openshots", () -> openDir(client.runDirectory.toPath().resolve("screenshots"))));
                    }
                }
            }
            case 4 -> {
                r.add(header(Lang.h("marks")));
                r.add(toggle("showmarks", () -> cfg.showMarks, v -> cfg.showMarks = v));
                r.add(toggle("showdist", () -> cfg.markDist, v -> cfg.markDist = v));
                r.add(cycle("markcolor", fixed(Opts.MARK_COLORS), () -> cfg.markColor, v -> cfg.markColor = v));
                r.add(cycle("markfx", fixed(Opts.MARK_FX), () -> cfg.markFx, v -> cfg.markFx = v));
                r.add(action("markhere", () -> WaypointManager.add(null)));
                r.add(info(Lang.i("marks")));
                for (FlutiConfig.Waypoint w : WaypointManager.here()) r.add(markRow(w));
            }
            case 5 -> {
                r.add(header(Lang.h("profiles")));
                r.add(cycle("profile", Profiles::names, () -> cfg.profile, v -> Profiles.apply(v)));
                r.add(action("profexport", () -> { client.keyboard.setClipboard(Profiles.exportJson()); msg("m.copied"); }));
                r.add(action("profimport", () -> msg(Profiles.importJson(client.keyboard.getClipboard()) ? "m.imported" : "m.importfail")));
                r.add(action("restorebackup", () -> msg(Backups.restoreLatest() ? "m.restored" : "m.restorefail")));
                r.add(keyRow("profilekey", () -> cfg.profileKey, v -> cfg.profileKey = v));
                r.add(info(Lang.i("profiles")));
            }
            case 7 -> {
                r.add(header(Lang.h("cosm")));
                r.add(toggle("swordskin", () -> cfg.swordSkin, v -> { cfg.swordSkin = v; CosmeticsPack.apply(); }));
                r.add(cycle("swordmodel", fixed(CosmeticsPack.MODELS), () -> cfg.swordModel,
                        v -> { cfg.swordModel = v; if (cfg.swordSkin) CosmeticsPack.apply(); }));
                r.add(cycle("swordtarget", fixed(CosmeticsPack.TARGETS), () -> cfg.swordTarget,
                        v -> { cfg.swordTarget = v; if (cfg.swordSkin) CosmeticsPack.apply(); }));
                r.add(header(Lang.h("headgear")));
                r.add(cycle("hat", fixed(List.of("OFF", "CHINESE")), () -> cfg.hat, v -> cfg.hat = v));
                r.add(slider("hatsize", 0.5f, 2.5f, () -> cfg.hatScale, v -> cfg.hatScale = v));
                r.add(cycle("hatcolor", fixed(Opts.HAT_COLORS), () -> cfg.hatColor, v -> cfg.hatColor = v));
                r.add(cycle("headring", fixed(Opts.TRAIL), () -> cfg.headRing, v -> cfg.headRing = v));
                r.add(slider("ringsize", 0.5f, 2f, () -> cfg.ringScale, v -> cfg.ringScale = v));
                r.add(header(Lang.h("godmode")));
                r.add(cycle("wings", fixed(Opts.TRAIL), () -> cfg.wings, v -> cfg.wings = v));
                r.add(toggle("god", () -> cfg.god, v -> cfg.god = v));
            }
            default -> {
                r.add(header(Lang.h("lang")));
                for (String code : Lang.CODES) r.add(langRow(code));
                r.add(header(Lang.h("theme")));
                r.add(cycle("theme", fixed(Opts.THEMES), () -> cfg.theme, v -> cfg.theme = v));
                r.add(toggle("customlobby", () -> cfg.customLobby, v -> cfg.customLobby = v));
            }
        }
        return r;
    }

    // ---------------- екран ----------------
    private boolean hasSubs() { return SUBS[tab].length > 1; }
    private boolean macroField() { return tab == 3 && sub[3] == 2; }
    private boolean hasField() { return macroField() || tab == 4 || tab == 5; }
    private int sIdx() { return tab * 8 + sub[tab]; }
    private int stripH() { return hasSubs() ? STRIP_H : 0; }
    private int viewH() { return ph - TOP - 12 - stripH() - (hasField() ? 30 : 0); }
    private int cx() { return px + SIDE + 8; }
    private int cw() { return pw - SIDE - 16; }
    private int ctop() { return py + TOP + 6 + stripH(); }

    private void clampPos() {
        px = Math.max(0, Math.min(width - pw, px));
        py = Math.max(0, Math.min(height - ph, py));
        memX = px; memY = py;
    }

    @Override protected void init() {
        pw = Math.min(410, width - 16);
        ph = Math.min(290, height - 16);
        if (memX == Integer.MIN_VALUE) { px = (width - pw) / 2; py = (height - ph) / 2; }
        else { px = memX; py = memY; }
        clampPos();

        cmdField = new TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.literal("cmd"));
        cmdField.setMaxLength(100);
        addBtn = ButtonWidget.builder(Text.literal("+"), b -> {
            String t = cmdField.getText().trim();
            if (t.isEmpty() || cfg.macros.size() >= MAX_MACROS) return;
            if (!t.startsWith("/")) t = "/" + t;
            FlutiConfig.Macro m = new FlutiConfig.Macro(t, -1);
            cfg.macros.add(m); listening = m; cmdField.setText("");
        }).dimensions(0, 0, 52, 20).build();
        nameField = new TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.literal("mark"));
        nameField.setMaxLength(24);
        markBtn = ButtonWidget.builder(Text.literal("+"), b -> {
            if (tab == 5) { if (Profiles.saveAs(nameField.getText())) msg("m.saved"); }
            else WaypointManager.add(nameField.getText());
            nameField.setText("");
        }).dimensions(0, 0, 52, 20).build();
        addDrawableChild(cmdField); addDrawableChild(addBtn);
        addDrawableChild(nameField); addDrawableChild(markBtn);
    }

    @Override public void renderBackground(DrawContext ctx, int mx, int my, float d) { ctx.fill(0, 0, width, height, 0x90000000); }

    private int stripX(int i, TextRenderer tr) {
        int x = cx();
        for (int k = 0; k < i; k++) x += tr.getWidth(Lang.get(SUBS[tab][k])) + 18;
        return x;
    }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        int acc = accent();
        ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, Opts.dim(acc, 0.25f));
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, Opts.dim(acc, 0.7f));
        ctx.fillGradient(px, py, px + pw, py + ph, 0xF0261340, 0xF00A0618);
        ctx.fillGradient(px, py, px + pw, py + TOP, Opts.dim(acc, 0.5f), 0xFF14141E);
        ctx.fill(px, py + TOP, px + pw, py + TOP + 1, acc);
        ctx.drawText(textRenderer, "Fluti Visuals", px + 10, py + 9, 0xFFFFFFFF, true);
        String ver = "v0.6.0";
        ctx.drawText(textRenderer, ver, px + pw - 8 - textRenderer.getWidth(ver), py + 9, 0xFFB8B8CC, false);
        ctx.fill(px, py + TOP + 1, px + SIDE, py + ph, SIDEBG);

        for (int i = 0; i < TAB_KEYS.length; i++) {
            int ty = py + TOP + 8 + i * 20;
            boolean hov = mx >= px + 5 && mx <= px + SIDE - 5 && my >= ty && my <= ty + 18;
            ctx.fill(px + 5, ty, px + SIDE - 5, ty + 18, i == tab ? Opts.dim(acc, 0.3f) : hov ? 0xFF1E1E2C : 0x00000000);
            if (i == tab) ctx.fill(px + 5, ty, px + 7, ty + 18, acc);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(Lang.get(TAB_KEYS[i]), SIDE - 20), px + 13, ty + 5,
                    i == tab ? 0xFFFFFFFF : 0xFF9999AA, false);
        }

        if (hasSubs()) {                                   // під-вкладки
            int sy = py + TOP + 6;
            for (int i = 0; i < SUBS[tab].length; i++) {
                String name = Lang.get(SUBS[tab][i]);
                int sx = stripX(i, textRenderer), sw = textRenderer.getWidth(name) + 12;
                boolean on = i == sub[tab], hov = mx >= sx && mx <= sx + sw && my >= sy && my <= sy + 16;
                ctx.fill(sx, sy, sx + sw, sy + 16, on ? Opts.dim(acc, 0.35f) : hov ? 0xFF22222F : 0xFF181822);
                if (on) ctx.fill(sx, sy + 15, sx + sw, sy + 16, acc);
                ctx.drawText(textRenderer, name, sx + 6, sy + 4, on ? 0xFFFFFFFF : 0xFF9999AA, false);
            }
        }

        int top = ctop(), vh = viewH(), x = cx(), w = cw();
        int y = top - scroll[sIdx()];
        ctx.enableScissor(x - 2, top, x + w + 2, top + vh);
        for (Row r : rows()) {
            int h = r.h();
            boolean hov = mx >= x && mx <= x + w && my >= Math.max(y, top) && my <= Math.min(y + h, top + vh);
            if (y + h > top && y < top + vh) r.render(ctx, textRenderer, x, y, w, hov);
            y += h + GAP;
        }
        ctx.disableScissor();
        contentH = y - (top - scroll[sIdx()]);
        int max = Math.max(0, contentH - vh);
        scroll[sIdx()] = Math.max(0, Math.min(max, scroll[sIdx()]));
        if (max > 0) {
            int barH = Math.max(14, vh * vh / contentH);
            int barY = top + (int) ((vh - barH) * (scroll[sIdx()] / (float) max));
            ctx.fill(px + pw - 4, top, px + pw - 2, top + vh, 0xFF1C1C28);
            ctx.fill(px + pw - 4, barY, px + pw - 2, barY + barH, acc);
        }

        int fy = py + ph - 28;
        cmdField.setX(cx()); cmdField.setY(fy); cmdField.setWidth(cw() - 56);
        addBtn.setX(cx() + cw() - 52); addBtn.setY(fy);
        nameField.setX(cx()); nameField.setY(fy); nameField.setWidth(cw() - 56);
        markBtn.setX(cx() + cw() - 52); markBtn.setY(fy);
        cmdField.setPlaceholder(Text.literal(Lang.get("ph.cmd")));
        nameField.setPlaceholder(Text.literal(Lang.get(tab == 5 ? "ph.profile" : "ph.mark")));
        addBtn.setMessage(Text.literal(Lang.get("b.add")));
        markBtn.setMessage(Text.literal(Lang.get("b.add")));
        cmdField.visible = macroField(); addBtn.visible = macroField();
        nameField.visible = tab == 4 || tab == 5; markBtn.visible = tab == 4 || tab == 5;
        super.render(ctx, mx, my, d);
    }

    private void play(int kind) {
        if (!cfg.guiSounds) return;
        switch (kind) {
            case ON -> SoundLibrary.ui("block.amethyst_block.chime", 1.8f, 0.5f);
            case OFF -> SoundLibrary.ui("block.note_block.bass", 0.8f, 0.5f);
            case TAB -> SoundLibrary.ui("block.note_block.harp", 1.5f, 0.35f);
            case DEL -> SoundLibrary.ui("block.glass.break", 1.6f, 0.25f);
            case CLICK -> SoundLibrary.ui("ui.button.click", 1.1f, 0.5f);
            default -> { }
        }
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (mx >= px && mx <= px + pw && my >= py && my <= py + TOP) {     // тягнемо меню за шапку
            dragging = true; dragDX = mx - px; dragDY = my - py; dragAcc = 0;
            play(CLICK);
            return true;
        }
        for (int i = 0; i < TAB_KEYS.length; i++) {
            int ty = py + TOP + 8 + i * 20;
            if (mx >= px + 5 && mx <= px + SIDE - 5 && my >= ty && my <= ty + 18) {
                if (tab != i) play(TAB);
                tab = i; listening = null; pickKey = null; pickId = null; return true;
            }
        }
        if (hasSubs()) {
            int sy = py + TOP + 6;
            for (int i = 0; i < SUBS[tab].length; i++) {
                int sx = stripX(i, textRenderer), sw = textRenderer.getWidth(Lang.get(SUBS[tab][i])) + 12;
                if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + 16) {
                    if (sub[tab] != i) play(TAB);
                    sub[tab] = i; listening = null; pickKey = null; pickId = null; return true;
                }
            }
        }
        if (super.mouseClicked(click, doubled)) return true;
        int top = ctop(), vh = viewH(), x = cx(), w = cw();
        if (my < top || my > top + vh) return false;
        int y = top - scroll[sIdx()];
        for (Row r : rows()) {
            int h = r.h();
            if (mx >= x && mx <= x + w && my >= y && my < y + h) {
                int res = r.click(mx, x, w, click.button());
                if (res >= 0) {
                    if (r.isSlider()) { slider = r; lastTickX = mx; }
                    play(res);
                    return true;
                }
            }
            y += h + GAP;
        }
        return false;
    }

    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging) {
            int ox = px, oy = py;
            px = (int) (click.x() - dragDX); py = (int) (click.y() - dragDY);
            clampPos();
            dragAcc += Math.abs(px - ox) + Math.abs(py - oy);
            if (dragAcc >= 24 && cfg.guiSounds) {
                dragAcc = 0;
                SoundLibrary.ui("block.note_block.hat", 1.3f + (float) Math.random() * 0.5f, 0.25f);
            }
            return true;
        }
        if (slider != null) {
            slider.click(click.x(), cx(), cw(), 0);
            if (Math.abs(click.x() - lastTickX) >= 10 && cfg.guiSounds) {
                lastTickX = click.x();
                SoundLibrary.ui("block.note_block.hat", 1.2f + (float) (click.x() % 7) / 10f, 0.2f);
            }
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override public boolean mouseReleased(Click click) { slider = null; dragging = false; return super.mouseReleased(click); }

    @Override public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scroll[sIdx()] -= (int) (vAmount * 18);
        return true;
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (pickKey != null) {
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) pickKey.accept(input.key());
            pickKey = null; pickId = null; return true;
        }
        if (listening != null) {
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) listening.key = input.key();
            listening = null; return true;
        }
        return super.keyPressed(input);
    }

    /** Файли можна класти в папки config/fluti/sounds і config/fluti/songs або перетягнути сюди. */
    @Override public void onFilesDropped(List<Path> paths) {
        int ok = 0;
        String err = null;
        boolean song = tab == 1 && sub[1] == 1;
        for (Path p : paths) {
            String n = p.getFileName().toString();
            String low = n.toLowerCase(Locale.ROOT);
            try {
                if (low.endsWith(".ogg")) {
                    String bad = OggPlayer.validate(p);
                    if (bad != null) { err = bad; continue; }
                    Path dir = song ? LyricsPlayer.dir() : SoundLibrary.dir();
                    Files.createDirectories(dir);
                    Files.copy(p, dir.resolve(n), StandardCopyOption.REPLACE_EXISTING);
                    if (song) cfg.song = n.substring(0, n.length() - 4);
                    ok++;
                } else if (low.endsWith(".lrc")) {
                    String target = cfg.song.isEmpty() ? n.substring(0, n.length() - 4) : cfg.song;
                    Files.createDirectories(LyricsPlayer.dir());
                    Files.copy(p, LyricsPlayer.dir().resolve(target + ".lrc"), StandardCopyOption.REPLACE_EXISTING);
                    ok++;
                }
            } catch (Exception e) { err = "Cannot copy " + n; }
        }
        if (ok > 0) msg("m.dropped"); else if (err != null) msgRaw(err);
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void removed() {
        ConfigManager.save();
        if (cfg.guiSounds) SoundLibrary.ui("block.amethyst_block.chime", 0.8f, 0.4f);
    }
}
