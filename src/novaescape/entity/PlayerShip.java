package novaescape.entity;

import novaescape.Config;
import novaescape.Input;

import java.awt.*;
import java.awt.geom.*;

/**
 * ยานอวกาศของผู้เล่น (หันหัวไปทางขวา เข้าหายานต่างดาว)
 * วาดด้วยโค้ดทั้งหมด ถ้าอยากใช้รูปภาพแทน ให้แก้แค่เมธอด draw()
 */
public class PlayerShip {
    public static final int W = 74;
    public static final int H = 36;

    public double x, y;
    public double shieldTimer;   // > 0 = มีโล่ (อมตะ)
    public double invulnTimer;   // > 0 = อมตะชั่วคราวหลังชุบชีวิต
    public boolean alive = true;

    public PlayerShip() {
        reset();
    }

    public void reset() {
        x = 70;
        y = (Config.HEIGHT + Config.HUD_HEIGHT) / 2.0 - H / 2.0;
        shieldTimer = 0;
        invulnTimer = 0;
        alive = true;
    }

    public void update(double dt, Input in) {
        double dx = (in.right ? 1 : 0) - (in.left ? 1 : 0);
        double dy = (in.down ? 1 : 0) - (in.up ? 1 : 0);
        double len = Math.hypot(dx, dy);
        if (len > 0) {
            x += dx / len * Config.PLAYER_SPEED * dt;
            y += dy / len * Config.PLAYER_SPEED * dt;
        }
        // จำกัดไม่ให้ออกนอกพื้นที่เล่น
        double maxX = Config.WIDTH * Config.PLAYER_MAX_X_RATIO;
        x = clamp(x, 8, maxX);
        y = clamp(y, Config.HUD_HEIGHT + 6, Config.HEIGHT - H - 6);

        if (shieldTimer > 0) shieldTimer = Math.max(0, shieldTimer - dt);
        if (invulnTimer > 0) invulnTimer = Math.max(0, invulnTimer - dt);
    }

    public boolean isInvulnerable() {
        return shieldTimer > 0 || invulnTimer > 0;
    }

    /** กล่องชนจริง (เล็กกว่ารูปนิดหน่อยให้เล่นแฟร์) */
    public Rectangle2D hitbox() {
        return new Rectangle2D.Double(x + 12, y + 9, W - 20, H - 18);
    }

    public double centerX() { return x + W / 2.0; }
    public double centerY() { return y + H / 2.0; }

    public void draw(Graphics2D g, double time) {
        if (!alive) return;

        // กระพริบตอนอมตะหลังชุบชีวิต
        if (invulnTimer > 0 && ((int) (time * 14)) % 2 == 0) return;

        double cy = centerY();

        // เปลวไฟท้ายยาน
        double flick = 6 + Math.sin(time * 40) * 3 + Math.random() * 3;
        Path2D flame = new Path2D.Double();
        flame.moveTo(x + 12, cy - 6);
        flame.lineTo(x + 2 - flick, cy);
        flame.lineTo(x + 12, cy + 6);
        flame.closePath();
        g.setColor(new Color(255, 140, 40));
        g.fill(flame);
        Path2D inner = new Path2D.Double();
        inner.moveTo(x + 12, cy - 3);
        inner.lineTo(x + 7 - flick / 2, cy);
        inner.lineTo(x + 12, cy + 3);
        inner.closePath();
        g.setColor(new Color(255, 230, 120));
        g.fill(inner);

        // บูสเตอร์ด้านข้าง (บน/ล่าง)
        g.setColor(new Color(205, 214, 230));
        g.fill(new RoundRectangle2D.Double(x + 12, cy - 17, 30, 7, 6, 6));
        g.fill(new RoundRectangle2D.Double(x + 12, cy + 10, 30, 7, 6, 6));
        g.setColor(new Color(70, 110, 200));
        g.fill(new RoundRectangle2D.Double(x + 36, cy - 17, 8, 7, 6, 6));
        g.fill(new RoundRectangle2D.Double(x + 36, cy + 10, 8, 7, 6, 6));

        // ครีบ
        g.setColor(new Color(90, 105, 150));
        Path2D finTop = new Path2D.Double();
        finTop.moveTo(x + 14, cy - 8);
        finTop.lineTo(x + 30, cy - 8);
        finTop.lineTo(x + 18, cy - 20);
        finTop.lineTo(x + 10, cy - 20);
        finTop.closePath();
        g.fill(finTop);
        Path2D finBot = new Path2D.Double();
        finBot.moveTo(x + 14, cy + 8);
        finBot.lineTo(x + 30, cy + 8);
        finBot.lineTo(x + 18, cy + 20);
        finBot.lineTo(x + 10, cy + 20);
        finBot.closePath();
        g.fill(finBot);

        // ลำตัว
        g.setPaint(new GradientPaint((float) x, (float) (cy - 10), Color.WHITE,
                (float) x, (float) (cy + 10), new Color(200, 208, 225)));
        g.fill(new RoundRectangle2D.Double(x + 10, cy - 10, 52, 20, 16, 16));

        // หัวยาน
        Path2D nose = new Path2D.Double();
        nose.moveTo(x + 56, cy - 10);
        nose.quadTo(x + W + 2, cy, x + 56, cy + 10);
        nose.closePath();
        g.setColor(new Color(225, 230, 242));
        g.fill(nose);

        // แถบสีน้ำเงิน + หน้าต่าง
        g.setColor(new Color(60, 100, 200));
        g.fill(new Rectangle2D.Double(x + 22, cy - 1.5, 22, 3));
        g.setColor(new Color(40, 120, 230));
        g.fill(new Ellipse2D.Double(x + 46, cy - 5, 10, 10));
        g.setColor(new Color(170, 215, 255));
        g.fill(new Ellipse2D.Double(x + 48, cy - 3.5, 4, 4));

        // โล่
        if (shieldTimer > 0) {
            boolean ending = shieldTimer < 1.2;
            if (!ending || ((int) (time * 10)) % 2 == 0) {
                double pulse = 1 + Math.sin(time * 8) * 0.04;
                double rw = (W + 26) * pulse, rh = (H + 30) * pulse;
                Ellipse2D bubble = new Ellipse2D.Double(centerX() - rw / 2, cy - rh / 2, rw, rh);
                g.setColor(new Color(90, 220, 255, 55));
                g.fill(bubble);
                g.setStroke(new BasicStroke(2.5f));
                g.setColor(new Color(140, 235, 255, 200));
                g.draw(bubble);
                g.setStroke(new BasicStroke(1f));
            }
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
