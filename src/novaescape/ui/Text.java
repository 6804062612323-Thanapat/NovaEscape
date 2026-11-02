package novaescape.ui;

import java.awt.*;

/** ตัวช่วยวาดข้อความ (จัดกึ่งกลาง / มีเงา) */
public final class Text {
    private Text() {}

    private static final String FAMILY = "Monospaced";

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size));
    }

    /** วาดข้อความให้กึ่งกลางอยู่ที่ (cx, cy) */
    public static void centered(Graphics2D g, String s, double cx, double cy, Color color, boolean shadow) {
        FontMetrics fm = g.getFontMetrics();
        int x = (int) Math.round(cx - fm.stringWidth(s) / 2.0);
        int y = (int) Math.round(cy - (fm.getAscent() + fm.getDescent()) / 2.0 + fm.getAscent());
        if (shadow) {
            g.setColor(new Color(0, 0, 0, 150));
            g.drawString(s, x + 3, y + 3);
        }
        g.setColor(color);
        g.drawString(s, x, y);
    }

    /** ตัวหนังสือใหญ่แบบมีขอบดำหนา ๆ (ใช้กับชื่อเกม / GAME OVER / WIN) */
    public static void title(Graphics2D g, String s, double cx, double cy, float size, Color fill) {
        g.setFont(font(Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        int x = (int) Math.round(cx - fm.stringWidth(s) / 2.0);
        int y = (int) Math.round(cy - (fm.getAscent() + fm.getDescent()) / 2.0 + fm.getAscent());
        int o = Math.max(2, Math.round(size / 18f));
        g.setColor(new Color(10, 15, 40));
        for (int dx = -o; dx <= o; dx++) {
            for (int dy = -o; dy <= o; dy++) {
                if (dx != 0 || dy != 0) g.drawString(s, x + dx, y + dy);
            }
        }
        g.drawString(s, x + o + 2, y + o + 3);
        g.setColor(fill);
        g.drawString(s, x, y);
    }
}
