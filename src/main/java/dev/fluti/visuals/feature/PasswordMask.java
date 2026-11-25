package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;

import java.util.Locale;

public final class PasswordMask {
    private static final String[] CMDS = {"/l ", "/login ", "/reg ", "/register "};

    private PasswordMask() {}

    /** Індекс, з якого починається пароль, або -1, якщо маскувати не треба. */
    public static int start(String text) {
        if (!ConfigManager.get().maskPasswords || text == null) return -1;
        String low = text.toLowerCase(Locale.ROOT);
        for (String c : CMDS) if (low.startsWith(c)) return c.length();
        return -1;
    }

    /** Замінює символи (крім пробілів) після команди на *, враховуючи зсув видимого фрагмента. */
    public static String mask(String visible, int firstCharIndex, int start) {
        StringBuilder sb = new StringBuilder(visible.length());
        for (int i = 0; i < visible.length(); i++) {
            char ch = visible.charAt(i);
            sb.append(firstCharIndex + i >= start && ch != 0x20 ? 0x2A : ch);
        }
        return sb.toString();
    }
}
