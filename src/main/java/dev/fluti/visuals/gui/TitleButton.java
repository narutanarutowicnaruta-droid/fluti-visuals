package dev.fluti.visuals.gui;

import dev.fluti.visuals.config.ConfigManager;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Замінює головний екран на лобі Fluti (можна вимкнути в меню), інакше додає кнопку в кутку ванільного. */
public final class TitleButton {
    public static boolean bypass;

    private TitleButton() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof TitleScreen)) return;
            if (ConfigManager.get().customLobby && !bypass) {
                client.execute(() -> client.setScreen(new FlutiTitleScreen()));
                return;
            }
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Fluti Visuals"),
                    b -> client.setScreen(new ClickGuiScreen())).dimensions(6, 6, 96, 20).build());
        });
    }
}
