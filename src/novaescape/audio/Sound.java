package novaescape.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ระบบเสียงของเกม: มิกซ์เสียงเองทั้งหมดลงบนช่องเสียงเดียว (เสถียรทุกเครื่อง)
 *
 *   Sound.init();                 // เรียกครั้งเดียวตอนเปิดเกม
 *   Sound.play(Sfx.EXPLOSION);    // เล่นเอฟเฟกต์
 *   Sound.music(Music.STAGE1);    // เปลี่ยนเพลง (null = หยุดเพลง)
 *   Sound.toggleMute();           // ปุ่ม M
 *
 * ถ้าเครื่องไม่มีลำโพง/การ์ดเสียง เกมยังเล่นได้ปกติ แค่ไม่มีเสียง
 */
public final class Sound {
    private Sound() {}

    public static float MASTER_VOLUME = 0.8f;
    public static float MUSIC_VOLUME = 0.32f;
    private static final int MAX_VOICES = 24;
    private static final int CHUNK = 441;            // 20 ms ต่อรอบการมิกซ์

    private static volatile boolean ready;
    private static volatile boolean muted;
    private static volatile Music currentMusic;
    private static volatile Music requestedMusic;

    private static final ConcurrentLinkedQueue<Voice> incoming = new ConcurrentLinkedQueue<>();
    private static final Map<Sfx, Long> lastPlayed = new EnumMap<>(Sfx.class);

    private static final class Voice {
        final short[] data;
        final float vol;
        int pos;

        Voice(short[] data, float vol) {
            this.data = data;
            this.vol = vol;
        }
    }

    /** เตรียมเสียงทั้งหมดแล้วเปิดช่องเสียง (ทำงานเบื้องหลัง ไม่ทำให้เกมเปิดช้า) */
    public static void init() {
        Thread t = new Thread(Sound::run, "nova-audio");
        t.setDaemon(true);
        t.start();
    }

    public static boolean isMuted() { return muted; }

    public static void toggleMute() { muted = !muted; }

    public static void play(Sfx s) {
        play(s, 1f);
    }

    public static void play(Sfx s, float volumeScale) {
        if (!ready || muted || s.pcm == null) return;
        long now = System.nanoTime();
        synchronized (lastPlayed) {
            Long last = lastPlayed.get(s);
            if (last != null && (now - last) / 1e9 < s.minGap) return;
            lastPlayed.put(s, now);
        }
        incoming.add(new Voice(s.pcm, s.volume * volumeScale));
    }

    /** เปลี่ยนเพลงประกอบ (เพลงเดิมอยู่แล้วจะไม่เริ่มใหม่) */
    public static void music(Music m) {
        requestedMusic = m;
    }

    // =====================================================================
    //  เธรดเสียง
    // =====================================================================
    private static void run() {
        // 1) สร้างเอฟเฟกต์ทั้งหมด
        try {
            for (Sfx s : Sfx.values()) s.pcm = Synth.toPcm(s.build());
        } catch (Throwable e) {
            return;
        }
        // 2) เปิดช่องเสียง
        SourceDataLine line;
        AudioFormat fmt = new AudioFormat(Synth.RATE, 16, 1, true, false);
        try {
            line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, CHUNK * 2 * 5);
            line.start();
        } catch (Throwable e) {
            return; // ไม่มีอุปกรณ์เสียง: เล่นต่อแบบเงียบ
        }
        ready = true;

        // 3) แต่งเพลงทีหลัง (ใช้เวลานิดหน่อย) — ระหว่างนั้นเอฟเฟกต์เล่นได้แล้ว
        Thread composer = new Thread(() -> {
            for (Music m : Music.values()) {
                try { m.pcm = Synth.toPcm(m.build()); } catch (Throwable ignored) { }
            }
        }, "nova-music-composer");
        composer.setDaemon(true);
        composer.setPriority(Thread.MIN_PRIORITY);
        composer.start();

        List<Voice> voices = new ArrayList<>();
        float[] mix = new float[CHUNK];
        byte[] out = new byte[CHUNK * 2];
        int musicPos = 0;

        while (true) {
            Voice v;
            while ((v = incoming.poll()) != null) {
                voices.add(v);
                if (voices.size() > MAX_VOICES) voices.remove(0);
            }
            Music want = requestedMusic;
            if (want != currentMusic && (want == null || want.pcm != null)) {
                currentMusic = want;
                musicPos = 0;
            }
            musicPos = mixChunk(mix, voices, currentMusic, musicPos);

            for (int i = 0; i < CHUNK; i++) {
                float x = mix[i] * MASTER_VOLUME;
                if (muted) x = 0;
                // ลิมิตเตอร์แบบนุ่ม กันเสียงแตก
                if (x > 0.8f) x = 0.8f + (x - 0.8f) / (1 + (x - 0.8f) * 5);
                if (x < -0.8f) x = -0.8f + (x + 0.8f) / (1 - (x + 0.8f) * 5);
                x = Math.max(-1f, Math.min(1f, x));
                short sv = (short) (x * 32767);
                out[i * 2] = (byte) sv;
                out[i * 2 + 1] = (byte) (sv >> 8);
            }
            line.write(out, 0, out.length); // บล็อกจนกว่าช่องเสียงจะว่าง = คุมจังหวะให้เอง
        }
    }

    /** มิกซ์เสียงทั้งหมด 1 ช่วง (แยกเป็นเมธอดเพื่อทดสอบได้) — คืนตำแหน่งเพลงถัดไป */
    static int mixChunk(float[] mix, List<Voice> voices, Music music, int musicPos) {
        java.util.Arrays.fill(mix, 0f);
        if (music != null && music.pcm != null) {
            short[] m = music.pcm;
            for (int i = 0; i < mix.length; i++) {
                mix[i] += m[musicPos] / 32768f * MUSIC_VOLUME;
                musicPos++;
                if (musicPos >= m.length) musicPos = 0;
            }
        }
        for (Iterator<Voice> it = voices.iterator(); it.hasNext(); ) {
            Voice v = it.next();
            int n = Math.min(mix.length, v.data.length - v.pos);
            for (int i = 0; i < n; i++) mix[i] += v.data[v.pos + i] / 32768f * v.vol;
            v.pos += n;
            if (v.pos >= v.data.length) it.remove();
        }
        return musicPos;
    }
}
