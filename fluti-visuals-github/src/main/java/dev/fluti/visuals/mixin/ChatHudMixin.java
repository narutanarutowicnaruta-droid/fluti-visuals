package dev.fluti.visuals.mixin;

import dev.fluti.visuals.feature.PasswordMask;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Не зберігає /login і /reg в історії відправлених (стрілка ↑), щоб пароль не світився на записі. */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Inject(method = "addToMessageHistory", at = @At("HEAD"), cancellable = true, require = 0)
    private void fluti$noHistory(String message, CallbackInfo ci) {
        if (PasswordMask.start(message) >= 0) ci.cancel();
    }
}
