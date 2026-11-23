package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;

/** Hitmarker, частинки, звуки удару/вбивства, промінь з неба, статистика. Усе тільки на клієнті. */
public final class CombatFx {
    private static int hitTicks, killTicks, trackTicks, comboTicks;
    private static int combo, kills;
    private static LivingEntity tracked;
    private static final boolean[] ARMOR_WARNED = new boolean[4];

    private CombatFx() {}

    public static int combo() { return combo; }
    public static int kills() { return kills; }

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity target) onAttack(player, target);
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
    }

    private static void onAttack(PlayerEntity p, LivingEntity target) {
        FlutiConfig c = ConfigManager.get();
        hitTicks = 10;
        tracked = target; trackTicks = 80;
        combo++; comboTicks = 60;
        SoundLibrary.play(c.hitSound, c.soundVolume, c.hitPitch, "hit.ogg");
        burst(target, c.hitParticles, 6);

        boolean crit = p.fallDistance > 0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater()
                && !p.hasStatusEffect(StatusEffects.BLINDNESS) && !p.hasVehicle()
                && p.getAttackCooldownProgress(0.5f) > 0.9f;
        if (crit) burst(target, c.critParticles, 12);
    }

    private static ParticleEffect particle(String n) {
        return switch (n) {
            case "HEARTS" -> ParticleTypes.HEART;
            case "STARS" -> ParticleTypes.END_ROD;
            case "SPARKS" -> ParticleTypes.FIREWORK;
            case "SMOKE" -> ParticleTypes.SMOKE;
            case "FLAMES" -> ParticleTypes.FLAME;
            case "SNOW" -> ParticleTypes.SNOWFLAKE;
            case "NOTES" -> ParticleTypes.NOTE;
            case "MAGIC" -> ParticleTypes.ENCHANTED_HIT;
            case "SOUL" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "CRIT" -> ParticleTypes.CRIT;
            default -> null;
        };
    }

    private static void burst(LivingEntity t, String mode, int count) {
        ParticleEffect type = particle(mode);
        var world = MinecraftClient.getInstance().world;
        if (type == null || world == null) return;
        var r = world.getRandom();
        for (int i = 0; i < count; i++) {
            world.addParticleClient(type,
                    t.getX() + (r.nextDouble() - 0.5) * t.getWidth(),
                    t.getBodyY(0.3 + r.nextDouble() * 0.6),
                    t.getZ() + (r.nextDouble() - 0.5) * t.getWidth(),
                    (r.nextDouble() - 0.5) * 0.15, 0.1 + r.nextDouble() * 0.1, (r.nextDouble() - 0.5) * 0.15);
        }
    }

    private static void tick() {
        if (hitTicks > 0) hitTicks--;
        if (killTicks > 0) killTicks--;
        if (comboTicks > 0 && --comboTicks == 0) combo = 0;
        OggPlayer.tick();
        KillFx.tick();
        armorCheck();
        if (tracked != null) {
            if (tracked.isDead()) {
                FlutiConfig c = ConfigManager.get();
                kills++;
                killTicks = 14; hitTicks = 0;
                SoundLibrary.play(c.killSound, c.soundVolume, c.killPitch, "kill.ogg");
                KillFx.trigger(tracked.getX(), tracked.getY(), tracked.getZ(), c);
                tracked = null;
            } else if (--trackTicks <= 0) tracked = null;
        }
    }

    private static void armorCheck() {
        var p = MinecraftClient.getInstance().player;
        if (p == null) return;
        FlutiConfig c = ConfigManager.get();
        net.minecraft.entity.EquipmentSlot[] slots = {net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST,
                net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            var s = p.getEquippedStack(slots[i]);
            if (s.isEmpty() || s.getMaxDamage() <= 0) { ARMOR_WARNED[i] = false; continue; }
            int pct = (s.getMaxDamage() - s.getDamage()) * 100 / s.getMaxDamage();
            if (pct <= 15) {
                if (!ARMOR_WARNED[i]) { ARMOR_WARNED[i] = true; SoundLibrary.play(c.armorWarn, c.soundVolume, 1f, "hit.ogg"); }
            } else if (pct > 25) ARMOR_WARNED[i] = false;
        }
    }

    /** Хітмаркер малюється 1px-квадратами: без шейдерів/пайплайнів, тож сумісно з Sodium/Iris. */
    public static void renderHitmarker(DrawContext ctx) {
        FlutiConfig c = ConfigManager.get();
        if (!c.hitmarkers || (hitTicks <= 0 && killTicks <= 0)) return;
        boolean kill = killTicks > 0;
        int alpha = (int) (255 * Math.min(1f, kill ? killTicks / 14f : hitTicks / 10f));
        int rgb = kill ? 0xFF3030 : Opts.color(c.hitmarkerColor);
        int col = (alpha << 24) | rgb;
        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        switch (c.hitmarkerStyle) {
            case "PLUS" -> {
                for (int i = 3; i <= 8; i++) {
                    ctx.fill(cx + i, cy, cx + i + 1, cy + 1, col);
                    ctx.fill(cx - i - 1, cy, cx - i, cy + 1, col);
                    ctx.fill(cx, cy + i, cx + 1, cy + i + 1, col);
                    ctx.fill(cx, cy - i - 1, cx + 1, cy - i, col);
                }
            }
            case "DOT" -> ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
            default -> {
                for (int i = 3; i <= 7; i++) {
                    ctx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, col);
                    ctx.fill(cx - i - 1, cy + i, cx - i, cy + i + 1, col);
                    ctx.fill(cx + i, cy - i - 1, cx + i + 1, cy - i, col);
                    ctx.fill(cx - i - 1, cy - i - 1, cx - i, cy - i, col);
                }
            }
        }
    }
}
