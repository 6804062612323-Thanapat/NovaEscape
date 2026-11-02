package novaescape.assault;

import novaescape.assault.enemy.*;
import novaescape.assault.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * ตัวเก็บสคริปต์ด่าน: "เมื่อกล้องเลื่อนมาถึง x จะมีอะไรเกิดขึ้น"
 * เนื้อหาจริงของแต่ละด่านเขียนอยู่ใน stage/Stage1..3 (เมธอด build)
 *
 * at(x, ...)     = เกิดเมื่อกล้องถึงตำแหน่ง x
 * lockAt(x, ...) = ล็อกกล้องไว้จนกว่าจะฆ่าศัตรูในระลอกนั้นหมด (แบบ Metal Slug)
 */
public class Level {

    private static final class Event {
        final double at;
        final boolean lock;
        final Consumer<AssaultGame> action;
        boolean done;

        Event(double at, boolean lock, Consumer<AssaultGame> action) {
            this.at = at;
            this.lock = lock;
            this.action = action;
        }
    }

    private final List<Event> events = new ArrayList<>();
    public final Random rng = new Random();

    public Level(Stage stage) {
        stage.build(this);
    }

    public void at(double camX, Consumer<AssaultGame> action) {
        events.add(new Event(camX, false, action));
    }

    public void lockAt(double camX, Consumer<AssaultGame> action) {
        events.add(new Event(camX, true, action));
    }

    // ---------------------------------------------------------------------
    //  ตัวช่วยสำหรับเขียนด่าน
    // ---------------------------------------------------------------------

    /** ขอบขวาของจอในพิกัดโลก */
    public static double right(AssaultGame g) {
        return g.camX() + g.viewW();
    }

    /** สุ่มความสูงที่บินได้จริง ณ ตำแหน่ง x (เว้นระยะจากขอบ/ผนัง) */
    public double randY(AssaultGame g, double x, double margin) {
        double lo = g.ceiling(x) + margin, hi = g.floor(x) - margin;
        if (hi <= lo) return (lo + hi) / 2;
        return lo + rng.nextDouble() * (hi - lo);
    }

    public void droneLine(AssaultGame g, int n, Drone.Pattern p, double gap) {
        for (int i = 0; i < n; i++) {
            double x = right(g) + 40;
            g.spawn(new Drone(x, randY(g, x, 50), p).withDelay(i * gap));
        }
    }

    public List<Enemy> kamikazes(AssaultGame g, int n, double gap) {
        List<Enemy> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double x = right(g) + 40;
            Enemy e = new Kamikaze(x, randY(g, x, 50)).withDelay(i * gap);
            g.spawn(e);
            list.add(e);
        }
        return list;
    }

    public Asteroid asteroid(AssaultGame g, double x, double y, double r) {
        Asteroid a = new Asteroid(x, y, r, -20 - rng.nextInt(30), (rng.nextDouble() - 0.5) * 20);
        g.spawn(a);
        return a;
    }

    /** เรียกทุกเฟรม: เช็กว่ากล้องถึงเหตุการณ์ไหนแล้ว */
    public void update(AssaultGame g) {
        for (Event e : events) {
            if (e.done || g.camX() < e.at) continue;
            e.done = true;
            if (e.lock) g.beginLock(e.at);
            e.action.accept(g);
            if (e.lock) g.endLockRecording();
        }
    }
}
