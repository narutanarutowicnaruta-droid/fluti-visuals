package dev.fluti.visuals.feature;

import dev.fluti.visuals.config.ConfigManager;
import dev.fluti.visuals.config.FlutiConfig;
import dev.fluti.visuals.config.Lang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Мітки зберігаються окремо для кожного сервера та виміру. */
public final class WaypointManager {
    private static final int[] PALETTE = {0xFF5C6C, 0x3DDC84, 0x4D9FFF, 0xFFC940, 0xB450FF, 0x30E6FF, 0xFF7CD0};

    private WaypointManager() {}

    public static String server(MinecraftClient mc) {
        var e = mc.getCurrentServerEntry();
        return e == null ? "local" : e.address;
    }

    public static String dim(MinecraftClient mc) {
        return mc.world == null ? "" : mc.world.getRegistryKey().getValue().toString();
    }

    public static List<FlutiConfig.Waypoint> here() {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<FlutiConfig.Waypoint> out = new ArrayList<>();
        if (mc.world == null) return out;
        String s = server(mc), d = dim(mc);
        for (FlutiConfig.Waypoint w : ConfigManager.get().waypoints)
            if (s.equals(w.server) && d.equals(w.dim)) out.add(w);
        return out;
    }

    public static void add(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        var list = ConfigManager.get().waypoints;
        FlutiConfig.Waypoint w = new FlutiConfig.Waypoint();
        w.name = (name == null || name.isBlank()) ? Lang.get("mark.default") + " " + (here().size() + 1) : name.trim();
        w.x = mc.player.getX(); w.y = mc.player.getY(); w.z = mc.player.getZ();
        w.server = server(mc); w.dim = dim(mc);
        w.color = PALETTE[list.size() % PALETTE.length];
        list.add(w);
        ConfigManager.save();
    }

    /** Клавіша B: якщо поруч (до 4 блоків) є мітка, прибирає її, інакше ставить нову. */
    public static void toggleHere(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        for (FlutiConfig.Waypoint w : here()) {
            double dx = w.x - mc.player.getX(), dy = w.y - mc.player.getY(), dz = w.z - mc.player.getZ();
            if (dx * dx + dy * dy + dz * dz < 16) {
                ConfigManager.get().waypoints.remove(w);
                ConfigManager.save();
                mc.player.sendMessage(Text.literal(Lang.get("m.removed")), true);
                SoundLibrary.ui("block.glass.break", 1.6f, 0.3f);
                return;
            }
        }
        add(null);
        mc.player.sendMessage(Text.literal(Lang.get("m.added")), true);
        SoundLibrary.ui("block.amethyst_block.chime", 1.5f, 0.5f);
    }
}
