package dev.fluti.visuals.feature;

import java.util.List;

/** Списки варіантів для меню. */
public final class Opts {
    public static final List<String> PARTICLES = List.of("OFF", "SPARKS", "HEARTS", "STARS", "SMOKE", "FLAMES", "SNOW", "NOTES", "MAGIC", "SOUL", "CRIT");
    public static final List<String> KILL_BEAM = List.of("OFF", "LIGHTNING", "BLUE", "CYAN", "GOLD", "RED", "GREEN", "PURPLE", "WHITE");
    public static final List<String> KILL_BURST = List.of("OFF", "TOTEM", "FIREWORK", "HEARTS", "SOUL", "EXPLOSION", "SMOKE", "STARS");
    public static final List<String> HM_COLOR = List.of("WHITE", "RED", "CYAN", "GREEN", "GOLD", "PURPLE");
    public static final List<String> HM_STYLE = List.of("X", "PLUS", "DOT");

    private Opts() {}

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
