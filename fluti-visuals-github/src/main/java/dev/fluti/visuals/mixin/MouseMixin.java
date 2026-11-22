package dev.fluti.visuals.mixin;

import dev.fluti.visuals.config.ConfigManager;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Safe Cinematic Camera: експоненційне згладжування ТІЛЬКИ дельти миші, що йде в changeLookDirection.
 * Камера лишається прив’язаною до очей гравця (1-ша/3-тя особа) — жодного freecam. Сума руху зберігається.
 */
@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Unique private double fluti$px, fluti$py;

    @ModifyArgs(method = "updateMouse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"),
            require = 0)
    private void fluti$smooth(Args args) {
        var c = ConfigManager.get();
        if (!c.cinematic) { fluti$px = fluti$py = 0; return; }
        fluti$px += (double) args.get(0);
        fluti$py += (double) args.get(1);
        double f = c.cinematicSmooth, ox = fluti$px * f, oy = fluti$py * f;
        fluti$px -= ox; fluti$py -= oy;
        args.set(0, ox); args.set(1, oy);
    }
}
