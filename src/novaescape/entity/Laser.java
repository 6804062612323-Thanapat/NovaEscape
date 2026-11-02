package novaescape.entity;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/** เลเซอร์ที่ยานต่างดาวยิงออกมา วิ่งจากขวาไปซ้าย */
public class Laser {
    public double x, y;          // x = ปลายด้านซ้าย, y = กึ่งกลางเส้น
    public final double length;
    public final double speed;

    public Laser(double x, double y, double length, double speed) {
        this.x = x;
        this.y = y;
        this.length = length;
        this.speed = speed;
    }

    public void update(double dt) {
        x -= speed * dt;
    }

    public boolean isOffScreen() {
        return x + length < 0;
    }

    public Rectangle2D hitbox() {
        return new Rectangle2D.Double(x, y - 2, length, 4);
    }

    public void draw(Graphics2D g) {
        Line2D line = new Line2D.Double(x, y, x + length, y);
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(60, 200, 255, 50));
        g.draw(line);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(110, 225, 255));
        g.draw(line);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(line);
        g.setStroke(old);
    }
}
