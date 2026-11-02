package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;
import novaescape.assault.Bullet;

import java.awt.*;
import java.awt.geom.*;

/**
 * (ด่าน 3) ทหารรังเอเลี่ยน เกราะหนา
 * ลอยค้างแล้วพ่นกรด เป็นระยะจะเรืองแดง (เตือน) แล้วพุ่งชาร์จตามแนวผู้เล่น
 * พุ่งเลยจอไปแล้วจะวนกลับมาจากด้านขวาอีกรอบ
 */
public class HiveGuard extends Enemy {
    private enum Mode { HOVER, WINDUP, CHARGE }

    private Mode mode = Mode.HOVER;
    private double modeT;
    private double chargeTimer = 3.0, acidTimer = 1.5;
    private final double hoverX;

    public HiveGuard(double x, double y) {
        super(x, y, 64, 46, 16, 400, 0.5);
        this.hoverX = 0.74 + Math.random() * 0.14;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        modeT += dt;
        switch (mode) {
            case HOVER: {
                double tx = g.camX() + g.viewW() * hoverX;
                x += (tx - x) * Math.min(1, 2 * dt);
                double ty = g.player().centerY() + Math.sin(t) * 60;
                y += (ty - y) * Math.min(1, 0.8 * dt);
                acidTimer -= dt;
                if (acidTimer <= 0 && onScreen(g)) {
                    acidTimer = 2.4;
                    double a = angleToPlayer(g);
                    g.enemyBullet(Bullet.acid(x - 30, y, Math.cos(a) * 220, Math.sin(a) * 220 - 90));
                    g.enemyBullet(Bullet.acid(x - 30, y, Math.cos(a) * 180, Math.sin(a) * 180 - 140));
                }
                chargeTimer -= dt;
                if (chargeTimer <= 0 && onScreen(g)) { mode = Mode.WINDUP; modeT = 0; }
                break;
            }
            case WINDUP:
                y += (g.player().centerY() - y) * Math.min(1, 3 * dt);
                x += 30 * dt;
                if (modeT > 0.7) { mode = Mode.CHARGE; modeT = 0; }
                break;
            case CHARGE:
                x -= 620 * dt;
                if (x < g.camX() - 100) {
                    // วนกลับมาทางขวา
                    x = g.camX() + g.viewW() + 80;
                    mode = Mode.HOVER;
                    chargeTimer = 3.5;
                }
                break;
            default:
                break;
        }
        y = Math.max(g.ceiling(x) + h / 2, Math.min(g.floor(x) - h / 2, y));
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        boolean warn = mode == Mode.WINDUP && ((int) (modeT * 14)) % 2 == 0;
        // ขา
        g.setColor(new Color(60, 20, 35));
        g.setStroke(new BasicStroke(3f));
        for (int i = -1; i <= 1; i++) {
            double k = Math.sin(time * 12 + i) * 4;
            g.draw(new Line2D.Double(x + i * 14, y + 12, x + i * 18 - 6, y + 26 + k));
        }
        g.setStroke(new BasicStroke(1f));
        // เกราะ
        Path2D shell = new Path2D.Double();
        shell.moveTo(x - 34, y + 4);
        shell.quadTo(x - 20, y - 30, x + 26, y - 20);
        shell.lineTo(x + 34, y + 8);
        shell.quadTo(x, y + 20, x - 34, y + 4);
        shell.closePath();
        g.setPaint(new GradientPaint((float) x, (float) (y - 25), tint(warn ? new Color(255, 90, 90) : new Color(150, 60, 90)),
                (float) x, (float) (y + 15), tint(new Color(70, 20, 45))));
        g.fill(shell);
        g.setColor(new Color(40, 10, 25));
        g.draw(shell);
        // ลายเกราะ
        g.setColor(new Color(210, 120, 150, 140));
        g.draw(new Line2D.Double(x - 10, y - 20, x - 4, y + 10));
        g.draw(new Line2D.Double(x + 8, y - 20, x + 10, y + 10));
        // หัว + เขี้ยว
        g.setColor(tint(new Color(110, 40, 60)));
        g.fill(new Ellipse2D.Double(x - 44, y - 10, 22, 20));
        g.setColor(new Color(240, 230, 200));
        g.fill(new Rectangle2D.Double(x - 50, y - 6, 8, 3));
        g.fill(new Rectangle2D.Double(x - 50, y + 3, 8, 3));
        g.setColor(mode == Mode.HOVER ? new Color(255, 220, 80) : new Color(255, 60, 60));
        g.fill(new Ellipse2D.Double(x - 40, y - 6, 6, 6));
        if (mode == Mode.CHARGE) {
            g.setColor(new Color(255, 120, 120, 120));
            g.fill(new Ellipse2D.Double(x + 30, y - 8, 40, 16));
        }
    }
}
