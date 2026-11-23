package dev.fluti.visuals.hud;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import dev.fluti.visuals.feature.CombatFx;
import dev.fluti.visuals.feature.LyricsPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Перетягувані віджети. Малюються через DrawContext, без кастомних шейдерів (Sodium/Iris-safe). */
public final class HudManager {
    public interface Custom { int[] draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit); }

    public static final class Widget {
        public final String name; public final FlutiConfig.HudCfg cfg;
        final Function<MinecraftClient, List<String>> lines; final Custom custom;
        public int w, h;
        Widget(String n, FlutiConfig.HudCfg c, Function<MinecraftClient, List<String>> l, Custom cu) { name = n; cfg = c; lines = l; custom = cu; }
        public boolean contains(double mx, double my) { return mx >= cfg.x && mx <= cfg.x + w && my >= cfg.y && my <= cfg.y + h; }
    }

    public static final List<Widget> WIDGETS = new ArrayList<>();

    private HudManager() {}

    private static float op() { return Math.max(0.05f, Math.min(1f, ConfigManager.get().hudOpacity)); }
    private static int bg() { return ((int) (255 * op())) << 24; }
    private static int bgDim() { return ((int) (127 * op())) << 24; }

    public static void init() {
        FlutiConfig c = ConfigManager.get();
        WIDGETS.add(new Widget("Armor", c.armor, HudManager::armor, null));
        WIDGETS.add(new Widget("Potions", c.potions, null, HudManager::potionsDraw));
        WIDGETS.add(new Widget("Ping", c.ping, HudManager::ping, null));
        WIDGETS.add(new Widget("Stats", c.stats, mc -> List.of("Kills: §a" + CombatFx.kills(), "Combo: §e" + CombatFx.combo()), null));
        WIDGETS.add(new Widget("FPS", c.fps, mc -> List.of("FPS: §a" + mc.getCurrentFps()), null));
        WIDGETS.add(new Widget("Coords", c.coords, HudManager::coords, null));
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.of("fluti", "hud"),
                (ctx, tick) -> {
                    dev.fluti.visuals.feature.ScreenFx.render(ctx);
                    CombatFx.renderHitmarker(ctx);
                    LyricsPlayer.render(ctx);
                    WaypointRenderer.render(ctx, tick.getTickProgress(false));
                    if (!MinecraftClient.getInstance().options.hudHidden) render(ctx, false);
                });
    }

    public static void render(DrawContext ctx, boolean edit) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        for (Widget wd : WIDGETS) {
            if (!edit && !wd.cfg.enabled) continue;
            int x = wd.cfg.x, y = wd.cfg.y;
            if (wd.custom != null) {
                int[] sz = wd.custom.draw(ctx, mc, x, y, edit);
                if (sz == null) continue;
                wd.w = sz[0]; wd.h = sz[1];
            } else {
                List<String> ls = wd.lines.apply(mc);
                if (ls.isEmpty()) { if (!edit) continue; ls = List.of("§7" + wd.name); }
                int w = 0;
                for (String s : ls) w = Math.max(w, tr.getWidth(s));
                wd.w = w + 8; wd.h = ls.size() * 10 + 6;
                ctx.fill(x, y, x + wd.w, y + wd.h, wd.cfg.enabled || !edit ? bg() : bgDim());
                int ty = y + 4;
                for (String s : ls) { ctx.drawText(tr, s, x + 4, ty, 0xFFFFFFFF, true); ty += 10; }
            }
            if (edit) {
                int o = 0xFF9B7CFF;
                ctx.fill(x, y, x + wd.w, y + 1, o); ctx.fill(x, y + wd.h - 1, x + wd.w, y + wd.h, o);
                ctx.fill(x, y, x + 1, y + wd.h, o); ctx.fill(x + wd.w - 1, y, x + wd.w, y + wd.h, o);
            }
        }
    }

    // ---------- ефекти з іконками та світінням ----------
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private static int[] potionsDraw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
        TextRenderer tr = mc.textRenderer;
        List<StatusEffectInstance> list = new ArrayList<>(mc.player.getStatusEffects());
        if (list.isEmpty()) {
            if (!edit) return null;
            ctx.fill(x, y, x + 70, y + 16, bg());
            ctx.drawText(tr, "Potions", x + 4, y + 4, 0xFF9999AA, true);
            return new int[]{70, 16};
        }
        int rowH = 24, w = 70;
        List<String> names = new ArrayList<>();
        for (StatusEffectInstance e : list) {
            int lvl = e.getAmplifier() + 1;
            String n = e.getEffectType().value().getName().getString() + " " + (lvl <= 10 ? ROMAN[lvl] : String.valueOf(lvl));
            names.add(n);
            w = Math.max(w, tr.getWidth(n) + 34);
        }
        int h = list.size() * rowH + 4;
        ctx.fill(x, y, x + w, y + h, bg());
        long now = System.currentTimeMillis();
        int yy = y + 3, i = 0;
        for (StatusEffectInstance e : list) {
            int rgb = e.getEffectType().value().getColor() & 0xFFFFFF;
            boolean low = !e.isInfinite() && e.getDuration() < 200;
            double pulse = low ? 0.55 + 0.45 * Math.sin(now / 110.0) : 0.8 + 0.2 * Math.sin(now / 350.0);
            int ix = x + 5, iy = yy + 2;
            for (int g = 5; g >= 1; g--) {                       // м'яке світіння навколо іконки
                int a = (int) Math.min(255, pulse * (8 + (6 - g) * 9));
                ctx.fill(ix - g, iy - g, ix + 18 + g, iy + 18 + g, (a << 24) | rgb);
            }
            ctx.fill(ix, iy, ix + 18, iy + 18, 0xE0101018);
            Identifier sprite = e.getEffectType().getKey().map(k -> k.getValue().withPrefixedPath("mob_effect/")).orElse(null);
            if (sprite != null) ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, ix, iy, 18, 18);
            ctx.drawText(tr, names.get(i), x + 28, yy + 4, 0xFFFFFFFF, true);
            String time;
            if (e.isInfinite()) time = "∞";
            else { int s = e.getDuration() / 20; time = (s / 60) + ":" + String.format("%02d", s % 60); }
            ctx.drawText(tr, time, x + 28, yy + 14, low ? 0xFFFF5555 : 0xFFAAAAAA, true);
            yy += rowH; i++;
        }
        return new int[]{w, h};
    }

    private static List<String> armor(MinecraftClient mc) {
        List<String> out = new ArrayList<>();
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] names = {"Helmet", "Chest", "Legs", "Boots"};
        for (int i = 0; i < 4; i++) {
            ItemStack s = mc.player.getEquippedStack(slots[i]);
            if (s.isEmpty() || s.getMaxDamage() <= 0) continue;
            int left = s.getMaxDamage() - s.getDamage();
            int pct = left * 100 / s.getMaxDamage();
            String col = pct > 50 ? "§a" : pct > 20 ? "§e" : "§c";
            out.add(names[i] + ": " + col + left + " §7(" + pct + "%)");
        }
        return out;
    }

    private static List<String> ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null) return List.of();
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (e == null) return List.of();
        int p = e.getLatency();
        return List.of("Ping: " + (p < 80 ? "§a" : p < 160 ? "§e" : "§c") + p + " ms");
    }

    private static List<String> coords(MinecraftClient mc) {
        var p = mc.player;
        return List.of("XYZ: §f" + (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " " + (int) Math.floor(p.getZ()),
                "Facing: §f" + p.getHorizontalFacing().asString());
    }
}
