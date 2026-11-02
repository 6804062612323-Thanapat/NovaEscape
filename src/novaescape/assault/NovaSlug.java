package novaescape.assault;

import novaescape.assault.enemy.Enemy;
import novaescape.ui.Text;

import java.awt.*;
import java.awt.geom.*;

/**
 * รถถังลอยฟ้า NOVA SLUG (แบบรถถังใน Metal Slug) — มีด่านละ 1 คัน
 *
 * จอดรออยู่กลางด่าน บินไปชนเพื่อขึ้น
 * - ป้อมปืนกลหมุนเล็งตามเมาส์ 360° (J / คลิกซ้าย)
 * - K / คลิกขวา = ปืนใหญ่ระเบิด (ไม่เสียลูกระเบิดของเรา)
 * - มีเกราะแยก ตัวคนขับไม่เจ็บ
 * - ใช้ได้ TIME_LIMIT วินาที หมดเวลา/เกราะแตก -> ดีดตัวออก + รถระเบิดใส่ศัตรูรอบ ๆ
 */
public class NovaSlug {
    public enum State { PARKED, ACTIVE, GONE }

    public static final double TIME_LIMIT = 25;
    public static final int MAX_ARMOR = 12;
    public static final double W = 112, H = 58;

    public State state = State.PARKED;
    public double x, y;            // จุดกึ่งกลาง (พิกัดโลก)
    public double timeLeft = TIME_LIMIT;
    public int armor = MAX_ARMOR;
    public double aim;             // มุมป้อมปืน
    private double vulcanCd, cannonCd, hitFlash, t;
    private int shot;

    public NovaSlug(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Rectangle2D hitbox() {
        return new Rectangle2D.Double(x - W / 2 + 6, y - H / 2 + 8, W - 12, H - 14);
    }

    public boolean isActive() { return state == State.ACTIVE; }

    /** ขณะจอดรอ */
    public void updateParked(double dt, AssaultGame g) {
        t += dt;
        y += Math.sin(t * 2) * 8 * dt;
        if (x < g.camX() - 120) state = State.GONE;   // ผ่านเลยไปแล้ว อดใช้
    }

    /** ขณะขับ: ตำแหน่งตามผู้เล่น ยิงตามปุ่ม */
    public void updateActive(double dt, AssaultGame g, AssaultPlayer p, boolean shooting) {
        t += dt;
        x = p.centerX();
        y = p.centerY();
        timeLeft -= dt;
        if (hitFlash > 0) hitFlash -= dt;

        Point2D m = g.mouseWorld();
        double tx = turretX(), ty = turretY();
        double want = m == null ? 0 : Math.atan2(m.getY() - ty, m.getX() - tx);
        double diff = Math.atan2(Math.sin(want - aim), Math.cos(want - aim));
        aim += Math.max(-12 * dt, Math.min(12 * dt, diff));

        vulcanCd -= dt;
        cannonCd -= dt;
        if (shooting && vulcanCd <= 0) {
            vulcanCd = 0.055;
            double a = aim + (Math.random() - 0.5) * 0.06;
            double off = (shot++ % 2 == 0) ? -4 : 4;
            double bx = tx + Math.cos(aim) * 34 - Math.sin(aim) * off;
            double by = ty + Math.sin(aim) * 34 + Math.cos(aim) * off;
            Bullet b = new Bullet(bx, by, Math.cos(a) * 980, Math.sin(a) * 980, true, Bullet.Style.HEAVY);
            b.damage = 1.25;
            g.addFriendlyBullet(b);
            novaescape.audio.Sound.play(novaescape.audio.Sfx.MG);
        }
        if (timeLeft <= 0 || armor <= 0) eject(g, p);
    }

    /** K / คลิกขวา ระหว่างขับ */
    public void fireCannon(AssaultGame g) {
        if (cannonCd > 0) return;
        cannonCd = 0.65;
        Bullet b = new Bullet(x + W / 2, y + 12, 620, 0, true, Bullet.Style.CANNON);
        b.damage = 9;
        b.size = 9;
        b.explosive = true;
        b.splashRadius = 120;
        b.splashDamage = 5;
        g.addFriendlyBullet(b);
        novaescape.audio.Sound.play(novaescape.audio.Sfx.CANNON);
        g.shake(5);
        g.sparks(x + W / 2, y + 12, new Color(255, 220, 120), 6);
    }

    public void takeHit(AssaultGame g) {
        armor--;
        novaescape.audio.Sound.play(novaescape.audio.Sfx.HURT, 0.6f);
        hitFlash = 0.12;
        g.shake(7);
        g.sparks(x, y, new Color(255, 200, 120), 8);
    }

    public void eject(AssaultGame g, AssaultPlayer p) {
        state = State.GONE;
        p.vehicle = null;
        p.invulnTimer = 2.2;
        g.explode(x, y, 80);
        g.shake(18);
        g.flash(0.15);
        for (Enemy e : g.enemies()) {
            if (e.active() && Math.hypot(e.x - x, e.y - y) < 200) e.damage(e.isBoss() ? 15 : 12, g);
        }
        g.toast(armor <= 0 ? "SLUG DESTROYED! EJECT!" : "TIME UP! EJECT!");
    }

    private double turretX() { return x + 4; }
    private double turretY() { return y - H / 2 + 6; }

    public void draw(Graphics2D g, double time) {
        if (state == State.GONE) return;
        boolean parked = state == State.PARKED;
        boolean low = !parked && (timeLeft < 5 || armor <= 3);
        boolean blinkWhite = hitFlash > 0 || (low && ((int) (time * 8)) % 2 == 0);

        // ไอพ่นลอยตัว
        for (int i = -1; i <= 1; i += 2) {
            double px = x + i * 30;
            g.setColor(new Color(120, 220, 255, parked ? 60 : 140 + (int) (Math.random() * 80)));
            g.fill(new Ellipse2D.Double(px - 12, y + H / 2 - 6, 24, 10 + (parked ? 0 : Math.random() * 8)));
        }
        // เครื่องยนต์ท้าย
        if (!parked) {
            g.setColor(new Color(255, 160, 60, 200));
            g.fill(new Ellipse2D.Double(x - W / 2 - 18 - Math.random() * 6, y - 6, 22, 12));
        }

        // ตัวถัง
        Path2D hull = new Path2D.Double();
        hull.moveTo(x - W / 2, y - 8);
        hull.lineTo(x - W / 2 + 14, y - H / 2 + 12);
        hull.lineTo(x + W / 2 - 18, y - H / 2 + 12);
        hull.lineTo(x + W / 2, y + 2);
        hull.lineTo(x + W / 2 - 10, y + H / 2 - 6);
        hull.lineTo(x - W / 2 + 8, y + H / 2 - 6);
        hull.closePath();
        Color top = blinkWhite ? new Color(255, 240, 220) : new Color(120, 140, 80);
        Color bot = blinkWhite ? new Color(230, 200, 180) : new Color(70, 85, 45);
        g.setPaint(new GradientPaint((float) x, (float) (y - H / 2), top, (float) x, (float) (y + H / 2), bot));
        g.fill(hull);
        g.setColor(new Color(40, 45, 25));
        g.setStroke(new BasicStroke(2f));
        g.draw(hull);
        g.setStroke(new BasicStroke(1f));
        // แถบลาย + ตัวอักษร
        g.setColor(new Color(230, 190, 60));
        g.fill(new Rectangle2D.Double(x - W / 2 + 10, y + 6, W - 26, 5));
        g.setFont(Text.font(Font.BOLD, 11));
        Text.centered(g, "NOVA SLUG", x - 6, y + 17, new Color(30, 35, 20), false);

        // ปืนใหญ่หน้า
        g.setColor(new Color(60, 65, 50));
        g.fill(new RoundRectangle2D.Double(x + W / 2 - 14, y + 8, 30, 9, 4, 4));

        // ป้อมปืน
        double tx = turretX(), ty = turretY();
        AffineTransform at = g.getTransform();
        g.translate(tx, ty);
        g.rotate(parked ? 0 : aim);
        g.setColor(new Color(50, 55, 40));
        g.fill(new RoundRectangle2D.Double(6, -6, 32, 5, 3, 3));
        g.fill(new RoundRectangle2D.Double(6, 1, 32, 5, 3, 3));
        g.setTransform(at);
        g.setColor(blinkWhite ? new Color(255, 240, 220) : new Color(100, 120, 65));
        g.fill(new Ellipse2D.Double(tx - 16, ty - 12, 32, 24));
        g.setColor(new Color(150, 220, 255, 200));
        g.fill(new Ellipse2D.Double(tx - 6, ty - 8, 14, 9));

        if (parked) {
            boolean on = ((int) (time * 3)) % 2 == 0;
            g.setFont(Text.font(Font.BOLD, 16));
            Text.centered(g, "GET IN!", x, y - H / 2 - 30 + (on ? 0 : -4), new Color(255, 230, 90), true);
            Path2D arrow = new Path2D.Double();
            double ay = y - H / 2 - 14 + (on ? 0 : -4);
            arrow.moveTo(x - 9, ay - 6);
            arrow.lineTo(x + 9, ay - 6);
            arrow.lineTo(x, ay + 5);
            arrow.closePath();
            g.setColor(new Color(255, 230, 90));
            g.fill(arrow);
        }
    }
}
