package dev.fluti.visuals.gui;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import dev.fluti.visuals.feature.GlintPack;
import dev.fluti.visuals.feature.GreenScreen;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Мінімалістичний власний ClickGUI: бічні вкладки + рядки (toggle / cycle / slider / action). */
public class ClickGuiScreen extends Screen {
    private static final String[] TABS = {"Visuals", "Audio", "Creator", "Utilities"};
    private static final String[] SOUNDS = {"OFF", "BELL", "CLICK", "PLING", "CUSTOM"};
    private static final int BG = 0xE60E0E14, SIDE = 0xFF15151F, ACCENT = 0xFF9B7CFF, ROW = 0xFF1C1C28, OFF = 0xFF3A3A4A;
    private static final int ROW_H = 22, ROW_STEP = 24, MAX_MACROS = 5;

    private final FlutiConfig cfg = ConfigManager.get();
    private int tab, px, py;
    private final int pw = 440, ph = 300;
    private Row slider;
    private FlutiConfig.Macro listening;
    private TextFieldWidget cmdField;
    private ButtonWidget addBtn;

    public ClickGuiScreen() { super(Text.literal("Fluti")); }

    // ---------------- rows ----------------
    private interface Row {
        void render(DrawContext c, TextRenderer tr, int x, int y, int w);
        boolean click(double mx, int x, int w, int button);
        default boolean isSlider() { return false; }
    }

    private static void base(DrawContext c, TextRenderer tr, int x, int y, int w, String label) {
        c.fill(x, y, x + w, y + ROW_H, ROW);
        c.drawText(tr, label, x + 8, y + 7, 0xFFE8E8F0, false);
    }

    private static Row toggle(String label, Supplier<Boolean> g, Consumer<Boolean> s) {
        return new Row() {
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w) {
                base(c, tr, x, y, w, label);
                boolean on = g.get();
                c.fill(x + w - 34, y + 5, x + w - 8, y + 17, on ? ACCENT : OFF);
                int k = on ? x + w - 20 : x + w - 32;
                c.fill(k, y + 7, k + 10, y + 15, 0xFFFFFFFF);
            }
            public boolean click(double mx, int x, int w, int b) { s.accept(!g.get()); return true; }
        };
    }

    private static Row cycle(String label, String[] opts, Supplier<String> g, Consumer<String> s) {
        return new Row() {
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w) {
                base(c, tr, x, y, w, label);
                String v = "< " + g.get() + " >";
                c.drawText(tr, v, x + w - 8 - tr.getWidth(v), y + 7, ACCENT, false);
            }
            public boolean click(double mx, int x, int w, int b) {
                int i = 0;
                for (int k = 0; k < opts.length; k++) if (opts[k].equals(g.get())) i = k;
                i = (i + (b == 1 ? opts.length - 1 : 1)) % opts.length;
                s.accept(opts[i]); return true;
            }
        };
    }

    private static Row slider(String label, float min, float max, Supplier<Float> g, Consumer<Float> s) {
        return new Row() {
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w) {
                base(c, tr, x, y, w, label);
                int bx = x + w - 130, bw = 110;
                float t = (g.get() - min) / (max - min);
                c.fill(bx, y + 10, bx + bw, y + 12, OFF);
                c.fill(bx, y + 10, bx + (int) (bw * t), y + 12, ACCENT);
                c.fill(bx + (int) (bw * t) - 2, y + 6, bx + (int) (bw * t) + 2, y + 16, 0xFFFFFFFF);
            }
            public boolean click(double mx, int x, int w, int b) {
                int bx = x + w - 130;
                float t = (float) Math.max(0, Math.min(1, (mx - bx) / 110.0));
                s.accept(min + t * (max - min)); return true;
            }
            public boolean isSlider() { return true; }
        };
    }

    private static Row action(String label, Runnable r) {
        return new Row() {
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w) {
                c.fill(x, y, x + w, y + ROW_H, 0xFF2A2347);
                c.drawText(tr, label, x + 8, y + 7, ACCENT, false);
            }
            public boolean click(double mx, int x, int w, int b) { r.run(); return true; }
        };
    }

    private static Row info(String text) {
        return new Row() {
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w) { c.drawText(tr, text, x + 4, y + 7, 0xFF8888A0, false); }
            public boolean click(double mx, int x, int w, int b) { return false; }
        };
    }

    private Row macroRow(FlutiConfig.Macro m) {
        return new Row() {
            public void render(DrawContext c, TextRenderer tr, int x, int y, int w) {
                base(c, tr, x, y, w, m.command);
                String k = listening == m ? "[ press key ]" : "[" + keyName(m.key) + "]";
                c.drawText(tr, k, x + w - 34 - tr.getWidth(k), y + 7, ACCENT, false);
                c.drawText(tr, "✕", x + w - 16, y + 7, 0xFFFF6060, false);
            }
            public boolean click(double mx, int x, int w, int b) {
                if (mx >= x + w - 22) { cfg.macros.remove(m); if (listening == m) listening = null; }
                else listening = m;
                return true;
            }
        };
    }

    private static String keyName(int k) {
        return k <= 0 ? "none" : InputUtil.Type.KEYSYM.createFromCode(k).getLocalizedText().getString();
    }

    private List<Row> rows() {
        List<Row> r = new ArrayList<>();
        switch (tab) {
            case 0 -> {
                r.add(cycle("Enchantment Glint", GlintPack.COLORS, () -> cfg.glintColor, v -> { cfg.glintColor = v; GlintPack.apply(v); }));
                r.add(info("Glint змінюється ресурспаком (перезавантаження ресурсів)."));
                r.add(toggle("Hitmarkers", () -> cfg.hitmarkers, v -> cfg.hitmarkers = v));
                r.add(cycle("Crit Particles", new String[]{"OFF", "HEARTS", "STARS", "SPARKS"}, () -> cfg.critParticles, v -> cfg.critParticles = v));
            }
            case 1 -> {
                r.add(cycle("Hit Sound", SOUNDS, () -> cfg.hitSound, v -> cfg.hitSound = v));
                r.add(cycle("Kill Sound", SOUNDS, () -> cfg.killSound, v -> cfg.killSound = v));
                r.add(slider("Volume", 0f, 1f, () -> cfg.soundVolume, v -> cfg.soundVolume = v));
                r.add(info("CUSTOM: .minecraft/config/fluti/sounds/hit.ogg та kill.ogg"));
            }
            case 2 -> {
                r.add(toggle("Safe Cinematic Camera", () -> cfg.cinematic, v -> cfg.cinematic = v));
                r.add(slider("Smoothing (лівіше = плавніше)", 0.05f, 0.5f, () -> cfg.cinematicSmooth, v -> cfg.cinematicSmooth = v));
                r.add(toggle("Password Masking (/l /login /reg)", () -> cfg.maskPasswords, v -> cfg.maskPasswords = v));
                r.add(toggle("Green Screen", () -> cfg.greenScreen, v -> { cfg.greenScreen = v; GreenScreen.apply(v); }));
            }
            default -> {
                r.add(toggle("HUD: Armor durability", () -> cfg.armor.enabled, v -> cfg.armor.enabled = v));
                r.add(toggle("HUD: Potion timers", () -> cfg.potions.enabled, v -> cfg.potions.enabled = v));
                r.add(toggle("HUD: Ping", () -> cfg.ping.enabled, v -> cfg.ping.enabled = v));
                r.add(action("Редагувати позиції HUD →", () -> client.setScreen(new HudEditScreen(this))));
                for (FlutiConfig.Macro m : cfg.macros) r.add(macroRow(m));
            }
        }
        return r;
    }

    // ---------------- screen ----------------
    @Override protected void init() {
        px = (width - pw) / 2; py = (height - ph) / 2;
        int fx = px + 100, fy = py + ph - 28;
        cmdField = new TextFieldWidget(textRenderer, fx, fy, pw - 100 - 10 - 60, 20, Text.literal("cmd"));
        cmdField.setPlaceholder(Text.literal("/home, /ah, /spawn ..."));
        cmdField.setMaxLength(100);
        addBtn = ButtonWidget.builder(Text.literal("Add"), b -> {
            String t = cmdField.getText().trim();
            if (t.isEmpty() || cfg.macros.size() >= MAX_MACROS) return;
            if (!t.startsWith("/")) t = "/" + t;
            FlutiConfig.Macro m = new FlutiConfig.Macro(t, -1);
            cfg.macros.add(m); listening = m; cmdField.setText("");
        }).dimensions(fx + pw - 100 - 10 - 56, fy, 56, 20).build();
        addDrawableChild(cmdField); addDrawableChild(addBtn);
    }

    @Override public void renderBackground(DrawContext ctx, int mx, int my, float d) { ctx.fill(0, 0, width, height, 0x90000000); }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        ctx.fill(px, py, px + pw, py + ph, BG);
        ctx.fill(px, py, px + 92, py + ph, SIDE);
        ctx.drawText(textRenderer, "Fluti Visuals", px + 10, py + 12, ACCENT, true);
        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 34 + i * 26;
            ctx.fill(px + 8, ty, px + 84, ty + 22, i == tab ? 0xFF2A2347 : 0x00000000);
            if (i == tab) ctx.fill(px + 8, ty, px + 10, ty + 22, ACCENT);
            ctx.drawText(textRenderer, TABS[i], px + 18, ty + 7, i == tab ? 0xFFFFFFFF : 0xFF9999AA, false);
        }
        int x = px + 100, w = pw - 110, y = py + 14;
        for (Row r : rows()) { r.render(ctx, textRenderer, x, y, w); y += ROW_STEP; }
        boolean util = tab == 3;
        cmdField.visible = util; addBtn.visible = util;
        super.render(ctx, mx, my, d);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 34 + i * 26;
            if (mx >= px + 8 && mx <= px + 84 && my >= ty && my <= ty + 22) { tab = i; listening = null; return true; }
        }
        if (super.mouseClicked(click, doubled)) return true;
        int x = px + 100, w = pw - 110, y = py + 14;
        for (Row r : rows()) {
            if (mx >= x && mx <= x + w && my >= y && my <= y + ROW_H) {
                if (r.click(mx, x, w, click.button())) { if (r.isSlider()) slider = r; return true; }
            }
            y += ROW_STEP;
        }
        return false;
    }

    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (slider != null) { slider.click(click.x(), px + 100, pw - 110, 0); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override public boolean mouseReleased(Click click) { slider = null; return super.mouseReleased(click); }

    @Override public boolean keyPressed(KeyInput input) {
        if (listening != null) {
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) listening.key = input.key();
            listening = null; return true;
        }
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void removed() { ConfigManager.save(); }
}
