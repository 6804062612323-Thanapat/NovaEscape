package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

import java.awt.*;
import java.awt.geom.*;

/** ป้อมปืนติดบนอุกกาบาต ไม่เคลื่อนที่ หันปากกระบอกตามผู้เล่นแล้วยิงชุดละ 3 นัด */
public class Turret extends Enemy {
    private double aim = Math.PI;
    private double fireTimer = 1.2;
    private int burstLeft;
    private double burstTimer;
    private final long seed = (long) (Math.random() * 1000);

    public Turret(double x, double y) {
        super(x, y, 62, 54, 10, 300, 0.5);
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        double want = angleToPlayer(g);
        double diff = Math.atan2(Math.sin(want - aim), Math.cos(want - aim));
        aim += Math.max(-2.5 * dt, Math.min(2.5 * dt, diff));

        if (!onScreen(g)) return;
        if (burstLeft > 0) {
            burstTimer -= dt;
            if (burstTimer <= 0) {
                burstTimer = 0.14;
                burstLeft--;
                g.enemyShoot(x + Math.cos(aim) * 28, y - 8 + Math.sin(aim) * 28, aim, 260);
            }
        } else {
            fireTimer -= dt;
            if (fireTimer <= 0) {
                fireTimer = 2.1 + Math.random();
                burstLeft = 3;
                burstTimer = 0;
            }
        }
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        // ก้อนหิน
        java.util.Random r = new java.util.Random(seed);
        Path2D rock = new Path2D.Double();
        int n = 9;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            double rad = 30 + r.nextDouble() * 8;
            double px = x + Math.cos(a) * rad, py = y + 12 + Math.sin(a) * rad * 0.6;
            if (i == 0) rock.moveTo(px, py); else rock.lineTo(px, py);
        }
        rock.closePath();
        g.setColor(tint(new Color(95, 85, 100)));
        g.fill(rock);
        g.setColor(new Color(60, 52, 66));
        g.fill(new Ellipse2D.Double(x - 12, y + 14, 10, 7));
        g.fill(new Ellipse2D.Double(x + 8, y + 20, 7, 5));

        // ลำกล้อง
        AffineTransform at = g.getTransform();
        g.translate(x, y - 8);
        g.rotate(aim);
        g.setColor(tint(new Color(70, 75, 95)));
        g.fill(new RoundRectangle2D.Double(0, -4, 30, 8, 4, 4));
        g.setColor(new Color(255, 80, 120));
        g.fill(new Rectangle2D.Double(26, -4, 4, 8));
        g.setTransform(at);

        // โดม
        g.setColor(tint(new Color(120, 70, 160)));
        g.fill(new Arc2D.Double(x - 18, y - 24, 36, 32, 0, 180, Arc2D.CHORD));
        g.setColor(burstLeft > 0 ? new Color(255, 90, 90) : new Color(255, 200, 120));
        g.fill(new Ellipse2D.Double(x - 4, y - 18, 8, 8));
    }
}
