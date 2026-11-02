package novaescape.assault;

import java.awt.Color;

/**
 * อาวุธของผู้เล่นในโหมด ASSAULT (แบบ Metal Slug)
 * เก็บกล่องอาวุธเพื่อเปลี่ยนปืน กระสุนหมดจะกลับไปใช้ PISTOL
 * อยากเพิ่มปืนใหม่: เพิ่มค่าใน enum + เขียนการยิงใน fire()
 */
public enum Weapon {
    //        ชื่อบน HUD          ชื่อตอนเก็บ                  ดีเลย์ยิง  กระสุนที่ได้  ตัวอักษร  สี
    PISTOL("PISTOL",   "PISTOL",             0.16,  0,   "P", new Color(255, 230, 120)),
    HEAVY ("HEAVY MG", "HEAVY MACHINE GUN!", 0.065, 200, "H", new Color(255, 150, 50)),
    SPREAD("SPREAD",   "SPREAD GUN!",        0.24,  50,  "S", new Color(255, 90, 140)),
    LASER ("LASER",    "LASER GUN!",         0.085, 120, "L", new Color(90, 255, 200)),
    ROCKET("ROCKET",   "ROCKET LAUNCHER!",   0.38,  30,  "R", new Color(255, 80, 60));

    public final String hudName;
    public final String announce;
    public final double cooldown;
    public final int ammoPerPickup;   // 0 = ไม่จำกัด
    public final String letter;
    public final Color color;

    Weapon(String hudName, String announce, double cooldown, int ammoPerPickup, String letter, Color color) {
        this.hudName = hudName;
        this.announce = announce;
        this.cooldown = cooldown;
        this.ammoPerPickup = ammoPerPickup;
        this.letter = letter;
        this.color = color;
    }

    /** เสียงปืนแต่ละชนิด */
    public novaescape.audio.Sfx sound() {
        switch (this) {
            case HEAVY: return novaescape.audio.Sfx.MG;
            case SPREAD: return novaescape.audio.Sfx.SPREAD;
            case LASER: return novaescape.audio.Sfx.LASER;
            case ROCKET: return novaescape.audio.Sfx.ROCKET;
            default: return novaescape.audio.Sfx.SHOOT;
        }
    }

    /**
     * ยิง 1 ครั้งจากปากกระบอก (x, y)
     * @param shot ลำดับการยิง (ใช้สลับตำแหน่ง เช่น ปืนกล/จรวด)
     */
    public void fire(AssaultGame g, double x, double y, int shot) {
        novaescape.audio.Sound.play(sound());
        switch (this) {
            case PISTOL: {
                Bullet b = new Bullet(x, y, 780, 0, true, Bullet.Style.PISTOL);
                b.damage = 1;
                g.addFriendlyBullet(b);
                break;
            }
            case HEAVY: {
                double off = (shot % 2 == 0) ? -4 : 4;
                Bullet b = new Bullet(x, y + off, 920, (Math.random() - 0.5) * 90, true, Bullet.Style.HEAVY);
                b.damage = 1;
                g.addFriendlyBullet(b);
                break;
            }
            case SPREAD: {
                double[] deg = {-16, -8, 0, 8, 16};
                for (double d : deg) {
                    double a = Math.toRadians(d);
                    Bullet b = new Bullet(x, y, Math.cos(a) * 680, Math.sin(a) * 680, true, Bullet.Style.SPREAD);
                    b.damage = 1.1;
                    b.size = 6;
                    g.addFriendlyBullet(b);
                }
                break;
            }
            case LASER: {
                Bullet b = new Bullet(x + 20, y, 1300, 0, true, Bullet.Style.LASER);
                b.len = 44;
                b.damage = 1.3;
                b.pierce = true;
                g.addFriendlyBullet(b);
                break;
            }
            case ROCKET: {
                double off = (shot % 2 == 0) ? -10 : 10;
                Bullet b = new Bullet(x - 10, y + off, 260, off * 6, true, Bullet.Style.ROCKET);
                b.damage = 5;
                b.size = 6;
                b.homing = true;
                b.explosive = true;
                g.addFriendlyBullet(b);
                break;
            }
            default:
                break;
        }
    }
}
