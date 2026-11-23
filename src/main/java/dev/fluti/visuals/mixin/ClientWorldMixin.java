package dev.fluti.visuals.mixin;

import dev.fluti.visuals.feature.GreenScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Green Screen: колір неба + прибрати зірки. Ці методи — найбільш версійно-залежна частина:
 * у 1.21.6+ getSkyColor повертає int (RGB). Якщо в твоїх маппінгах інакше — підправ тип CIR.
 * require=0 → при невідповідності мод не впаде, просто ефект не спрацює.
 */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void fluti$sky(Vec3d cameraPos, float tickProgress, CallbackInfoReturnable<Integer> cir) {
        if (GreenScreen.on()) { cir.setReturnValue(GreenScreen.COLOR); return; }
        int s = dev.fluti.visuals.feature.Opts.skyRgb(dev.fluti.visuals.config.ConfigManager.get().sky);
        if (s >= 0) cir.setReturnValue(s);
    }

    @Inject(method = "getStarBrightness", at = @At("HEAD"), cancellable = true, require = 0)
    private void fluti$stars(float tickProgress, CallbackInfoReturnable<Float> cir) {
        if (GreenScreen.on()) { cir.setReturnValue(0f); return; }
        String sk = dev.fluti.visuals.config.ConfigManager.get().sky;
        if (!sk.equals("OFF") && !sk.equals("SUNSET")) cir.setReturnValue(1f);
    }
}
