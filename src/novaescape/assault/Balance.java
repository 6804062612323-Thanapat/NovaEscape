package novaescape.assault;

/**
 * ค่าความสมดุลของโหมด ASSAULT รวมไว้ที่เดียว — จูนความยากตรงนี้ได้เลย
 */
public final class Balance {
    private Balance() {}

    // ---------- ระเบิด (K / คลิกขวา) ----------
    public static final int BOMB_START = 2;          // จำนวนตอนเริ่มด่าน
    public static final int BOMB_MAX = 6;            // ถือได้สูงสุด
    public static final int BOMB_PER_PICKUP = 1;     // เก็บกล่อง B ได้กี่ลูก
    public static final double BOMB_DAMAGE = 10;     // ดาเมจต่อศัตรูธรรมดาบนจอ
    public static final double BOMB_BOSS_DAMAGE = 12;// ดาเมจต่อบอส
    public static final double BOMB_COOLDOWN = 1.0;  // วินาที กันกดรัว

    // ---------- รถถัง NOVA SLUG ----------
    public static final double SLUG_TIME_PER_WEAPON = 1; // เก็บกล่องปืนตอนอยู่ในรถ = เพิ่มเวลารถกี่วินาที
    public static final double SLUG_MAX_TIME = 35;       // เวลารถสูงสุดที่สะสมได้

    // ---------- ดาเมจที่ผู้เล่นโดน ----------
    public static final int NORMAL_HIT = 1;          // กระสุน / ชนศัตรูทั่วไป
    public static final int BIG_HIT = 2;             // ท่าใหญ่ของบอส (ลำแสง, หนวด, งูพุ่ง)

    // ---------- ความยากตามด่าน (index 0 = ด่าน 1) ----------
    private static final double[] ENEMY_HP = {1.0, 1.25, 1.5};
    private static final double[] ENEMY_BULLET_SPEED = {1.0, 1.1, 1.2};
    private static final double[] MEDKIT_DROP = {0.09, 0.06, 0.04};
    /** ตัวคูณโอกาสดรอปของจากมอนทั่วไป (มินิบอส/ตัวประกัน/กล่องเสบียง ไม่โดนคูณ) */
    private static final double[] ENEMY_DROP = {0.5, 0.4, 0.33};

    private static int idx(int stage) {
        return Math.max(0, Math.min(ENEMY_HP.length - 1, stage - 1));
    }

    /** ตัวคูณเลือดศัตรูทั่วไป (บอสกำหนดเลือดเองในคลาสของบอส) */
    public static double enemyHp(int stage) { return ENEMY_HP[idx(stage)]; }

    /** ตัวคูณความเร็วกระสุนศัตรู */
    public static double enemyBulletSpeed(int stage) { return ENEMY_BULLET_SPEED[idx(stage)]; }

    /** ตัวคูณโอกาสดรอปของจากมอนทั่วไป */
    public static double enemyDrop(int stage) { return ENEMY_DROP[idx(stage)]; }

    /** โอกาสที่ของดรอปแบบสุ่มจะเป็น Medkit */
    public static double medkitDrop(int stage) { return MEDKIT_DROP[idx(stage)]; }
}
