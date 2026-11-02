package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

import java.awt.*;
import java.awt.geom.*;

/**
 * (ด่าน 3) สิ่งที่งอกจากผนังอุโมงค์
 * SPORE_POD = ฝักสปอร์ ปล่อยสปอร์ไล่ตามเป็นระยะ
 * EGG_SAC   = ถุงไข่ ถ้าบินเข้าใกล้จะฟักตัวอ่อน 3 ตัว (ยิงทิ้งก่อนได้)
 */
public class WallGrowth extends Enemy {
    public enum Kind { SPORE_POD, EGG_SAC }

    private final Kind kind;
    private final boolean onCeiling;
    private double fireTimer = 1.5 + Math.random();
    private boolean placed;

    public WallGrowth(double x, Kind kind, boolean onCeiling) {
        super(x, 0, kind == Kind.SPORE_POD ? 44 : 50, kind == Kind.SPORE_POD ? 40 : 44,
                kind == Kind.SPORE_POD ? 8 : 4, kind == Kind.SPORE_POD ? 250 : 150, 0.35);
        this.kind = kind;
        this.onCeiling = onCeiling;
        this.touchDamage = false;
    }

    /** กล่องชนใหญ่กว่าตัว ยิงโดนง่าย (ยื่นไปทางกลางอุโมงค์) */
    @Override
    public java.awt.geom.Rectangle2D hitbox() {
        double extra = 26;
        double top = onCeiling ? y - h / 2 : y - h / 2 - extra;
        return new java.awt.geom.Rectangle2D.Double(x - w / 2 - 8, top, w + 16, h + extra);
    }

    @Override
    protected void update(double dt, AssaultGame g) {
        // ยื่นออกมาจากผนังมากขึ้น จะได้อยู่ในระดับที่ยานยิงถึง
        y = onCeiling ? g.ceiling(x) + h / 2 + 8 : g.floor(x) - h / 2 - 8;
        placed = true;
        if (!onScreen(g)) return;
        if (kind == Kind.SPORE_POD) {
            fireTimer -= dt;
            if (fireTimer <= 0 && !g.enemiesJammed()) {
                fireTimer = 2.8;
                g.spawn(new Larva(x, y + (onCeiling ? 20 : -20), Larva.Kind.SPORE, -40, onCeiling ? 80 : -80));
            }
        } else {
            double dx = x - g.player().centerX();
            if (dx < 260 && dx > -60) {
                // ฟัก!
                dead = true;
                g.sparks(x, y, new Color(220, 255, 140), 14);
                for (int i = 0; i < 3; i++) {
                    g.spawn(new Larva(x, y, Larva.Kind.LARVA, -120 + i * 40, onCeiling ? 140 : -140));
                }
            }
        }
    }

    @Override
    protected void draw(Graphics2D g, double time) {
        if (!placed) return;
        double dir = onCeiling ? 1 : -1;
        if (kind == Kind.SPORE_POD) {
            double pulse = 1 + Math.max(0, 0.5 - fireTimer) * 0.4;
            g.setColor(tint(new Color(120, 40, 70)));
            g.fill(new Ellipse2D.Double(x - 20 * pulse, y - 18 * pulse, 40 * pulse, 36 * pulse));
            g.setColor(tint(new Color(190, 240, 100)));
            g.fill(new Ellipse2D.Double(x - 9, y + dir * 6 - 7, 18, 14));
            g.setColor(new Color(90, 20, 40));
            for (int i = -1; i <= 1; i++) {
                g.draw(new Line2D.Double(x + i * 12, y - dir * 16, x + i * 6, y + dir * 2));
            }
        } else {
            double wob = Math.sin(time * 4 + x) * 2;
            g.setColor(tint(new Color(230, 200, 150, 220)));
            g.fill(new Ellipse2D.Double(x - 24 + wob, y - 20, 48 - wob * 2, 40));
            g.setColor(new Color(150, 220, 90, 160));
            for (int i = 0; i < 3; i++) {
                g.fill(new Ellipse2D.Double(x - 16 + i * 11, y - 6 + Math.sin(time * 3 + i) * 3, 10, 10));
            }
            g.setColor(new Color(120, 70, 50));
            g.draw(new Ellipse2D.Double(x - 24 + wob, y - 20, 48 - wob * 2, 40));
        }
    }
}
