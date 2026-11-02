package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

/** อุกกาบาต ลอยไปมา ชนแล้วเจ็บ ยิงแตกได้ (มีโอกาสดรอปของ) */
public class Asteroid extends Enemy {
    private final double vx, vy, spin;
    private double rot;
    private final double radius;
    private final double[] shape;
    private Color light = new Color(150, 135, 140), dark = new Color(70, 60, 75);

    /** เปลี่ยนสีหิน (เช่น ด่านน้ำแข็งใช้สีฟ้า) */
    public Asteroid colors(Color light, Color dark) {
        this.light = light;
        this.dark = dark;
        return this;
    }

    public Asteroid(double x, double y, double radius, double vx, double vy) {
        super(x, y, radius * 1.6, radius * 1.6, Math.max(3, radius / 5), 50, 0.25);
        this.radius = radius;
        this.vx = vx;
        this.vy = vy;
        Random r = new Random();
        this.spin = (r.nextDouble() - 0.5) * 1.5;
        this.shape = new double[10];
        for (int i = 0; i < shape.length; i++) shape[i] = 0.75 + r.nextDouble() * 0.3;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        x += vx * dt;
        y += vy * dt;
        rot += spin * dt;
        if (y < g.top() - radius * 2 || y > g.bottom() + radius * 2) removed = true;
    }

    @Override
    public void onTouchPlayer(AssaultGame g) {
        damage(2, g);
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < shape.length; i++) {
            double a = rot + Math.PI * 2 * i / shape.length;
            double rr = radius * shape[i];
            double px = x + Math.cos(a) * rr, py = y + Math.sin(a) * rr;
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.closePath();
        g.setPaint(new GradientPaint((float) (x - radius), (float) (y - radius), tint(light),
                (float) (x + radius), (float) (y + radius), tint(dark)));
        g.fill(p);
        g.setColor(new Color(55, 48, 60));
        double cr = radius * 0.25;
        g.fill(new Ellipse2D.Double(x - radius * 0.35 + Math.cos(rot) * 4, y - radius * 0.2, cr, cr * 0.8));
        g.fill(new Ellipse2D.Double(x + radius * 0.15, y + radius * 0.2 + Math.sin(rot) * 3, cr * 0.7, cr * 0.6));
    }
}
