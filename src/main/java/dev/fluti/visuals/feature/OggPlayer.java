package dev.fluti.visuals.feature;

import dev.fluti.visuals.FlutiClient;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Відтворення твоїх .ogg (формат OGG VORBIS) напряму через OpenAL: без ресурспаків і перезавантажень.
 * Короткі звуки кешуються (без лагів на ударах), довгі пісні декодуються при запуску.
 * Якщо щось не так, причина лежить у lastError, і мод показує її на екрані.
 */
public final class OggPlayer {
    public static volatile String lastError = "";
    private static final List<int[]> ACTIVE = new ArrayList<>();   // {source, buffer, cached(0/1), ticksLeft(-1 = без ліміту)}
    private static final Map<String, Integer> CACHE = new HashMap<>();

    private OggPlayer() {}

    /** null, якщо файл придатний; інакше текст причини. */
    public static String validate(Path file) {
        ByteBuffer data = null;
        ShortBuffer pcm = null;
        try (MemoryStack st = MemoryStack.stackPush()) {
            byte[] bytes = Files.readAllBytes(file);
            data = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
            pcm = STBVorbis.stb_vorbis_decode_memory(data, st.mallocInt(1), st.mallocInt(1));
            return pcm == null ? "Not a valid OGG Vorbis file (Opus/MP3 not supported)" : null;
        } catch (Throwable t) {
            return "Cannot read file";
        } finally {
            if (pcm != null) LibCStdlib.free(pcm);
            if (data != null) MemoryUtil.memFree(data);
        }
    }

    private static int decode(Path file) {
        if (!Files.isRegularFile(file)) { lastError = "File not found: " + file.getFileName(); return -1; }
        ByteBuffer data = null;
        ShortBuffer pcm = null;
        try (MemoryStack st = MemoryStack.stackPush()) {
            byte[] bytes = Files.readAllBytes(file);
            data = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
            IntBuffer ch = st.mallocInt(1), rate = st.mallocInt(1);
            pcm = STBVorbis.stb_vorbis_decode_memory(data, ch, rate);
            if (pcm == null) { lastError = "Not a valid OGG Vorbis file (Opus/MP3 not supported)"; return -1; }
            if (ch.get(0) > 2) { lastError = "More than 2 channels is not supported"; return -1; }
            AL10.alGetError();
            int buf = AL10.alGenBuffers();
            AL10.alBufferData(buf, ch.get(0) == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, pcm, rate.get(0));
            int err = AL10.alGetError();
            if (err != AL10.AL_NO_ERROR) { lastError = "OpenAL error " + err; AL10.alDeleteBuffers(buf); return -1; }
            return buf;
        } catch (Throwable t) {
            lastError = "Audio error: " + t.getClass().getSimpleName();
            FlutiClient.LOG.warn("Cannot decode {}", file, t);
            return -1;
        } finally {
            if (pcm != null) LibCStdlib.free(pcm);
            if (data != null) MemoryUtil.memFree(data);
        }
    }

    /** Запускає звук. maxSec більше 0: автоматично обрізає звук до цієї кількості секунд. Повертає source або -1. */
    public static int start(Path file, float volume, float pitch, float maxSec, boolean cache) {
        lastError = "";
        try {
            int buf;
            if (cache) {
                String key = file + "|" + Files.size(file) + "|" + Files.getLastModifiedTime(file).toMillis();
                Integer c = CACHE.get(key);
                if (c != null) buf = c;
                else { buf = decode(file); if (buf < 0) return -1; CACHE.put(key, buf); }
            } else {
                buf = decode(file);
                if (buf < 0) return -1;
            }
            AL10.alGetError();
            int src = AL10.alGenSources();
            AL10.alSourcei(src, AL10.AL_BUFFER, buf);
            AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSourcef(src, AL10.AL_GAIN, Math.max(0f, Math.min(1f, volume)));
            AL10.alSourcef(src, AL10.AL_PITCH, Math.max(0.5f, Math.min(2f, pitch)));
            AL10.alSourcePlay(src);
            int err = AL10.alGetError();
            if (err != AL10.AL_NO_ERROR) {
                lastError = "OpenAL error " + err;
                AL10.alDeleteSources(src);
                if (!cache) AL10.alDeleteBuffers(buf);
                return -1;
            }
            ACTIVE.add(new int[]{src, buf, cache ? 1 : 0, maxSec > 0 ? Math.max(1, (int) (maxSec * 20)) : -1});
            return src;
        } catch (Throwable t) {
            lastError = "Audio error: " + t.getClass().getSimpleName();
            FlutiClient.LOG.warn("Cannot play {}", file, t);
            return -1;
        }
    }

    public static void stop(int src) {
        try { AL10.alSourceStop(src); } catch (Throwable ignored) { }
    }

    public static boolean isPlaying(int src) {
        try { return AL10.alGetSourcei(src, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING; }
        catch (Throwable t) { return false; }
    }

    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        try {
            Iterator<int[]> it = ACTIVE.iterator();
            while (it.hasNext()) {
                int[] s = it.next();
                if (AL10.alGetSourcei(s[0], AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                    AL10.alDeleteSources(s[0]);
                    if (s[2] == 0) AL10.alDeleteBuffers(s[1]);
                    it.remove();
                } else if (s[3] > 0 && --s[3] == 0) {
                    AL10.alSourceStop(s[0]);
                }
            }
        } catch (Throwable t) { ACTIVE.clear(); }
    }
}
