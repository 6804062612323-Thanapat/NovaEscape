package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

import java.awt.*;
import java.awt.geom.*;

/**
 * บอสท้ายด่าน: MOTHERSHIP (ยานต่างดาวจากโหมด CLASSIC แต่ใหญ่และโหดกว่า)
 * เฟส 1 (HP > 66%)  : ยิงเลเซอร์เป็นชุด
 * เฟส 2 (HP 33-66%) : + เรียกลูกน้อง + ยิงกระสุนเล็งเป้า
 * เฟส 3 (HP < 33%)  : + กระสุนหมุนเป็นเกลียว + ลำแสงกวาด (มีเส้นเตือนก่อน 1 วิ)
 */
public class MothershipBoss extends BossEnemy {
    public static final double MAX_HP = 420;

    private final double homeX;
    private double laserTimer = 1.5;
    private double summonTimer = 2.0;
    private double aimedTimer = 2.5;
    private double spiralTimer;
    private double spiralAngle;
    private double beamTimer = 3.0;
    private double beamWarn;      // > 0 = กำลังเตือน
    private double beamFire;      // > 0 = กำลังยิงลำแสง
    private double beamY;
    private double summonGlow;
    private boolean entered;

    public MothershipBoss(AssaultGame g) {
        super(g.arenaX() + g.viewW() + 200, g.centerY(), 180, 280, MAX_HP, 20000);
        this.homeX = g.arenaX() + g.viewW() - 150;
        this.touchDamage = true;
    }

    @Override
    public String displayName() { return "MOTHERSHIP"; }

    @Override
    public int phase() {
        double r = hp / maxHp;
        return r > 0.66 ? 1 : r > 0.33 ? 2 : 3;
    }

    private double mouthX() { return x - 150; }

    @Override
    public Rectangle2D hitbox() {
        // ลำแสงด้านหน้าก็โดนได้ (เป้าใหญ่ ยิงง่าย)
        return new Rectangle2D.Double(x - 150, y - 120, 190, 240);
    }

    @Override
    public void damage(double d, AssaultGame g) {
        if (!entered) return; // ยังเข้าฉากไม่เสร็จ ยิงไม่เข้า
        super.damage(d, g);
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        if (!entered) {
            x += (homeX - x) * Math.min(1, 1.2 * dt);
            if (Math.abs(homeX - x) < 4) entered = true;
            return;
        }
        int ph = phase();
        double bob = ph == 3 ? 1.3 : 0.8;
        double targetY = g.centerY() + Math.sin(t * bob) * (g.bottom() - g.top()) * 0.28;
        y += (targetY - y) * Math.min(1, 3 * dt);
        if (summonGlow > 0) summonGlow -= dt;

        boolean jammed = g.enemiesJammed();

        // ลำแสงกวาด (เดินต่อแม้โดน EMP ถ้าเริ่มยิงไปแล้ว)
        if (beamFire > 0) {
            beamFire -= dt;
            Rectangle2D beam = new Rectangle2D.Double(g.camX() - 50, beamY - 22, mouthX() - g.camX() + 50, 44);
            if (beam.intersects(g.player().hitbox())) g.player().hit(g, novaescape.assault.Balance.BIG_HIT);
            if (Math.random() < 0.5) g.sparks(mouthX(), beamY, new Color(255, 120, 200), 2);
        } else if (beamWarn > 0) {
            beamWarn -= dt;
            if (beamWarn <= 0) { beamFire = 0.9; g.shake(10); }
        }

        if (jammed) return;

        // เฟส 1+: เลเซอร์
        laserTimer -= dt;
        if (laserTimer <= 0) {
            laserTimer = ph == 1 ? 0.75 : 1.0;
            int n = ph == 1 ? 2 : 1;
            for (int i = 0; i < n; i++) {
                double ly = y + (Math.random() - 0.5) * 260;
                ly = Math.max(g.top() + 10, Math.min(g.bottom() - 10, ly));
                g.enemyLaser(mouthX() - 40, ly, 90, 430);
            }
        }

        if (ph >= 2) {
            summonTimer -= dt;
            if (summonTimer <= 0) {
                summonTimer = ph == 2 ? 5.0 : 6.5;
                summonGlow = 0.8;
                for (int i = -1; i <= 1; i++) {
                    g.spawn(new Minion(mouthX(), y + i * 60, -160, i * 120));
                }
            }
            aimedTimer -= dt;
            if (aimedTimer <= 0) {
                aimedTimer = 2.4;
                double a = angleToPlayer(g);
                for (int i = -2; i <= 2; i++) g.enemyShoot(mouthX(), y, a + i * 0.14, 260);
            }
        }

        if (ph >= 3) {
            spiralTimer -= dt;
            if (spiralTimer <= 0) {
                spiralTimer = 0.11;
                spiralAngle += 0.45;
                // ยิงแค่ครึ่งซ้าย (ฝั่งผู้เล่น)
                double a = Math.PI + Math.sin(spiralAngle) * 1.2;
                g.enemyShoot(mouthX() + 20, y, a, 190);
            }
            if (beamWarn <= 0 && beamFire <= 0) {
                beamTimer -= dt;
                if (beamTimer <= 0) {
                    beamTimer = 5.5;
                    beamWarn = 1.0;
                    beamY = g.player().centerY();
                }
            }
        }
    }


    @Override
    protected void draw(Graphics2D g, double time) {
        // ลำแสงกวาด
        if (beamWarn > 0) {
            boolean on = ((int) (beamWarn * 16)) % 2 == 0;
            g.setColor(new Color(255, 60, 60, on ? 200 : 80));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Line2D.Double(mouthX(), beamY, mouthX() - 2000, beamY));
            g.setStroke(new BasicStroke(1f));
        }
        if (beamFire > 0) {
            double thick = 44 * Math.min(1, beamFire * 4);
            g.setColor(new Color(255, 80, 180, 90));
            g.fill(new Rectangle2D.Double(mouthX() - 2000, beamY - thick, 2000, thick * 2));
            g.setColor(new Color(255, 170, 230));
            g.fill(new Rectangle2D.Double(mouthX() - 2000, beamY - thick / 2, 2000, thick));
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Double(mouthX() - 2000, beamY - thick / 6, 2000, thick / 3));
        }

        double cy = y;
        // ลำแสงหลัก (รูปคางหมู)
        Path2D beam = new Path2D.Double();
        beam.moveTo(x - 20, cy - 95);
        beam.lineTo(mouthX(), cy - 150);
        beam.lineTo(mouthX(), cy + 150);
        beam.lineTo(x - 20, cy + 95);
        beam.closePath();
        Color beamColor = summonGlow > 0 ? new Color(150, 255, 170, 220) : new Color(255, 255, 255, 215);
        g.setPaint(new GradientPaint((float) (x - 20), 0f, beamColor,
                (float) mouthX(), 0f, new Color(175, 185, 230, 160)));
        g.fill(beam);

        // โดม
        g.setColor(tint(new Color(40, 55, 120)));
        g.fill(new Ellipse2D.Double(x + 5, cy - 70, 90, 140));
        // แกนพลัง (เต้นเร็วขึ้นตามเฟส)
        int ph = phase();
        double pulse = 0.5 + 0.5 * Math.sin(time * (3 + ph * 3));
        Color core = ph == 1 ? new Color(120, 200, 255) : ph == 2 ? new Color(255, 200, 80) : new Color(255, 70, 70);
        g.setColor(new Color(core.getRed(), core.getGreen(), core.getBlue(), (int) (120 + 120 * pulse)));
        g.fill(new Ellipse2D.Double(x + 25, cy - 25, 40, 50));

        // จาน
        g.setPaint(new GradientPaint((float) (x - 30), 0f, tint(new Color(110, 120, 220)),
                (float) (x + 40), 0f, tint(new Color(55, 60, 150))));
        g.fill(new Ellipse2D.Double(x - 30, cy - 190, 66, 380));
        g.setColor(tint(new Color(170, 180, 255)));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Double(x - 20, cy - 172, 46, 344));
        g.setStroke(new BasicStroke(1f));
        double[] offs = {-130, -65, 0, 65, 130};
        for (int i = 0; i < offs.length; i++) {
            double tw = 0.6 + 0.4 * Math.sin(time * 4 + i * 1.7);
            double r = 8 * tw;
            double sx = x + 3, sy = cy + offs[i];
            Path2D s = new Path2D.Double();
            s.moveTo(sx, sy - r); s.lineTo(sx + r * .25, sy - r * .25); s.lineTo(sx + r, sy);
            s.lineTo(sx + r * .25, sy + r * .25); s.lineTo(sx, sy + r); s.lineTo(sx - r * .25, sy + r * .25);
            s.lineTo(sx - r, sy); s.lineTo(sx - r * .25, sy - r * .25); s.closePath();
            g.setColor(Color.WHITE);
            g.fill(s);
        }
        // ควันเมื่อเสียหายหนัก
        if (ph >= 2 && Math.random() < 0.3) {
            g.setColor(new Color(60, 60, 70, 120));
            g.fill(new Ellipse2D.Double(x + (Math.random() - 0.5) * 40, cy + (Math.random() - 0.5) * 300, 14, 14));
        }
    }
}
