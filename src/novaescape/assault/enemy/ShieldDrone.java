package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;
import novaescape.assault.Bullet;

import java.awt.*;
import java.awt.geom.*;

/**
 * (ด่าน 2) โดรนมีโล่ด้านหน้า
 * โล่กันกระสุนที่ยิงมาตรง ๆ แนวนอน — ต้องใช้ Spread (ยิงเฉียง), Laser (ทะลุ), Rocket/ปืนใหญ่ (ระเบิด),
 * ป้อมปืนรถถัง (เล็งเฉียง) หรือยิงใส่โล่จนแตก
 */
public class ShieldDrone extends Enemy {
    private static final double SHIELD_MAX = 8;
    private double shield = SHIELD_MAX;
    private final double baseY;
    private final double hoverX;
    private double fireTimer = 1.5 + Math.random();

    public ShieldDrone(double x, double y) {
        super(x, y, 40, 32, 4, 250, 0.3);
        this.baseY = y;
        this.hoverX = 0.72 + Math.random() * 0.18;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        double tx = g.camX() + g.viewW() * hoverX;
        x += (tx - x) * Math.min(1, 1.6 * dt);
        y = baseY + Math.sin(t * 1.3) * 70;
        y = Math.max(g.ceiling(x) + 24, Math.min(g.floor(x) - 24, y));
        fireTimer -= dt;
        if (fireTimer <= 0 && onScreen(g)) {
            fireTimer = 2.2 + Math.random();
            g.enemyShoot(x - 26, y, angleToPlayer(g), 240);
        }
    }

    @Override
    public boolean blocksBullet(Bullet b) {
        if (shield <= 0 || b.pierce || b.explosive) return false;
        // ยิงมาจากด้านหน้าแบบเกือบแนวนอน = โดนโล่
        return b.vx > 0 && Math.abs(b.vy) < Math.abs(b.vx) * 0.2;
    }

    @Override
    public void onBlocked(Bullet b, AssaultGame g) {
        shield -= b.damage;
        if (shield <= 0) {
            g.sparks(x - 22, y, new Color(150, 230, 255), 16);
            g.toast("SHIELD BROKEN!");
        }
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        // ตัว
        g.setColor(tint(new Color(70, 110, 160)));
        g.fill(new Ellipse2D.Double(x - 18, y - 13, 36, 26));
        g.setColor(tint(new Color(200, 230, 255)));
        g.fill(new Ellipse2D.Double(x - 4, y - 7, 14, 14));
        g.setColor(new Color(255, 80, 80));
        g.fill(new Ellipse2D.Double(x - 1, y - 3, 6, 6));
        // ใบพัด
        double spin = time * 20;
        g.setColor(new Color(180, 200, 220, 160));
        g.fill(new Ellipse2D.Double(x - 14, y - 22, 28 * Math.abs(Math.cos(spin)), 5));
        // โล่
        if (shield > 0) {
            float a = (float) (0.35 + 0.65 * shield / SHIELD_MAX);
            Arc2D arc = new Arc2D.Double(x - 34, y - 28, 40, 56, 100, 160, Arc2D.OPEN);
            g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(120, 220, 255, (int) (90 * a)));
            g.draw(arc);
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(200, 245, 255, (int) (255 * a)));
            g.draw(arc);
            g.setStroke(new BasicStroke(1f));
        }
    }
}
