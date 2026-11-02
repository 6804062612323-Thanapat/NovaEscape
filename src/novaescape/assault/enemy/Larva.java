package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

import java.awt.*;
import java.awt.geom.*;

/**
 * (ด่าน 3) ตัวอ่อนเอเลี่ยน / สปอร์
 * LARVA = ไล่กัดเร็ว ๆ ส่ายไปมา, SPORE = ลอยช้า ๆ ไล่ตาม หายไปเองใน 7 วิ
 */
public class Larva extends Enemy {
    public enum Kind { LARVA, SPORE }

    private final Kind kind;
    private double vx, vy;

    public Larva(double x, double y, Kind kind, double vx, double vy) {
        super(x, y, kind == Kind.LARVA ? 24 : 16, kind == Kind.LARVA ? 18 : 16, 1,
                kind == Kind.LARVA ? 40 : 10, kind == Kind.LARVA ? 0.06 : 0);
        this.kind = kind;
        this.vx = vx;
        this.vy = vy;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        double a = angleToPlayer(g);
        double speed = kind == Kind.LARVA ? 190 : 105;
        double wobble = kind == Kind.LARVA ? Math.sin(t * 9) * 0.9 : 0;
        double k = kind == Kind.LARVA ? 2.2 : 1.0;
        vx += (Math.cos(a + wobble) * speed - vx) * Math.min(1, k * dt);
        vy += (Math.sin(a + wobble) * speed - vy) * Math.min(1, k * dt);
        x += vx * dt;
        y += vy * dt;
        y = Math.max(g.ceiling(x) + 10, Math.min(g.floor(x) - 10, y));
        if (kind == Kind.SPORE && t > 7) { removed = true; g.sparks(x, y, new Color(200, 255, 120), 4); }
    }

    @Override
    public void onTouchPlayer(AssaultGame g) {
        dead = true;
        g.sparks(x, y, new Color(180, 255, 100), 8);
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        if (kind == Kind.SPORE) {
            double p = 1 + Math.sin(time * 10 + x) * 0.15;
            g.setColor(new Color(190, 255, 110, 80));
            g.fill(new Ellipse2D.Double(x - 12 * p, y - 12 * p, 24 * p, 24 * p));
            g.setColor(tint(new Color(170, 230, 90)));
            g.fill(new Ellipse2D.Double(x - 7, y - 7, 14, 14));
            g.setColor(new Color(90, 140, 40));
            g.fill(new Ellipse2D.Double(x - 3, y - 3, 3, 3));
            g.fill(new Ellipse2D.Double(x + 1, y + 1, 3, 3));
            return;
        }
        double ang = Math.atan2(vy, vx);
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.rotate(ang);
        double wig = Math.sin(time * 20) * 3;
        g.setColor(tint(new Color(230, 160, 170)));
        g.fill(new Ellipse2D.Double(-14, -7 + wig * 0.3, 14, 12));
        g.fill(new Ellipse2D.Double(-4, -8, 16, 16));
        g.setColor(new Color(120, 30, 50));
        Path2D jaw = new Path2D.Double();
        jaw.moveTo(10, -5); jaw.lineTo(16, -1); jaw.lineTo(10, 0); jaw.lineTo(16, 2); jaw.lineTo(10, 5);
        g.draw(jaw);
        g.setColor(new Color(255, 230, 60));
        g.fill(new Ellipse2D.Double(4, -5, 4, 4));
        g.setTransform(at);
    }
}
