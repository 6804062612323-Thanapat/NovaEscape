package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;
import novaescape.assault.Bullet;
import novaescape.assault.Pickup;

import java.awt.*;
import java.awt.geom.*;

/**
 * มินิบอสด่าน 3: BROOD MOTHER — แม่ตัวอ่อนเกาะผนังอุโมงค์
 * วิธีสู้: มันเกาะเพดานหรือพื้น แล้ว "ไต่สลับข้าง" ทุก ๆ ไม่กี่วินาที (ตอนข้ามจะลอยกลางจอ ยิงง่ายสุด)
 * ท่า: พ่นกรดเป็นวิถีโค้ง, วางไข่บนผนัง (เข้าใกล้แล้วฟัก), ปล่อยตัวอ่อนเป็นชุด
 * เลือดต่ำกว่าครึ่ง: สลับข้างบ่อยขึ้น + พ่นกรดถี่ขึ้น
 */
public class BroodMother extends Enemy {
    private enum Mode { CLING, CROSS }

    private Mode mode = Mode.CLING;
    private boolean onCeiling = true;
    private double modeT;
    private double acidTimer = 1.5, eggTimer = 3.0, larvaTimer = 5.0;
    private double crossFromY, crossToY;
    private double facing; // ทิศของขาเกาะ (1 = เพดาน, -1 = พื้น) ใช้วาด

    public BroodMother(double x, double y) {
        super(x, y, 120, 90, 160, 5000, 0);
        this.miniBoss = true;
    }

    private boolean enraged() { return hp < maxHp * 0.5; }

    private double clingY(AssaultGame g, boolean ceiling) {
        return ceiling ? g.ceiling(x) + h / 2 + 4 : g.floor(x) - h / 2 - 4;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        modeT += dt;
        double tx = g.camX() + g.viewW() - 190;
        x += (tx - x) * Math.min(1, 1.2 * dt);

        if (mode == Mode.CLING) {
            y += (clingY(g, onCeiling) - y) * Math.min(1, 6 * dt);
            facing += ((onCeiling ? 1 : -1) - facing) * Math.min(1, 6 * dt);
            if (modeT > (enraged() ? 5.5 : 8.0)) {
                mode = Mode.CROSS;
                modeT = 0;
                crossFromY = y;
                crossToY = clingY(g, !onCeiling);
                g.toast("SHE'S CROSSING - FIRE!");
            }
        } else {
            double k = Math.min(1, modeT / 1.8);
            double ease = k * k * (3 - 2 * k);
            y = crossFromY + (crossToY - crossFromY) * ease;
            facing = (onCeiling ? 1 : -1) * (1 - 2 * ease);
            if (k >= 1) {
                onCeiling = !onCeiling;
                mode = Mode.CLING;
                modeT = 0;
            }
        }

        if (!onScreen(g) || g.enemiesJammed()) return;

        // พ่นกรดโค้ง ๆ ใส่ผู้เล่น
        acidTimer -= dt;
        if (acidTimer <= 0) {
            acidTimer = enraged() ? 1.4 : 2.1;
            double dx = g.player().centerX() - x;
            for (int i = 0; i < 3; i++) {
                double vx = dx / 1.3 * (0.8 + i * 0.2);
                double vy = onCeiling ? 40 + i * 30 : -280 - i * 40;
                g.enemyBullet(Bullet.acid(x - 50, y, vx, vy));
            }
        }
        // วางไข่บนผนังด้านที่เกาะอยู่ (เฉพาะตอนเกาะ)
        eggTimer -= dt;
        if (eggTimer <= 0 && mode == Mode.CLING) {
            eggTimer = enraged() ? 5.0 : 6.5;
            g.spawn(new WallGrowth(x - 140, WallGrowth.Kind.EGG_SAC, onCeiling));
        }
        // ปล่อยตัวอ่อน
        larvaTimer -= dt;
        if (larvaTimer <= 0) {
            larvaTimer = enraged() ? 4.5 : 6.0;
            for (int i = 0; i < 3; i++) {
                g.spawn(new Larva(x - 40, y, Larva.Kind.LARVA, -160, (i - 1) * 120));
            }
        }
    }

    @Override
    protected void onDeath(AssaultGame g) {
        for (int i = 0; i < 4; i++) g.explode(x + (Math.random() - 0.5) * w, y + (Math.random() - 0.5) * h, 25);
        g.sparks(x, y, new Color(200, 255, 120), 24);
        g.shake(16);
        g.enemyKilled(this);
        g.dropPickup(x - 60, g.centerY(), Pickup.Kind.MEDKIT);
        g.dropPickup(x - 20, g.centerY() + 40, Pickup.Kind.LIFE);
        g.dropRandom(x - 20, g.centerY() - 40);
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        double f = facing; // 1 = ขาชี้ขึ้น (เกาะเพดาน), -1 = ขาชี้ลง
        // ขา
        g.setColor(new Color(70, 15, 35));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            double lx = x - 45 + i * 30;
            double wig = Math.sin(time * 6 + i) * 5;
            g.draw(new QuadCurve2D.Double(lx, y, lx + 14, y - f * 40 + wig, lx + 4, y - f * 58));
        }
        g.setStroke(new BasicStroke(1f));

        // ท้องถุงไข่ (ด้านหลัง)
        double pulse = 1 + Math.sin(time * 4) * 0.05;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x + 20, y), (float) (60 * pulse),
                new float[]{0f, 1f}, new Color[]{tint(new Color(240, 150, 180)), tint(new Color(150, 40, 80))}));
        g.fill(new Ellipse2D.Double(x - 20, y - 40 * pulse, 110 * pulse, 80 * pulse));
        // ไข่เรืองแสงข้างใน
        g.setColor(new Color(190, 255, 110, 170));
        for (int i = 0; i < 5; i++) {
            double ex = x + 5 + i * 15, ey = y + Math.sin(time * 2 + i) * 12;
            g.fill(new Ellipse2D.Double(ex, ey - 6, 12, 12));
        }
        // หัว + เขี้ยว (หันซ้าย)
        g.setColor(tint(new Color(120, 30, 60)));
        g.fill(new Ellipse2D.Double(x - 62, y - 26, 56, 52));
        g.setColor(new Color(240, 230, 200));
        Path2D fang = new Path2D.Double();
        fang.moveTo(x - 58, y - 10); fang.lineTo(x - 78, y - 2); fang.lineTo(x - 56, y + 2); fang.closePath();
        g.fill(fang);
        Path2D fang2 = new Path2D.Double();
        fang2.moveTo(x - 58, y + 10); fang2.lineTo(x - 78, y + 4); fang2.lineTo(x - 56, y - 1); fang2.closePath();
        g.fill(fang2);
        g.setColor(enraged() ? new Color(255, 60, 60) : new Color(255, 220, 60));
        for (int i = 0; i < 3; i++) g.fill(new Ellipse2D.Double(x - 48 + i * 10, y - 16, 6, 6));

        if (mode == Mode.CROSS) {
            g.setColor(new Color(255, 230, 120, 120));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(x - 75, y - 55, 170, 110));
            g.setStroke(new BasicStroke(1f));
        }
        double barY = onCeiling && mode == Mode.CLING ? y + 70 : y - 80;
        drawHpBar(g, "BROOD MOTHER", x, barY, 150);
    }
}
