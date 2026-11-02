package novaescape.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** ปุ่มสีน้ำเงินแบบใน storyboard กดด้วยเมาส์ */
public class Button {
    public final String label;
    public final Rectangle bounds;
    private final Runnable onClick;
    private boolean hover;

    public Button(String label, int centerX, int centerY, int width, int height, Runnable onClick) {
        this.label = label;
        this.bounds = new Rectangle(centerX - width / 2, centerY - height / 2, width, height);
        this.onClick = onClick;
    }

    public void setHover(Point p) {
        hover = p != null && bounds.contains(p);
    }

    public boolean isHover() { return hover; }

    /** คืน true ถ้าคลิกโดนปุ่มนี้ */
    public boolean click(Point p) {
        if (bounds.contains(p)) {
            novaescape.audio.Sound.play(novaescape.audio.Sfx.CLICK);
            onClick.run();
            return true;
        }
        return false;
    }

    public void draw(Graphics2D g) {
        int x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        int lift = hover ? -2 : 0;
        // เงา/ขอบล่าง
        g.setColor(new Color(10, 40, 110));
        g.fill(new RoundRectangle2D.Double(x, y + 5, w, h, h * 0.6, h * 0.6));
        // ตัวปุ่ม
        Color top = hover ? new Color(70, 150, 255) : new Color(40, 125, 245);
        Color bottom = hover ? new Color(30, 105, 235) : new Color(20, 90, 220);
        g.setPaint(new GradientPaint(0, y + lift, top, 0, y + h + lift, bottom));
        g.fill(new RoundRectangle2D.Double(x, y + lift, w, h, h * 0.6, h * 0.6));
        g.setColor(new Color(255, 255, 255, 60));
        g.fill(new RoundRectangle2D.Double(x + 6, y + 4 + lift, w - 12, h * 0.35, h * 0.3, h * 0.3));

        g.setFont(Text.font(Font.BOLD, Math.max(16, h * 0.45f)));
        Text.centered(g, label, x + w / 2, y + h / 2 + lift, Color.WHITE, false);
    }
}
