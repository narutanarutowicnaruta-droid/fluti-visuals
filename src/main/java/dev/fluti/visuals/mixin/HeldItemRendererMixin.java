package dev.fluti.visuals.mixin;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Положення предмета в руці (крім блоків). Чисто візуальний зсув матриці першої особи.
 * ModifyVariable вибирає аргумент за ТИПОМ, тому не залежить від точного списку параметрів методу.
 * Порядок: спершу запам'ятовуємо руку, потім міняємо матрицю.
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Unique private Hand fluti$hand = Hand.MAIN_HAND;

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private Hand fluti$captureHand(Hand hand) {
        fluti$hand = hand;
        return hand;
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private MatrixStack fluti$transform(MatrixStack m) {
        FlutiConfig c = ConfigManager.get();
        var player = MinecraftClient.getInstance().player;
        if (!c.heldEnabled || player == null) return m;
        ItemStack s = player.getStackInHand(fluti$hand);
        if (s.isEmpty() || s.getItem() instanceof BlockItem) return m;

        boolean main = fluti$hand == Hand.MAIN_HAND;
        boolean right = player.getMainArm() == Arm.RIGHT;
        float side = (main == right) ? 1f : -1f;

        m.translate(side * c.hx, c.hy, c.hz);
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(c.hrx));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * c.hry));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * c.hrz));
        m.scale(c.hscale, c.hscale, c.hscale);
        return m;
    }
}
