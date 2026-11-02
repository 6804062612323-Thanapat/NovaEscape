package novaescape.entity;

import novaescape.Config;
import novaescape.Difficulty;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.Random;

/**
 * ยานของมนุษย์ต่างดาว (จานบิน) อยู่ทางขวาของจอ ยิงเลเซอร์ออกมาจากลำแสง
 */
public class AlienShip {
    /** ตำแหน่ง x ที่เลเซอร์ถูกปล่อยออกมา (ปากลำแสง) */
    public static final double MOUTH_X = Config.WIDTH - 215;

    public double centerY;
    public double disabledTimer;     // > 0 = โดนไอเทมโจมตี หยุดยิง
    private double fireCooldown;
    private double phase;
    private final Random rng = new Random();

    public AlienShip() {
        reset();
    }

    public void reset() {
        centerY = playCenter();
        disabledTimer = 0;
        fireCooldown = 0.6;
        phase = 0;
    }

    private static double playCenter() {
        return (Config.HEIGHT + Config.HUD_HEIGHT) / 2.0;
    }

    public boolean isDisabled() {
        return disabledTimer > 0;
    }

    /**
     * @param canFire false ระหว่างนับถอยหลังก่อนเริ่มตา
     */
    public void update(double dt, Difficulty diff, PlayerShip player, List<Laser> lasers, boolean canFire) {
        phase += dt * diff.alienBobSpeed();
        centerY = playCenter() + Math.sin(phase) * 110;

        if (disabledTimer > 0) {
            disabledTimer = Math.max(0, disabledTimer - dt);
            return;
        }
        if (!canFire) return;

        fireCooldown -= dt;
        if (fireCooldown <= 0) {
            fireCooldown = diff.fireInterval() * (0.75 + rng.nextDouble() * 0.5);
            fireVolley(diff, player, lasers);
        }
    }

    private void fireVolley(Difficulty diff, PlayerShip player, List<Laser> lasers) {
        novaescape.audio.Sound.play(novaescape.audio.Sfx.ENEMY_SHOT);
        double top = Config.HUD_HEIGHT + 10;
        double bottom = Config.HEIGHT - 10;
        int n = diff.lasersPerVolley();
        for (int i = 0; i < n; i++) {
            double y;
            if (rng.nextDouble() < diff.aimedChance()) {
                // เล็งไปที่ผู้เล่น (คลาดเคลื่อนนิดหน่อย)
                y = player.centerY() + (rng.nextDouble() - 0.5) * 40;
            } else {
                y = top + rng.nextDouble() * (bottom - top);
            }
            y = Math.max(top, Math.min(bottom, y));
            double length = 60 + rng.nextDouble() * 70;
            double speed = diff.laserSpeed() * (0.9 + rng.nextDouble() * 0.25);
            // ยิงหลายเส้นพร้อมกันให้เหลื่อมกันเล็กน้อย
            lasers.add(new Laser(MOUTH_X + i * 35, y, length, speed));
        }
    }

    public void draw(Graphics2D g, double time) {
        double cy = centerY;
        int w = Config.WIDTH;
        boolean off = isDisabled();

        // ลำแสง (รูปสี่เหลี่ยมคางหมู)
        Path2D beam = new Path2D.Double();
        beam.moveTo(w - 100, cy - 72);
        beam.lineTo(MOUTH_X, cy - 120);
        beam.lineTo(MOUTH_X, cy + 120);
        beam.lineTo(w - 100, cy + 72);
        beam.closePath();
        int beamAlpha = off ? 35 : 215;
        g.setPaint(new GradientPaint((float) (w - 100), 0f, new Color(255, 255, 255, beamAlpha),
                (float) MOUTH_X, 0f, new Color(175, 185, 230, (int) (beamAlpha * 0.75))));
        g.fill(beam);

        // ตัวโดม (หันออกไปทางขวา)
        g.setColor(off ? new Color(60, 60, 80) : new Color(40, 55, 120));
        g.fill(new Ellipse2D.Double(w - 78, cy - 55, 70, 110));

        // จานบิน (วงรีแนวตั้ง)
        Color discA = off ? new Color(95, 95, 110) : new Color(110, 120, 220);
        Color discB = off ? new Color(60, 60, 75) : new Color(55, 60, 150);
        g.setPaint(new GradientPaint((float) (w - 110), 0f, discA, (float) (w - 60), 0f, discB));
        g.fill(new Ellipse2D.Double(w - 112, cy - 150, 52, 300));
        g.setColor(off ? new Color(120, 120, 135) : new Color(170, 180, 255));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(w - 104, cy - 135, 36, 270));
        g.setStroke(new BasicStroke(1f));

        // ประกายดาวบนจาน
        if (!off) {
            double[] offs = {-95, -30, 35, 100};
            for (int i = 0; i < offs.length; i++) {
                double tw = 0.6 + 0.4 * Math.sin(time * 4 + i * 1.7);
                drawSparkle(g, w - 86, cy + offs[i], 7 * tw);
            }
        } else {
            // ประกายไฟฟ้าช็อตตอนโดนโจมตี
            g.setColor(new Color(255, 200, 80));
            for (int i = 0; i < 3; i++) {
                double sy = cy + (Math.random() - 0.5) * 240;
                double sx = w - 100 + Math.random() * 30;
                g.draw(new Line2D.Double(sx, sy, sx + 8, sy + 6));
                g.draw(new Line2D.Double(sx + 8, sy + 6, sx + 2, sy + 12));
            }
        }
    }

    private static void drawSparkle(Graphics2D g, double x, double y, double r) {
        Path2D s = new Path2D.Double();
        s.moveTo(x, y - r);
        s.lineTo(x + r * 0.25, y - r * 0.25);
        s.lineTo(x + r, y);
        s.lineTo(x + r * 0.25, y + r * 0.25);
        s.lineTo(x, y + r);
        s.lineTo(x - r * 0.25, y + r * 0.25);
        s.lineTo(x - r, y);
        s.lineTo(x - r * 0.25, y - r * 0.25);
        s.closePath();
        g.setColor(Color.WHITE);
        g.fill(s);
    }
}
