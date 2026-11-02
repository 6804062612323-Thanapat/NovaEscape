package novaescape.assault;

import novaescape.Config;
import novaescape.assault.stage.Stage;
import novaescape.ui.Background;

import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

/**
 * พื้นหลังหลายชั้นสำหรับแมพกว้าง (ยิ่งใกล้ยิ่งเลื่อนเร็ว) วาดในพิกัดจอ
 * ชั้นไกล: ดาว + ดาวเคราะห์ / ชั้นกลาง: ฉากประจำด่าน / ชั้นหน้า: เศษวัตถุบังจอ
 * หน้าตาแต่ละด่านกำหนดด้วย Stage.Theme
 */
public class ParallaxBackground {
    private final Background stars = new Background();
    private final Stage stage;
    private final Stage.Theme theme;

    private static final double FAR = 0.12, MID = 0.4, NEAR = 1.35;

    private final double[] midX, midY, midS;
    private final double[] debrisX, debrisY, debrisS;

    public ParallaxBackground(Stage stage) {
        this.stage = stage;
        this.theme = stage.theme();
        stars.setColors(theme.skyTop, theme.skyBottom, theme.nebula);
        Random r = new Random(42 + stage.number());
        int n = (int) ((stage.length() + 1200) * MID / 230) + 6;   // พอสำหรับแมพยาว
        midX = new double[n]; midY = new double[n]; midS = new double[n];
        for (int i = 0; i < n; i++) {
            midX[i] = 200 + i * 230 + r.nextInt(120);
            midY[i] = r.nextBoolean() ? 120 + r.nextInt(80) : 380 + r.nextInt(100);
            midS[i] = 0.45 + r.nextDouble() * 0.5;
        }
        int m = (int) ((stage.length() + 1200) * NEAR / 340) + 4;
        debrisX = new double[m]; debrisY = new double[m]; debrisS = new double[m];
        for (int i = 0; i < m; i++) {
            debrisX[i] = 600 + i * 340 + r.nextInt(200);
            debrisY[i] = r.nextBoolean() ? Config.HUD_HEIGHT + 10 + r.nextInt(40) : Config.HEIGHT - 10 - r.nextInt(40);
            debrisS[i] = 14 + r.nextInt(26);
        }
    }

    public void update(double dt, double cameraSpeed) {
        stars.update(dt, 20 + cameraSpeed * 0.3);
    }

    public void drawBack(Graphics2D g, double camX, double time) {
        stars.draw(g, time);

        double fx = camX * FAR;
        switch (theme.style) {
            case NOVA:
                planet(g, 700 - fx, 150, 90, new Color(210, 120, 90), new Color(110, 50, 60), true);
                planet(g, 1350 - fx, 420, 46, new Color(120, 200, 190), new Color(40, 90, 110), false);
                planet(g, 1900 - fx, 130, 140, new Color(170, 120, 220), new Color(70, 40, 120), true);
                break;
            case ICE:
                planet(g, 650 - fx, 170, 150, new Color(220, 245, 255), new Color(90, 150, 200), true);
                planet(g, 1500 - fx, 430, 60, new Color(180, 220, 255), new Color(60, 100, 160), false);
                // วงแหวนน้ำแข็งพาดผ่านฟ้า
                g.setColor(new Color(220, 240, 255, 35));
                g.setStroke(new BasicStroke(26f));
                g.draw(new Line2D.Double(-50, 470 - fx * 0.05, Config.WIDTH + 50, 260 - fx * 0.05));
                g.setStroke(new BasicStroke(1f));
                break;
            case HIVE:
                // ก้อนเนื้อเรืองแสงเต้นตุบ ๆ
                for (int i = 0; i < 4; i++) {
                    double px = 300 + i * 520 - fx;
                    double pulse = 1 + Math.sin(time * 1.5 + i) * 0.06;
                    planet(g, px, 150 + (i % 2) * 260, (70 + i * 15) * pulse,
                            new Color(200, 70, 110), new Color(80, 15, 40), false);
                }
                break;
            default:
                break;
        }

        // สีประจำเขต
        double[] starts = stage.sectorStarts();
        Color[] tints = theme.sectorTints;
        for (int i = starts.length - 1; i >= 1; i--) {
            if (camX >= starts[i] - 400) {
                double a = Math.min(1, (camX - (starts[i] - 400)) / 400);
                Color c = tints[Math.min(i, tints.length - 1)];
                if (c != null) {
                    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (45 * a)));
                    g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
                }
                break;
            }
        }
        if (camX >= stage.arenaX() - 600) {
            double a = Math.min(1, (camX - (stage.arenaX() - 600)) / 600);
            Color c = theme.bossTint;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (50 * a)));
            g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
        }

        // ชั้นกลาง
        double mx = camX * MID;
        for (int i = 0; i < midX.length; i++) {
            double sx = midX[i] - mx;
            if (sx < -200 || sx > Config.WIDTH + 200) continue;
            switch (theme.style) {
                case NOVA: island(g, sx, midY[i], midS[i], time, i); break;
                case ICE: crystal(g, sx, midY[i], midS[i], i); break;
                case HIVE: tendril(g, sx, midY[i], midS[i], time, i); break;
                default: break;
            }
        }
    }

    /** ชั้นหน้าสุด วาดทับตัวเกม (โปร่งนิดหน่อย) */
    public void drawFront(Graphics2D g, double camX) {
        double nx = camX * NEAR;
        g.setColor(theme.debris);
        for (int i = 0; i < debrisX.length; i++) {
            double sx = debrisX[i] - nx;
            if (sx < -60 || sx > Config.WIDTH + 60) continue;
            double s = debrisS[i];
            Path2D p = new Path2D.Double();
            p.moveTo(sx - s, debrisY[i]);
            p.lineTo(sx - s * 0.3, debrisY[i] - s * 0.7);
            p.lineTo(sx + s * 0.8, debrisY[i] - s * 0.4);
            p.lineTo(sx + s, debrisY[i] + s * 0.3);
            p.lineTo(sx, debrisY[i] + s * 0.6);
            p.closePath();
            g.fill(p);
        }
    }

    private static void planet(Graphics2D g, double x, double y, double r, Color light, Color dark, boolean ring) {
        if (x < -r * 2 || x > Config.WIDTH + r * 2) return;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x - r * 0.4, y - r * 0.4), (float) (r * 1.6),
                new float[]{0f, 1f}, new Color[]{withAlpha(light, 170), withAlpha(dark, 170)}));
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        if (ring) {
            g.setColor(withAlpha(light, 90));
            g.setStroke(new BasicStroke((float) (r * 0.08)));
            g.draw(new Ellipse2D.Double(x - r * 1.7, y - r * 0.3, r * 3.4, r * 0.6));
            g.setStroke(new BasicStroke(1f));
        }
    }

    private static void island(Graphics2D g, double x, double y, double s, double time, int seed) {
        Path2D p = new Path2D.Double();
        p.moveTo(x - 70 * s, y);
        p.quadTo(x - 30 * s, y - 8 * s, x + 70 * s, y);
        p.lineTo(x + 45 * s, y + 22 * s);
        p.lineTo(x + 15 * s, y + 30 * s);
        p.lineTo(x - 5 * s, y + 52 * s);
        p.lineTo(x - 25 * s, y + 28 * s);
        p.lineTo(x - 55 * s, y + 18 * s);
        p.closePath();
        g.setColor(new Color(25, 30, 60, 130));
        g.fill(p);
        g.setColor(new Color(35, 40, 80, 150));
        g.fill(new Rectangle2D.Double(x - 12 * s, y - 60 * s, 24 * s, 60 * s));
        g.fill(new Ellipse2D.Double(x - 26 * s, y - 75 * s, 52 * s, 22 * s));
        boolean on = ((int) (time * 2) + seed) % 3 != 0;
        g.setColor(on ? new Color(120, 255, 180, 180) : new Color(60, 120, 90, 150));
        g.fill(new Ellipse2D.Double(x - 4 * s, y - 70 * s, 8 * s, 8 * s));
    }

    private static void crystal(Graphics2D g, double x, double y, double s, int seed) {
        Random r = new Random(seed);
        int n = 3 + r.nextInt(3);
        for (int i = 0; i < n; i++) {
            double ox = (i - n / 2.0) * 22 * s;
            double h = (60 + r.nextInt(70)) * s;
            double w = (14 + r.nextInt(10)) * s;
            Path2D p = new Path2D.Double();
            p.moveTo(x + ox - w, y + 30 * s);
            p.lineTo(x + ox - w * 0.6, y + 30 * s - h * 0.7);
            p.lineTo(x + ox, y + 30 * s - h);
            p.lineTo(x + ox + w * 0.6, y + 30 * s - h * 0.7);
            p.lineTo(x + ox + w, y + 30 * s);
            p.closePath();
            g.setColor(new Color(170, 220, 255, 70));
            g.fill(p);
            g.setColor(new Color(230, 250, 255, 90));
            g.draw(p);
        }
    }

    private static void tendril(Graphics2D g, double x, double y, double s, double time, int seed) {
        boolean fromTop = y < 300;
        double baseY = fromTop ? Config.HUD_HEIGHT : Config.HEIGHT;
        double tipY = fromTop ? y + 60 * s : y - 20 * s;
        double sway = Math.sin(time * 0.8 + seed) * 25 * s;
        Path2D p = new Path2D.Double();
        p.moveTo(x - 22 * s, baseY);
        p.quadTo(x - 10 * s + sway, (baseY + tipY) / 2, x + sway * 1.5, tipY);
        p.quadTo(x + 10 * s + sway, (baseY + tipY) / 2, x + 22 * s, baseY);
        p.closePath();
        g.setColor(new Color(90, 20, 50, 110));
        g.fill(p);
        g.setColor(new Color(255, 120, 170, 90));
        g.fill(new Ellipse2D.Double(x + sway * 1.5 - 5 * s, tipY - 5 * s, 10 * s, 10 * s));
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
