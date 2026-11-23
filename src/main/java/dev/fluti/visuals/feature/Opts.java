package dev.fluti.visuals.feature;

import java.util.List;

/** Списки варіантів для меню. */
public final class Opts {
    public static final List<String> PARTICLES = List.of("OFF", "SPARKS", "HEARTS", "STARS", "SMOKE", "FLAMES", "SNOW", "NOTES", "MAGIC", "SOUL", "CRIT");
    public static final List<String> KILL_BEAM = List.of("OFF", "LIGHTNING", "BLUE", "CYAN", "GOLD", "RED", "GREEN", "PURPLE", "WHITE");
    public static final List<String> KILL_BURST = List.of("OFF", "TOTEM", "FIREWORK", "HEARTS", "SOUL", "EXPLOSION", "SMOKE", "STARS");
    public static final List<String> HM_COLOR = List.of("WHITE", "RED", "CYAN", "GREEN", "GOLD", "PURPLE");
    public static final List<String> HM_STYLE = List.of("X", "PLUS", "DOT");

    public static final List<String> THEMES = List.of("PURPLE", "BLUE", "GREEN", "RED", "PINK", "GOLD");

    public static final List<String> MARK_COLORS = List.of("BLUE", "CYAN", "PURPLE", "PINK", "GOLD", "GREEN", "RED", "WHITE", "AUTO");
    public static final List<String> MARK_FX = List.of("OFF", "GLOW", "PULSE", "ORBIT", "ALL");

    private Opts() {}

    public static int markRgb(String n, int auto) {
        return switch (n) {
            case "BLUE" -> 0x4D9FFF;
            case "CYAN" -> 0x30E6FF;
            case "PURPLE" -> 0xB450FF;
            case "PINK" -> 0xFF7CD0;
            case "GOLD" -> 0xFFC940;
            case "GREEN" -> 0x3DDC84;
            case "RED" -> 0xFF5C6C;
            case "WHITE" -> 0xFFFFFF;
            default -> auto & 0xFFFFFF;
        };
    }

    public static int accent(String t) {
        return switch (t) {
            case "BLUE" -> 0xFF4D9FFF;
            case "GREEN" -> 0xFF3DDC84;
            case "RED" -> 0xFFFF5C6C;
            case "PINK" -> 0xFFFF7CD0;
            case "GOLD" -> 0xFFFFC940;
            default -> 0xFF9B7CFF;
        };
    }

    public static int dim(int argb, float f) {
        int r = (int) (((argb >> 16) & 255) * f), g = (int) (((argb >> 8) & 255) * f), b = (int) ((argb & 255) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int color(String n) {
        return switch (n) {
            case "RED" -> 0xFF3030;
            case "CYAN" -> 0x30E6FF;
            case "GREEN" -> 0x30FF70;
            case "GOLD" -> 0xFFC828;
            case "PURPLE" -> 0xB450FF;
            default -> 0xFFFFFF;
        };
    }
}
