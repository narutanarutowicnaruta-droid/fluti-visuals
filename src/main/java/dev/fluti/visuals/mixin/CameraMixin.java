package dev.fluti.visuals.mixin;

import dev.fluti.visuals.feature.Freelook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Підміняє кут камери на кут «огляду» (Camera.update -> setRotation(FF)V, підтверджено для yarn 1.21.11).
 * Позицію не чіпає. Другий виклик (вигляд спереду, yaw+180) пропускаємо, щоб він лишався перевернутим.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArgs(method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"),
            require = 0)
    private void fluti$free(Args args) {
        if (!Freelook.active) return;
        float inYaw = (float) args.get(0), inPitch = (float) args.get(1);
        if (Math.abs(inYaw - (Freelook.yaw + 180f)) < 0.01f && Math.abs(inPitch + Freelook.pitch) < 0.01f) return;
        args.set(0, Freelook.yaw);
        args.set(1, Freelook.pitch);
        Freelook.lastCamNs = System.nanoTime();
    }
}
