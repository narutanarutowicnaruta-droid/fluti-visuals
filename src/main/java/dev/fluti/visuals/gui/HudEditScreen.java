package dev.fluti.visuals.gui;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.hud.HudManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {
    private final Screen parent;
    private HudManager.Widget drag;
    private int offX, offY;

    public HudEditScreen(Screen parent) { super(Text.literal("HUD editor")); this.parent = parent; }

    @Override public void renderBackground(DrawContext ctx, int mx, int my, float d) { ctx.fill(0, 0, width, height, 0x80000000); }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        HudManager.render(ctx, true);
        ctx.drawCenteredTextWithShadow(textRenderer, "Перетягни віджети мишкою • ESC — зберегти", width / 2, 10, 0xFFFFFFFF);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        for (HudManager.Widget w : HudManager.WIDGETS) if (w.contains(click.x(), click.y())) {
            drag = w; offX = (int) click.x() - w.cfg.x; offY = (int) click.y() - w.cfg.y; return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (drag == null) return super.mouseDragged(click, dx, dy);
        int nx = (int) click.x() - offX, ny = (int) click.y() - offY;
        if (ConfigManager.get().hudSnap) { nx = Math.round(nx / 4f) * 4; ny = Math.round(ny / 4f) * 4; }
        drag.cfg.x = Math.max(0, Math.min(width - drag.w, nx));
        drag.cfg.y = Math.max(0, Math.min(height - drag.h, ny));
        return true;
    }

    @Override public boolean mouseReleased(Click click) { drag = null; return super.mouseReleased(click); }

    @Override public void close() { ConfigManager.save(); client.setScreen(parent); }
}
