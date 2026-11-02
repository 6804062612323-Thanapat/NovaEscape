package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

import novaescape.assault.Bullet;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;

/**
 * คลาสแม่ของศัตรูทุกตัวในโหมด ASSAULT
 * (x, y) = จุดกึ่งกลาง (พิกัดโลก), w/h = ขนาดกล่องชน
 *
 * เพิ่มศัตรูใหม่: extends Enemy แล้วเขียน update() กับ draw()
 * จากนั้นเรียก g.spawn(new ศัตรูใหม่(...)) ใน Level.java
 */
public abstract class Enemy {
    public double x, y, w, h;
    public double hp, maxHp;
    public int score;
    public double dropChance;
    public boolean dead;          // ตายแล้ว (โดนยิง)
    public boolean removed;       // หายไปเฉย ๆ (ออกนอกจอ)
    public boolean touchDamage = true;
    public double delay;          // รอเวลานี้ก่อนจะโผล่ (ใช้ทำศัตรูมาเป็นระลอก)
    public boolean scaleHp = true; // false = ไม่คูณเลือดตามความยากของด่าน
    public boolean miniBoss;       // มินิบอส: ไม่ถูกลบเมื่อล็อกกล้องนานเกิน

    protected double t;           // เวลาที่มีชีวิตอยู่
    protected double flash;       // กระพริบขาวตอนโดนยิง

    protected Enemy(double x, double y, double w, double h, double hp, int score, double dropChance) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.hp = this.maxHp = hp;
        this.score = score;
        this.dropChance = dropChance;
    }

    public Enemy withDelay(double d) {
        this.delay = d;
        return this;
    }

    public boolean active() {
        return delay <= 0 && !dead && !removed;
    }

    public final void tick(double dt, AssaultGame g) {
        if (dead || removed) return;
        if (delay > 0) { delay -= dt; return; }
        t += dt;
        if (flash > 0) flash -= dt;
        update(dt, g);
        if (!isBoss() && x < g.camX() - 160) removed = true;
    }

    protected abstract void update(double dt, AssaultGame g);

    public final void render(Graphics2D g, double time) {
        if (delay > 0 || dead || removed) return;
        draw(g, time);
    }

    protected abstract void draw(Graphics2D g, double time);

    public Rectangle2D hitbox() {
        return new Rectangle2D.Double(x - w / 2, y - h / 2, w, h);
    }

    public boolean isBoss() { return false; }

    /** ยิงโดนส่วน blockers() แล้วเข้าดาเมจกี่ส่วน (0 = กันหมด, 0.3 = เข้า 30%) */
    public double armorDamageRatio() { return 0; }

    /** ชนตัวนี้แล้วเสีย HP กี่ช่อง */
    public int contactDamage() { return novaescape.assault.Balance.NORMAL_HIT; }

    /** จรวดติดตามเป้าจะเล็งตัวนี้ได้ไหม */
    public boolean targetable() { return active(); }

    /** มองเห็นบนจอแล้วหรือยัง (ใช้กันศัตรูยิงจากนอกจอ) */
    protected boolean onScreen(AssaultGame g) {
        return x < g.camX() + g.viewW() - 10 && x > g.camX() + 10;
    }

    /** true = กระสุนนี้โดนโล่/เกราะ ไม่เข้าเนื้อ (เช่น Shield Drone) */
    public boolean blocksBullet(Bullet b) { return false; }

    /** เรียกเมื่อกระสุนโดนโล่ (เช่น ลดพลังโล่) */
    public void onBlocked(Bullet b, AssaultGame g) {}

    /** ส่วนตัวที่กันกระสุนและชนแล้วเจ็บ แต่ยิงไม่เข้า (เช่น ลำตัวงู, ตัวราชินี) */
    public List<Rectangle2D> blockers() { return Collections.emptyList(); }

    public void damage(double d, AssaultGame g) {
        if (!active()) return;
        hp -= d;
        flash = 0.07;
        if (hp <= 0) {
            dead = true;
            onDeath(g);
        }
    }

    protected void onDeath(AssaultGame g) {
        g.explode(x, y, (int) (10 + w / 3));
        g.enemyKilled(this);
        if (Math.random() < dropChance * novaescape.assault.Balance.enemyDrop(g.stage().number())) g.dropRandom(x, y);
    }

    /** ถูกผู้เล่นชน (เช่น kamikaze จะระเบิดตัวเอง) */
    public void onTouchPlayer(AssaultGame g) {}

    /** สีตอนโดนยิง: ผสมขาวให้สว่างขึ้น (ไม่ขาวทึบ จะได้ยังเห็นรูปร่าง) */
    protected Color tint(Color c) {
        if (flash <= 0) return c;
        double k = 0.55;
        return new Color((int) (c.getRed() + (255 - c.getRed()) * k),
                (int) (c.getGreen() + (255 - c.getGreen()) * k),
                (int) (c.getBlue() + (255 - c.getBlue()) * k), c.getAlpha());
    }

    /** แถบเลือดเหนือตัว (ใช้กับมินิบอส) */
    protected void drawHpBar(Graphics2D g, String name, double cx, double topY, double width) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect((int) (cx - width / 2), (int) topY, (int) width, 8);
        g.setColor(new Color(255, 90, 90));
        g.fillRect((int) (cx - width / 2), (int) topY, (int) (width * Math.max(0, hp / maxHp)), 8);
        g.setFont(novaescape.ui.Text.font(Font.BOLD, 12));
        novaescape.ui.Text.centered(g, name, cx, topY - 12, Color.WHITE, true);
    }

    protected double angleToPlayer(AssaultGame g) {
        return Math.atan2(g.player().centerY() - y, g.player().centerX() - x);
    }
}
