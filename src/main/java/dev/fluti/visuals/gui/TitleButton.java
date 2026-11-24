package dev.fluti.visuals.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Кнопка меню мода на головному екрані гри. */
public final class TitleButton {
    private TitleButton() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Fluti Visuals"),
                        b -> client.setScreen(new ClickGuiScreen())).dimensions(6, 6, 96, 20).build());
            }
        });
    }
}
