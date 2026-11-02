package novaescape.audio;

import static novaescape.audio.Synth.*;

/**
 * เอฟเฟกต์เสียงทั้งหมดของเกม (สร้างด้วยโค้ดตอนเปิดเกม)
 * อยากปรับเสียงไหน แก้ในเมธอด build() ของเสียงนั้นได้เลย
 * minGap = เว้นระยะขั้นต่ำระหว่างการเล่นเสียงเดียวกัน (กันเสียงซ้อนรัวจนหนวกหู)
 */
public enum Sfx {
    //          เว้นระยะ  ความดัง
    CLICK      (0.03, 0.45),
    SHOOT      (0.05, 0.22),
    MG         (0.05, 0.22),
    SPREAD     (0.08, 0.25),
    LASER      (0.06, 0.2),
    ROCKET     (0.12, 0.3),
    CANNON     (0.10, 0.55),
    ENEMY_SHOT (0.09, 0.14),
    EXPLOSION  (0.06, 0.4),
    BIG_EXPLOSION(0.15, 0.7),
    HURT       (0.15, 0.6),
    FREEZE     (0.20, 0.45),
    PICKUP     (0.05, 0.45),
    POWERUP    (0.10, 0.5),
    ONEUP      (0.20, 0.55),
    RESCUE     (0.20, 0.5),
    BOMB       (0.20, 0.8),
    EMP        (0.20, 0.5),
    SHIELD     (0.20, 0.5),
    TANK       (0.30, 0.55),
    WARNING    (0.50, 0.5),
    GO         (0.30, 0.45),
    BOSS_ROAR  (0.50, 0.65),
    WIN        (0.50, 0.6),
    GAME_OVER  (0.50, 0.6),
    MISSION    (0.50, 0.6);

    final double minGap;
    final float volume;
    volatile short[] pcm;

    Sfx(double minGap, double volume) {
        this.minGap = minGap;
        this.volume = (float) volume;
    }

    float[] build() {
        float[] b;
        switch (this) {
            case CLICK:
                b = buffer(0.06);
                tone(b, 0, 0.05, 900, 1300, Wave.SQUARE, 0.4, 0, 1.5);
                break;
            case SHOOT:
                b = buffer(0.08);
                tone(b, 0, 0.07, 1200, 450, Wave.SQUARE, 0.5, 0, 1.5);
                break;
            case MG:
                b = buffer(0.06);
                noise(b, 0, 0.05, 0.7, 2, 0.6);
                tone(b, 0, 0.04, 320, 140, Wave.SQUARE, 0.35, 0, 1.5);
                break;
            case SPREAD:
                b = buffer(0.11);
                tone(b, 0, 0.1, 800, 280, Wave.SQUARE, 0.45, 0, 1.3);
                noise(b, 0, 0.06, 0.35, 2, 0.4);
                break;
            case LASER:
                b = buffer(0.13);
                tone(b, 0, 0.12, 2000, 600, Wave.SAW, 0.45, 0, 1.0);
                tone(b, 0, 0.12, 2010, 610, Wave.SQUARE, 0.15, 0, 1.0);
                break;
            case ROCKET:
                b = buffer(0.3);
                noise(b, 0, 0.28, 0.6, 1.5, 0.15);
                tone(b, 0, 0.22, 180, 420, Wave.SAW, 0.25, 0.01, 1.2);
                break;
            case CANNON:
                b = buffer(0.4);
                tone(b, 0, 0.35, 170, 45, Wave.SINE, 0.9, 0, 1.5);
                noise(b, 0, 0.25, 0.6, 2, 0.3);
                break;
            case ENEMY_SHOT:
                b = buffer(0.09);
                tone(b, 0, 0.08, 520, 900, Wave.TRIANGLE, 0.5, 0, 1.2);
                break;
            case EXPLOSION:
                b = buffer(0.4);
                noise(b, 0, 0.38, 0.9, 2.2, 0.3);
                tone(b, 0, 0.3, 140, 40, Wave.SINE, 0.6, 0, 1.8);
                break;
            case BIG_EXPLOSION:
                b = buffer(1.0);
                noise(b, 0, 0.95, 1.0, 1.8, 0.22);
                tone(b, 0, 0.8, 100, 28, Wave.SINE, 0.8, 0, 1.5);
                noise(b, 0.08, 0.5, 0.5, 2.5, 0.5);
                break;
            case HURT:
                b = buffer(0.28);
                tone(b, 0, 0.26, 420, 110, Wave.SQUARE, 0.6, 0, 1.2);
                noise(b, 0, 0.12, 0.4, 2, 0.5);
                break;
            case FREEZE:
                b = buffer(0.25);
                tone(b, 0, 0.22, 1500, 350, Wave.TRIANGLE, 0.6, 0, 1.0);
                noise(b, 0, 0.2, 0.3, 1.5, 0.9);
                break;
            case PICKUP:
                b = buffer(0.2);
                notes(b, 0, 0.05, new double[]{76, 81, 88}, Wave.SQUARE, 0.4);
                break;
            case POWERUP:
                b = buffer(0.42);
                tone(b, 0, 0.25, 300, 1200, Wave.TRIANGLE, 0.5, 0.005, 0.6);
                notes(b, 0.12, 0.06, new double[]{72, 76, 79, 84}, Wave.SQUARE, 0.35);
                break;
            case ONEUP:
                b = buffer(0.5);
                notes(b, 0, 0.07, new double[]{72, 76, 79, 84, 88, 91}, Wave.SQUARE, 0.45);
                break;
            case RESCUE:
                b = buffer(0.35);
                notes(b, 0, 0.08, new double[]{79, 83, 86, 91}, Wave.TRIANGLE, 0.6);
                break;
            case BOMB:
                b = buffer(1.2);
                tone(b, 0, 1.1, 220, 28, Wave.SINE, 0.9, 0, 1.2);
                noise(b, 0, 0.9, 0.9, 1.6, 0.18);
                tone(b, 0, 0.4, 200, 1800, Wave.SAW, 0.18, 0.01, 1.0);
                break;
            case EMP:
                b = buffer(0.6);
                tone(b, 0, 0.55, 1800, 90, Wave.SQUARE, 0.35, 0, 0.8);
                tone(b, 0, 0.55, 60, 60, Wave.SAW, 0.25, 0, 1.0);
                break;
            case SHIELD:
                b = buffer(0.4);
                tone(b, 0, 0.35, 400, 1200, Wave.SINE, 0.6, 0.01, 0.8);
                tone(b, 0.05, 0.3, 800, 1600, Wave.TRIANGLE, 0.3, 0.01, 1.0);
                break;
            case TANK:
                b = buffer(0.75);
                tone(b, 0, 0.18, 110, 110, Wave.SQUARE, 0.4, 0, 0.5);
                tone(b, 0.18, 0.18, 165, 165, Wave.SQUARE, 0.4, 0, 0.5);
                tone(b, 0.36, 0.35, 220, 330, Wave.SQUARE, 0.45, 0, 1.0);
                noise(b, 0, 0.7, 0.25, 1, 0.1);
                break;
            case WARNING:
                b = buffer(1.5);
                for (int i = 0; i < 4; i++) {
                    tone(b, i * 0.36, 0.17, 880, 880, Wave.SQUARE, 0.4, 0, 0.3);
                    tone(b, i * 0.36 + 0.18, 0.17, 660, 660, Wave.SQUARE, 0.4, 0, 0.3);
                }
                break;
            case GO:
                b = buffer(0.25);
                notes(b, 0, 0.09, new double[]{72, 79}, Wave.SQUARE, 0.45);
                break;
            case BOSS_ROAR:
                b = buffer(1.0);
                tone(b, 0, 0.9, 85, 45, Wave.SAW, 0.6, 0.05, 1.0);
                noise(b, 0, 0.9, 0.5, 1.2, 0.08);
                break;
            case WIN:
                b = buffer(0.9);
                notes(b, 0, 0.1, new double[]{72, 76, 79, 84}, Wave.SQUARE, 0.45);
                tone(b, 0.4, 0.45, midi(84), midi(84), Wave.TRIANGLE, 0.5, 0, 1.5);
                break;
            case GAME_OVER:
                b = buffer(1.6);
                notes(b, 0, 0.22, new double[]{67, 64, 60, 55}, Wave.TRIANGLE, 0.6);
                tone(b, 0.88, 0.7, midi(48), midi(47), Wave.SQUARE, 0.3, 0, 1.5);
                break;
            case MISSION:
                b = buffer(1.8);
                notes(b, 0, 0.11, new double[]{72, 72, 72, 0, 67, 0, 72}, Wave.SQUARE, 0.4);
                notes(b, 0.8, 0.12, new double[]{76, 79}, Wave.SQUARE, 0.4);
                tone(b, 1.04, 0.7, midi(84), midi(84), Wave.TRIANGLE, 0.55, 0, 1.3);
                tone(b, 1.04, 0.7, midi(60), midi(60), Wave.SQUARE, 0.25, 0, 1.3);
                break;
            default:
                b = buffer(0.01);
        }
        normalize(b, 0.9);
        return b;
    }
}
