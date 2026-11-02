package novaescape;

/**
 * คำนวณความยากของศัตรูตามเลขตา (round)
 * ยิ่งตาสูง เลเซอร์ยิ่งเร็ว ยิงถี่ขึ้น ยิงทีละหลายเส้น และเล็งใส่ผู้เล่นบ่อยขึ้น
 */
public class Difficulty {
    private final int round;

    public Difficulty(int round) {
        this.round = Math.max(1, round);
    }

    public int round() { return round; }

    /** ความเร็วเลเซอร์ (พิกเซล/วินาที) */
    public double laserSpeed() {
        return 340 + 55 * (round - 1);
    }

    /** เวลาห่างระหว่างการยิงแต่ละชุด (วินาที) */
    public double fireInterval() {
        return Math.max(0.22, 0.85 * Math.pow(0.86, round - 1));
    }

    /** จำนวนเลเซอร์ต่อการยิง 1 ชุด */
    public int lasersPerVolley() {
        return 1 + (round - 1) / 2;
    }

    /** โอกาสที่เลเซอร์จะเล็งไปที่ตำแหน่งผู้เล่น (0..1) */
    public double aimedChance() {
        return Math.min(0.55, 0.12 * (round - 1));
    }

    /** ความเร็วที่ยานต่างดาวขยับขึ้นลง */
    public double alienBobSpeed() {
        return 0.6 + 0.15 * (round - 1);
    }
}
