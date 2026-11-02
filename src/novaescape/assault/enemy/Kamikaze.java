package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

import java.awt.*;
import java.awt.geom.*;

/** ยานพุ่งชน: บินเข้ามา -> เล็ง (กระพริบแดง) -> พุ่งใส่ผู้เล่นเร็วมาก */
public class Kamikaze extends Enemy {
    private enum Mode { ENTER, AIM, DASH }

    private Mode mode = Mode.ENTER;
    private double modeT;
    private double vx, vy;
    private final double stopScreenX;

    public Kamikaze(double x, double y) {
        super(x, y, 40, 22, 2, 150, 0.12);
        stopScreenX = 700 + Math.random() * 180;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        modeT += dt;
        switch (mode) {
            case ENTER: {
                double tx = g.camX() + stopScreenX;
                x += (tx - x) * Math.min(1, 3 * dt);
                if (Math.abs(tx - x) < 12) { mode = Mode.AIM; modeT = 0; }
                break;
            }
            case AIM:
                y += (g.player().centerY() - y) * Math.min(1, 2.5 * dt);
                if (modeT > 0.7) {
                    mode = Mode.DASH;
                    double a = angleToPlayer(g);
                    vx = Math.cos(a) * 620;
                    vy = Math.sin(a) * 620;
                }
                break;
            case DASH:
                x += vx * dt;
                y += vy * dt;
                if (y < -40 || y > g.bottom() + 40) removed = true;
                break;
            default:
                break;
        }
    }

    @Override
    public void onTouchPlayer(AssaultGame g) {
        dead = true;
        g.explode(x, y, 20);
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        double ang = mode == Mode.DASH ? Math.atan2(vy, vx) : Math.PI;
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.rotate(ang);
        boolean warn = mode == Mode.AIM && ((int) (modeT * 14)) % 2 == 0;

        if (mode == Mode.DASH) {
            g.setColor(new Color(255, 120, 40, 180));
            g.fill(new Ellipse2D.Double(-34, -5, 22, 10));
        }
        Path2D body = new Path2D.Double();
        body.moveTo(22, 0);
        body.lineTo(-10, -12);
        body.lineTo(-16, 0);
        body.lineTo(-10, 12);
        body.closePath();
        g.setColor(tint(warn ? new Color(255, 70, 70) : new Color(190, 40, 60)));
        g.fill(body);
        g.setColor(tint(new Color(255, 200, 80)));
        g.fill(new Ellipse2D.Double(0, -4, 8, 8));
        g.setTransform(at);

        if (mode == Mode.AIM) {
            g.setColor(new Color(255, 60, 60, 90));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{6, 6}, 0));
            g.draw(new Line2D.Double(x - 20, y, x - 620, y));
            g.setStroke(new BasicStroke(1f));
        }
    }
}
