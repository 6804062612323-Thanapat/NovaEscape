package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

import java.awt.*;
import java.awt.geom.*;

/**
 * โดรนจานบินเล็ก
 * SWEEP = บินผ่านจอแบบคลื่น, HOVER = บินมาลอยค้างแล้วยิงใส่
 */
public class Drone extends Enemy {
    public enum Pattern { SWEEP, HOVER }

    private final Pattern pattern;
    private final double baseY;
    private final double hoverScreenX;
    private double fireTimer;

    public Drone(double x, double y, Pattern pattern) {
        super(x, y, 44, 26, 3, 100, 0.18);
        this.pattern = pattern;
        this.baseY = y;
        this.hoverScreenX = 760 + Math.random() * 130; // อยู่หน้าปากกระบอกปืนผู้เล่นเสมอ
        this.fireTimer = 1.0 + Math.random() * 1.5;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        if (pattern == Pattern.SWEEP) {
            x -= 120 * dt;
            y = baseY + Math.sin(t * 2.2) * 70;
        } else {
            double tx = g.camX() + hoverScreenX;
            x += (tx - x) * Math.min(1, 2.0 * dt);
            y = baseY + Math.sin(t * 1.6) * 90;
        }
        y = Math.max(g.top() + 20, Math.min(g.bottom() - 20, y));

        fireTimer -= dt;
        if (fireTimer <= 0 && onScreen(g)) {
            fireTimer = 1.8 + Math.random() * 1.4;
            g.enemyShoot(x - 18, y, angleToPlayer(g), 230);
        }
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        // โดม
        g.setColor(tint(new Color(140, 200, 255, 200)));
        g.fill(new Ellipse2D.Double(x - 11, y - 15, 22, 18));
        // จาน
        g.setPaint(new GradientPaint((float) x, (float) (y - 6), tint(new Color(150, 110, 230)),
                (float) x, (float) (y + 8), tint(new Color(70, 50, 140))));
        g.fill(new Ellipse2D.Double(x - 22, y - 6, 44, 14));
        // ไฟกระพริบ
        for (int i = -1; i <= 1; i++) {
            boolean on = ((int) (time * 6) + i) % 2 == 0;
            g.setColor(on ? new Color(255, 230, 120) : new Color(120, 100, 60));
            g.fill(new Ellipse2D.Double(x + i * 11 - 2.5, y - 1, 5, 5));
        }
    }
}
