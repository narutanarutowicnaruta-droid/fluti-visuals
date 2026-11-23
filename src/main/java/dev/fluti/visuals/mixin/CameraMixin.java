package dev.fluti.visuals.mixin;

import dev.fluti.visuals.feature.Freelook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Підміняє кут камери на кут «огляду». Позицію не чіпає: камера не виходить за межі, які дозволяє ваніла. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArgs(method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0),
            require = 0)
    private void fluti$free(Args args) {
        if (Freelook.active) {
            args.set(0, Freelook.yaw);
            args.set(1, Freelook.pitch);
        }
    }
}
