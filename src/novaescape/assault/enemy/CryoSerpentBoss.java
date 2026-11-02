package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;
import novaescape.assault.Bullet;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * บอสด่าน 2: CRYO SERPENT — งูน้ำแข็งยักษ์
 * วิธีสู้: ยิงได้แค่ "หัว" — ลำตัวกันกระสุนและชนแล้วเจ็บ ต้องหาจังหวะยิงหัวตอนมันเลี้ยว
 * เฟส 1 : เลื้อยไปทั่วห้อง พ่นกระสุน 3 ทาง + ลำตัวยิงเข็มน้ำแข็งออกด้านข้าง
 * เฟส 2 : + มุดหายแล้ว "พุ่งตัดจอ" ตามเลนที่มีเส้นแดงเตือนก่อน
 * เฟส 3 : + ฝนน้ำแข็งตกจากด้านบน, เลื้อยเร็วขึ้น
 */
public class CryoSerpentBoss extends BossEnemy {
    public static final double MAX_HP = 360;
    private static final int SEGMENTS = 16;
    private static final double SPACING = 26;

    private enum Mode { WEAVE, DIVE_OUT, HIDDEN, LANE_WARN, LUNGE }

    private Mode mode = Mode.WEAVE;
    private double modeT;
    private double heading = Math.PI;
    private final List<double[]> trail = new ArrayList<>();
    private double spitTimer = 2.0, needleTimer = 3.0, diveTimer = 7.0, rainTimer;
    private int spitCount;
    private double laneY;
    private double mouthOpen;

    public CryoSerpentBoss(AssaultGame g) {
        super(g.arenaX() + g.viewW() + 120, g.centerY(), 66, 50, MAX_HP, 25000);
        resetTrailBehind(0);
    }

    @Override public String displayName() { return "CRYO SERPENT"; }

    /** ลำตัวเป็นเกราะเกล็ดน้ำแข็ง: ยิงโดนลำตัวเข้าแค่ 40% (หัวเข้าเต็ม) */
    @Override
    public double armorDamageRatio() { return 0.4; }

    /** ชนตอนพุ่งตัดจอเจ็บ 2 ช่อง */
    @Override
    public int contactDamage() {
        return mode == Mode.LUNGE ? novaescape.assault.Balance.BIG_HIT : novaescape.assault.Balance.NORMAL_HIT;
    }

    @Override
    public int phase() {
        double r = hp / maxHp;
        return r > 0.66 ? 1 : r > 0.33 ? 2 : 3;
    }

    private void resetTrailBehind(double dirAngle) {
        trail.clear();
        for (int i = SEGMENTS + 2; i >= 0; i--) {
            trail.add(new double[]{x + Math.cos(dirAngle) * SPACING * i, y + Math.sin(dirAngle) * SPACING * i});
        }
    }

    @Override
    public Rectangle2D hitbox() {
        if (mode == Mode.HIDDEN || mode == Mode.LANE_WARN) return new Rectangle2D.Double(-9999, -9999, 1, 1);
        return new Rectangle2D.Double(x - 42, y - 32, 84, 64);
    }

    @Override
    public List<Rectangle2D> blockers() {
        List<Rectangle2D> list = new ArrayList<>();
        if (mode == Mode.HIDDEN || mode == Mode.LANE_WARN) return list;
        for (int i = 1; i <= SEGMENTS; i++) {
            double[] p = segment(i);
            double r = segRadius(i);
            list.add(new Rectangle2D.Double(p[0] - r * 0.8, p[1] - r * 0.8, r * 1.6, r * 1.6));
        }
        return list;
    }

    private double segRadius(int i) {
        return 22 - 12.0 * i / SEGMENTS;
    }

    private double[] segment(int i) {
        int idx = trail.size() - 1 - i;
        if (idx < 0) idx = 0;
        return trail.get(idx);
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        int ph = phase();
        modeT += dt;
        if (mouthOpen > 0) mouthOpen -= dt;
        double left = g.camX(), right = g.camX() + g.viewW();
        double top = g.top(), bottom = g.bottom();
        double height = bottom - top;

        switch (mode) {
            case WEAVE: {
                double tx = left + g.viewW() * 0.66 + Math.cos(t * 0.55) * g.viewW() * 0.26;
                // หัวต้องอยู่หน้าผู้เล่นเสมอ (ผู้เล่นยิงไปทางขวา)
                tx = Math.max(tx, g.player().centerX() + 280);
                tx = Math.min(tx, right - 80);
                double ty = g.centerY() + Math.sin(t * 1.1) * height * 0.36;
                steer(tx, ty, ph == 3 ? 3.4 : 2.5, ph == 3 ? 270 : 215, dt);
                if (ph >= 2) {
                    diveTimer -= dt;
                    if (diveTimer <= 0) { mode = Mode.DIVE_OUT; modeT = 0; }
                }
                break;
            }
            case DIVE_OUT:
                steer(x, bottom + 300, 3.5, 300, dt);
                if (y > bottom + 120 || modeT > 4) { mode = Mode.HIDDEN; modeT = 0; }
                break;
            case HIDDEN:
                if (modeT > 0.9) {
                    mode = Mode.LANE_WARN;
                    modeT = 0;
                    laneY = Math.max(top + 40, Math.min(bottom - 40, g.player().centerY()));
                    x = right + 220;
                    y = laneY;
                    heading = Math.PI;
                    resetTrailBehind(0);
                }
                break;
            case LANE_WARN:
                if (modeT > 1.0) { mode = Mode.LUNGE; modeT = 0; g.shake(10); }
                break;
            case LUNGE:
                x -= 860 * dt;
                pushTrail();
                if (x < left - 260) {
                    // พุ่งพ้นจอแล้ว วนกลับมาโผล่ทางขวา (ด้านหน้าผู้เล่น) จะได้ยิงหัวได้
                    mode = Mode.WEAVE;
                    modeT = 0;
                    diveTimer = 8.5;
                    x = right + 160;
                    y = Math.max(top + 60, Math.min(bottom - 60, g.centerY() + (Math.random() - 0.5) * height * 0.5));
                    heading = Math.PI;
                    resetTrailBehind(0);
                }
                break;
            default:
                break;
        }

        if (g.enemiesJammed() || mode == Mode.HIDDEN || mode == Mode.LANE_WARN) return;

        // พ่นกระสุน
        spitTimer -= dt;
        if (spitTimer <= 0 && mode == Mode.WEAVE && x < right - 30) {
            spitTimer = ph == 1 ? 1.7 : 1.3;
            mouthOpen = 0.3;
            double a = angleToPlayer(g);
            if (++spitCount % 3 == 0) {
                g.enemyBullet(Bullet.frost(x, y, a, 190));
            } else {
                for (int i = -1; i <= 1; i++) g.enemyShoot(x, y, a + i * 0.22, 250);
            }
        }
        // เข็มน้ำแข็งจากลำตัว
        needleTimer -= dt;
        if (needleTimer <= 0 && mode == Mode.WEAVE) {
            needleTimer = ph == 1 ? 2.6 : 2.0;
            for (int k = 0; k < 3; k++) {
                int i = 3 + (int) (Math.random() * (SEGMENTS - 4));
                double[] p = segment(i), q = segment(i - 1);
                double dir = Math.atan2(q[1] - p[1], q[0] - p[0]);
                g.enemyShoot(p[0], p[1], dir + Math.PI / 2, 170);
                g.enemyShoot(p[0], p[1], dir - Math.PI / 2, 170);
            }
        }
        // ฝนน้ำแข็ง
        if (ph >= 3) {
            rainTimer -= dt;
            if (rainTimer <= 0) {
                rainTimer = 0.24;
                double rx = left + 40 + Math.random() * (g.viewW() - 80);
                g.enemyBullet(Bullet.icicle(rx, top - 10, 320));
            }
        }
    }

    private void steer(double tx, double ty, double turnRate, double speed, double dt) {
        double want = Math.atan2(ty - y, tx - x);
        double diff = Math.atan2(Math.sin(want - heading), Math.cos(want - heading));
        heading += Math.max(-turnRate * dt, Math.min(turnRate * dt, diff));
        x += Math.cos(heading) * speed * dt;
        y += Math.sin(heading) * speed * dt;
        pushTrail();
    }

    /** บันทึกเส้นทางหัว เพื่อให้ลำตัวเลื้อยตาม */
    private void pushTrail() {
        double[] last = trail.get(trail.size() - 1);
        double d = Math.hypot(x - last[0], y - last[1]);
        while (d >= SPACING) {
            double k = SPACING / d;
            double nx = last[0] + (x - last[0]) * k, ny = last[1] + (y - last[1]) * k;
            last = new double[]{nx, ny};
            trail.add(last);
            d = Math.hypot(x - nx, y - ny);
        }
        while (trail.size() > SEGMENTS + 4) trail.remove(0);
    }

    @Override
    protected void onDeath(AssaultGame g) {
        for (int i = 1; i <= SEGMENTS; i += 2) {
            double[] p = segment(i);
            g.explode(p[0], p[1], 14);
        }
        super.onDeath(g);
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        // เส้นเตือนก่อนพุ่ง
        if (mode == Mode.LANE_WARN) {
            boolean on = ((int) (modeT * 14)) % 2 == 0;
            g.setColor(new Color(255, 60, 60, on ? 110 : 45));
            g.fill(new Rectangle2D.Double(x - 4000, laneY - 30, 4300, 60));
            g.setColor(new Color(255, 120, 120, 200));
            g.draw(new Line2D.Double(x - 4000, laneY - 30, x + 300, laneY - 30));
            g.draw(new Line2D.Double(x - 4000, laneY + 30, x + 300, laneY + 30));
            return;
        }
        if (mode == Mode.HIDDEN) return;

        // ลำตัว (วาดจากหางมาหัว)
        for (int i = SEGMENTS; i >= 1; i--) {
            double[] p = segment(i), q = segment(i - 1);
            double r = segRadius(i);
            double dir = Math.atan2(q[1] - p[1], q[0] - p[0]);
            // หนามบนหลัง
            Path2D spike = new Path2D.Double();
            double nx = Math.cos(dir - Math.PI / 2), ny = Math.sin(dir - Math.PI / 2);
            spike.moveTo(p[0] + Math.cos(dir) * r * 0.5 + nx * r * 0.6, p[1] + Math.sin(dir) * r * 0.5 + ny * r * 0.6);
            spike.lineTo(p[0] + nx * (r + 12), p[1] + ny * (r + 12));
            spike.lineTo(p[0] - Math.cos(dir) * r * 0.5 + nx * r * 0.6, p[1] - Math.sin(dir) * r * 0.5 + ny * r * 0.6);
            spike.closePath();
            g.setColor(new Color(200, 240, 255));
            g.fill(spike);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(p[0] - r * 0.3, p[1] - r * 0.3), (float) (r * 1.3),
                    new float[]{0f, 1f}, new Color[]{new Color(170, 220, 255), new Color(50, 100, 170)}));
            g.fill(new Ellipse2D.Double(p[0] - r, p[1] - r, r * 2, r * 2));
            g.setColor(new Color(230, 248, 255, 150));
            g.draw(new Ellipse2D.Double(p[0] - r, p[1] - r, r * 2, r * 2));
        }

        // หัว
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.rotate(heading);
        double open = mouthOpen > 0 ? 0.35 : 0.08;
        Path2D upper = new Path2D.Double();
        upper.moveTo(-26, -20);
        upper.lineTo(30, -6 - open * 20);
        upper.lineTo(40, 0 - open * 10);
        upper.lineTo(-22, 4);
        upper.closePath();
        Path2D lower = new Path2D.Double();
        lower.moveTo(-22, 4);
        lower.lineTo(36, 4 + open * 26);
        lower.lineTo(-24, 20);
        lower.closePath();
        g.setColor(tint(new Color(70, 130, 200)));
        g.fill(lower);
        g.setColor(tint(new Color(150, 210, 255)));
        g.fill(upper);
        // เขา
        g.setColor(new Color(235, 250, 255));
        Path2D horn = new Path2D.Double();
        horn.moveTo(-18, -16);
        horn.lineTo(-44, -34);
        horn.lineTo(-10, -20);
        horn.closePath();
        g.fill(horn);
        // ตา
        double glow = 0.6 + 0.4 * Math.sin(time * 8);
        Color eye = phase() == 3 ? new Color(255, 80, 80) : new Color(80, 255, 255);
        g.setColor(new Color(eye.getRed(), eye.getGreen(), eye.getBlue(), (int) (150 + 100 * glow)));
        g.fill(new Ellipse2D.Double(4, -14, 10, 7));
        g.setTransform(at);

        // หัวคือจุดอ่อน
        g.setColor(new Color(255, 230, 90, 120 + (int) (80 * Math.sin(time * 6))));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(x - 40, y - 40, 80, 80));
        g.setStroke(new BasicStroke(1f));
    }
}
