package novaescape.audio;

import java.util.Random;

import static novaescape.audio.Synth.*;

/**
 * เพลงประกอบแบบชิปทูน (แต่งด้วยโค้ด) วนลูปได้
 * แต่ละเพลงกำหนด: จังหวะ (BPM), โน้ตตั้งต้น, ลำดับคอร์ด, สไตล์กลอง/เบส
 * chords: {ระยะห่างจากโน้ตตั้งต้น (ครึ่งเสียง), 0 = minor / 1 = major}
 */
public enum Music {
    //       bpm  root  seed  drums  bassWave        chords
    MENU   (96,  57,   11,   1,     Wave.TRIANGLE,  new int[][]{{0, 0}, {8, 1}, {3, 1}, {10, 1}}),
    CLASSIC(132, 55,   22,   2,     Wave.SQUARE,    new int[][]{{0, 0}, {8, 1}, {10, 1}, {0, 0}}),
    STAGE1 (140, 57,   33,   2,     Wave.SQUARE,    new int[][]{{0, 0}, {0, 0}, {8, 1}, {10, 1}}),
    STAGE2 (124, 52,   44,   1,     Wave.TRIANGLE,  new int[][]{{0, 0}, {8, 1}, {10, 1}, {7, 0}}),
    STAGE3 (116, 50,   55,   2,     Wave.SAW,       new int[][]{{0, 0}, {1, 1}, {0, 0}, {-2, 1}}),
    BOSS   (164, 48,   66,   3,     Wave.SAW,       new int[][]{{0, 0}, {8, 1}, {10, 1}, {7, 1}});

    final int bpm, root, seed, drums;
    final Wave bassWave;
    final int[][] chords;
    volatile short[] pcm;

    Music(int bpm, int root, int seed, int drums, Wave bassWave, int[][] chords) {
        this.bpm = bpm;
        this.root = root;
        this.seed = seed;
        this.drums = drums;
        this.bassWave = bassWave;
        this.chords = chords;
    }

    /** แต่งเพลง 8 ห้อง (คอร์ดวน 2 รอบ ทำนองไม่ซ้ำกัน) */
    float[] build() {
        Random rng = new Random(seed);
        double beat = 60.0 / bpm;
        int bars = chords.length * 2;
        float[] b = buffer(bars * 4 * beat + 0.01);
        boolean intense = this == BOSS;
        boolean calm = this == MENU;

        int[] minorScale = {0, 2, 3, 5, 7, 8, 10};
        int prevMelody = 3;

        for (int bar = 0; bar < bars; bar++) {
            int[] ch = chords[bar % chords.length];
            int cr = root + ch[0];
            int third = ch[1] == 1 ? 4 : 3;
            int[] tones = {cr, cr + third, cr + 7};
            double t0 = bar * 4 * beat;

            // ---- เบส: โน้ตเขบ็ตหนึ่งชั้น ----
            int[] bassPat = calm ? new int[]{0, -1, 0, -1, 7, -1, 0, -1}
                    : intense ? new int[]{0, 0, 12, 0, 0, 7, 12, 0}
                    : new int[]{0, 0, 7, 0, 12, 0, 7, 0};
            for (int i = 0; i < 8; i++) {
                if (bassPat[i] < 0) continue;
                double f = midi(cr - 12 + bassPat[i]);
                tone(b, t0 + i * beat / 2, beat / 2 * 0.9, f, f, bassWave, calm ? 0.16 : 0.13, 0.003, 0.8);
            }

            // ---- อาร์เปจจิโอ: โน้ตเขบ็ตสองชั้น ----
            int arpOct = (this == STAGE2) ? 24 : 12;
            for (int i = 0; i < 16; i++) {
                if (calm && i % 2 == 1) continue;
                if (!intense && rng.nextDouble() < 0.12) continue;
                int note = tones[(i + (i / 4)) % 3] + arpOct;
                double f = midi(note);
                tone(b, t0 + i * beat / 4, beat / 4 * 0.8, f, f,
                        this == STAGE3 ? Wave.PULSE : Wave.TRIANGLE, 0.055, 0.002, 1.0);
            }

            // ---- ทำนองหลัก: โน้ตยาว เดินในสเกล ----
            if (!calm || bar % 2 == 0) {
                int steps = intense ? 8 : 4;
                for (int i = 0; i < steps; i++) {
                    if (rng.nextDouble() < (calm ? 0.4 : 0.25)) continue;
                    prevMelody = Math.max(0, Math.min(minorScale.length * 2 - 1,
                            prevMelody + rng.nextInt(5) - 2));
                    int deg = prevMelody % minorScale.length;
                    int oct = prevMelody / minorScale.length;
                    int note = root + 12 + minorScale[deg] + oct * 12;
                    // ตัวโน้ตแรกของห้องให้ตรงกับคอร์ด จะได้ฟังกลมกลืน
                    if (i == 0) note = tones[rng.nextInt(3)] + 12;
                    double len = 4 * beat / steps;
                    double f = midi(note);
                    tone(b, t0 + i * len, len * 0.92, f, f, Wave.PULSE, intense ? 0.06 : 0.07, 0.005, 0.5);
                    tone(b, t0 + i * len, len * 0.92, f * 1.003, f * 1.003, Wave.SQUARE, 0.025, 0.005, 0.5);
                }
            }

            // ---- กลอง ----
            for (int beatI = 0; beatI < 4; beatI++) {
                double bt = t0 + beatI * beat;
                if (drums >= 1 && (beatI == 0 || (drums >= 2 && beatI == 2) || drums >= 3)) {
                    tone(b, bt, 0.14, 130, 38, Wave.SINE, 0.45, 0, 2.0);   // กระเดื่อง
                }
                if (drums >= 2 && (beatI == 1 || beatI == 3)) {
                    noise(b, bt, 0.12, 0.25, 2.5, 0.55);                   // สแนร์
                }
                if (drums >= 1) {
                    noise(b, bt + beat / 2, 0.03, 0.08, 2, 0.95);          // ไฮแฮท
                    if (drums >= 3) noise(b, bt + beat / 4 * 3, 0.03, 0.06, 2, 0.95);
                }
            }
        }
        normalize(b, 0.85);
        return b;
    }
}
