package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

import java.awt.*;
import java.awt.geom.*;

/**
 * (ด่าน 2) คริสตัลน้ำแข็งหมุน บินซิกแซก
 * ตัวใหญ่ตายแล้วแตกเป็นเศษเล็ก 3 ชิ้นพุ่งกระจาย (shard = true)
 */
public class IceSpinner extends Enemy {
    private final boolean shard;
    private double vx, vy;
    private double rot;

    public IceSpinner(double x, double y) {
        this(x, y, false, -95, (Math.random() < 0.5 ? -1 : 1) * 110);
    }

    private IceSpinner(double x, double y, boolean shard, double vx, double vy) {
        super(x, y, shard ? 16 : 34, shard ? 16 : 34, shard ? 1 : 4, shard ? 30 : 150, shard ? 0 : 0.2);
        this.shard = shard;
        this.vx = vx;
        this.vy = vy;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        rot += dt * (shard ? 9 : 4);
        x += vx * dt;
        y += vy * dt;
        double lo = g.ceiling(x) + h / 2, hi = g.floor(x) - h / 2;
        if (y < lo) { y = lo; vy = Math.abs(vy); }
        if (y > hi) { y = hi; vy = -Math.abs(vy); }
        if (shard && t > 4) removed = true;
    }

    @Override
    protected void onDeath(AssaultGame g) {
        super.onDeath(g);
        if (!shard) {
            for (int i = 0; i < 3; i++) {
                double a = Math.PI + (i - 1) * 0.6;
                g.spawn(new IceSpinner(x, y, true, Math.cos(a) * 230, Math.sin(a) * 230));
            }
        }
    }

    @Override
    public void onTouchPlayer(AssaultGame g) {
        if (shard) { dead = true; g.sparks(x, y, new Color(200, 240, 255), 6); }
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        double r = w / 2;
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.rotate(rot);
        Path2D star = new Path2D.Double();
        int n = 6;
        for (int i = 0; i < n * 2; i++) {
            double a = Math.PI * i / n;
            double rr = (i % 2 == 0) ? r : r * 0.4;
            double px = Math.cos(a) * rr, py = Math.sin(a) * rr;
            if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
        }
        star.closePath();
        g.setColor(tint(new Color(160, 220, 255, 230)));
        g.fill(star);
        g.setColor(new Color(240, 250, 255));
        g.draw(star);
        g.setColor(new Color(255, 255, 255, 200));
        g.fill(new Ellipse2D.Double(-r * 0.25, -r * 0.25, r * 0.5, r * 0.5));
        g.setTransform(at);
    }
}
