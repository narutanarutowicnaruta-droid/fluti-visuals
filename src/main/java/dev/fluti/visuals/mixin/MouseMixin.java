package dev.fluti.visuals.mixin;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.feature.Freelook;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * 1) Safe Cinematic Camera: експоненційне згладжування дельти миші (сума руху зберігається).
 * 2) Free look: поки тримаєш клавішу, дельта йде в кут камери, а гравець не повертається.
 */
@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Unique private double fluti$px, fluti$py;

    @ModifyArgs(method = "updateMouse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"),
            require = 0)
    private void fluti$smooth(Args args) {
        var c = ConfigManager.get();
        Freelook.update();
        dev.fluti.visuals.feature.Zoom.update();
        dev.fluti.visuals.feature.Cps.update();
        double dx = (double) args.get(0), dy = (double) args.get(1);
        if (c.cinematic) {
            fluti$px += dx; fluti$py += dy;
            double f = c.cinematicSmooth;
            dx = fluti$px * f; dy = fluti$py * f;
            fluti$px -= dx; fluti$py -= dy;
        } else {
            fluti$px = 0; fluti$py = 0;
        }
        if (Freelook.active && Freelook.hookWorks()) {
            Freelook.rotate(dx, dy);
            args.set(0, 0.0);
            args.set(1, 0.0);
            return;
        }
        args.set(0, dx);
        args.set(1, dy);
    }
}
