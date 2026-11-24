package dev.fluti.visuals.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Мови меню: assets/fluti/ui/{uk,ru,en,pl}.json. Якщо ключа немає, береться англійський. */
public final class Lang {
    public static final List<String> CODES = List.of("uk", "ru", "en", "pl");
    public static final Map<String, String> NAMES = new LinkedHashMap<>();

    static {
        NAMES.put("uk", "Українська");
        NAMES.put("ru", "Русский");
        NAMES.put("en", "English");
        NAMES.put("pl", "Polski");
    }

    private static Map<String, String> en = new HashMap<>(), cur = new HashMap<>();

    private Lang() {}

    public static void load(String code) {
        en = read("en");
        cur = code.equals("en") ? en : read(code);
    }

    private static Map<String, String> read(String code) {
        try (InputStream in = Lang.class.getResourceAsStream("/assets/fluti/ui/" + code + ".json")) {
            if (in == null) return new HashMap<>();
            Map<String, String> m = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() {}.getType());
            return m == null ? new HashMap<>() : m;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static String get(String key) {
        String v = cur.get(key);
        if (v == null) v = en.get(key);
        return v == null ? key : v;
    }

    public static String t(String id) { return get("t." + id); }
    public static String d(String id) { return get("d." + id); }
    public static String h(String id) { return get("h." + id); }
    public static String i(String id) { return get("i." + id); }
}
