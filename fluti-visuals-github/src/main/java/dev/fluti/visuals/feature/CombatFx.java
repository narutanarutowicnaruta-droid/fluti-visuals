package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

/** Hitmarker + кастомні частинки на крит + звуки удару/вбивства. Тільки клієнтська візуалізація. */
public final class CombatFx {
    private static int hitTicks, killTicks, trackTicks;
    private static LivingEntity tracked;

    private CombatFx() {}

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity target) onAttack(player, target);
            return ActionResult.PASS; // нічого не скасовуємо і не змінюємо
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
    }

    private static void onAttack(net.minecraft.entity.player.PlayerEntity p, LivingEntity target) {
        FlutiConfig c = ConfigManager.get();
        hitTicks = 10;
        tracked = target; trackTicks = 60;
        play(c.hitSound, "hit.ogg");

        boolean crit = p.fallDistance > 0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater()
                && !p.hasStatusEffect(StatusEffects.BLINDNESS) && !p.hasVehicle()
                && p.getAttackCooldownProgress(0.5f) > 0.9f;
        if (crit) spawnParticles(target, c.critParticles);
    }

    private static void spawnParticles(LivingEntity t, String mode) {
        ParticleEffect type = switch (mode) {
            case "HEARTS" -> ParticleTypes.HEART;
            case "STARS" -> ParticleTypes.END_ROD;
            case "SPARKS" -> ParticleTypes.FIREWORK;
            default -> null;
        };
        var world = MinecraftClient.getInstance().world;
        if (type == null || world == null) return;
        var r = world.getRandom();
        for (int i = 0; i < 8; i++) {
            // NB: у 1.21.5+ метод називається addParticleClient (раніше addParticle)
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
        OggPlayer.tick();
        if (tracked != null) {
            if (!tracked.isAlive()) {
                killTicks = 14; hitTicks = 0;
                play(ConfigManager.get().killSound, "kill.ogg");
                tracked = null;
            } else if (--trackTicks <= 0) tracked = null;
        }
    }

    private static void play(String preset, String customFile) {
        if (preset == null || preset.equals("OFF")) return;
        float vol = ConfigManager.get().soundVolume;
        SoundEvent ev = switch (preset) {
            case "BELL" -> SoundEvents.BLOCK_BELL_USE;
            case "CLICK" -> SoundEvents.UI_BUTTON_CLICK.value();
            case "PLING" -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            default -> null;
        };
        if (ev != null) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(ev, 1.0f, vol));
        } else if (preset.equals("CUSTOM")) {
            OggPlayer.play(FabricLoader.getInstance().getConfigDir().resolve("fluti/sounds/" + customFile), vol);
        }
    }

    /** Викликається з HUD-елемента. Хрестик малюється 1px-квадратами, без шейдерів/пайплайнів → сумісно з Sodium/Iris. */
    public static void renderHitmarker(DrawContext ctx) {
        if (!ConfigManager.get().hitmarkers || (hitTicks <= 0 && killTicks <= 0)) return;
        boolean kill = killTicks > 0;
        int alpha = (int) (255 * Math.min(1f, (kill ? killTicks / 14f : hitTicks / 10f)));
        int color = (alpha << 24) | (kill ? 0xFF3030 : 0xFFFFFF);
        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        for (int i = 3; i <= 7; i++) {
            ctx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
            ctx.fill(cx - i - 1, cy + i, cx - i, cy + i + 1, color);
            ctx.fill(cx + i, cy - i - 1, cx + i + 1, cy - i, color);
            ctx.fill(cx - i - 1, cy - i - 1, cx - i, cy - i, color);
        }
    }
}
