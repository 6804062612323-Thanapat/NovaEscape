package novaescape.assault;

import novaescape.assault.enemy.Enemy;

import java.awt.*;
import java.awt.geom.*;
import java.util.HashSet;
import java.util.Set;

/**
 * กระสุนทุกชนิดในโหมด ASSAULT ทั้งของผู้เล่น (friendly) และของศัตรู
 * ตำแหน่ง (x, y) คือจุดกึ่งกลางของกระสุน
 */
public class Bullet {
    public enum Style {
        PISTOL, HEAVY, SPREAD, LASER, ROCKET, CANNON,     // ของผู้เล่น (CANNON = ปืนใหญ่รถถัง)
        ORB, ENEMY_LASER, FROST, ICEBOMB, ICICLE, ACID    // ของศัตรู
    }

    public double x, y, vx, vy;
    public final boolean friendly;
    public final Style style;
    public double damage = 1;
    public double size = 4;       // รัศมีโดยประมาณ
    public double len = 0;        // ความยาว (เฉพาะเลเซอร์)
    public boolean pierce;        // ทะลุศัตรู
    public boolean homing;        // ติดตามเป้า
    public boolean explosive;     // ระเบิดเมื่อโดน
    public boolean dead;
    public double age;
    public boolean freezes;            // โดนแล้วยานช้าลง (ลูกเยือกแข็ง)
    public double fuse = -1;           // > 0 = แตกกระจายเมื่อครบเวลา (ระเบิดน้ำแข็ง)
    public double gravity;             // แรงดึงลง (กระสุนตก)
    public double splashRadius = 85;   // รัศมีระเบิด (กระสุน explosive)
    public double splashDamage = 3;
    public final Set<Enemy> alreadyHit = new HashSet<>();

    public Bullet(double x, double y, double vx, double vy, boolean friendly, Style style) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.friendly = friendly;
        this.style = style;
    }

    // ---------- กระสุนศัตรู ----------
    public static Bullet orb(double x, double y, double angle, double speed) {
        Bullet b = new Bullet(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, false, Style.ORB);
        b.size = 6;
        return b;
    }

    public static Bullet frost(double x, double y, double angle, double speed) {
        Bullet b = new Bullet(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, false, Style.FROST);
        b.size = 10;
        b.freezes = true;
        return b;
    }

    public static Bullet iceBomb(double x, double y, double vx) {
        Bullet b = new Bullet(x, y, vx, 40, false, Style.ICEBOMB);
        b.size = 8;
        b.gravity = 260;
        b.fuse = 1.0 + Math.random() * 0.4;
        return b;
    }

    public static Bullet icicle(double x, double y, double vy) {
        Bullet b = new Bullet(x, y, 0, vy, false, Style.ICICLE);
        b.size = 5;
        b.len = 26;
        return b;
    }

    public static Bullet acid(double x, double y, double vx, double vy) {
        Bullet b = new Bullet(x, y, vx, vy, false, Style.ACID);
        b.size = 6;
        b.gravity = 220;
        return b;
    }

    public static Bullet enemyLaser(double x, double y, double len, double speed) {
        Bullet b = new Bullet(x, y, -speed, 0, false, Style.ENEMY_LASER);
        b.len = len;
        b.size = 2.5;
        return b;
    }

    public void update(double dt, AssaultGame g) {
        age += dt;
        if (homing && age > 0.12) steer(dt, g);
        vy += gravity * dt;
        x += vx * dt;
        y += vy * dt;

        if ((style == Style.ROCKET || style == Style.CANNON) && Math.random() < 0.6) {
            g.smoke(x - 8, y);
        }
        if (fuse > 0 && age >= fuse) {
            // ระเบิดน้ำแข็งแตกเป็นเศษ 6 ทิศ
            dead = true;
            g.sparks(x, y, new Color(200, 240, 255), 8);
            for (int i = 0; i < 6; i++) {
                Bullet s = Bullet.orb(x, y, Math.PI * 2 * i / 6 + 0.3, 170);
                s.size = 4;
                g.addEnemyBullet(s);
            }
            return;
        }
        double cam = g.camX();
        if (x < cam - 120 || x > cam + g.viewW() + 120 || y < -80 || y > g.bottom() + 80 || age > 7) {
            dead = true;
            return;
        }
        // ชนผนังอุโมงค์ (เฉพาะกระสุนศัตรู — กระสุนเราทะลุผนังได้ จะได้ยิงมอนที่เกาะผนังโดน)
        if (!friendly && g.hasWalls() && style != Style.ICICLE && (y < g.ceiling(x) || y > g.floor(x))) {
            dead = true;
            g.sparks(x, y, new Color(255, 170, 200), 3);
        }
    }

    private void steer(double dt, AssaultGame g) {
        Enemy target = null;
        double best = Double.MAX_VALUE;
        for (Enemy e : g.enemies()) {
            if (!e.targetable() || e.x < x - 40) continue;
            double d = Math.hypot(e.x - x, e.y - y);
            if (d < best) { best = d; target = e; }
        }
        double speed = Math.min(760, Math.hypot(vx, vy) + 900 * dt);
        double ang = Math.atan2(vy, vx);
        if (target != null) {
            double want = Math.atan2(target.y - y, target.x - x);
            double diff = Math.atan2(Math.sin(want - ang), Math.cos(want - ang));
            double maxTurn = 5.0 * dt;
            ang += Math.max(-maxTurn, Math.min(maxTurn, diff));
        }
        vx = Math.cos(ang) * speed;
        vy = Math.sin(ang) * speed;
    }

    public Rectangle2D hitbox() {
        if (style == Style.LASER || style == Style.ENEMY_LASER) {
            return new Rectangle2D.Double(x - len / 2, y - 3, len, 6);
        }
        if (style == Style.ICICLE) {
            return new Rectangle2D.Double(x - 4, y - len / 2, 8, len);
        }
        return new Rectangle2D.Double(x - size, y - size, size * 2, size * 2);
    }

    public void draw(Graphics2D g) {
        Stroke old = g.getStroke();
        switch (style) {
            case PISTOL:
                g.setColor(new Color(255, 240, 150));
                g.fill(new Ellipse2D.Double(x - 6, y - 3, 12, 6));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(x - 3, y - 1.5, 6, 3));
                break;
            case HEAVY: {
                double ang = Math.atan2(vy, vx);
                double dx = Math.cos(ang) * 9, dy = Math.sin(ang) * 9;
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 140, 40, 160));
                g.draw(new Line2D.Double(x - dx, y - dy, x + dx, y + dy));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 240, 180));
                g.draw(new Line2D.Double(x - dx, y - dy, x + dx, y + dy));
                break;
            }
            case SPREAD:
                g.setColor(new Color(255, 70, 120, 120));
                g.fill(new Ellipse2D.Double(x - 8, y - 8, 16, 16));
                g.setColor(new Color(255, 160, 190));
                g.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));
                break;
            case LASER: {
                Line2D line = new Line2D.Double(x - len / 2, y, x + len / 2, y);
                g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(80, 255, 200, 70));
                g.draw(line);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(150, 255, 220));
                g.draw(line);
                g.setStroke(new BasicStroke(1f));
                g.setColor(Color.WHITE);
                g.draw(line);
                break;
            }
            case ROCKET: {
                double ang = Math.atan2(vy, vx);
                AffineTransform at = g.getTransform();
                g.translate(x, y);
                g.rotate(ang);
                g.setColor(new Color(255, 170, 60));
                g.fill(new Ellipse2D.Double(-14, -3, 8, 6));
                g.setColor(new Color(200, 205, 215));
                g.fill(new RoundRectangle2D.Double(-8, -3.5, 16, 7, 4, 4));
                g.setColor(new Color(220, 50, 50));
                g.fill(new Ellipse2D.Double(4, -3.5, 7, 7));
                g.setTransform(at);
                break;
            }
            case CANNON: {
                g.setColor(new Color(255, 200, 80, 120));
                g.fill(new Ellipse2D.Double(x - 12, y - 12, 24, 24));
                g.setColor(new Color(90, 90, 100));
                g.fill(new Ellipse2D.Double(x - 8, y - 8, 16, 16));
                g.setColor(new Color(255, 230, 150));
                g.fill(new Ellipse2D.Double(x - 3, y - 6, 6, 5));
                break;
            }
            case FROST: {
                double r = size * (1 + Math.sin(age * 10) * 0.1);
                g.setColor(new Color(150, 230, 255, 80));
                g.fill(new Ellipse2D.Double(x - r * 1.6, y - r * 1.6, r * 3.2, r * 3.2));
                g.setColor(new Color(200, 245, 255));
                g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
                g.setColor(new Color(100, 190, 255));
                g.setStroke(new BasicStroke(2f));
                for (int i = 0; i < 3; i++) {
                    double a = age * 3 + i * Math.PI / 3;
                    g.draw(new Line2D.Double(x - Math.cos(a) * r, y - Math.sin(a) * r, x + Math.cos(a) * r, y + Math.sin(a) * r));
                }
                break;
            }
            case ICEBOMB: {
                boolean blink = fuse - age < 0.35 && ((int) (age * 20)) % 2 == 0;
                g.setColor(blink ? Color.WHITE : new Color(130, 200, 255));
                Path2D d = new Path2D.Double();
                d.moveTo(x, y - 11); d.lineTo(x + 8, y); d.lineTo(x, y + 11); d.lineTo(x - 8, y); d.closePath();
                g.fill(d);
                g.setColor(new Color(230, 250, 255));
                g.draw(d);
                break;
            }
            case ICICLE: {
                Path2D d = new Path2D.Double();
                d.moveTo(x - 5, y - len / 2);
                d.lineTo(x + 5, y - len / 2);
                d.lineTo(x, y + len / 2);
                d.closePath();
                g.setColor(new Color(210, 245, 255));
                g.fill(d);
                g.setColor(new Color(120, 200, 255));
                g.draw(d);
                break;
            }
            case ACID: {
                g.setColor(new Color(120, 255, 80, 90));
                g.fill(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
                g.setColor(new Color(170, 255, 90));
                g.fill(new Ellipse2D.Double(x - 5, y - 6, 10, 12));
                break;
            }
            case ORB: {
                double pulse = 1 + Math.sin(age * 20) * 0.15;
                double r = size * pulse;
                g.setColor(new Color(255, 60, 160, 90));
                g.fill(new Ellipse2D.Double(x - r * 1.7, y - r * 1.7, r * 3.4, r * 3.4));
                g.setColor(new Color(255, 90, 190));
                g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
                g.setColor(new Color(255, 230, 245));
                g.fill(new Ellipse2D.Double(x - r * 0.45, y - r * 0.45, r * 0.9, r * 0.9));
                break;
            }
            case ENEMY_LASER: {
                Line2D line = new Line2D.Double(x - len / 2, y, x + len / 2, y);
                g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(60, 200, 255, 50));
                g.draw(line);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(110, 225, 255));
                g.draw(line);
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Color.WHITE);
                g.draw(line);
                break;
            }
            default:
                break;
        }
        g.setStroke(old);
    }
}
