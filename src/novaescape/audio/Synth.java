package novaescape.audio;

import java.util.Random;

/**
 * ตัวสังเคราะห์เสียงแบบง่าย (สไตล์ชิปเกม 8-bit) — สร้างคลื่นเสียงด้วยโค้ด ไม่ต้องมีไฟล์เสียง
 * ทุกฟังก์ชันเขียนเสียงลงในอาร์เรย์ float (ค่า -1..1) ที่อัตรา RATE ตัวอย่างต่อวินาที
 */
final class Synth {
    static final int RATE = 22050;

    enum Wave { SQUARE, PULSE, TRIANGLE, SAW, SINE }

    private static final Random RNG = new Random(1234);

    private Synth() {}

    static float[] buffer(double seconds) {
        return new float[(int) Math.ceil(seconds * RATE)];
    }

    static double midi(double note) {
        return 440.0 * Math.pow(2, (note - 69) / 12.0);
    }

    private static double wave(Wave w, double phase) {
        double p = phase - Math.floor(phase);
        switch (w) {
            case SQUARE: return p < 0.5 ? 1 : -1;
            case PULSE: return p < 0.25 ? 1 : -1;
            case TRIANGLE: return p < 0.5 ? 4 * p - 1 : 3 - 4 * p;
            case SAW: return 2 * p - 1;
            default: return Math.sin(p * Math.PI * 2);
        }
    }

    /**
     * เสียงโทน: ความถี่เปลี่ยนจาก f0 ไป f1 (แบบเอ็กซ์โพเนนเชียล)
     * @param curve ความเร็วการจางเสียง (0 = คงที่, 1 = เส้นตรง, 2+ = จางเร็ว)
     */
    static void tone(float[] b, double start, double dur, double f0, double f1, Wave w,
                     double vol, double attack, double curve) {
        int s = (int) (start * RATE);
        int n = (int) (dur * RATE);
        double phase = 0;
        for (int i = 0; i < n && s + i < b.length; i++) {
            double t = i / (double) n;
            double f = f0 * Math.pow(f1 / f0, t);
            phase += f / RATE;
            double env = Math.pow(1 - t, curve);
            double at = attack > 0 ? Math.min(1, i / (attack * RATE)) : 1;
            if (s + i >= 0) b[s + i] += (float) (wave(w, phase) * vol * env * at);
        }
    }

    /**
     * เสียงซ่า (noise) ผ่านตัวกรองความถี่ต่ำ
     * @param tone 0..1 ยิ่งน้อยยิ่งทุ้ม (ค่า lowpass)
     */
    static void noise(float[] b, double start, double dur, double vol, double curve, double tone) {
        int s = (int) (start * RATE);
        int n = (int) (dur * RATE);
        double lp = 0;
        for (int i = 0; i < n && s + i < b.length; i++) {
            double t = i / (double) n;
            double r = RNG.nextDouble() * 2 - 1;
            lp += (r - lp) * tone;
            double env = Math.pow(1 - t, curve);
            if (s + i >= 0) b[s + i] += (float) (lp * vol * env);
        }
    }

    /** ไล่โน้ตต่อกัน (เช่น เสียงเก็บของ / จิงเกิ้ล) */
    static void notes(float[] b, double start, double each, double[] midiNotes, Wave w, double vol) {
        for (int i = 0; i < midiNotes.length; i++) {
            if (midiNotes[i] <= 0) continue;
            double f = midi(midiNotes[i]);
            tone(b, start + i * each, each * 1.15, f, f, w, vol, 0.002, 1.2);
        }
    }

    /** ปรับเสียงดังสุดให้เท่ากับ peak */
    static void normalize(float[] b, double peak) {
        double max = 1e-6;
        for (float v : b) max = Math.max(max, Math.abs(v));
        float k = (float) (peak / max);
        for (int i = 0; i < b.length; i++) b[i] *= k;
    }

    static short[] toPcm(float[] b) {
        short[] out = new short[b.length];
        for (int i = 0; i < b.length; i++) {
            double v = Math.max(-1, Math.min(1, b[i]));
            out[i] = (short) (v * 32767);
        }
        return out;
    }
}
