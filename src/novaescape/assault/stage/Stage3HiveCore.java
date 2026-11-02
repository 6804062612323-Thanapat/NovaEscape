package novaescape.assault.stage;

import novaescape.assault.AssaultGame;
import novaescape.assault.Hostage;
import novaescape.assault.Level;
import novaescape.assault.Pickup;
import novaescape.assault.enemy.*;

import java.awt.*;
import java.awt.geom.*;

/**
 * ด่าน 3: HIVE CORE — อุโมงค์ภายในรังเอเลี่ยน (6 เขต, มินิบอส BROOD MOTHER ในเขต 4, กรดหยดในเขต 4)
 * กลไกพิเศษ: ผนังเนื้อเยื่อบน/ล่างบีบเข้ามา ชนแล้วเจ็บ กระสุนชนผนังแล้วหาย
 * ศัตรู: Spore Pod / Egg Sac (ติดผนัง), Larva (ตัวอ่อน), Hive Guard (เกราะหนา พุ่งชาร์จ)
 * บอส HIVE QUEEN: ทำลายตา 3 ดวงก่อน แล้วค่อยยิงหัวใจ
 */
public class Stage3HiveCore extends Stage {

    @Override public int number() { return 3; }
    @Override public String name() { return "HIVE CORE"; }
    @Override public String tagline() { return "Living tunnels and the queen's eyes"; }
    @Override public String bossName() { return "HIVE QUEEN"; }
    @Override public String bossWarning() { return "THE QUEEN AWAKENS"; }
    @Override public double length() { return 13000; }
    @Override public double[] sectorStarts() { return new double[]{0, 1500, 4000, 6000, 8000, 10000}; }
    @Override public String[] sectorNames() {
        return new String[]{"HIVE MOUTH", "BROOD NEST", "CORE VEINS", "ACID GROTTO", "NERVE CLUSTER", "QUEEN'S PASSAGE"};
    }
    @Override public double vehicleX() { return 6500; }

    /** เขต ACID GROTTO: กรดหยดจากเพดาน */
    private static final double ACID_FROM = 6000, ACID_TO = 8000;
    private double dripTimer = 1.0;
    @Override public boolean hasWalls() { return true; }

    @Override
    public Theme theme() {
        return new Theme(Style.HIVE,
                new Color(38, 6, 24), new Color(85, 18, 45), new Color(200, 60, 110),
                new Color(30, 5, 15, 210), new Color(150, 20, 60), new Color(255, 120, 170),
                null, new Color(120, 160, 40), new Color(180, 30, 60), new Color(110, 200, 40),
                new Color(200, 80, 160), new Color(150, 10, 40));
    }

    @Override
    public BossEnemy createBoss(AssaultGame g) {
        return new HiveQueenBoss(g);
    }

    // ---------- ผนังอุโมงค์ ----------
    /** ความหนาของผนัง (0 = ไม่มีผนัง) */
    private double depth(double x, int side) {
        double ramp = Math.max(0, Math.min(1, (x - 500) / 500));       // ช่วงต้นด่านยังไม่มีผนัง
        double arena = arenaX() - 200;
        double taper = x > arena ? Math.max(0.18, 1 - (x - arena) / 400) : 1; // ห้องบอสผนังบางลง
        double k = side * 2.1;
        // ผนังหนาสุดข้างละประมาณ 100 px (ช่องบินกว้างอย่างน้อยประมาณ 290 px)
        double base = 28 + 38 * (0.5 + 0.5 * Math.sin(x / 340 + k)) + 16 * Math.sin(x / 131 + k * 2);
        // ช่วงคอขวด
        if (x > 3300 && x < 3900) base += 18 * Math.sin((x - 3300) / 600 * Math.PI);
        if (x > 10300 && x < 10900) base += 15 * Math.sin((x - 10300) / 600 * Math.PI);
        return Math.max(0, base) * ramp * taper;
    }

    @Override
    public double ceiling(double x, AssaultGame g) {
        return g.top() + depth(x, 0);
    }

    @Override
    public double floor(double x, AssaultGame g) {
        return g.bottom() - depth(x, 1);
    }

    @Override
    public void drawWorld(Graphics2D g2, AssaultGame g) {
        double x0 = g.camX() - 30, x1 = g.camX() + g.viewW() + 30;
        double step = 12;
        Path2D top = new Path2D.Double();
        Path2D bot = new Path2D.Double();
        top.moveTo(x0, -20);
        bot.moveTo(x0, g.bottom() + 20);
        for (double x = x0; x <= x1 + step; x += step) {
            top.lineTo(x, ceiling(x, g));
            bot.lineTo(x, floor(x, g));
        }
        top.lineTo(x1 + step, -20);
        top.closePath();
        bot.lineTo(x1 + step, g.bottom() + 20);
        bot.closePath();

        Color dark = new Color(70, 12, 35), mid = new Color(140, 35, 70);
        g2.setPaint(new GradientPaint(0f, (float) g.top(), dark, 0f, (float) (g.top() + 140), mid));
        g2.fill(top);
        g2.setPaint(new GradientPaint(0f, (float) (g.bottom() - 140), mid, 0f, (float) g.bottom(), dark));
        g2.fill(bot);

        // ขอบเนื้อเยื่อ + ฟันแหลม
        double pulse = 0.5 + 0.5 * Math.sin(g.time() * 2.5);
        g2.setStroke(new BasicStroke(4f));
        g2.setColor(new Color(255, 120, 160, (int) (120 + 80 * pulse)));
        drawEdge(g2, g, x0, x1, step, true);
        drawEdge(g2, g, x0, x1, step, false);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(240, 220, 200));
        double firstTooth = Math.floor(x0 / 70) * 70;
        for (double x = firstTooth; x < x1; x += 70) {
            double c = ceiling(x, g), f = floor(x, g);
            if (c > g.top() + 8) {
                Path2D t = new Path2D.Double();
                t.moveTo(x - 7, c - 2); t.lineTo(x, c + 14); t.lineTo(x + 7, c - 2); t.closePath();
                g2.fill(t);
            }
            if (f < g.bottom() - 8) {
                Path2D t = new Path2D.Double();
                t.moveTo(x + 28, f + 2); t.lineTo(x + 35, f - 14); t.lineTo(x + 42, f + 2); t.closePath();
                g2.fill(t);
            }
        }
        // เส้นเลือดในผนัง
        g2.setColor(new Color(255, 80, 130, 70));
        g2.setStroke(new BasicStroke(2f));
        for (double x = Math.floor(x0 / 160) * 160; x < x1; x += 160) {
            double c = ceiling(x, g);
            if (c > g.top() + 30) g2.draw(new QuadCurve2D.Double(x, g.top(), x + 30, c - 20, x + 10, c - 4));
            double f = floor(x + 80, g);
            if (f < g.bottom() - 30) g2.draw(new QuadCurve2D.Double(x + 80, g.bottom(), x + 50, f + 20, x + 70, f + 4));
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawEdge(Graphics2D g2, AssaultGame g, double x0, double x1, double step, boolean ceil) {
        Path2D edge = new Path2D.Double();
        boolean first = true;
        for (double x = x0; x <= x1 + step; x += step) {
            double y = ceil ? ceiling(x, g) : floor(x, g);
            if (first) { edge.moveTo(x, y); first = false; } else edge.lineTo(x, y);
        }
        g2.draw(edge);
    }

    // ---------- กรดหยด (เขต ACID GROTTO) ----------
    @Override
    public void update(double dt, AssaultGame g) {
        if (g.camX() < ACID_FROM - 300 || g.camX() > ACID_TO || g.inBossArena()) return;
        dripTimer -= dt;
        if (dripTimer <= 0) {
            dripTimer = 0.5 + Math.random() * 0.3;
            double x = g.camX() + 80 + Math.random() * (g.viewW() - 120);
            g.enemyBullet(novaescape.assault.Bullet.acid(x, g.ceiling(x) + 4, 0, 20));
        }
    }

    // ---------- สคริปต์ด่าน ----------
    @Override
    public void build(Level L) {
        // ===== เขต 1: HIVE MOUTH =====
        L.at(0, g -> {
            L.droneLine(g, 3, Drone.Pattern.SWEEP, 0.8);
            g.toast("WATCH THE WALLS!");
        });
        L.at(300, g -> {
            g.spawn(new WallGrowth(1450, WallGrowth.Kind.SPORE_POD, true));
            g.spawn(new WallGrowth(1650, WallGrowth.Kind.EGG_SAC, false));
            g.addHostage(new Hostage(1750, 270));
        });
        L.at(650, g -> {
            for (int i = 0; i < 4; i++) {
                double x = Level.right(g) + 40;
                g.spawn(new Larva(x, L.randY(g, x, 40), Larva.Kind.LARVA, -150, 0).withDelay(i * 0.5));
            }
            g.spawn(new SupplyPod(Level.right(g) + 100, 290, Pickup.Kind.HEAVY));
        });
        L.at(1000, g -> {
            g.spawn(new WallGrowth(2050, WallGrowth.Kind.SPORE_POD, false));
            g.spawn(new WallGrowth(2200, WallGrowth.Kind.EGG_SAC, true));
            g.spawn(new HiveGuard(Level.right(g) + 80, 290));
        });

        // ===== เขต 2: BROOD NEST =====
        L.lockAt(1500, g -> {
            g.showBanner("SECTOR 2", "BROOD NEST");
            g.spawn(new HiveGuard(Level.right(g) + 60, 200));
            g.spawn(new HiveGuard(Level.right(g) + 100, 380).withDelay(2));
            for (int i = 0; i < 5; i++) {
                double x = Level.right(g) + 40;
                g.spawn(new Larva(x, L.randY(g, x, 40), Larva.Kind.LARVA, -150, 0).withDelay(3.5 + i * 0.4));
            }
        });
        L.at(1800, g -> {
            for (int i = 0; i < 4; i++) {
                g.spawn(new WallGrowth(2900 + i * 220, i % 2 == 0 ? WallGrowth.Kind.EGG_SAC : WallGrowth.Kind.SPORE_POD, i % 2 == 0));
            }
            g.spawn(new SupplyPod(Level.right(g) + 60, 300, Pickup.Kind.SPREAD));
            g.addHostage(new Hostage(3100, 300));
        });
        L.at(2300, g -> {
            L.kamikazes(g, 3, 0.9);
            g.spawn(new HiveGuard(Level.right(g) + 60, 300).withDelay(2));
        });
        L.at(2900, g -> {
            g.toast("THE TUNNEL NARROWS...");
            for (int i = 0; i < 3; i++) g.spawn(new WallGrowth(3900 + i * 150, WallGrowth.Kind.SPORE_POD, i % 2 == 1));
            g.spawn(new SupplyPod(Level.right(g) + 60, 290, Pickup.Kind.MEDKIT));
        });
        L.lockAt(3400, g -> {
            g.showBanner("WARNING", "BROOD SWARM");
            for (int i = 0; i < 8; i++) {
                double x = Level.right(g) + 40;
                g.spawn(new Larva(x, L.randY(g, x, 40), Larva.Kind.LARVA, -150, 0).withDelay(i * 0.45));
            }
            g.spawn(new HiveGuard(Level.right(g) + 80, 220).withDelay(2.5));
            g.spawn(new HiveGuard(Level.right(g) + 80, 360).withDelay(4.5));
        });

        // ===== เขต 3: CORE VEINS =====
        L.at(4000, g -> {
            g.showBanner("SECTOR 3", "CORE VEINS");
            g.spawn(new SupplyPod(Level.right(g) + 60, 220, Pickup.Kind.ROCKET));
            for (int i = 0; i < 5; i++) {
                g.spawn(new WallGrowth(5000 + i * 180, i % 2 == 0 ? WallGrowth.Kind.SPORE_POD : WallGrowth.Kind.EGG_SAC, i % 2 == 0));
            }
            g.addHostage(new Hostage(5300, 290));
        });
        L.at(4500, g -> {
            L.droneLine(g, 4, Drone.Pattern.SWEEP, 0.5);
            g.spawn(new HiveGuard(Level.right(g) + 60, 280).withDelay(1.5));
        });
        L.lockAt(5000, g -> {
            g.spawn(new HiveGuard(Level.right(g) + 60, 180));
            g.spawn(new HiveGuard(Level.right(g) + 80, 400).withDelay(1.5));
            L.kamikazes(g, 4, 0.8).forEach(e -> e.delay += 3);
            for (int i = 0; i < 6; i++) {
                double x = Level.right(g) + 40;
                g.spawn(new Larva(x, L.randY(g, x, 40), Larva.Kind.LARVA, -150, 0).withDelay(5 + i * 0.35));
            }
        });
        L.at(5400, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 60, 200, Pickup.Kind.LASER));
            g.spawn(new SupplyPod(Level.right(g) + 220, 380, Pickup.Kind.BOMB));
            g.spawn(new WallGrowth(6300, WallGrowth.Kind.SPORE_POD, true));
            g.spawn(new WallGrowth(6450, WallGrowth.Kind.SPORE_POD, false));
        });
        L.at(5700, g -> {
            for (int i = 0; i < 5; i++) {
                double x = Level.right(g) + 40;
                g.spawn(new Larva(x, L.randY(g, x, 40), Larva.Kind.LARVA, -150, 0).withDelay(i * 0.5));
            }
        });

        // ===== เขต 4: ACID GROTTO (กรดหยด + มินิบอส BROOD MOTHER) =====
        L.at(6000, g -> {
            g.showBanner("SECTOR 4", "ACID GROTTO");
            g.toast("WATCH FOR ACID DRIPS!");
            g.spawn(new WallGrowth(7000, WallGrowth.Kind.SPORE_POD, false));
            g.spawn(new WallGrowth(7200, WallGrowth.Kind.EGG_SAC, true));
        });
        L.at(6300, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 60, 300, Pickup.Kind.HEAVY));
            g.addHostage(new Hostage(7250, 290));
            for (int i = 0; i < 4; i++) {
                double x = Level.right(g) + 40;
                g.spawn(new Larva(x, L.randY(g, x, 40), Larva.Kind.LARVA, -150, 0).withDelay(1 + i * 0.4));
            }
        });
        L.at(6700, g -> {
            L.kamikazes(g, 3, 0.9);
            g.spawn(new HiveGuard(Level.right(g) + 60, 290).withDelay(1.5));
        });
        L.lockAt(7300, g -> {
            g.showBanner("WARNING", "BROOD MOTHER");
            g.spawn(new BroodMother(Level.right(g) + 160, 200));
        });
        L.at(7600, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 60, 220, Pickup.Kind.ROCKET));
            g.spawn(new SupplyPod(Level.right(g) + 240, 380, Pickup.Kind.MEDKIT));
        });

        // ===== เขต 5: NERVE CLUSTER =====
        L.at(8000, g -> {
            g.showBanner("SECTOR 5", "NERVE CLUSTER");
            for (int i = 0; i < 4; i++) {
                g.spawn(new WallGrowth(9000 + i * 190, i % 2 == 0 ? WallGrowth.Kind.SPORE_POD : WallGrowth.Kind.EGG_SAC, i % 2 == 1));
            }
            L.droneLine(g, 3, Drone.Pattern.SWEEP, 0.6);
        });
        L.at(8400, g -> {
            for (int i = 0; i < 6; i++) {
                double x = Level.right(g) + 40;
                g.spawn(new Larva(x, L.randY(g, x, 40), Larva.Kind.LARVA, -150, 0).withDelay(i * 0.4));
            }
            g.spawn(new HiveGuard(Level.right(g) + 80, 250).withDelay(2));
        });
        L.lockAt(9000, g -> {
            g.showBanner("WARNING", "ELITE GUARDS");
            g.spawn(new HiveGuard(Level.right(g) + 60, 160));
            g.spawn(new HiveGuard(Level.right(g) + 80, 300).withDelay(1.2));
            g.spawn(new HiveGuard(Level.right(g) + 100, 430).withDelay(2.4));
            L.kamikazes(g, 3, 0.8).forEach(e -> e.delay += 4);
        });
        L.at(9400, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 60, 220, Pickup.Kind.LASER));
            g.spawn(new SupplyPod(Level.right(g) + 240, 380, Pickup.Kind.BOMB));
            g.addHostage(new Hostage(10300, 300));
        });

        // ===== เขต 6: QUEEN'S PASSAGE =====
        L.at(10000, g -> {
            g.showBanner("SECTOR 6", "QUEEN'S PASSAGE");
            g.spawn(new WallGrowth(11000, WallGrowth.Kind.SPORE_POD, true));
            g.spawn(new WallGrowth(11150, WallGrowth.Kind.SPORE_POD, false));
            g.spawn(new WallGrowth(11300, WallGrowth.Kind.EGG_SAC, true));
            for (int i = 0; i < 5; i++) {
                double x = Level.right(g) + 40;
                g.spawn(new Larva(x, L.randY(g, x, 40), Larva.Kind.LARVA, -150, 0).withDelay(i * 0.45));
            }
        });
        L.at(10500, g -> {
            g.spawn(new HiveGuard(Level.right(g) + 60, 200));
            g.spawn(new HiveGuard(Level.right(g) + 100, 400).withDelay(1.5));
            L.kamikazes(g, 3, 0.9).forEach(e -> e.delay += 2);
        });
        L.at(10900, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 60, 220, Pickup.Kind.MEDKIT));
            g.spawn(new SupplyPod(Level.right(g) + 240, 380, Pickup.Kind.SPREAD));
        });
        L.at(11200, g -> {
            for (int i = 0; i < 6; i++) {
                double x = Level.right(g) + 40;
                g.spawn(new Larva(x, L.randY(g, x, 40), Larva.Kind.LARVA, -150, 0).withDelay(i * 0.4));
            }
        });
    }
}
