package dev.fluti.visuals.mixin;

import dev.fluti.visuals.feature.PasswordMask;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Малює ***** замість пароля в полі вводу. Сам текст, який піде на сервер, НЕ змінюється. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow protected TextFieldWidget chatField;

    @Inject(method = "provideRenderText", at = @At("HEAD"), cancellable = true, require = 0)
    private void fluti$mask(String original, int firstCharIndex, CallbackInfoReturnable<OrderedText> cir) {
        int start = PasswordMask.start(chatField.getText());
        if (start < 0) return;
        cir.setReturnValue(OrderedText.styledForwardsVisitedString(
                PasswordMask.mask(original, firstCharIndex, start), net.minecraft.text.Style.EMPTY));
    }
}
