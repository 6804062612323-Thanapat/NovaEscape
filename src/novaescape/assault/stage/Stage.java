package novaescape.assault.stage;

import novaescape.assault.AssaultGame;
import novaescape.assault.Level;
import novaescape.assault.enemy.BossEnemy;

import java.awt.*;

/**
 * คลาสแม่ของด่านในโหมด ASSAULT
 * แต่ละด่านกำหนด: ชื่อ, ความยาวแมพ, เขตย่อย, หน้าตาฉาก, สคริปต์ศัตรู, บอส,
 * ตำแหน่งรถถัง NOVA SLUG และกลไกพิเศษ (เช่น ลมพายุ, ผนังอุโมงค์)
 *
 * เพิ่มด่านใหม่: สร้างคลาส extends Stage แล้วเพิ่มใน create() และ COUNT
 */
public abstract class Stage {
    public static final int COUNT = 3;

    public static Stage create(int number) {
        switch (number) {
            case 2: return new Stage2FrozenRings();
            case 3: return new Stage3HiveCore();
            default: return new Stage1NovaSector();
        }
    }

    // ---------- หน้าตาฉาก ----------
    public enum Style { NOVA, ICE, HIVE }

    public static final class Theme {
        public final Style style;
        public final Color skyTop, skyBottom, nebula, debris, bossTint, accent;
        public final Color[] sectorTints;   // index = เขต (เขตแรกไม่ใช้)

        public Theme(Style style, Color skyTop, Color skyBottom, Color nebula, Color debris,
                     Color bossTint, Color accent, Color... sectorTints) {
            this.style = style;
            this.skyTop = skyTop;
            this.skyBottom = skyBottom;
            this.nebula = nebula;
            this.debris = debris;
            this.bossTint = bossTint;
            this.accent = accent;
            this.sectorTints = sectorTints;
        }
    }

    // ---------- ข้อมูลด่าน ----------
    public abstract int number();
    public abstract String name();
    public abstract String tagline();          // คำอธิบายสั้น ๆ ในหน้าเลือกด่าน
    public abstract String bossName();
    public abstract String bossWarning();      // ข้อความตอนบอสมา
    public abstract double length();           // ความยาวแมพทั้งหมด
    public abstract double[] sectorStarts();   // ตำแหน่งเริ่มของแต่ละเขต (เขตแรก = 0)
    public abstract String[] sectorNames();
    public abstract Theme theme();
    public abstract double vehicleX();         // ตำแหน่งรถถัง NOVA SLUG (มีด่านละ 1 คัน)

    /** สร้างศัตรู/เหตุการณ์ทั้งหมดของด่าน */
    public abstract void build(Level level);

    /** สร้างบอสของด่าน */
    public abstract BossEnemy createBoss(AssaultGame g);

    /** ตำแหน่งกล้องตอนเข้าห้องบอส (ห้องบอสกว้างกว่าจอปกติ เพราะกล้องซูมออก) */
    public double arenaX() {
        return length() - AssaultGame.BOSS_VIEW_W;
    }

    public String sectorName(double camX) {
        if (camX >= arenaX() - 1) return bossName();
        double[] s = sectorStarts();
        for (int i = s.length - 1; i >= 0; i--) {
            if (camX >= s[i]) return sectorNames()[i];
        }
        return sectorNames()[0];
    }

    // ---------- กลไกพิเศษ (ด่านไหนไม่มีก็ไม่ต้องเขียนทับ) ----------

    /** ขอบบนที่บินได้ ณ ตำแหน่ง x (ด่านอุโมงค์จะมีผนัง) */
    public double ceiling(double x, AssaultGame g) { return g.top(); }

    /** ขอบล่างที่บินได้ ณ ตำแหน่ง x */
    public double floor(double x, AssaultGame g) { return g.bottom(); }

    /** true = มีผนัง ชนแล้วเจ็บ กระสุนชนผนังแล้วหาย */
    public boolean hasWalls() { return false; }

    /** อัปเดตกลไกพิเศษทุกเฟรม (เช่น พายุหิมะ) */
    public void update(double dt, AssaultGame g) {}

    /** วาดสิ่งที่อยู่ในโลก (เช่น ผนัง) — พิกัดโลก */
    public void drawWorld(Graphics2D g2, AssaultGame g) {}

    /** วาดเอฟเฟกต์ทับจอ (เช่น หิมะ) — พิกัดจอ */
    public void drawScreen(Graphics2D g2, AssaultGame g) {}
}
