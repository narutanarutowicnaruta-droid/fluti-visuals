package dev.fluti.visuals.feature;

import dev.fluti.visuals.FlutiClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Власний відтворювач .ogg (Ogg Vorbis): декод через STB Vorbis (є в грі), відтворення через Java Sound.
 * Не залежить від OpenAL-контексту гри та ресурспаків. Підтримує гучність, тон, обрізання за секундами.
 */
public final class AudioPlayer {
    public static final class Pcm {
        final byte[] data; final int channels, rate;
        Pcm(byte[] d, int c, int r) { data = d; channels = c; rate = r; }
    }

    private static final Map<String, Pcm> CACHE = new ConcurrentHashMap<>();
    private static final List<Object[]> STOPS = new ArrayList<>();   // {Clip, Integer ticksLeft}

    private AudioPlayer() {}

    // ---------- декод ----------
    private static Pcm decode(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length < 4 || bytes[0] != 'O' || bytes[1] != 'g' || bytes[2] != 'g' || bytes[3] != 'S')
            throw new IOException("not an .ogg file: " + file.getFileName());
        String head = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.ISO_8859_1);
        if (head.contains("OpusHead"))
            throw new IOException("Ogg OPUS is not supported, convert to Ogg VORBIS: " + file.getFileName());
        ByteBuffer data = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
        ShortBuffer pcm = null;
        try (MemoryStack st = MemoryStack.stackPush()) {
            IntBuffer ch = st.mallocInt(1), rate = st.mallocInt(1);
            pcm = STBVorbis.stb_vorbis_decode_memory(data, ch, rate);
            if (pcm == null) throw new IOException("cannot decode (need Ogg Vorbis): " + file.getFileName());
            byte[] out = new byte[pcm.remaining() * 2];
            ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm);
            return new Pcm(out, ch.get(0), rate.get(0));
        } finally {
            if (pcm != null) LibCStdlib.free(pcm);
            MemoryUtil.memFree(data);
        }
    }

    /** Завантажує (з кешем). Викликати можна з будь-якого потоку; помилку покаже в грі. */
    public static Pcm load(Path file) {
        String key = file.toString();
        Pcm p = CACHE.get(key);
        if (p != null) return p;
        try {
            p = decode(file);
            CACHE.put(key, p);
            return p;
        } catch (Throwable t) {
            report(t.getMessage() == null ? t.toString() : t.getMessage());
            return null;
        }
    }

    public static void preload(Path file) {
        if (CACHE.containsKey(file.toString()) || !Files.isRegularFile(file)) return;
        CompletableFuture.runAsync(() -> load(file));
    }

    public static void invalidate(Path file) { CACHE.remove(file.toString()); }

    // ---------- відтворення ----------
    public static Clip play(Path file, float vol, float pitch, float maxSec) {
        Pcm p = load(file);                 // короткі звуки декодуються миттєво й кешуються
        return p == null ? null : playPcm(p, vol, pitch, maxSec);
    }

    public static Clip playPcm(Pcm p, float vol, float pitch, float maxSec) {
        try {
            Clip clip;
            try {
                clip = AudioSystem.getClip();
                clip.open(new AudioFormat(p.rate * Math.max(0.5f, Math.min(2f, pitch)), 16, p.channels, true, false), p.data, 0, p.data.length);
            } catch (Exception e) {                               // пристрій не прийняв нестандартну частоту
                clip = AudioSystem.getClip();
                clip.open(new AudioFormat(p.rate, 16, p.channels, true, false), p.data, 0, p.data.length);
            }
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl g = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float db = (float) (20.0 * Math.log10(Math.max(0.0001, Math.min(1f, vol))));
                g.setValue(Math.max(g.getMinimum(), Math.min(g.getMaximum(), db)));
            }
            final Clip c = clip;
            c.addLineListener(e -> { if (e.getType() == LineEvent.Type.STOP) c.close(); });
            c.start();
            if (maxSec > 0f) synchronized (STOPS) { STOPS.add(new Object[]{c, (int) (maxSec * 20)}); }
            return c;
        } catch (Throwable t) {
            report("audio output failed: " + t);
            return null;
        }
    }

    public static void stop(Clip c) {
        if (c == null) return;
        try { c.stop(); c.close(); } catch (Throwable ignored) { }
    }

    /** Щотіку: обрізає довгі звуки до заданих секунд. */
    public static void tick() {
        synchronized (STOPS) {
            if (STOPS.isEmpty()) return;
            Iterator<Object[]> it = STOPS.iterator();
            while (it.hasNext()) {
                Object[] s = it.next();
                int left = (Integer) s[1] - 1;
                if (left <= 0) { stop((Clip) s[0]); it.remove(); } else s[1] = left;
            }
        }
    }

    public static void report(String msg) {
        FlutiClient.LOG.warn("Fluti audio: {}", msg);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> { if (mc.player != null) mc.player.sendMessage(Text.literal("Fluti audio: " + msg), true); });
    }
}
