package com.fluti.aihelper.client;

import com.fluti.aihelper.FlutiAiMod;
import com.fluti.aihelper.command.ChatCommandHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class FlutiAiClient implements ClientModInitializer {

    public static final EntityModelLayer HELPER_BOT_LAYER = new EntityModelLayer(
            Identifier.of(FlutiAiMod.MOD_ID, "helper_bot"),
            "main"
    );

    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(HELPER_BOT_LAYER, HelperBotModel::getTexturedModelData);
        EntityRendererRegistry.register(FlutiAiMod.HELPER_BOT, HelperBotRenderer::new);

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fluti_ai.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_0,
                "category.fluti_ai.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new BotControlScreen());
                }
            }
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(ChatCommandHandler::handleOutgoingMessage);
    }
}
