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
import java.util.Iterator;
import java.util.List;

/** Відтворення власних .ogg (STB Vorbis -> OpenAL), без ресурспаків. Викликати з клієнтського потоку. */
public final class OggPlayer {
    private static final List<int[]> ACTIVE = new ArrayList<>(); // {source, buffer}

    private OggPlayer() {}

    public static void play(Path file, float volume) { start(file, volume, 1f); }

    public static void play(Path file, float volume, float pitch) { start(file, volume, pitch); }

    /** Повертає id джерела або -1. */
    public static int start(Path file, float volume, float pitch) {
        if (!Files.isRegularFile(file)) return -1;
        ByteBuffer data = null;
        ShortBuffer pcm = null;
        try (MemoryStack st = MemoryStack.stackPush()) {
            byte[] bytes = Files.readAllBytes(file);
            data = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
            IntBuffer ch = st.mallocInt(1), rate = st.mallocInt(1);
            pcm = STBVorbis.stb_vorbis_decode_memory(data, ch, rate);
            if (pcm == null) return -1;
            int buf = AL10.alGenBuffers();
            AL10.alBufferData(buf, ch.get(0) == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, pcm, rate.get(0));
            int src = AL10.alGenSources();
            AL10.alSourcei(src, AL10.AL_BUFFER, buf);
            AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSourcef(src, AL10.AL_GAIN, Math.max(0f, Math.min(1f, volume)));
            AL10.alSourcef(src, AL10.AL_PITCH, Math.max(0.5f, Math.min(2f, pitch)));
            AL10.alSourcePlay(src);
            ACTIVE.add(new int[]{src, buf});
            return src;
        } catch (Throwable t) {
            FlutiClient.LOG.warn("Cannot play {}", file, t);
            return -1;
        } finally {
            if (pcm != null) LibCStdlib.free(pcm);
            if (data != null) MemoryUtil.memFree(data);
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
                    AL10.alDeleteBuffers(s[1]);
                    it.remove();
                }
            }
        } catch (Throwable t) { ACTIVE.clear(); }
    }
}
