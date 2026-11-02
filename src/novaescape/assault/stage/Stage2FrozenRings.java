package novaescape.assault.stage;

import novaescape.Config;
import novaescape.assault.AssaultGame;
import novaescape.assault.Hostage;
import novaescape.assault.Level;
import novaescape.assault.Pickup;
import novaescape.assault.enemy.*;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 * ด่าน 2: FROZEN RINGS — วงแหวนน้ำแข็งรอบดาวเยือกแข็ง (4 เขต, มินิบอส FROST WARDEN ในเขต 3)
 * กลไกพิเศษ: พายุหิมะ (BLIZZARD) พัดยานถอยหลังเป็นระยะ ต้องฝืนบังคับ
 * ศัตรู: Shield Drone (โล่หน้า), Ice Spinner (แตกเป็นเศษ), Frost Bomber (ทิ้งระเบิด), Frost Cannon (ลูกเยือกแข็ง)
 * บอส CRYO SERPENT: ยิงได้แค่หัว
 */
public class Stage2FrozenRings extends Stage {

    // ---------- พายุหิมะ ----------
    private enum Weather { CALM, WARN, GUST }
    private Weather weather = Weather.CALM;
    private double weatherT = 9;
    private static final double GUST_PUSH = -170;
    private final double[] snowX = new double[140], snowY = new double[140], snowS = new double[140];

    public Stage2FrozenRings() {
        for (int i = 0; i < snowX.length; i++) {
            snowX[i] = Math.random() * Config.WIDTH;
            snowY[i] = Math.random() * Config.HEIGHT;
            snowS[i] = 0.4 + Math.random() * 0.8;
        }
    }

    @Override public int number() { return 2; }
    @Override public String name() { return "FROZEN RINGS"; }
    @Override public String tagline() { return "Blizzards, shields and an ice serpent"; }
    @Override public String bossName() { return "CRYO SERPENT"; }
    @Override public String bossWarning() { return "SOMETHING HUGE MOVES UNDER THE ICE"; }
    @Override public double length() { return 9800; }
    @Override public double[] sectorStarts() { return new double[]{0, 1600, 4200, 6400}; }
    @Override public String[] sectorNames() { return new String[]{"ICE FIELD", "CRYSTAL RINGS", "GLACIER FORTRESS", "BLIZZARD PASS"}; }
    @Override public double vehicleX() { return 5000; }

    @Override
    public Theme theme() {
        return new Theme(Style.ICE,
                new Color(10, 30, 62), new Color(50, 100, 150), new Color(150, 210, 255),
                new Color(200, 230, 255, 110), new Color(40, 120, 200), new Color(160, 230, 255),
                null, new Color(120, 200, 255), new Color(90, 140, 220), new Color(220, 240, 255));
    }

    @Override
    public BossEnemy createBoss(AssaultGame g) {
        return new CryoSerpentBoss(g);
    }

    private static Asteroid ice(Asteroid a) {
        return a.colors(new Color(200, 235, 255), new Color(90, 140, 190));
    }

    @Override
    public void build(Level L) {
        // ===== เขต 1: ICE FIELD =====
        L.at(0, g -> {
            L.droneLine(g, 3, Drone.Pattern.SWEEP, 0.8);
            ice(L.asteroid(g, 1200, 160, 32));
            ice(L.asteroid(g, 1350, 420, 26));
        });
        L.at(350, g -> {
            for (int i = 0; i < 3; i++) g.spawn(new IceSpinner(Level.right(g) + 40 + i * 140, L.randY(g, 900, 60)).withDelay(i * 0.6));
            g.addHostage(new Hostage(1650, 400));
        });
        L.at(700, g -> {
            g.spawn(new ShieldDrone(Level.right(g) + 40, 200));
            g.spawn(new SupplyPod(Level.right(g) + 120, 380, Pickup.Kind.SPREAD));
            g.toast("SHIELD DRONE: SHOOT AT AN ANGLE OR BREAK THE SHIELD");
        });
        L.at(1000, g -> {
            g.spawn(new FrostBomber(Level.right(g) + 60, 0));
            g.spawn(new FrostCannon(2150, 430));
        });
        L.lockAt(1300, g -> {
            g.spawn(new ShieldDrone(Level.right(g) + 40, 150));
            g.spawn(new ShieldDrone(Level.right(g) + 60, 400).withDelay(0.8));
            for (int i = 0; i < 4; i++) g.spawn(new IceSpinner(Level.right(g) + 40, L.randY(g, 1500, 60)).withDelay(1.5 + i * 0.7));
        });

        // ===== เขต 2: CRYSTAL RINGS =====
        L.at(1600, g -> {
            g.showBanner("SECTOR 2", "CRYSTAL RINGS");
            g.spawn(new FrostCannon(2700, 150));
            g.spawn(new FrostCannon(2850, 440));
            L.droneLine(g, 3, Drone.Pattern.SWEEP, 0.6);
        });
        L.at(2000, g -> {
            g.spawn(new FrostBomber(Level.right(g) + 60, 0));
            g.spawn(new FrostBomber(Level.right(g) + 260, 30));
            g.spawn(new SupplyPod(Level.right(g) + 150, 300, Pickup.Kind.LASER));
        });
        L.at(2400, g -> {
            for (int i = 0; i < 5; i++) ice(L.asteroid(g, 3300 + i * 150, 90 + L.rng.nextInt(380), 22 + L.rng.nextInt(18)));
            g.addHostage(new Hostage(3500, 160));
        });
        L.lockAt(3000, g -> {
            g.showBanner("WARNING", "SHIELD SQUADRON");
            for (int i = 0; i < 4; i++) g.spawn(new ShieldDrone(Level.right(g) + 40, 100 + i * 110).withDelay(i * 0.6));
            g.spawn(new FrostBomber(Level.right(g) + 80, 0).withDelay(3));
            L.kamikazes(g, 3, 1.0).forEach(e -> e.delay += 4);
        });
        L.at(3400, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 40, 380, Pickup.Kind.ROCKET));
            g.spawn(new SupplyPod(Level.right(g) + 220, 140, Pickup.Kind.MEDKIT));
            for (int i = 0; i < 3; i++) g.spawn(new IceSpinner(Level.right(g) + 300 + i * 120, L.randY(g, 4400, 60)));
        });

        // ===== เขต 3: GLACIER FORTRESS (มินิบอส FROST WARDEN) =====
        L.at(4200, g -> {
            g.showBanner("SECTOR 3", "GLACIER FORTRESS");
            g.spawn(new FrostCannon(5250, 150));
            g.spawn(new FrostCannon(5400, 440));
            for (int i = 0; i < 4; i++) ice(L.asteroid(g, 5000 + i * 130, 90 + L.rng.nextInt(380), 26 + L.rng.nextInt(16)));
            L.droneLine(g, 3, Drone.Pattern.SWEEP, 0.6);
        });
        L.at(4600, g -> {
            g.spawn(new ShieldDrone(Level.right(g) + 40, 160));
            g.spawn(new ShieldDrone(Level.right(g) + 80, 400).withDelay(1.0));
            g.spawn(new FrostBomber(Level.right(g) + 200, 10).withDelay(1.5));
            g.addHostage(new Hostage(5550, 290));
        });
        L.lockAt(5300, g -> {
            g.showBanner("WARNING", "FROST WARDEN APPROACHING");
            g.spawn(new FrostWarden(Level.right(g) + 160, 290));
        });
        L.at(5600, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 40, 200, Pickup.Kind.MEDKIT));
            g.spawn(new SupplyPod(Level.right(g) + 220, 400, Pickup.Kind.SPREAD));
            for (int i = 0; i < 3; i++) g.spawn(new IceSpinner(Level.right(g) + 300 + i * 120, L.randY(g, 6600, 60)));
        });

        // ===== เขต 4: BLIZZARD PASS =====
        L.at(6400, g -> {
            g.showBanner("SECTOR 4", "BLIZZARD PASS");
            g.spawn(new FrostCannon(7400, 140));
            g.spawn(new FrostCannon(7550, 430));
            for (int i = 0; i < 4; i++) g.spawn(new IceSpinner(Level.right(g) + 40, L.randY(g, 7400, 60)).withDelay(i * 0.5));
        });
        L.at(6800, g -> {
            g.spawn(new FrostBomber(Level.right(g) + 60, 0));
            g.spawn(new FrostBomber(Level.right(g) + 300, 20));
            g.addHostage(new Hostage(7900, 300));
        });
        L.lockAt(7200, g -> {
            for (int i = 0; i < 3; i++) g.spawn(new ShieldDrone(Level.right(g) + 40, 120 + i * 150).withDelay(i * 0.5));
            L.droneLine(g, 4, Drone.Pattern.HOVER, 0.5);
            for (int i = 0; i < 4; i++) g.spawn(new IceSpinner(Level.right(g) + 40, L.randY(g, 8200, 60)).withDelay(3 + i * 0.5));
        });
        L.at(7700, g -> {
            g.spawn(new SupplyPod(Level.right(g) + 40, 200, Pickup.Kind.HEAVY));
            g.spawn(new SupplyPod(Level.right(g) + 200, 400, Pickup.Kind.BOMB));
            g.spawn(new FrostCannon(8500, 300));
        });
        L.at(8100, g -> {
            L.droneLine(g, 4, Drone.Pattern.SWEEP, 0.5);
            g.spawn(new FrostBomber(Level.right(g) + 60, 0));
        });
    }

    // ---------- พายุหิมะ ----------
    @Override
    public void update(double dt, AssaultGame g) {
        boolean allowed = !g.inBossArena();
        weatherT -= dt;
        switch (weather) {
            case CALM:
                if (weatherT <= 0 && allowed) {
                    weather = Weather.WARN;
                    weatherT = 1.6;
                    g.toast("BLIZZARD INCOMING!");
                }
                break;
            case WARN:
                if (weatherT <= 0) { weather = Weather.GUST; weatherT = 4.5; g.shake(4); }
                break;
            case GUST:
                g.player().pushX = GUST_PUSH;
                if (weatherT <= 0 || !allowed) { weather = Weather.CALM; weatherT = 10 + Math.random() * 3; }
                break;
            default:
                break;
        }
        double wind = weather == Weather.GUST ? 900 : weather == Weather.WARN ? 300 : 70;
        for (int i = 0; i < snowX.length; i++) {
            snowX[i] -= wind * snowS[i] * dt;
            snowY[i] += (40 + wind * 0.15) * snowS[i] * dt;
            if (snowX[i] < -40) { snowX[i] = Config.WIDTH + 20; snowY[i] = Math.random() * Config.HEIGHT; }
            if (snowY[i] > Config.HEIGHT + 10) { snowY[i] = -10; snowX[i] = Math.random() * Config.WIDTH; }
        }
    }

    @Override
    public void drawScreen(Graphics2D g2, AssaultGame g) {
        boolean gust = weather == Weather.GUST;
        float len = gust ? 34 : weather == Weather.WARN ? 12 : 2;
        g2.setStroke(new BasicStroke(gust ? 2f : 2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < snowX.length; i++) {
            int a = (int) (90 + 120 * snowS[i]);
            g2.setColor(new Color(240, 250, 255, Math.min(255, a)));
            g2.draw(new Line2D.Double(snowX[i], snowY[i], snowX[i] + len * snowS[i], snowY[i] - len * 0.15));
        }
        g2.setStroke(new BasicStroke(1f));
        if (gust) {
            g2.setColor(new Color(220, 240, 255, 45));
            g2.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
            g2.setFont(novaescape.ui.Text.font(Font.BOLD, 16));
            novaescape.ui.Text.centered(g2, "<<< BLIZZARD <<<", Config.WIDTH / 2.0, Config.HUD_HEIGHT + 40,
                    new Color(230, 245, 255), true);
        }
    }
}
