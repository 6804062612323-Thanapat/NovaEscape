package novaescape.assault.stage;

import novaescape.assault.AssaultGame;
import novaescape.assault.Hostage;
import novaescape.assault.Level;
import novaescape.assault.Pickup;
import novaescape.assault.enemy.*;

import java.awt.*;

/**
 * ด่าน 1: NOVA SECTOR — แถบอุกกาบาต, ฐานต่างดาว, พายุเศษซาก
 * บอส MOTHERSHIP: ยิงตรง ๆ หลบกระสุน (บอสพื้นฐาน)
 */
public class Stage1NovaSector extends Stage {

    @Override public int number() { return 1; }
    @Override public String name() { return "NOVA SECTOR"; }
    @Override public String tagline() { return "Asteroids, outposts and a mothership"; }
    @Override public String bossName() { return "MOTHERSHIP"; }
    @Override public String bossWarning() { return "THE MOTHERSHIP IS COMING"; }
    @Override public double length() { return 7200; }
    @Override public double[] sectorStarts() { return new double[]{0, 1400, 4000}; }
    @Override public String[] sectorNames() { return new String[]{"ASTEROID BELT", "ALIEN OUTPOST", "DEBRIS STORM"}; }
    @Override public double vehicleX() { return 2900; }

    @Override
    public Theme theme() {
        return new Theme(Style.NOVA,
                new Color(14, 20, 58), new Color(30, 52, 120), new Color(70, 90, 180),
                new Color(15, 12, 25, 200), new Color(120, 40, 160), new Color(120, 200, 255),
                null, new Color(30, 140, 90), new Color(160, 50, 40));
    }

    @Override
    public BossEnemy createBoss(AssaultGame g) {
        return new MothershipBoss(g);
    }

    @Override
    public void build(Level L) {
        // ===== เขต 1: ASTEROID BELT =====
        L.at(0, g -> {
            L.droneLine(g, 3, Drone.Pattern.SWEEP, 0.7);
            L.asteroid(g, 1150, 140, 34);
            L.asteroid(g, 1300, 420, 28);
        });
        L.at(300, g -> {
            for (int i = 0; i < 4; i++) L.asteroid(g, 1500 + i * 160, 100 + L.rng.nextInt(360), 22 + L.rng.nextInt(20));
            g.addHostage(new Hostage(1750, 210));
        });
        L.at(550, g -> L.droneLine(g, 4, Drone.Pattern.SWEEP, 0.45));
        L.at(800, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 40, 290, Pickup.Kind.HEAVY));
            g.spawn(new Turret(2050, 430));
        });
        L.at(1100, g -> L.kamikazes(g, 2, 0.8));

        // ===== เขต 2: ALIEN OUTPOST =====
        L.at(1400, g -> {
            g.showBanner("SECTOR 2", "ALIEN OUTPOST");
            g.spawn(new Turret(2550, 150));
            g.spawn(new Turret(2700, 440));
            L.droneLine(g, 3, Drone.Pattern.SWEEP, 0.6);
        });
        L.lockAt(1900, g -> {
            L.droneLine(g, 6, Drone.Pattern.HOVER, 0.5);
            L.kamikazes(g, 3, 1.2).forEach(e -> e.delay += 2.0);
        });
        L.at(2300, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 30, 150, Pickup.Kind.SPREAD));
            g.addHostage(new Hostage(3400, 380));
            for (int i = 0; i < 4; i++) L.asteroid(g, 3100 + i * 170, 90 + L.rng.nextInt(380), 24 + L.rng.nextInt(18));
        });
        L.at(2700, g -> {
            L.droneLine(g, 4, Drone.Pattern.SWEEP, 0.5);
            L.kamikazes(g, 2, 1.0);
            g.spawn(new Turret(3750, 300));
        });
        L.lockAt(3200, g -> {
            g.showBanner("WARNING", "CARRIER APPROACHING");
            g.spawn(new Carrier(Level.right(g) + 150, 290));
        });
        L.at(3500, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 40, 380, Pickup.Kind.LASER));
            g.spawn(new SupplyPod(Level.right(g) + 260, 140, Pickup.Kind.MEDKIT));
        });

        // ===== เขต 3: DEBRIS STORM =====
        L.at(4000, g -> {
            g.showBanner("SECTOR 3", "DEBRIS STORM");
            for (int i = 0; i < 8; i++) {
                g.spawn(new Asteroid(Level.right(g) + 60 + i * 120, 70 + L.rng.nextInt(420), 18 + L.rng.nextInt(22),
                        -120 - L.rng.nextInt(100), (L.rng.nextDouble() - 0.5) * 60).withDelay(i * 0.4));
            }
            L.droneLine(g, 4, Drone.Pattern.SWEEP, 0.6);
        });
        L.at(4500, g -> {
            g.spawn(new Turret(5550, 140));
            g.spawn(new Turret(5700, 420));
            g.spawn(new Turret(5850, 280));
            L.kamikazes(g, 3, 0.9);
            g.addHostage(new Hostage(5300, 270));
        });
        L.lockAt(4900, g -> {
            L.droneLine(g, 6, Drone.Pattern.HOVER, 0.4);
            L.kamikazes(g, 4, 0.8).forEach(e -> e.delay += 2.5);
            for (int i = 0; i < 4; i++) {
                g.spawn(new Minion(Level.right(g) + 40, 80 + i * 120, -150, 0).withDelay(4 + i * 0.3));
            }
        });
        L.at(5200, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 40, 200, Pickup.Kind.ROCKET));
            g.spawn(new SupplyPod(Level.right(g) + 200, 400, Pickup.Kind.BOMB));
            g.addHostage(new Hostage(6150, 160));
        });
        L.at(5500, g -> {
            L.droneLine(g, 5, Drone.Pattern.SWEEP, 0.45);
            L.kamikazes(g, 3, 1.0);
        });
    }
}
