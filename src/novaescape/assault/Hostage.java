package novaescape.assault;

import novaescape.ui.Text;

import java.awt.*;
import java.awt.geom.*;

/**
 * นักบินอวกาศที่ลอยติดอยู่ (เหมือน POW ใน Metal Slug)
 * บินไปชนเพื่อช่วย จะได้ไอเทม + คะแนน
 */
public class Hostage {
    public double x, y;
    private final double baseY;
    public boolean rescued;
    public boolean gone;
    private double t;
    private double rescueT;

    public Hostage(double x, double y) {
        this.x = x;
        this.y = y;
        this.baseY = y;
        this.t = Math.random() * 5;
    }

    public void update(double dt, AssaultGame g) {
        t += dt;
        if (rescued) {
            rescueT += dt;
            y -= 160 * dt * Math.min(1, rescueT);
            x += 60 * dt;
            if (rescueT > 2.5) gone = true;
        } else {
            y = baseY + Math.sin(t * 1.5) * 10;
            y = Math.max(g.ceiling(x) + 36, Math.min(g.floor(x) - 32, y));
            if (x < g.camX() - 60) gone = true;
        }
    }

    public Rectangle2D hitbox() {
        return new Rectangle2D.Double(x - 22, y - 26, 44, 52);
    }

    public void draw(Graphics2D g) {
        double tilt = rescued ? 0 : Math.sin(t * 1.2) * 0.2;
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.rotate(tilt);

        if (!rescued) {
            // ฟองอากาศที่ขังไว้
            g.setColor(new Color(150, 220, 255, 45));
            g.fill(new Ellipse2D.Double(-30, -34, 60, 66));
            g.setColor(new Color(170, 230, 255, 140));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(-30, -34, 60, 66));
            g.setStroke(new BasicStroke(1f));
        }

        // เป้หลัง
        g.setColor(new Color(170, 175, 190));
        g.fill(new RoundRectangle2D.Double(-14, -6, 8, 18, 4, 4));
        // ตัว
        g.setColor(new Color(240, 240, 245));
        g.fill(new RoundRectangle2D.Double(-9, -6, 18, 22, 8, 8));
        // ขา
        g.fill(new RoundRectangle2D.Double(-8, 12, 7, 12, 4, 4));
        g.fill(new RoundRectangle2D.Double(1, 12, 7, 12, 4, 4));
        // แขน (โบกมือ)
        double wave = rescued ? -1.2 : Math.sin(t * 8) * 0.6 - 0.6;
        AffineTransform a2 = g.getTransform();
        g.translate(8, -2);
        g.rotate(wave);
        g.fill(new RoundRectangle2D.Double(0, -3, 14, 6, 5, 5));
        g.setTransform(a2);
        // หมวก
        g.setColor(new Color(245, 245, 250));
        g.fill(new Ellipse2D.Double(-11, -26, 22, 22));
        g.setColor(new Color(255, 170, 60));
        g.fill(new Ellipse2D.Double(-6, -21, 15, 12));
        g.setColor(new Color(255, 230, 180, 180));
        g.fill(new Ellipse2D.Double(-3, -19, 5, 4));
        // ธงที่อก
        g.setColor(new Color(60, 110, 220));
        g.fill(new Rectangle2D.Double(-4, 0, 6, 4));

        g.setTransform(at);

        if (!rescued && ((int) (t * 3)) % 2 == 0) {
            g.setFont(Text.font(Font.BOLD, 13));
            Text.centered(g, "HELP!", x, y - 46, new Color(255, 230, 120), true);
        }
        if (rescued && rescueT < 1.8) {
            g.setFont(Text.font(Font.BOLD, 15));
            Text.centered(g, "THANK YOU!", x, y - 46, Color.WHITE, true);
        }
    }
}
