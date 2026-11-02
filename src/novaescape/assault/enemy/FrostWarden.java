package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;
import novaescape.assault.Bullet;
import novaescape.assault.Pickup;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * มินิบอสด่าน 2: FROST WARDEN — แกนคริสตัลน้ำแข็งที่มี "โล่น้ำแข็ง 4 ชิ้น" หมุนรอบตัว
 * วิธีสู้: โล่กันกระสุนทุกชนิด ต้องจับจังหวะยิงลอดช่องว่างระหว่างโล่ไปโดนแกนกลาง
 *          (จรวด/ปืนใหญ่ระเบิดใส่โล่ แรงระเบิดยังโดนแกนได้)
 * ท่า: ยิงลูกเยือกแข็ง, เรียก Ice Spinner, "Shard Storm" โล่กางออกกว้างแล้วหมุนเร็ว (แกนโล่งกว่าเดิม แต่โล่ฟาดไกล)
 * เลือดต่ำกว่าครึ่ง: โล่หมุนเร็วขึ้น + ยิงวงกระสุน 8 ทิศ
 */
public class FrostWarden extends Enemy {
    private static final int SHARDS = 4;
    private double angle;
    private double radius = 85;
    private double stormT;                 // > 0 = กำลัง Shard Storm
    private double frostTimer = 2.0, summonTimer = 5.0, stormTimer = 6.5, ringTimer = 3.0;

    public FrostWarden(double x, double y) {
        super(x, y, 60, 60, 120, 4000, 0);
        this.miniBoss = true;
    }

    private boolean enraged() { return hp < maxHp * 0.5; }

    private double[] shardPos(int i) {
        double a = angle + Math.PI * 2 * i / SHARDS;
        return new double[]{x + Math.cos(a) * radius, y + Math.sin(a) * radius, a};
    }

    @Override
    public List<Rectangle2D> blockers() {
        List<Rectangle2D> list = new ArrayList<>();
        for (int i = 0; i < SHARDS; i++) {
            double[] p = shardPos(i);
            list.add(new Rectangle2D.Double(p[0] - 20, p[1] - 20, 40, 40));
        }
        return list;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        double tx = g.camX() + g.viewW() - 200;
        x += (tx - x) * Math.min(1, 1.4 * dt);
        y = g.centerY() + Math.sin(t * 0.7) * 110;

        boolean storm = stormT > 0;
        double spin = (enraged() ? 1.9 : 1.3) * (storm ? 2.4 : 1);
        angle += spin * dt;
        double targetR = storm ? 175 : 85;
        radius += (targetR - radius) * Math.min(1, 4 * dt);
        if (stormT > 0) stormT -= dt;

        if (!onScreen(g) || g.enemiesJammed()) return;

        frostTimer -= dt;
        if (frostTimer <= 0) {
            frostTimer = enraged() ? 1.8 : 2.4;
            g.enemyBullet(Bullet.frost(x - 30, y, angleToPlayer(g), 190));
        }
        summonTimer -= dt;
        if (summonTimer <= 0) {
            summonTimer = 7.5;
            g.spawn(new IceSpinner(x - 60, y - 70));
            g.spawn(new IceSpinner(x - 60, y + 70));
        }
        stormTimer -= dt;
        if (stormTimer <= 0) {
            stormTimer = 7.0;
            stormT = 2.2;
            g.shake(5);
        }
        if (enraged()) {
            ringTimer -= dt;
            if (ringTimer <= 0) {
                ringTimer = 3.6;
                for (int i = 0; i < 8; i++) g.enemyShoot(x, y, Math.PI * 2 * i / 8 + t, 200);
            }
        }
    }

    @Override
    protected void onDeath(AssaultGame g) {
        for (int i = 0; i < SHARDS; i++) {
            double[] p = shardPos(i);
            g.explode(p[0], p[1], 14);
        }
        g.explode(x, y, 40);
        g.shake(14);
        g.enemyKilled(this);
        g.dropPickup(x - 40, y, Pickup.Kind.LIFE);
        g.dropRandom(x - 40, y + 40);
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        // แสงเย็นรอบตัว
        g.setColor(new Color(150, 220, 255, 40));
        g.fill(new Ellipse2D.Double(x - radius - 30, y - radius - 30, (radius + 30) * 2, (radius + 30) * 2));

        // แกนคริสตัล (ทรงแปดหน้า)
        double pulse = 1 + Math.sin(time * 5) * 0.06;
        double r = 32 * pulse;
        Path2D core = new Path2D.Double();
        core.moveTo(x, y - r * 1.3);
        core.lineTo(x + r, y);
        core.lineTo(x, y + r * 1.3);
        core.lineTo(x - r, y);
        core.closePath();
        g.setPaint(new GradientPaint((float) (x - r), (float) (y - r), tint(new Color(230, 250, 255)),
                (float) (x + r), (float) (y + r), tint(enraged() ? new Color(120, 120, 255) : new Color(80, 160, 230))));
        g.fill(core);
        g.setColor(Color.WHITE);
        g.draw(core);
        g.setColor(enraged() ? new Color(255, 80, 120) : new Color(80, 240, 255));
        g.fill(new Ellipse2D.Double(x - 7, y - 7, 14, 14));

        // โล่น้ำแข็ง
        for (int i = 0; i < SHARDS; i++) {
            double[] p = shardPos(i);
            AffineTransform at = g.getTransform();
            g.translate(p[0], p[1]);
            g.rotate(p[2] + Math.PI / 2);
            Path2D s = new Path2D.Double();
            s.moveTo(-22, 8);
            s.lineTo(-12, -14);
            s.lineTo(12, -14);
            s.lineTo(22, 8);
            s.lineTo(0, 16);
            s.closePath();
            g.setColor(stormT > 0 ? new Color(255, 255, 255, 230) : new Color(180, 230, 255, 220));
            g.fill(s);
            g.setColor(new Color(90, 160, 220));
            g.draw(s);
            g.setTransform(at);
        }

        drawHpBar(g, "FROST WARDEN", x, y - radius - 50, 150);
    }
}
