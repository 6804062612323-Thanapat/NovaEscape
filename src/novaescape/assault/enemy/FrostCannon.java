package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;
import novaescape.assault.Bullet;

import java.awt.*;
import java.awt.geom.*;

/** (ด่าน 2) ปืนใหญ่น้ำแข็งบนก้อนน้ำแข็ง ยิงลูกเยือกแข็ง (โดนแล้วยานช้าลง 2.5 วิ) */
public class FrostCannon extends Enemy {
    private double aim = Math.PI;
    private double fireTimer = 1.4;
    private double charge;

    public FrostCannon(double x, double y) {
        super(x, y, 64, 56, 10, 300, 0.5);
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        double want = angleToPlayer(g);
        double diff = Math.atan2(Math.sin(want - aim), Math.cos(want - aim));
        aim += Math.max(-2 * dt, Math.min(2 * dt, diff));
        if (!onScreen(g)) return;
        fireTimer -= dt;
        charge = Math.max(0, 1 - fireTimer / 0.8);
        if (fireTimer <= 0) {
            fireTimer = 2.6;
            g.enemyBullet(Bullet.frost(x + Math.cos(aim) * 34, y - 8 + Math.sin(aim) * 34, aim, 175));
        }
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        // ก้อนน้ำแข็ง
        Path2D ice = new Path2D.Double();
        ice.moveTo(x - 34, y + 8);
        ice.lineTo(x - 22, y - 6);
        ice.lineTo(x + 20, y - 8);
        ice.lineTo(x + 34, y + 10);
        ice.lineTo(x + 14, y + 30);
        ice.lineTo(x - 18, y + 28);
        ice.closePath();
        g.setColor(tint(new Color(170, 215, 245)));
        g.fill(ice);
        g.setColor(new Color(235, 250, 255));
        g.draw(ice);

        AffineTransform at = g.getTransform();
        g.translate(x, y - 8);
        g.rotate(aim);
        g.setColor(tint(new Color(70, 100, 140)));
        g.fill(new RoundRectangle2D.Double(0, -6, 36, 12, 6, 6));
        g.setColor(new Color(150, 230, 255, (int) (80 + 170 * charge)));
        g.fill(new Ellipse2D.Double(30, -7, 14, 14));
        g.setTransform(at);

        g.setColor(tint(new Color(60, 85, 120)));
        g.fill(new Arc2D.Double(x - 18, y - 24, 36, 32, 0, 180, Arc2D.CHORD));
    }
}
