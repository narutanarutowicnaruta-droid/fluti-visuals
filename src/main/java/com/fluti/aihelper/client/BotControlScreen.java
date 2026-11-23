package com.fluti.aihelper.client;

import com.fluti.aihelper.FlutiAiMod;
import com.fluti.aihelper.entity.HelperBotEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class BotControlScreen extends Screen {

    public BotControlScreen() {
        super(Text.literal("Fluti AI Bot Control"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Добавить / Призвать бота"), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.world != null) {
                HelperBotEntity bot = FlutiAiMod.HELPER_BOT.create(client.world, net.minecraft.entity.SpawnReason.COMMAND);
                if (bot != null) {
                    bot.refreshPositionAndAngles(
                            client.player.getX() + 1.5,
                            client.player.getY(),
                            client.player.getZ() + 1.5,
                            client.player.getYaw(),
                            0.0f
                    );
                    client.world.spawnEntity(bot);
                    client.player.sendMessage(Text.literal("[Fluti AI] Bot spawned nearby."), false);
                }
            }
        }).dimensions(centerX - 100, centerY - 35, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Стоп / Очистить задачу"), button -> {
            HelperBotEntity.clearActiveTarget();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[Fluti AI] Task cleared. Bot in idle mode."), false);
            }
        }).dimensions(centerX - 100, centerY - 10, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Команды: .добыть / .добути / .mine"), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[Fluti AI] Examples: .добыть дерево | .добути камінь | .mine wood | .mine stone"), false);
            }
        }).dimensions(centerX - 100, centerY + 15, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "FLUTI AI HELPER (Key 0)", this.width / 2, this.height / 2 - 60, 0x38BDF8);
        context.drawCenteredTextWithShadow(this.textRenderer, "Status: " + HelperBotEntity.getStatusText(), this.width / 2, this.height / 2 - 47, 0xE2E8F0);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
