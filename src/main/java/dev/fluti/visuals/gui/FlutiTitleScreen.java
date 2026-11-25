package dev.fluti.visuals.gui;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.Lang;
import dev.fluti.visuals.feature.Opts;
import dev.fluti.visuals.feature.SoundLibrary;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Лобі Fluti: анімований фіолетовий фон, годинник, дата й кнопки по центру. */
public class FlutiTitleScreen extends Screen {
    private static final String[] KEYS = {"lobby.single", "lobby.multi", "lobby.fluti", "lobby.settings", "lobby.quit"};
    private static final int BW = 180, BH = 20, GAP = 5;

    public FlutiTitleScreen() {
        super(Text.literal("Fluti"));
        TitleButton.bypass = false;
    }

    private void swirl(DrawContext ctx) {
        int w = width, h = height;
        ctx.fillGradient(0, 0, w, h, 0xFF1D0A3A, 0xFF05020E);
        double t = System.currentTimeMillis() / 1000.0;
        int[] cols = {0x8A2BE2, 0x6A0DAD, 0xC040FF, 0x4B0082, 0xFF4DFF};
        for (int i = 0; i < 6; i++) {
            int cx = (int) (w * (0.5 + 0.42 * Math.sin(t * 0.23 + i * 1.7)));
            int cy = (int) (h * (0.5 + 0.38 * Math.cos(t * 0.19 + i * 2.3)));
            int r = (int) (Math.min(w, h) * (0.28 + 0.1 * Math.sin(t * 0.4 + i)));
            for (int layer = 0; layer < 4; layer++) {
                int rr = r * (4 - layer) / 4, a = 14 + layer * 10;
                for (int dy = -rr; dy <= rr; dy += 3) {
                    int hw = (int) Math.sqrt((double) rr * rr - (double) dy * dy);
                    ctx.fill(cx - hw, cy + dy, cx + hw, cy + dy + 3, (a << 24) | cols[i % 5]);
                }
            }
        }
        ctx.fillGradient(0, 0, w, h, 0x40000000, 0xA0000000);
    }

    private int top() { return height / 2 - 10; }

    @Override public void renderBackground(DrawContext ctx, int mx, int my, float d) { swirl(ctx); }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        var tr = textRenderer;
        String clock = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag(ConfigManager.get().lang)));
        if (!date.isEmpty()) date = Character.toUpperCase(date.charAt(0)) + date.substring(1);
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(width / 2f, height * 0.12f);
        m.scale(5f, 5f);
        ctx.drawCenteredTextWithShadow(tr, clock, 0, 0, 0xFFFFFFFF);
        m.popMatrix();
        ctx.drawCenteredTextWithShadow(tr, date, width / 2, (int) (height * 0.12f) + 48, 0xFFE8D8FF);

        int acc = Opts.accent(ConfigManager.get().theme);
        int x = (width - BW) / 2, y = top();
        for (int i = 0; i < KEYS.length; i++) {
            boolean hov = mx >= x && mx <= x + BW && my >= y && my <= y + BH;
            ctx.fill(x + 1, y, x + BW - 1, y + BH, hov ? 0xCC2A1650 : 0xAA120A22);
            ctx.fill(x, y + 1, x + BW, y + BH - 1, hov ? 0xCC2A1650 : 0xAA120A22);
            int edge = hov ? acc : 0xFF4A3380;
            ctx.fill(x + 1, y, x + BW - 1, y + 1, edge); ctx.fill(x + 1, y + BH - 1, x + BW - 1, y + BH, edge);
            ctx.fill(x, y + 1, x + 1, y + BH - 1, edge); ctx.fill(x + BW - 1, y + 1, x + BW, y + BH - 1, edge);
            ctx.drawCenteredTextWithShadow(tr, Lang.get(KEYS[i]), width / 2, y + 6, 0xFFFFFFFF);
            y += BH + GAP;
        }
        String v = Lang.get("lobby.vanilla");
        ctx.drawText(tr, v, 6, height - 12, 0xFF9988BB, false);
        super.render(ctx, mx, my, d);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (my >= height - 14 && mx <= 6 + textRenderer.getWidth(Lang.get("lobby.vanilla"))) {
            TitleButton.bypass = true;
            client.setScreen(new TitleScreen());
            return true;
        }
        int x = (width - BW) / 2, y = top();
        for (int i = 0; i < KEYS.length; i++) {
            if (mx >= x && mx <= x + BW && my >= y && my <= y + BH) {
                SoundLibrary.ui("ui.button.click", 1.1f, 0.5f);
                switch (i) {
                    case 0 -> client.setScreen(new SelectWorldScreen(this));
                    case 1 -> client.setScreen(new MultiplayerScreen(this));
                    case 2 -> client.setScreen(new ClickGuiScreen());
                    case 3 -> client.setScreen(new OptionsScreen(this, client.options));
                    default -> client.scheduleStop();
                }
                return true;
            }
            y += BH + GAP;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
}
