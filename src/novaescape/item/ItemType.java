package novaescape.item;

import java.awt.*;
import java.awt.geom.*;

/**
 * ไอเทมที่เลือกได้หลังผ่านแต่ละตา
 * อยากเพิ่มไอเทมใหม่: เพิ่มค่าใน enum นี้ + วาดไอคอนใน drawIcon() + ใส่ผลของมันใน GamePanel.useItem()
 */
public enum ItemType {
    HEART("HEART", "Revive once when hit", false),
    SHIELD("SHIELD", "Invincible for 5 sec", true),
    ATTACK("ATTACK", "Enemy stops firing 5 sec", true);

    public final String label;
    public final String description;
    /** true = ต้องกด Spacebar เพื่อใช้, false = ทำงานเองอัตโนมัติ */
    public final boolean activatable;

    ItemType(String label, String description, boolean activatable) {
        this.label = label;
        this.description = description;
        this.activatable = activatable;
    }

    /** วาดไอคอนไอเทม โดย (x, y) คือมุมซ้ายบน, size คือขนาดกล่อง */
    public void drawIcon(Graphics2D g, double x, double y, double size) {
        switch (this) {
            case HEART:  drawHeart(g, x, y, size); break;
            case SHIELD: drawShield(g, x, y, size); break;
            case ATTACK: drawFireball(g, x, y, size); break;
            default: break;
        }
    }

    // หัวใจสไตล์พิกเซล
    private static final String[] HEART_PIXELS = {
            ".XX...XX.",
            "XRRX.XRRX",
            "XRWRXRRRX",
            "XRRRRRRRX",
            "XRRRRRRRX",
            ".XRRRRRX.",
            "..XRRRX..",
            "...XRX...",
            "....X....",
    };

    private static void drawHeart(Graphics2D g, double x, double y, double size) {
        double p = size / HEART_PIXELS.length;
        for (int r = 0; r < HEART_PIXELS.length; r++) {
            for (int c = 0; c < HEART_PIXELS[r].length(); c++) {
                char ch = HEART_PIXELS[r].charAt(c);
                if (ch == '.') continue;
                g.setColor(ch == 'X' ? new Color(40, 10, 20) : ch == 'W' ? Color.WHITE : new Color(235, 30, 45));
                g.fill(new Rectangle2D.Double(x + c * p, y + r * p, Math.ceil(p), Math.ceil(p)));
            }
        }
    }

    private static void drawShield(Graphics2D g, double x, double y, double s) {
        Path2D sh = new Path2D.Double();
        sh.moveTo(x + s * 0.12, y + s * 0.08);
        sh.lineTo(x + s * 0.88, y + s * 0.08);
        sh.lineTo(x + s * 0.88, y + s * 0.55);
        sh.lineTo(x + s * 0.5, y + s * 0.95);
        sh.lineTo(x + s * 0.12, y + s * 0.55);
        sh.closePath();
        g.setColor(new Color(110, 110, 118));
        g.fill(sh);
        g.setStroke(new BasicStroke((float) (s * 0.07)));
        g.setColor(new Color(30, 30, 36));
        g.draw(sh);
        g.setStroke(new BasicStroke(1f));
        Path2D inner = new Path2D.Double();
        inner.moveTo(x + s * 0.25, y + s * 0.2);
        inner.lineTo(x + s * 0.5, y + s * 0.2);
        inner.lineTo(x + s * 0.5, y + s * 0.78);
        inner.lineTo(x + s * 0.25, y + s * 0.52);
        inner.closePath();
        g.setColor(new Color(150, 150, 160));
        g.fill(inner);
    }

    private static void drawFireball(Graphics2D g, double x, double y, double s) {
        // หาง
        Path2D tail = new Path2D.Double();
        tail.moveTo(x + s * 0.95, y + s * 0.05);
        tail.lineTo(x + s * 0.62, y + s * 0.62);
        tail.lineTo(x + s * 0.35, y + s * 0.42);
        tail.closePath();
        g.setColor(new Color(255, 170, 60, 200));
        g.fill(tail);
        Path2D tail2 = new Path2D.Double();
        tail2.moveTo(x + s * 0.8, y + s * 0.02);
        tail2.lineTo(x + s * 0.55, y + s * 0.5);
        tail2.lineTo(x + s * 0.42, y + s * 0.42);
        tail2.closePath();
        g.setColor(new Color(255, 230, 140, 220));
        g.fill(tail2);
        // ลูกไฟ
        Ellipse2D ball = new Ellipse2D.Double(x + s * 0.1, y + s * 0.4, s * 0.5, s * 0.5);
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(x + s * 0.35, y + s * 0.65), (float) (s * 0.25),
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(255, 250, 200), new Color(255, 160, 40), new Color(230, 60, 30)}));
        g.fill(ball);
    }
}
