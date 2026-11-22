package dev.fluti.visuals.mixin;

import dev.fluti.visuals.feature.GreenScreen;
import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ховає сонце/місяць, щоб на chroma-key не було артефактів. Таргет за ім’ям (без дескриптора) + require=0. */
@Mixin(SkyRendering.class)
public abstract class SkyRenderingMixin {
    @Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true, require = 0)
    private void fluti$noCelestials(CallbackInfo ci) {
        if (GreenScreen.on()) ci.cancel();
    }
}
