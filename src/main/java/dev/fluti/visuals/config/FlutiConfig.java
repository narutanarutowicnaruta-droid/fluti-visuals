package dev.fluti.visuals.config;

import java.util.ArrayList;
import java.util.List;

public class FlutiConfig {
    // ---- Visuals ----
    public String glintColor = "DEFAULT";
    public boolean hitmarkers = true;
    public String hitmarkerColor = "WHITE";
    public String hitmarkerStyle = "X";        // X, PLUS, DOT
    public String hitParticles = "OFF";        // на кожен удар
    public String critParticles = "OFF";       // на крит
    public String killBeam = "OFF";            // OFF, LIGHTNING, BLUE, CYAN, GOLD, RED, GREEN, PURPLE, WHITE
    public String killBurst = "OFF";           // OFF, TOTEM, FIREWORK, HEARTS, SOUL, EXPLOSION, SMOKE, STARS
    // Предмети в руках (крім блоків)
    public boolean heldEnabled = false;
    public String heldPreset = "DEFAULT";
    public float hx, hy, hz, hrx, hry, hrz;
    public float hscale = 1f;
    // ---- Audio ----
    public String hitSound = "OFF";            // пресет або FILE:назва.ogg
    public String killSound = "OFF";
    public float soundVolume = 1.0f, hitPitch = 1.0f, killPitch = 1.0f;
    public boolean guiSounds = true;
    // ---- Creator ----
    public boolean cinematic = false;
    public float cinematicSmooth = 0.15f;
    public boolean maskPasswords = true;
    public boolean greenScreen = false;
    public String song = "";
    public boolean lyricsOn = true;
    public float songVolume = 0.8f;
    public float lyricsY = 48f;
    // ---- Settings ----
    public String lang = "uk";                 // uk, ru, en, pl
    public String theme = "PURPLE";
    public boolean freelook = true;
    public int freelookKey = 342;              // Left Alt
    public String markColor = "BLUE";          // BLUE, CYAN, PURPLE, PINK, GOLD, GREEN, RED, WHITE, AUTO
    public String markFx = "ALL";              // OFF, GLOW, PULSE, ORBIT, ALL
    public String profile = "Default";
    public int clipKey = 297;                  // F8
    public boolean zoomEnabled = true;
    public int zoomKey = 67;                   // C
    public float zoomFactor = 2.5f;
    public String armorWarn = "OFF";
    public float hudOpacity = 0.5f;
    public boolean hudSnap = true;
    public String trail = "OFF", halo = "OFF", stepFx = "OFF", jumpRing = "OFF", totemFx = "OFF";
    public String vignette = "OFF", tint = "OFF", weather = "OFF", sky = "OFF";
    public int joinDelay = 40;                 // тіків (40 = 2 секунди) до авто-команд після входу
    public boolean showMarks = true;
    public boolean markDist = true;
    public List<Waypoint> waypoints = new ArrayList<>();
    // ---- Utilities ----
    public List<Macro> macros = new ArrayList<>();
    public HudCfg armor = new HudCfg(true, 6, 6);
    public HudCfg potions = new HudCfg(true, 6, 70);
    public HudCfg ping = new HudCfg(true, 6, 130);
    public HudCfg stats = new HudCfg(false, 6, 160);
    public HudCfg fps = new HudCfg(false, 6, 200);
    public HudCfg coords = new HudCfg(false, 6, 230);

    public static class Macro {
        public String command;
        public int key = -1;
        public transient boolean down;
        public boolean onJoin;
        public Macro(String command, int key) { this.command = command; this.key = key; }
    }

    public static class Waypoint {
        public String name, dim, server;
        public double x, y, z;
        public int color;
    }

    public static class HudCfg {
        public boolean enabled; public int x, y;
        public HudCfg(boolean enabled, int x, int y) { this.enabled = enabled; this.x = x; this.y = y; }
    }
}
