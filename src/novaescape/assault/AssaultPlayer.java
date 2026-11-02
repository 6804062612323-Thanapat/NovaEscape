package novaescape.assault;

import novaescape.Config;
import novaescape.Input;
import novaescape.entity.PlayerShip;

import java.awt.*;
import java.awt.geom.*;

/**
 * ยานผู้เล่นในโหมด ASSAULT: มี HP, ชีวิต, ปืน, กระสุน, ระเบิด
 * ใช้รูปยานเดียวกับโหมดเดิม (PlayerShip) แค่เพิ่มปากกระบอกปืน
 */
public class AssaultPlayer {
    public static final int MAX_HP = 5;
    public static final int MAX_BOMBS = Balance.BOMB_MAX;

    public double x, y;               // มุมซ้ายบน (พิกัดโลก)
    public int hp = MAX_HP;
    public int lives = 2;             // ชีวิตสำรอง
    public int bombs = Balance.BOMB_START;
    public Weapon weapon = Weapon.PISTOL;
    public int ammo = 0;
    public double shieldTimer;
    public double invulnTimer;
    public boolean alive = true;
    public double respawnTimer;
    public NovaSlug vehicle;          // != null = กำลังขับรถถัง
    public double slowTimer;          // > 0 = โดนแช่แข็ง เคลื่อนที่ช้า
    public double pushX;              // แรงผลักจากภายนอก (เช่น พายุ) ใส่ใหม่ทุกเฟรม

    private double cooldown;
    private int shotCount;
    private double muzzleFlash;
    private final PlayerShip look = new PlayerShip();

    public AssaultPlayer(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double centerX() { return x + PlayerShip.W / 2.0; }
    public double centerY() { return y + PlayerShip.H / 2.0; }

    /** ระดับความสูงของปากกระบอกปืน (อยู่ใต้หัวยาน) */
    public double gunY() { return centerY() + 9; }

    public Rectangle2D hitbox() {
        if (vehicle != null) return vehicle.hitbox();
        return new Rectangle2D.Double(x + 12, y + 9, PlayerShip.W - 20, PlayerShip.H - 18);
    }

    public void freeze() {
        if (slowTimer <= 0) novaescape.audio.Sound.play(novaescape.audio.Sfx.FREEZE);
        slowTimer = 2.5;
    }

    public boolean isInvulnerable() {
        return shieldTimer > 0 || invulnTimer > 0 || !alive;
    }

    public void update(double dt, Input in, AssaultGame g) {
        if (!alive) {
            respawnTimer -= dt;
            if (respawnTimer <= 0 && lives >= 0) respawn(g);
            return;
        }
        double speed = Config.PLAYER_SPEED * (vehicle != null ? 0.85 : 1) * (slowTimer > 0 ? 0.55 : 1);
        double dx = (in.right ? 1 : 0) - (in.left ? 1 : 0);
        double dy = (in.down ? 1 : 0) - (in.up ? 1 : 0);
        double len = Math.hypot(dx, dy);
        if (len > 0) {
            x += dx / len * speed * dt;
            y += dy / len * speed * dt;
        }
        x += pushX * dt;
        pushX = 0;

        // ขอบจอ / ผนังอุโมงค์
        double cam = g.camX();
        double maxX = cam + g.viewW() * (g.inBossArena() ? 0.6 : 0.72);
        x = Math.max(cam + 8, Math.min(maxX, x));
        double halfH = vehicle != null ? NovaSlug.H / 2 : PlayerShip.H / 2.0 - 4;
        double cx = centerX();
        double minCY = g.ceiling(cx) + halfH + 2, maxCY = g.floor(cx) - halfH - 2;
        double cy = centerY();
        boolean touchWall = false;
        if (cy < minCY) { cy = minCY; touchWall = true; }
        if (cy > maxCY) { cy = maxCY; touchWall = true; }
        y = cy - PlayerShip.H / 2.0;
        if (touchWall && g.hasWalls()) {
            hit(g);
            g.sparks(cx, cy, new Color(255, 120, 160), 4);
        }

        if (shieldTimer > 0) shieldTimer = Math.max(0, shieldTimer - dt);
        if (invulnTimer > 0) invulnTimer = Math.max(0, invulnTimer - dt);
        if (slowTimer > 0) slowTimer = Math.max(0, slowTimer - dt);
        if (muzzleFlash > 0) muzzleFlash -= dt;

        if (vehicle != null) {
            vehicle.updateActive(dt, g, this, in.isShooting());
            return;
        }

        cooldown -= dt;
        if (in.isShooting() && cooldown <= 0) {
            cooldown = weapon.cooldown;
            weapon.fire(g, x + PlayerShip.W + 10, gunY(), shotCount++);
            muzzleFlash = 0.05;
            if (weapon.ammoPerPickup > 0) {
                ammo--;
                if (ammo <= 0) {
                    weapon = Weapon.PISTOL;
                    ammo = 0;
                    g.toast("OUT OF AMMO");
                }
            }
        }
    }

    public void giveWeapon(Weapon w) {
        if (weapon == w) ammo += w.ammoPerPickup;
        else { weapon = w; ammo = w.ammoPerPickup; }
    }

    /** โดนโจมตี 1 ครั้ง */
    public void hit(AssaultGame g) {
        hit(g, Balance.NORMAL_HIT);
    }

    /** โดนโจมตี โดยระบุดาเมจ (ท่าใหญ่ของบอส = Balance.BIG_HIT) */
    public void hit(AssaultGame g, int damage) {
        if (isInvulnerable()) return;
        if (vehicle != null) {
            // ขับรถถังอยู่: เกราะรับแทน
            for (int i = 0; i < damage; i++) vehicle.takeHit(g);
            invulnTimer = 0.3;
            return;
        }
        hp -= damage;
        novaescape.audio.Sound.play(novaescape.audio.Sfx.HURT);
        invulnTimer = 1.3;
        g.shake(9);
        g.flash(0.12);
        g.explode(centerX(), centerY(), 12);
        if (hp <= 0) die(g);
    }

    private void die(AssaultGame g) {
        alive = false;
        g.explode(centerX(), centerY(), 70);
        g.shake(16);
        respawnTimer = 1.6;
        weapon = Weapon.PISTOL;
        ammo = 0;
        lives--;
        if (lives < 0) g.playerOutOfLives();
    }

    private void respawn(AssaultGame g) {
        alive = true;
        hp = MAX_HP;
        bombs = Math.max(bombs, 1);
        invulnTimer = 2.5;
        x = g.camX() + 60;
        y = g.centerY() - PlayerShip.H / 2.0;
        slowTimer = 0;
        g.clearEnemyBullets();
    }

    public void draw(Graphics2D g, double time) {
        if (!alive) return;
        if (vehicle != null) {
            vehicle.draw(g, time);
            return;
        }
        look.x = x;
        look.y = y;
        look.shieldTimer = shieldTimer;
        look.invulnTimer = invulnTimer;
        look.alive = true;
        if (invulnTimer > 0 && ((int) (time * 14)) % 2 == 0) return;

        // ปากกระบอกปืนตามสีของอาวุธ
        look.draw(g, time);

        double gy = gunY();
        g.setColor(new Color(60, 65, 80));
        g.fill(new RoundRectangle2D.Double(x + 40, gy - 3, 42, 6, 3, 3));
        g.setColor(weapon.color);
        g.fill(new Rectangle2D.Double(x + 74, gy - 3, 8, 6));

        if (slowTimer > 0) {
            // น้ำแข็งเกาะยาน
            g.setColor(new Color(170, 230, 255, 120));
            g.fill(new RoundRectangle2D.Double(x + 6, y + 4, PlayerShip.W - 8, PlayerShip.H - 8, 14, 14));
            g.setColor(new Color(235, 250, 255, 200));
            g.draw(new Line2D.Double(x + 20, y + 8, x + 30, y + 18));
            g.draw(new Line2D.Double(x + 44, y + 26, x + 54, y + 14));
        }

        if (muzzleFlash > 0) {
            double mx = x + PlayerShip.W + 12, my = gunY();
            g.setColor(new Color(255, 240, 160, 220));
            g.fill(new Ellipse2D.Double(mx - 6, my - 6, 14, 12));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(mx - 3, my - 3, 7, 6));
        }
    }
}
