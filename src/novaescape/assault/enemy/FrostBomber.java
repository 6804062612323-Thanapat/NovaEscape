package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;
import novaescape.assault.Bullet;

import java.awt.*;
import java.awt.geom.*;

/** (ด่าน 2) เครื่องบินทิ้งระเบิด บินผ่านแถวบนแล้วทิ้งระเบิดน้ำแข็งที่แตกกระจาย 6 ทิศ */
public class FrostBomber extends Enemy {
    private double dropTimer = 0.6;
    private final double laneOffset;

    public FrostBomber(double x, double laneOffset) {
        super(x, 0, 70, 26, 6, 250, 0.35);
        this.laneOffset = laneOffset;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        x -= 150 * dt;
        y = g.ceiling(x) + 40 + laneOffset + Math.sin(t * 2) * 6;
        dropTimer -= dt;
        if (dropTimer <= 0 && onScreen(g)) {
            dropTimer = 1.1;
            g.enemyBullet(Bullet.iceBomb(x, y + 14, -60));
        }
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        Path2D body = new Path2D.Double();
        body.moveTo(x - 36, y);
        body.lineTo(x - 10, y - 9);
        body.lineTo(x + 30, y - 7);
        body.lineTo(x + 36, y - 16);
        body.lineTo(x + 38, y + 4);
        body.lineTo(x - 10, y + 9);
        body.closePath();
        g.setColor(tint(new Color(220, 235, 250)));
        g.fill(body);
        g.setColor(tint(new Color(80, 130, 190)));
        Path2D wing = new Path2D.Double();
        wing.moveTo(x - 4, y + 2);
        wing.lineTo(x + 20, y + 2);
        wing.lineTo(x + 26, y + 16);
        wing.lineTo(x + 12, y + 16);
        wing.closePath();
        g.fill(wing);
        g.setColor(new Color(90, 200, 255));
        g.fill(new Ellipse2D.Double(x - 24, y - 6, 12, 7));
        g.setColor(new Color(150, 230, 255, 150 + (int) (Math.random() * 100)));
        g.fill(new Ellipse2D.Double(x + 36, y - 5, 12, 9));
    }
}
