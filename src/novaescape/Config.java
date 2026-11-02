package novaescape;

/**
 * ค่าตั้งต้นทั้งหมดของเกมอยู่ที่นี่ ปรับตัวเลขตรงนี้เพื่อจูนความยาก/ความเร็วได้เลย
 */
public final class Config {
    private Config() {}

    // ขนาดหน้าจอ
    public static final int WIDTH = 960;
    public static final int HEIGHT = 540;
    public static final int HUD_HEIGHT = 44;          // แถบข้อมูลด้านบน

    // เวลาในแต่ละตา (วินาที) ต้องหลบให้ครบเวลานี้ถึงจะ WIN
    public static final double ROUND_TIME = 20.0;
    public static final double READY_TIME = 1.5;      // นับถอยหลังก่อนเริ่มตา

    // ยานผู้เล่น
    public static final double PLAYER_SPEED = 300;    // พิกเซล/วินาที
    public static final double PLAYER_MAX_X_RATIO = 0.62; // ยานไปได้ไกลสุดกี่ % ของจอ

    // ไอเทม
    public static final double SHIELD_TIME = 5.0;     // โล่ อมตะ 5 วินาที
    public static final double ATTACK_TIME = 5.0;     // โจมตี ศัตรูหยุดยิง 5 วินาที
    public static final double REVIVE_INVULN = 2.0;   // หลังชุบชีวิตด้วยหัวใจ อมตะชั่วคราว

    // คะแนน
    public static final double SCORE_PER_SECOND = 10; // คูณด้วยเลขตา
    public static final int ROUND_CLEAR_BONUS = 100;  // คูณด้วยเลขตา

    // FPS เป้าหมาย
    public static final int FRAME_MS = 16;
}
