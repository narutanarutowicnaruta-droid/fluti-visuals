package dev.fluti.visuals;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.feature.CombatFx;
import dev.fluti.visuals.hud.HudManager;
import dev.fluti.visuals.input.KeybindHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

public class FlutiClient implements ClientModInitializer {
    public static final String ID = "fluti";
    public static final Logger LOG = LoggerFactory.getLogger("Fluti");

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        try { Files.createDirectories(FabricLoader.getInstance().getConfigDir().resolve("fluti/sounds"));
            Files.createDirectories(FabricLoader.getInstance().getConfigDir().resolve("fluti/songs")); }
        catch (IOException e) { LOG.warn("Cannot create sounds dir", e); }

        KeybindHandler.init();   // Right Shift + макроси
        CombatFx.init();         // hitmarker, частинки, звуки
        HudManager.init();       // Safe HUD
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> ConfigManager.save());
        LOG.info("Fluti Visuals loaded");
    }
}
