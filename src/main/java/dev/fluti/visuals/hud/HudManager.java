package dev.fluti.visuals.hud;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import dev.fluti.visuals.feature.CombatFx;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Перетягувані віджети. Малюються стандартним DrawContext → жодних кастомних шейдерів (Sodium/Iris-safe). */
public final class HudManager {
    public static final class Widget {
        public final String name; public final FlutiConfig.HudCfg cfg;
        final Function<MinecraftClient, List<String>> lines;
        public int w, h;
        Widget(String n, FlutiConfig.HudCfg c, Function<MinecraftClient, List<String>> l) { name = n; cfg = c; lines = l; }
        public boolean contains(double mx, double my) { return mx >= cfg.x && mx <= cfg.x + w && my >= cfg.y && my <= cfg.y + h; }
    }

    public static final List<Widget> WIDGETS = new ArrayList<>();

    private HudManager() {}

    public static void init() {
        FlutiConfig c = ConfigManager.get();
        WIDGETS.add(new Widget("Armor", c.armor, HudManager::armor));
        WIDGETS.add(new Widget("Potions", c.potions, HudManager::potions));
        WIDGETS.add(new Widget("Ping", c.ping, HudManager::ping));
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.of("fluti", "hud"),
                (ctx, tick) -> {
                    CombatFx.renderHitmarker(ctx);
                    if (!MinecraftClient.getInstance().options.hudHidden) render(ctx, false);
                });
    }

    public static void render(DrawContext ctx, boolean edit) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        for (Widget wd : WIDGETS) {
            if (!edit && !wd.cfg.enabled) continue;
            List<String> ls = wd.lines.apply(mc);
            if (ls.isEmpty()) { if (!edit) continue; ls = List.of("§7" + wd.name); }
            int w = 0;
            for (String s : ls) w = Math.max(w, tr.getWidth(s));
            wd.w = w + 8; wd.h = ls.size() * 10 + 6;
            ctx.fill(wd.cfg.x, wd.cfg.y, wd.cfg.x + wd.w, wd.cfg.y + wd.h, wd.cfg.enabled || !edit ? 0x80000000 : 0x40000000);
            if (edit) {
                int o = 0xFF9B7CFF, x = wd.cfg.x, y = wd.cfg.y;
                ctx.fill(x, y, x + wd.w, y + 1, o); ctx.fill(x, y + wd.h - 1, x + wd.w, y + wd.h, o);
                ctx.fill(x, y, x + 1, y + wd.h, o); ctx.fill(x + wd.w - 1, y, x + wd.w, y + wd.h, o);
            }
            int ty = wd.cfg.y + 4;
            for (String s : ls) { ctx.drawText(tr, s, wd.cfg.x + 4, ty, 0xFFFFFFFF, true); ty += 10; }
        }
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

    private static List<String> potions(MinecraftClient mc) {
        List<String> out = new ArrayList<>();
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String time;
            if (e.isInfinite()) time = "∞";
            else { int s = e.getDuration() / 20; time = (s / 60) + ":" + String.format("%02d", s % 60); }
            out.add(e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1) + " §7" + time);
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
}
