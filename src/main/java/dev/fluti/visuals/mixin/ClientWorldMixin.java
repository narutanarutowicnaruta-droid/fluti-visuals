package dev.fluti.visuals.mixin;

import dev.fluti.visuals.feature.GreenScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Green Screen: колір неба + прибрати зірки. require=0: якщо метод в цій версії інший, ефект просто не спрацює. */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void fluti$sky(Vec3d cameraPos, float tickProgress, CallbackInfoReturnable<Integer> cir) {
        if (GreenScreen.on()) cir.setReturnValue(GreenScreen.COLOR);
    }

    @Inject(method = "getStarBrightness", at = @At("HEAD"), cancellable = true, require = 0)
    private void fluti$stars(float tickProgress, CallbackInfoReturnable<Float> cir) {
        if (GreenScreen.on()) cir.setReturnValue(0f);
    }
}
