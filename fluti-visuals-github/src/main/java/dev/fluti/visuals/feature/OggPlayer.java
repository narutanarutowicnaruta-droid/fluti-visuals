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

/**
 * Відтворення власного .ogg з config/fluti/sounds/ без ресурспаків:
 * STB Vorbis (є в LWJGL ігри) -> OpenAL source. Експериментально: викликати лише з клієнтського потоку.
 */
public final class OggPlayer {
    private static final List<int[]> ACTIVE = new ArrayList<>(); // {source, buffer}

    private OggPlayer() {}

    public static void play(Path file, float volume) {
        if (!Files.isRegularFile(file)) return;
        ByteBuffer data = null;
        ShortBuffer pcm = null;
        try (MemoryStack st = MemoryStack.stackPush()) {
            byte[] bytes = Files.readAllBytes(file);
            data = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
            IntBuffer ch = st.mallocInt(1), rate = st.mallocInt(1);
            pcm = STBVorbis.stb_vorbis_decode_memory(data, ch, rate);
            if (pcm == null) return;
            int buf = AL10.alGenBuffers();
            AL10.alBufferData(buf, ch.get(0) == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, pcm, rate.get(0));
            int src = AL10.alGenSources();
            AL10.alSourcei(src, AL10.AL_BUFFER, buf);
            AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE); // звук "в голові", без 3D-позиції
            AL10.alSourcef(src, AL10.AL_GAIN, Math.max(0f, Math.min(1f, volume)));
            AL10.alSourcePlay(src);
            ACTIVE.add(new int[]{src, buf});
        } catch (Throwable t) {
            FlutiClient.LOG.warn("Cannot play {}", file, t);
        } finally {
            if (pcm != null) LibCStdlib.free(pcm);
            if (data != null) MemoryUtil.memFree(data);
        }
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
