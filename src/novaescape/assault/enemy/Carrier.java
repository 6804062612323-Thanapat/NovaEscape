package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;
import novaescape.ui.Text;

import java.awt.*;
import java.awt.geom.*;

/** มินิบอสกลางด่าน: ยานบรรทุก ปล่อยลูกน้องออกมาเรื่อย ๆ และยิงกระสุนกระจาย 3 ทาง */
public class Carrier extends Enemy {
    private double spawnTimer = 2.0;
    private double fireTimer = 1.5;
    private double hatchOpen;

    public Carrier(double x, double y) {
        super(x, y, 190, 90, 80, 3000, 0);
        this.miniBoss = true;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        double tx = g.camX() + g.viewW() - 160;
        x += (tx - x) * Math.min(1, 1.5 * dt);
        y = g.centerY() + Math.sin(t * 0.8) * 130;

        if (hatchOpen > 0) hatchOpen -= dt;
        if (!onScreen(g)) return;

        spawnTimer -= dt;
        if (spawnTimer <= 0) {
            spawnTimer = 3.2;
            hatchOpen = 0.6;
            g.spawn(new Minion(x - 60, y - 20, -120, -80));
            g.spawn(new Minion(x - 60, y + 20, -120, 80));
        }
        fireTimer -= dt;
        if (fireTimer <= 0) {
            fireTimer = 1.6;
            double a = angleToPlayer(g);
            for (int i = -1; i <= 1; i++) g.enemyShoot(x - 90, y, a + i * 0.22, 240);
        }
    }

    @Override
    protected void onDeath(AssaultGame g) {
        for (int i = 0; i < 5; i++) {
            g.explode(x + (Math.random() - 0.5) * w, y + (Math.random() - 0.5) * h, 25);
        }
        g.shake(14);
        g.enemyKilled(this);
        g.dropRandom(x - 40, y - 20);
        g.dropRandom(x - 40, y + 20);
        g.dropPickup(x, y, novaescape.assault.Pickup.Kind.LIFE);
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        // ลำตัว
        Path2D hull = new Path2D.Double();
        hull.moveTo(x - 95, y);
        hull.lineTo(x - 50, y - 40);
        hull.lineTo(x + 90, y - 34);
        hull.lineTo(x + 95, y + 34);
        hull.lineTo(x - 50, y + 40);
        hull.closePath();
        g.setPaint(new GradientPaint((float) x, (float) (y - 40), tint(new Color(120, 100, 170)),
                (float) x, (float) (y + 40), tint(new Color(50, 40, 90))));
        g.fill(hull);
        g.setColor(new Color(30, 25, 60));
        g.setStroke(new BasicStroke(2f));
        g.draw(hull);
        g.setStroke(new BasicStroke(1f));

        // ช่องปล่อยลูกน้อง
        double open = Math.max(0, hatchOpen) / 0.6;
        g.setColor(new Color(20, 15, 35));
        g.fill(new RoundRectangle2D.Double(x - 70, y - 10 - 8 * open, 40, 20 + 16 * open, 8, 8));
        if (open > 0) {
            g.setColor(new Color(120, 255, 150, (int) (150 * open)));
            g.fill(new RoundRectangle2D.Double(x - 66, y - 6 - 6 * open, 32, 12 + 12 * open, 6, 6));
        }
        // หน้าต่าง
        for (int i = 0; i < 4; i++) {
            boolean on = ((int) (time * 3) + i) % 3 != 0;
            g.setColor(on ? new Color(255, 220, 120) : new Color(110, 90, 60));
            g.fill(new Ellipse2D.Double(x - 10 + i * 22, y - 22, 9, 9));
        }
        // เครื่องยนต์
        g.setColor(new Color(120, 200, 255, 160));
        g.fill(new Ellipse2D.Double(x + 90, y - 22, 16 + Math.random() * 6, 14));
        g.fill(new Ellipse2D.Double(x + 90, y + 8, 16 + Math.random() * 6, 14));

        // แถบเลือด
        double bw = 140;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect((int) (x - bw / 2), (int) (y - 62), (int) bw, 8);
        g.setColor(new Color(255, 90, 90));
        g.fillRect((int) (x - bw / 2), (int) (y - 62), (int) (bw * Math.max(0, hp / maxHp)), 8);
        g.setFont(Text.font(Font.BOLD, 12));
        Text.centered(g, "CARRIER", x, y - 74, Color.WHITE, true);
    }
}
