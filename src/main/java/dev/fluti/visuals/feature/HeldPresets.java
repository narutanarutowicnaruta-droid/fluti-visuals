package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.FlutiConfig;

import java.util.LinkedHashMap;
import java.util.List;

/** Готові положення предметів у руці: {x, y, z, rotX, rotY, rotZ, scale}. */
public final class HeldPresets {
    public static final LinkedHashMap<String, float[]> MAP = new LinkedHashMap<>();

    static {
        MAP.put("DEFAULT", new float[]{0, 0, 0, 0, 0, 0, 1});
        MAP.put("LOW", new float[]{0.02f, -0.12f, 0, 0, 0, 0, 0.9f});
        MAP.put("FAR", new float[]{0.08f, -0.03f, -0.18f, 0, 0, 0, 0.9f});
        MAP.put("TILTED", new float[]{0.05f, -0.05f, 0, -8, 0, 14, 0.95f});
        MAP.put("WIDE", new float[]{0.22f, -0.04f, 0, 0, -10, 6, 0.9f});
        MAP.put("CINEMATIC", new float[]{0.02f, -0.06f, -0.1f, 10, 15, 0, 0.9f});
        MAP.put("POKE", new float[]{0, -0.02f, -0.1f, -25, 0, 0, 0.95f});
        MAP.put("BIG", new float[]{0, 0, 0, 0, 0, 0, 1.2f});
        MAP.put("MINI", new float[]{0, -0.1f, 0, 0, 0, 0, 0.7f});
    }

    private HeldPresets() {}

    public static List<String> names() { return List.copyOf(MAP.keySet()); }

    public static void apply(FlutiConfig c, String name) {
        float[] v = MAP.get(name);
        if (v == null) return;
        c.heldPreset = name;
        c.hx = v[0]; c.hy = v[1]; c.hz = v[2];
        c.hrx = v[3]; c.hry = v[4]; c.hrz = v[5];
        c.hscale = v[6];
    }
}
