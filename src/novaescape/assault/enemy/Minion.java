package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

import java.awt.*;
import java.awt.geom.*;

/** ลูกน้องที่ถูกบอส/ยานแม่เรียกออกมา ไล่ตามผู้เล่นแล้วยิงบ้างเป็นครั้งคราว */
public class Minion extends Enemy {
    private double vx, vy;
    private double fireTimer;

    public Minion(double x, double y, double vx, double vy) {
        super(x, y, 26, 22, 2, 50, 0.08);
        this.vx = vx;
        this.vy = vy;
        this.fireTimer = 1.5 + Math.random() * 2;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        double a = angleToPlayer(g);
        double speed = 150;
        vx += (Math.cos(a) * speed - vx) * Math.min(1, 1.4 * dt);
        vy += (Math.sin(a) * speed - vy) * Math.min(1, 1.4 * dt);
        x += vx * dt;
        y += vy * dt;
        y = Math.max(g.top() + 12, Math.min(g.bottom() - 12, y));

        fireTimer -= dt;
        if (fireTimer <= 0 && onScreen(g)) {
            fireTimer = 2.6 + Math.random() * 1.8;
            g.enemyShoot(x, y, a, 200);
        }
    }

    @Override
    public void onTouchPlayer(AssaultGame g) {
        dead = true;
        g.explode(x, y, 10);
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        double wing = Math.sin(time * 25) * 4;
        g.setColor(tint(new Color(120, 230, 140)));
        g.fill(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
        g.setColor(tint(new Color(70, 160, 90)));
        g.fill(new Ellipse2D.Double(x - 14, y - 4 - wing, 8, 8 + wing));
        g.fill(new Ellipse2D.Double(x + 6, y - 4 - wing, 8, 8 + wing));
        g.setColor(new Color(255, 60, 60));
        g.fill(new Ellipse2D.Double(x - 6, y - 3, 5, 5));
        g.fill(new Ellipse2D.Double(x + 1, y - 3, 5, 5));
    }
}
