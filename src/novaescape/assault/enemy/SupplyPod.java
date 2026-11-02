package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;
import novaescape.assault.Pickup;
import novaescape.ui.Text;

import java.awt.*;
import java.awt.geom.*;

/** กล่องเสบียงลอยในอวกาศ ยิงให้แตกแล้วจะได้ของที่กำหนดไว้แน่นอน */
public class SupplyPod extends Enemy {
    private final Pickup.Kind content;
    private final double baseY;

    public SupplyPod(double x, double y, Pickup.Kind content) {
        super(x, y, 46, 40, 3, 100, 0);
        this.content = content;
        this.baseY = y;
        this.touchDamage = false;
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        x -= 25 * dt;
        y = baseY + Math.sin(t * 2) * 8;
        y = Math.max(g.ceiling(x) + 30, Math.min(g.floor(x) - 26, y));
    }

    @Override
    protected void onDeath(AssaultGame g) {
        g.explode(x, y, 14);
        g.enemyKilled(this);
        g.dropPickup(x, y, content);
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        // ไอพ่นด้านล่าง
        g.setColor(new Color(120, 200, 255, 120 + (int) (Math.random() * 80)));
        g.fill(new Ellipse2D.Double(x - 6, y + 18, 12, 10 + Math.random() * 5));
        g.setColor(tint(new Color(200, 160, 60)));
        g.fill(new RoundRectangle2D.Double(x - 23, y - 20, 46, 40, 8, 8));
        g.setColor(tint(new Color(150, 110, 40)));
        g.fill(new Rectangle2D.Double(x - 23, y - 4, 46, 8));
        g.setColor(new Color(60, 45, 20));
        g.draw(new RoundRectangle2D.Double(x - 23, y - 20, 46, 40, 8, 8));
        boolean on = ((int) (time * 4)) % 2 == 0;
        g.setColor(on ? new Color(255, 80, 80) : new Color(120, 40, 40));
        g.fill(new Ellipse2D.Double(x - 4, y - 28, 8, 8));
        g.setFont(Text.font(Font.BOLD, 12));
        Text.centered(g, "SUPPLY", x, y - 12, new Color(60, 40, 10), false);
    }
}
