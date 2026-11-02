package novaescape.entity;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Random;

/** อนุภาคสำหรับเอฟเฟกต์ระเบิด/ประกายไฟ */
public class Particle {
    private static final Random RNG = new Random();

    double x, y, vx, vy, life, maxLife, size;
    Color color;

    public Particle(double x, double y, double vx, double vy, double life, double size, Color color) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = this.maxLife = life;
        this.size = size;
        this.color = color;
    }

    /** สร้างเอฟเฟกต์ระเบิดที่ตำแหน่ง (x, y) */
    public static void explode(List<Particle> out, double x, double y, int count) {
        Color[] palette = {new Color(255, 230, 120), new Color(255, 150, 50),
                new Color(255, 80, 40), new Color(230, 230, 240)};
        for (int i = 0; i < count; i++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            double s = 60 + RNG.nextDouble() * 260;
            out.add(new Particle(x, y, Math.cos(a) * s, Math.sin(a) * s,
                    0.4 + RNG.nextDouble() * 0.7, 3 + RNG.nextDouble() * 5,
                    palette[RNG.nextInt(palette.length)]));
        }
    }

    public void update(double dt) {
        x += vx * dt;
        y += vy * dt;
        vx *= 0.96;
        vy *= 0.96;
        life -= dt;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void draw(Graphics2D g) {
        float a = (float) Math.max(0, life / maxLife);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * a)));
        double s = size * (0.5 + a * 0.5);
        g.fill(new Ellipse2D.Double(x - s / 2, y - s / 2, s, s));
    }
}
