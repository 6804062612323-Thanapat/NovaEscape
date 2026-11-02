package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;
import novaescape.assault.Bullet;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * บอสด่าน 3: HIVE QUEEN — ราชินีรังเอเลี่ยน
 * วิธีสู้: ตัวราชินียิงไม่เข้า! ต้องทำลาย "ดวงตา 3 ดวง" ก่อน (แต่ละดวงยิงกระสุนใส่เอง)
 *          พอตาแตกหมด หน้าจะแยกออก เผย "หัวใจ" ให้ยิง
 * ระหว่างนั้น: ฟาดหนวดตามเลน (มีแถบแดงเตือน), ปล่อยตัวอ่อน, ช่วงสุดท้ายฝนกรด + วงกระสุน
 */
public class HiveQueenBoss extends BossEnemy {
    public static final double HEART_HP = 220;
    private static final double EYE_HP = 100;
    private static final double[][] EYE_OFFSETS = {{-95, -125}, {-120, 0}, {-95, 125}};

    private final double homeX;
    private boolean entered;
    private final List<QueenEye> eyes = new ArrayList<>();
    private double tentacleTimer = 2.5, larvaTimer = 4.0, rainTimer, ringTimer = 2.0;
    private double openAmount;   // 0 = หน้าปิด, 1 = เปิดเผยหัวใจ
    private final List<Tentacle> tentacles = new ArrayList<>();

    /** หนวดฟาดตามเลน */
    private static final class Tentacle {
        final double laneY;
        double t;
        static final double WARN = 1.0, EXTEND = 0.35, HOLD = 0.35, RETRACT = 0.35;

        Tentacle(double laneY) { this.laneY = laneY; }

        double reach() {   // 0..1
            if (t < WARN) return 0;
            double u = t - WARN;
            if (u < EXTEND) return u / EXTEND;
            u -= EXTEND;
            if (u < HOLD) return 1;
            u -= HOLD;
            return Math.max(0, 1 - u / RETRACT);
        }

        boolean done() { return t > WARN + EXTEND + HOLD + RETRACT; }
    }

    public HiveQueenBoss(AssaultGame g) {
        super(g.arenaX() + g.viewW() + 260, g.centerY(), 70, 80, HEART_HP, 30000);
        this.homeX = g.arenaX() + g.viewW() - 150;
    }

    @Override public String displayName() { return "HIVE QUEEN"; }

    private int eyesAlive() {
        int n = 0;
        for (QueenEye e : eyes) if (!e.dead) n++;
        return entered ? n : 3;
    }

    private boolean exposed() { return entered && eyesAlive() == 0; }

    @Override
    public int phase() {
        int n = eyesAlive();
        return n == 3 ? 1 : n > 0 ? 2 : 3;
    }

    @Override
    public double healthRatio() {
        double total = EYE_HP * 3 + HEART_HP;
        double cur = Math.max(0, hp);
        if (!entered) cur += EYE_HP * 3;
        for (QueenEye e : eyes) if (!e.dead) cur += Math.max(0, e.hp);
        return cur / total;
    }

    @Override
    public boolean targetable() { return exposed() && active(); }

    @Override
    public Rectangle2D hitbox() {
        if (!exposed()) return new Rectangle2D.Double(-9999, -9999, 1, 1);
        return new Rectangle2D.Double(x - 95, y - 40, 70, 80);
    }

    @Override
    public List<Rectangle2D> blockers() {
        if (!entered) return Collections.emptyList();
        List<Rectangle2D> list = new ArrayList<>();
        list.add(new Rectangle2D.Double(x - 120, y - 190, 260, 140));
        list.add(new Rectangle2D.Double(x - 120, y + 50, 260, 140));
        list.add(new Rectangle2D.Double(x - 30, y - 50, 170, 100));
        return list;
    }

    @Override
    public void damage(double d, AssaultGame g) {
        if (!exposed()) return;
        super.damage(d, g);
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        if (!entered) {
            x += (homeX - x) * Math.min(1, 1.3 * dt);
            if (Math.abs(homeX - x) < 4) {
                entered = true;
                for (double[] o : EYE_OFFSETS) {
                    QueenEye e = new QueenEye(this, o[0], o[1], EYE_HP);
                    eyes.add(e);
                    g.spawn(e);
                }
                g.shake(12);
                g.toast("DESTROY THE EYES!");
            }
            return;
        }
        y = g.centerY() + Math.sin(t * 0.7) * 30;
        for (QueenEye e : eyes) e.follow(this);

        int ph = phase();
        if (ph == 3 && openAmount < 1) {
            if (openAmount == 0) { g.shake(16); g.toast("THE HEART IS EXPOSED!"); }
            openAmount = Math.min(1, openAmount + dt * 1.5);
        }

        // หนวด
        for (Tentacle tn : tentacles) {
            tn.t += dt;
            double r = tn.reach();
            if (r > 0.2) {
                double tipX = x - 100 - (x - 100 - (g.camX() - 40)) * r;
                Rectangle2D band = new Rectangle2D.Double(tipX, tn.laneY - 26, x - 100 - tipX, 52);
                if (band.intersects(g.player().hitbox())) g.player().hit(g, novaescape.assault.Balance.BIG_HIT);
            }
        }
        tentacles.removeIf(Tentacle::done);

        if (g.enemiesJammed()) return;

        tentacleTimer -= dt;
        if (tentacleTimer <= 0) {
            tentacleTimer = ph == 1 ? 4.2 : ph == 2 ? 3.4 : 3.0;
            double top = g.top() + 40, bottom = g.bottom() - 40;
            double py = Math.max(top, Math.min(bottom, g.player().centerY()));
            tentacles.add(new Tentacle(py));
            if (ph >= 2) {
                double other = top + Math.random() * (bottom - top);
                if (Math.abs(other - py) < 120) other = py > g.centerY() ? py - 180 : py + 180;
                tentacles.add(new Tentacle(Math.max(top, Math.min(bottom, other))));
            }
        }

        larvaTimer -= dt;
        if (larvaTimer <= 0) {
            larvaTimer = ph == 1 ? 6.0 : 4.8;
            for (int i = -1; i <= 1; i++) {
                g.spawn(new Larva(x - 120, y + i * 40, Larva.Kind.LARVA, -200, i * 120));
            }
        }

        if (ph == 3) {
            rainTimer -= dt;
            if (rainTimer <= 0) {
                rainTimer = 0.22;
                double rx = g.camX() + 40 + Math.random() * (g.viewW() - 240);
                g.enemyBullet(Bullet.acid(rx, g.top() - 10, (Math.random() - 0.5) * 30, 40));
            }
            ringTimer -= dt;
            if (ringTimer <= 0) {
                ringTimer = 2.2;
                for (int i = 0; i < 10; i++) {
                    double a = Math.PI / 2 + Math.PI * i / 9.0;
                    g.enemyShoot(x - 60, y, a, 210);
                }
            }
        }
    }

    @Override
    protected void onDeath(AssaultGame g) {
        for (int i = 0; i < 6; i++) g.explode(x + (Math.random() - 0.5) * 220, y + (Math.random() - 0.5) * 340, 30);
        super.onDeath(g);
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        // แถบเตือน + หนวด
        for (Tentacle tn : tentacles) {
            if (tn.t < Tentacle.WARN) {
                boolean on = ((int) (tn.t * 14)) % 2 == 0;
                g.setColor(new Color(255, 60, 90, on ? 100 : 40));
                g.fill(new Rectangle2D.Double(x - 4000, tn.laneY - 26, 4000 - 100, 52));
            }
            double r = tn.reach();
            if (r > 0) {
                double startX = x - 100;
                double tipX = startX - 1300 * r;
                int segs = 28;
                for (int i = segs; i >= 0; i--) {
                    double u = i / (double) segs;
                    double px = startX + (tipX - startX) * u;
                    double py = tn.laneY + Math.sin(u * 12 + time * 10) * 6 * u;
                    double rad = 24 - 16 * u;
                    g.setColor(new Color(150, 50, 90));
                    g.fill(new Ellipse2D.Double(px - rad, py - rad, rad * 2, rad * 2));
                    if (i % 3 == 0) {
                        g.setColor(new Color(240, 170, 190));
                        g.fill(new Ellipse2D.Double(px - rad * 0.3, py + rad * 0.4, rad * 0.6, rad * 0.4));
                    }
                }
            }
        }

        // ลำตัวราชินี
        double cy = y;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x - 40, cy - 60), 280f,
                new float[]{0f, 1f}, new Color[]{tint(new Color(170, 60, 100)), tint(new Color(60, 10, 35))}));
        g.fill(new Ellipse2D.Double(x - 130, cy - 200, 290, 400));
        // มงกุฎหนาม
        g.setColor(new Color(90, 20, 50));
        for (int i = 0; i < 5; i++) {
            double bx = x - 80 + i * 45;
            Path2D sp = new Path2D.Double();
            sp.moveTo(bx - 14, cy - 170 + Math.abs(i - 2) * 12);
            sp.lineTo(bx, cy - 250 + Math.abs(i - 2) * 25);
            sp.lineTo(bx + 14, cy - 170 + Math.abs(i - 2) * 12);
            sp.closePath();
            g.fill(sp);
        }
        // เส้นเลือด
        g.setColor(new Color(255, 120, 170, 90));
        g.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 4; i++) {
            double vy = cy - 150 + i * 100;
            g.draw(new QuadCurve2D.Double(x + 120, vy, x + 40, vy + 30 + Math.sin(time * 2 + i) * 10, x - 40, vy + 10));
        }
        g.setStroke(new BasicStroke(1f));

        // หน้าแยกออก เผยหัวใจ
        double open = openAmount * 40;
        g.setColor(new Color(40, 5, 20));
        g.fill(new Ellipse2D.Double(x - 110, cy - 50 - open, 100, 100 + open * 2));
        if (openAmount > 0) {
            double beat = 1 + Math.sin(time * 9) * 0.08;
            double hr = 34 * beat;
            g.setColor(new Color(255, 60, 90, 90));
            g.fill(new Ellipse2D.Double(x - 60 - hr * 1.4, cy - hr * 1.4, hr * 2.8, hr * 2.8));
            g.setColor(flash > 0 ? Color.WHITE : new Color(230, 40, 70));
            g.fill(new Ellipse2D.Double(x - 60 - hr, cy - hr, hr * 2, hr * 2));
            g.setColor(new Color(255, 170, 190));
            g.fill(new Ellipse2D.Double(x - 70, cy - 14, 12, 10));
        }
        // ปาก/เขี้ยว
        g.setColor(new Color(240, 225, 200));
        for (int i = 0; i < 4; i++) {
            double fx = x - 100 + i * 22;
            Path2D f = new Path2D.Double();
            f.moveTo(fx, cy + 55 + open);
            f.lineTo(fx + 8, cy + 30 + open);
            f.lineTo(fx + 16, cy + 55 + open);
            f.closePath();
            g.fill(f);
        }
    }

    // =====================================================================
    /** ดวงตาของราชินี: เป้าที่ต้องทำลายก่อน */
    public static class QueenEye extends Enemy {
        private final double ox, oy;
        private double fireTimer;
        private double lookA = Math.PI;

        QueenEye(HiveQueenBoss q, double ox, double oy, double hp) {
            super(q.x + ox, q.y + oy, 54, 46, hp, 2000, 0);
            this.ox = ox;
            this.oy = oy;
            this.touchDamage = false;
            this.scaleHp = false;   // เลือดตากำหนดตายตัว ไม่คูณตามด่าน
            this.fireTimer = 1.0 + Math.random() * 1.5;
        }

        void follow(HiveQueenBoss q) {
            x = q.x + ox;
            y = q.y + oy;
        }

        @Override
        protected void update(double dt, AssaultGame g) {
            lookA = angleToPlayer(g);
            fireTimer -= dt;
            if (fireTimer <= 0) {
                fireTimer = 2.4 + Math.random() * 0.6;
                for (int i = -1; i <= 1; i++) g.enemyShoot(x - 20, y, lookA + i * 0.18, 230);
            }
        }

        @Override
        protected void onDeath(AssaultGame g) {
            g.explode(x, y, 30);
            g.sparks(x, y, new Color(200, 255, 120), 16);
            g.shake(10);
            g.enemyKilled(this);
            g.dropRandom(x - 40, y);
        }

        @Override
        protected void draw(Graphics2D g, double time) {
            g.setColor(new Color(60, 10, 30));
            g.fill(new Ellipse2D.Double(x - 32, y - 27, 64, 54));
            g.setColor(tint(new Color(240, 230, 150)));
            g.fill(new Ellipse2D.Double(x - 27, y - 22, 54, 44));
            // ม่านตาแบบเส้นตั้ง ขยับตามผู้เล่น
            double px = x + Math.cos(lookA) * 10, py = y + Math.sin(lookA) * 8;
            g.setColor(new Color(255, 120, 40));
            g.fill(new Ellipse2D.Double(px - 12, py - 14, 24, 28));
            g.setColor(new Color(20, 5, 10));
            g.fill(new Ellipse2D.Double(px - 3, py - 12, 6, 24));
            // เลือดตา
            double r = Math.max(0, hp / maxHp);
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect((int) (x - 26), (int) (y - 38), 52, 5);
            g.setColor(new Color(255, 200, 80));
            g.fillRect((int) (x - 26), (int) (y - 38), (int) (52 * r), 5);
        }
    }
}
