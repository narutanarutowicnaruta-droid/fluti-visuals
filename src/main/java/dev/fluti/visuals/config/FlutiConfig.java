package dev.fluti.visuals.config;

import java.util.ArrayList;
import java.util.List;

public class FlutiConfig {
    // Visuals
    public String glintColor = "DEFAULT";     // DEFAULT, CYAN, BLUE, LAVA, CRIMSON, EMERALD
    public boolean hitmarkers = true;
    public String critParticles = "OFF";      // OFF, HEARTS, STARS, SPARKS
    // Audio
    public String hitSound = "OFF";           // OFF, BELL, CLICK, PLING, CUSTOM (config/fluti/sounds/hit.ogg)
    public String killSound = "OFF";          // ... CUSTOM (config/fluti/sounds/kill.ogg)
    public float soundVolume = 1.0f;
    // Creator
    public boolean cinematic = false;
    public float cinematicSmooth = 0.15f;     // 0.05 (дуже плавно) .. 0.5 (майже без згладжування)
    public boolean maskPasswords = true;
    public boolean greenScreen = false;
    // Utilities
    public List<Macro> macros = new ArrayList<>();
    public HudCfg armor = new HudCfg(true, 6, 6);
    public HudCfg potions = new HudCfg(true, 6, 70);
    public HudCfg ping = new HudCfg(true, 6, 130);

    public static class Macro {
        public String command;
        public int key = -1;
        public transient boolean down;
        public Macro(String command, int key) { this.command = command; this.key = key; }
    }

    public static class HudCfg {
        public boolean enabled; public int x, y;
        public HudCfg(boolean enabled, int x, int y) { this.enabled = enabled; this.x = x; this.y = y; }
    }
}
