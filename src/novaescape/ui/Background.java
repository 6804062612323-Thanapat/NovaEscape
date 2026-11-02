package novaescape.ui;

import novaescape.Config;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Random;

/** พื้นหลังอวกาศ: ไล่สีน้ำเงินเข้ม + ดาวเลื่อนแบบหลายชั้น (parallax) */
public class Background {
    private static final int STAR_COUNT = 140;
    private final double[] sx = new double[STAR_COUNT];
    private final double[] sy = new double[STAR_COUNT];
    private final double[] layer = new double[STAR_COUNT];
    private final Random rng = new Random(7);
    private Color top = new Color(14, 20, 58), bottom = new Color(30, 52, 120);
    private Color nebula = new Color(70, 90, 180);

    /** เปลี่ยนสีพื้นหลัง (แต่ละด่านใช้สีต่างกัน) */
    public void setColors(Color top, Color bottom, Color nebula) {
        this.top = top;
        this.bottom = bottom;
        this.nebula = nebula;
    }

    public Background() {
        for (int i = 0; i < STAR_COUNT; i++) {
            sx[i] = rng.nextDouble() * Config.WIDTH;
            sy[i] = rng.nextDouble() * Config.HEIGHT;
            layer[i] = 0.2 + rng.nextDouble() * 0.8;
        }
    }

    /** @param speed ความเร็วการเลื่อน (0 = นิ่ง) */
    public void update(double dt, double speed) {
        for (int i = 0; i < STAR_COUNT; i++) {
            sx[i] -= speed * layer[i] * dt;
            if (sx[i] < -2) {
                sx[i] = Config.WIDTH + 2;
                sy[i] = rng.nextDouble() * Config.HEIGHT;
            }
        }
    }

    public void draw(Graphics2D g, double time) {
        g.setPaint(new GradientPaint(0, 0, top, 0, Config.HEIGHT, bottom));
        g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);

        // หมอกเนบิวลาจาง ๆ
        g.setPaint(new RadialGradientPaint(Config.WIDTH * 0.35f, Config.HEIGHT * 0.6f, 380f,
                new float[]{0f, 1f}, new Color[]{new Color(nebula.getRed(), nebula.getGreen(), nebula.getBlue(), 60),
                        new Color(nebula.getRed(), nebula.getGreen(), nebula.getBlue(), 0)}));
        g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);

        for (int i = 0; i < STAR_COUNT; i++) {
            double twinkle = 0.65 + 0.35 * Math.sin(time * 3 + i);
            int a = (int) (90 + 160 * layer[i] * twinkle);
            g.setColor(new Color(255, 255, 255, Math.min(255, a)));
            double s = 1 + layer[i] * 1.8;
            g.fill(new Ellipse2D.Double(sx[i], sy[i], s, s));
        }
    }
}
