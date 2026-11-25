package dev.fluti.visuals.input;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import dev.fluti.visuals.feature.WaypointManager;
import dev.fluti.visuals.gui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindHandler {
    public static KeyBinding openGui, markKey;
    private static boolean clipDown, profDown;
    private static int joinTimer = -1, joinIdx;

    private KeybindHandler() {}

    public static void init() {
        // У 1.21.9+ категорія — окремий тип. Клавішу можна перепризначити в Controls (напр. на Left Shift).
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("fluti", "main"));
        openGui = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.fluti.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        markKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.fluti.mark", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, cat));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> { joinTimer = ConfigManager.get().joinDelay; joinIdx = 0; });
        ClientTickEvents.END_CLIENT_TICK.register(KeybindHandler::tick);
    }

    private static void tick(MinecraftClient mc) {
        while (openGui.wasPressed()) mc.setScreen(new ClickGuiScreen());
        while (markKey.wasPressed()) WaypointManager.toggleHere(mc);
        if (mc.player == null) return;

        if (joinTimer >= 0) {                        // авто-команди після входу на сервер
            if (joinTimer == 0) {
                var js = new java.util.ArrayList<FlutiConfig.Macro>();
                for (FlutiConfig.Macro m : ConfigManager.get().macros) if (m.onJoin) js.add(m);
                if (joinIdx < js.size()) { run(mc, js.get(joinIdx++).command); joinTimer = 20; } else joinTimer = -1;
            } else joinTimer--;
        }
        boolean canFire = mc.currentScreen == null;
        int ck = ConfigManager.get().clipKey;
        boolean cd = canFire && ck > 0 && InputUtil.isKeyPressed(mc.getWindow(), ck);
        if (cd && !clipDown) mc.options.hudHidden = !mc.options.hudHidden;   // режим чистого кадру (як F1)
        clipDown = cd;
        int pk = ConfigManager.get().profileKey;
        boolean pd = canFire && pk > 0 && InputUtil.isKeyPressed(mc.getWindow(), pk);
        if (pd && !profDown) {                       // клавіша: наступний профіль
            var names = dev.fluti.visuals.feature.Profiles.names();
            int i = names.indexOf(ConfigManager.get().profile);
            String next = names.get((i + 1) % names.size());
            dev.fluti.visuals.feature.Profiles.apply(next);
            mc.player.sendMessage(net.minecraft.text.Text.literal("Profile: " + next), true);
        }
        profDown = pd;
        for (FlutiConfig.Macro m : ConfigManager.get().macros) {
            boolean down = canFire && m.key > 0 && InputUtil.isKeyPressed(mc.getWindow(), m.key);
            if (down && !m.down) run(mc, m.command);   // один раз на натискання
            m.down = down;
        }
    }

    private static void run(MinecraftClient mc, String cmd) {
        if (mc.player == null || mc.player.networkHandler == null || cmd == null || cmd.isBlank()) return;
        if (cmd.startsWith("/")) mc.player.networkHandler.sendChatCommand(cmd.substring(1));
        else mc.player.networkHandler.sendChatMessage(cmd);
    }
}
