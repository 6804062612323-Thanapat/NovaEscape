package novaescape.assault;

import novaescape.item.ItemType;
import novaescape.ui.Text;

import java.awt.*;
import java.awt.geom.*;

/** ไอเทมที่ดรอปจากศัตรู/กล่องเสบียง/นักบินที่ช่วยไว้ บินไปชนเพื่อเก็บ */
public class Pickup {
    public enum Kind {
        HEAVY, SPREAD, LASER, ROCKET,   // อาวุธ
        BOMB,                           // ระเบิด +3
        MEDKIT,                         // ฟื้น HP +2
        SHIELD,                         // เข้ากระเป๋า กด Space ใช้
        EMP,                            // เข้ากระเป๋า กด Space ใช้ (ศัตรูหยุดยิง 5 วิ)
        LIFE,                           // ชีวิต +1
        GEM                             // คะแนน +100
    }

    public double x, y;
    private double vy, vx;
    public final Kind kind;
    public boolean dead;
    private double life = 14;
    private double t;

    public Pickup(double x, double y, Kind kind) {
        this.x = x;
        this.y = y;
        this.kind = kind;
        this.vy = -120;
        this.vx = 40;
    }

    public Weapon weapon() {
        switch (kind) {
            case HEAVY: return Weapon.HEAVY;
            case SPREAD: return Weapon.SPREAD;
            case LASER: return Weapon.LASER;
            case ROCKET: return Weapon.ROCKET;
            default: return null;
        }
    }

    public void update(double dt, AssaultGame g) {
        t += dt;
        life -= dt;
        x += vx * dt;
        // ห้องบอส: ไอเทมที่เกิดตรงตัวบอสจะค่อย ๆ ลอยกลับมาในระยะที่เก็บได้
        if (g.inBossArena() && x > g.pickupReachX()) {
            x -= (x - g.pickupReachX()) * Math.min(1, 3.0 * dt);
            vx = Math.min(vx, 0);
        }
        y += vy * dt;
        vx *= Math.pow(0.05, dt);
        vy *= Math.pow(0.05, dt);
        y = Math.max(g.ceiling(x) + 20, Math.min(g.floor(x) - 20, y));
        if (life <= 0 || x < g.camX() - 40) dead = true;
    }

    public Rectangle2D hitbox() {
        return new Rectangle2D.Double(x - 18, y - 18, 36, 36);
    }

    public void draw(Graphics2D g) {
        if (life < 3 && ((int) (t * 10)) % 2 == 0) return;
        double by = y + Math.sin(t * 4) * 4;

        // วงแสง
        float glow = (float) (0.5 + 0.5 * Math.sin(t * 6));
        g.setColor(new Color(255, 255, 200, (int) (40 + 40 * glow)));
        g.fill(new Ellipse2D.Double(x - 24, by - 24, 48, 48));

        Weapon w = weapon();
        if (w != null) {
            drawCrate(g, x, by, w.letter, w.color);
            return;
        }
        switch (kind) {
            case BOMB:
                g.setColor(new Color(40, 40, 55));
                g.fill(new Ellipse2D.Double(x - 13, by - 11, 26, 26));
                g.setColor(new Color(120, 120, 140));
                g.fill(new Rectangle2D.Double(x - 4, by - 17, 8, 6));
                g.setColor(new Color(255, 200, 60));
                g.fill(new Ellipse2D.Double(x + 1 + Math.random() * 3, by - 22 - Math.random() * 3, 5, 5));
                g.setFont(Text.font(Font.BOLD, 14));
                Text.centered(g, "B", x, by + 2, Color.WHITE, false);
                break;
            case MEDKIT:
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(x - 15, by - 13, 30, 26, 8, 8));
                g.setColor(new Color(40, 190, 90));
                g.fill(new Rectangle2D.Double(x - 4, by - 9, 8, 18));
                g.fill(new Rectangle2D.Double(x - 9, by - 4, 18, 8));
                break;
            case SHIELD:
                ItemType.SHIELD.drawIcon(g, x - 15, by - 15, 30);
                break;
            case EMP:
                ItemType.ATTACK.drawIcon(g, x - 15, by - 15, 30);
                break;
            case LIFE:
                ItemType.HEART.drawIcon(g, x - 14, by - 16, 28);
                g.setFont(Text.font(Font.BOLD, 11));
                Text.centered(g, "1UP", x, by + 20, new Color(255, 230, 120), true);
                break;
            case GEM: {
                Path2D d = new Path2D.Double();
                d.moveTo(x, by - 12);
                d.lineTo(x + 10, by);
                d.lineTo(x, by + 12);
                d.lineTo(x - 10, by);
                d.closePath();
                g.setColor(new Color(90, 230, 255));
                g.fill(d);
                g.setColor(Color.WHITE);
                g.draw(d);
                break;
            }
            default:
                break;
        }
    }

    /** กล่องอาวุธมีตัวอักษรแบบ Metal Slug */
    private static void drawCrate(Graphics2D g, double x, double y, String letter, Color c) {
        g.setColor(new Color(30, 35, 50));
        g.fill(new RoundRectangle2D.Double(x - 17, y - 15, 34, 30, 6, 6));
        g.setColor(new Color(90, 100, 120));
        g.fill(new RoundRectangle2D.Double(x - 15, y - 13, 30, 26, 5, 5));
        g.setColor(new Color(30, 35, 50));
        g.fill(new Rectangle2D.Double(x - 15, y - 2, 30, 4));
        g.setFont(Text.font(Font.BOLD, 22));
        Text.centered(g, letter, x, y, c, true);
    }
}
