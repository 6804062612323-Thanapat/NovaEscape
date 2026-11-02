package novaescape.assault;

import novaescape.Config;
import novaescape.HighScore;
import novaescape.Input;
import novaescape.assault.enemy.BossEnemy;
import novaescape.assault.enemy.Enemy;
import novaescape.assault.stage.Stage;
import novaescape.audio.Music;
import novaescape.audio.Sfx;
import novaescape.audio.Sound;
import novaescape.entity.Particle;
import novaescape.item.Inventory;
import novaescape.item.ItemType;
import novaescape.ui.Button;
import novaescape.ui.Text;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * โหมด ASSAULT: ยิงสู้แบบ Metal Slug บนแมพยาวที่เลื่อนตามผู้เล่น
 * คลาสนี้ดูแลทุกอย่างของโหมดนี้ (อัปเดต, ชน, กล้อง/ซูม, วาด, HUD, หน้าจบ)
 * เนื้อหาของแต่ละด่าน (ศัตรู, บอส, ฉาก) อยู่ใน stage/
 */
public class AssaultGame {

    public enum Phase { PLAYING, BOSS_WARNING, BOSS, VICTORY, MISSION_COMPLETE, GAME_OVER, PAUSED }

    /** ห้องบอส: กล้องซูมออกให้เห็นกว้างขึ้น */
    public static final double BOSS_ZOOM = 0.8;
    public static final double BOSS_VIEW_W = Config.WIDTH / BOSS_ZOOM;

    // ---------- ระบบ ----------
    private final Input input;
    private final AssaultHost host;
    private final Stage stage;
    private final RunState startRun;     // ไว้ RETRY ด่านเดิม
    private Phase phase = Phase.PLAYING;
    private Phase phaseBeforePause;
    private double phaseTimer;
    private double gameOverTimer;
    private double t;
    private Point mouseScreen;

    // ---------- กล้อง ----------
    private double camX;
    private double camVel;
    private double zoom = 1;
    private double lockX = Double.MAX_VALUE;
    private final List<Enemy> lockWave = new ArrayList<>();
    private boolean recordingLock;
    private double goTimer;
    private double lockTime;
    private static final double LOCK_TIMEOUT = 45; // กันติด: ล็อกนานเกินนี้ศัตรูที่เหลือจะถอยไป

    // ---------- โลก ----------
    private final AssaultPlayer player;
    private final NovaSlug slug;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Enemy> spawnQueue = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Bullet> newBullets = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private final List<Hostage> hostages = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatText> floatTexts = new ArrayList<>();
    private final Inventory inventory = new Inventory();
    private final Level level;
    private final ParallaxBackground bg;
    private BossEnemy boss;
    private int lastBossPhase = 1;    // ไว้เช็กว่าบอสเพิ่งเปลี่ยนเฟส
    private double bossX, bossY;

    // ---------- เอฟเฟกต์ ----------
    private double empTimer;
    private double shakeAmt;
    private double flashTimer;
    private double bombRingT = -1;
    private double bombX, bombY;
    private double bombCooldown;
    private String banner, subBanner;
    private double bannerTimer;
    private double hintTimer = 5;
    private String toast = "";
    private double toastTimer;

    // ---------- คะแนน ----------
    private double score;
    private int kills;
    private int rescued;
    private double missionTime;
    private int highScore = HighScore.load(HighScore.ASSAULT_FILE);
    private boolean newHighScore;
    private int bonusRescue, bonusLives, bonusTime;

    // ---------- UI ----------
    private final List<Button> buttons = new ArrayList<>();

    public AssaultGame(Input input, RunState run, AssaultHost host) {
        this.input = input;
        this.host = host;
        this.startRun = run.copy();
        this.stage = Stage.create(run.stage);
        this.level = new Level(stage);
        this.bg = new ParallaxBackground(stage);
        this.player = new AssaultPlayer(80, centerY() - 18);
        player.lives = run.lives;
        player.bombs = run.bombs;
        this.score = run.score;
        this.slug = new NovaSlug(stage.vehicleX(), centerY());
        inventory.add(ItemType.SHIELD);
        showBanner("STAGE " + stage.number(), stage.name());
        Sound.music(stage.number() == 2 ? Music.STAGE2 : stage.number() == 3 ? Music.STAGE3 : Music.STAGE1);
        bannerTimer = 3.5;
    }

    // =====================================================================
    //  API ให้ศัตรู/กระสุน/ด่าน เรียกใช้
    // =====================================================================
    public double camX() { return camX; }
    public double zoom() { return zoom; }
    public double viewW() { return Config.WIDTH / zoom; }
    /** ขอบบนของพื้นที่เล่น (ใต้ HUD) ในพิกัดโลก */
    public double top() { return Config.HUD_HEIGHT / zoom + 4; }
    /** ขอบล่างของพื้นที่เล่นในพิกัดโลก */
    public double bottom() { return Config.HEIGHT / zoom; }
    public double centerY() { return (top() + bottom()) / 2; }
    public double ceiling(double x) { return stage.ceiling(x, this); }
    public double floor(double x) { return stage.floor(x, this); }
    public boolean hasWalls() { return stage.hasWalls(); }
    public double arenaX() { return stage.arenaX(); }
    public boolean inBossArena() { return camX >= stage.arenaX() - 1; }

    /** ระยะขวาสุดที่ผู้เล่นบินไปเก็บของได้ (ใช้ในห้องบอส ไม่ให้ไอเทมไปตกตรงตัวบอส) */
    public double pickupReachX() {
        return camX + viewW() * 0.6 + 50;
    }
    public double time() { return t; }
    public Stage stage() { return stage; }
    public AssaultPlayer player() { return player; }
    public List<Enemy> enemies() { return enemies; }
    public boolean enemiesJammed() { return empTimer > 0; }

    /** ตำแหน่งเมาส์ในพิกัดโลก (null = ยังไม่เคยขยับเมาส์) */
    public Point2D mouseWorld() {
        if (mouseScreen == null) return null;
        return new Point2D.Double(camX + mouseScreen.x / zoom, mouseScreen.y / zoom);
    }

    public void spawn(Enemy e) {
        if (e.scaleHp && !e.isBoss()) {
            double m = Balance.enemyHp(stage.number());
            e.hp *= m;
            e.maxHp *= m;
            e.scaleHp = false;   // คูณแค่ครั้งเดียว
        }
        spawnQueue.add(e);
        if (recordingLock) lockWave.add(e);
    }

    public void addHostage(Hostage h) { hostages.add(h); }

    public void addFriendlyBullet(Bullet b) { newBullets.add(b); }

    /** เพิ่มกระสุนศัตรูโดยไม่สน EMP (เช่น เศษระเบิดน้ำแข็ง) */
    public void addEnemyBullet(Bullet b) {
        double k = Balance.enemyBulletSpeed(stage.number());
        b.vx *= k;
        b.vy *= k;
        newBullets.add(b);
    }

    /** ศัตรูยิงกระสุนแบบใดก็ได้ (EMP จะกันไว้) */
    public void enemyBullet(Bullet b) {
        if (empTimer > 0 || phase == Phase.VICTORY) return;
        Sound.play(Sfx.ENEMY_SHOT);
        addEnemyBullet(b);
    }

    public void enemyShoot(double x, double y, double angle, double speed) {
        enemyBullet(Bullet.orb(x, y, angle, speed));
    }

    public void enemyLaser(double x, double y, double len, double speed) {
        enemyBullet(Bullet.enemyLaser(x, y, len, speed));
    }

    public void explode(double x, double y, int count) {
        Particle.explode(particles, x, y, count);
        if (count >= 40) Sound.play(Sfx.BIG_EXPLOSION);
        else if (count >= 14) Sound.play(Sfx.EXPLOSION);
    }

    public void sparks(double x, double y, Color c, int count) {
        for (int i = 0; i < count; i++) {
            double a = Math.random() * Math.PI * 2, s = 80 + Math.random() * 160;
            particles.add(new Particle(x, y, Math.cos(a) * s, Math.sin(a) * s, 0.15 + Math.random() * 0.2, 2.5, c));
        }
    }

    public void smoke(double x, double y) {
        particles.add(new Particle(x, y, -30 + Math.random() * 20, (Math.random() - 0.5) * 30,
                0.35, 6, new Color(180, 180, 190)));
    }

    /** อนุภาคทั่วไป (เช่น หิมะ, สปอร์) */
    public void particle(double x, double y, double vx, double vy, double life, double size, Color c) {
        particles.add(new Particle(x, y, vx, vy, life, size, c));
    }

    public void shake(double amount) { shakeAmt = Math.max(shakeAmt, amount); }

    public void flash(double secs) { flashTimer = Math.max(flashTimer, secs); }

    public void toast(String msg) { toast = msg; toastTimer = 1.8; }

    public void showBanner(String title, String sub) {
        banner = title;
        subBanner = sub;
        bannerTimer = 2.6;
    }

    public void enemyKilled(Enemy e) {
        kills++;
        addScore(e.score, e.x, e.y - e.h / 2);
    }

    private void addScore(int pts, double x, double y) {
        if (pts <= 0) return;
        score += pts;
        floatTexts.add(new FloatText(x, y, "+" + pts, new Color(255, 230, 120)));
    }

    public void dropRandom(double x, double y) {
        double r = Math.random();
        Pickup.Kind k;
        double med = Balance.medkitDrop(stage.number());
        // ระเบิดหายากขึ้น (6%), ส่วนที่เหลือไปเป็น Gem
        if (r < 0.10) k = Pickup.Kind.HEAVY;
        else if (r < 0.19) k = Pickup.Kind.SPREAD;
        else if (r < 0.27) k = Pickup.Kind.LASER;
        else if (r < 0.33) k = Pickup.Kind.ROCKET;
        else if (r < 0.39) k = Pickup.Kind.BOMB;
        else if (r < 0.39 + med) k = Pickup.Kind.MEDKIT;
        else if (r < 0.44 + med) k = Pickup.Kind.SHIELD;
        else if (r < 0.48 + med) k = Pickup.Kind.EMP;
        else k = Pickup.Kind.GEM;
        dropPickup(x, y, k);
    }

    public void dropPickup(double x, double y, Pickup.Kind k) {
        pickups.add(new Pickup(x, y, k));
    }

    public void clearEnemyBullets() {
        for (Bullet b : bullets) {
            if (!b.friendly && !b.dead) { b.dead = true; sparks(b.x, b.y, new Color(255, 150, 220), 2); }
        }
        newBullets.removeIf(b -> !b.friendly);
    }

    public void beginLock(double atX) {
        lockX = Math.max(atX, camX);
        lockTime = 0;
        lockWave.clear();
        recordingLock = true;
    }

    public void endLockRecording() { recordingLock = false; }

    public void playerOutOfLives() {
        gameOverTimer = 1.6; // รอให้เห็นระเบิดก่อนขึ้น GAME OVER
    }

    public void bossDefeated(BossEnemy b) {
        phase = Phase.VICTORY;
        Sound.music(null);
        Sound.play(Sfx.BIG_EXPLOSION);
        phaseTimer = 3.2;
        bossX = b.x;
        bossY = b.y;
        clearEnemyBullets();
        for (Enemy e : enemies) {
            if (e != b && e.active()) { e.dead = true; explode(e.x, e.y, 15); }
        }
        shake(20);
        flash(0.3);
    }

    // =====================================================================
    //  UPDATE
    // =====================================================================
    public void update(double dt) {
        t += dt;
        if (input.consumePause() && (phase == Phase.PLAYING || phase == Phase.BOSS || phase == Phase.BOSS_WARNING)) {
            pause();
            return;
        }
        switch (phase) {
            case PAUSED:
                input.consumePause();
                return;
            case PLAYING:
            case BOSS_WARNING:
            case BOSS:
                updateWorld(dt);
                if (phase == Phase.BOSS_WARNING) {
                    phaseTimer -= dt;
                    if (phaseTimer <= 0) {
                        boss = stage.createBoss(this);
                        spawn(boss);
                        Sound.play(Sfx.BOSS_ROAR);
                        Sound.music(Music.BOSS);
                        phase = Phase.BOSS;
                    }
                }
                if (player.lives < 0 && !player.alive) {
                    gameOverTimer -= dt;
                    if (gameOverTimer <= 0) gameOver();
                }
                break;
            case VICTORY:
                updateWorld(dt);
                phaseTimer -= dt;
                if (phaseTimer > 1.0 && Math.random() < 0.35) {
                    explode(bossX + (Math.random() - 0.5) * 200, bossY + (Math.random() - 0.5) * 300, 25);
                    shake(8);
                }
                if (phaseTimer <= 0) missionComplete();
                break;
            case MISSION_COMPLETE:
            case GAME_OVER:
                updateParticles(dt);
                bg.update(dt, 0);
                input.consumeSpace();
                input.consumeBomb();
                break;
            default:
                break;
        }
    }

    private void updateWorld(double dt) {
        boolean fighting = phase != Phase.VICTORY;
        if (fighting) missionTime += dt;
        if (empTimer > 0) empTimer -= dt;
        if (flashTimer > 0) flashTimer -= dt;
        if (bannerTimer > 0) bannerTimer -= dt;
        if (hintTimer > 0) hintTimer -= dt;
        if (toastTimer > 0) toastTimer -= dt;
        if (goTimer > 0) goTimer -= dt;
        shakeAmt = Math.max(0, shakeAmt - 40 * dt);
        if (bombRingT >= 0) { bombRingT += dt; if (bombRingT > 0.6) bombRingT = -1; }
        if (bombCooldown > 0) bombCooldown -= dt;

        // ซูมออกในห้องบอส
        double targetZoom = inBossArena() ? BOSS_ZOOM : 1;
        zoom += (targetZoom - zoom) * Math.min(1, 2.2 * dt);
        if (Math.abs(targetZoom - zoom) < 0.001) zoom = targetZoom;

        if (fighting) stage.update(dt, this);
        player.update(dt, input, this);
        if (input.consumeBomb() && fighting) {
            if (player.vehicle != null) player.vehicle.fireCannon(this);
            else useBomb();
        }
        if (input.consumeSpace() && fighting) useItem();

        // รถถัง NOVA SLUG
        if (slug.state == NovaSlug.State.PARKED) {
            slug.updateParked(dt, this);
            if (player.alive && player.vehicle == null && slug.hitbox().intersects(player.hitbox())) {
                slug.state = NovaSlug.State.ACTIVE;
                Sound.play(Sfx.TANK);
                player.vehicle = slug;
                showBanner("NOVA SLUG!", "MOUSE = AIM TURRET    K = CANNON    " + (int) NovaSlug.TIME_LIMIT + " SEC");
                bannerTimer = 2.2;
                flash(0.12);
            }
        }

        level.update(this);
        updateCamera(dt);

        enemies.addAll(spawnQueue);
        spawnQueue.clear();
        for (Enemy e : enemies) e.tick(dt, this);
        enemies.addAll(spawnQueue);
        spawnQueue.clear();

        // ด่าน 2-3: บอสเปลี่ยนเฟส = ดรอปปืน 1 อัน
        if (boss != null && phase == Phase.BOSS && stage.number() >= 2) {
            int bp = boss.phase();
            if (bp > lastBossPhase) {
                lastBossPhase = bp;
                phaseWeaponDrop();
            }
        }

        bullets.addAll(newBullets);
        newBullets.clear();
        for (Bullet b : bullets) b.update(dt, this);
        bullets.addAll(newBullets);
        newBullets.clear();
        for (Pickup p : pickups) p.update(dt, this);
        for (Hostage h : hostages) h.update(dt, this);

        handleCollisions();

        bullets.removeIf(b -> b.dead);
        pickups.removeIf(p -> p.dead);
        hostages.removeIf(h -> h.gone);
        enemies.removeIf(e -> e.dead || e.removed);
        updateParticles(dt);

        // ปลดล็อกกล้องเมื่อเคลียร์ระลอกแล้ว
        if (lockX != Double.MAX_VALUE) {
            lockTime += dt;
            if (lockTime > LOCK_TIMEOUT) {
                for (Enemy e : lockWave) if (e.active() && !e.miniBoss) { e.removed = true; sparks(e.x, e.y, Color.WHITE, 6); }
            }
            boolean cleared = true;
            for (Enemy e : lockWave) if (!e.dead && !e.removed) { cleared = false; break; }
            if (cleared) {
                lockX = Double.MAX_VALUE;
                lockWave.clear();
                goTimer = 2.5;
                Sound.play(Sfx.GO);
            }
        }

        // เข้าห้องบอส
        if (phase == Phase.PLAYING && boss == null && inBossArena()) {
            phase = Phase.BOSS_WARNING;
            phaseTimer = 2.8;
            Sound.music(null);
            Sound.play(Sfx.WARNING);
            showBanner("WARNING!!", stage.bossWarning());
            bannerTimer = 2.8;
        }
        bg.update(dt, camVel);
    }

    private void updateCamera(double dt) {
        double before = camX;
        double maxCam = Math.min(lockX, stage.arenaX());
        double target = player.x - viewW() * 0.38;
        if (player.alive && target > camX) {
            camX = Math.min(maxCam, camX + Math.min(target - camX, 430 * dt));
        }
        camX = Math.min(camX, maxCam);
        camVel = dt > 0 ? (camX - before) / dt : 0;
    }

    private void updateParticles(double dt) {
        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            p.update(dt);
            if (p.isDead()) it.remove();
        }
        for (Iterator<FloatText> it = floatTexts.iterator(); it.hasNext(); ) {
            FloatText f = it.next();
            f.life -= dt;
            f.y -= 40 * dt;
            if (f.life <= 0) it.remove();
        }
    }

    private void handleCollisions() {
        Rectangle2D pBox = player.hitbox();

        for (Bullet b : bullets) {
            if (b.dead) continue;
            if (b.friendly) {
                friendlyBulletHits(b);
            } else if (player.alive && b.hitbox().intersects(pBox)) {
                if (player.shieldTimer > 0) {
                    b.dead = true;
                    sparks(b.x, b.y, new Color(140, 235, 255), 4);
                } else if (player.invulnTimer <= 0) {
                    b.dead = true;
                    if (b.freezes && player.vehicle == null) player.freeze();
                    player.hit(this);
                }
            }
        }

        if (!player.alive) return;
        for (Enemy e : enemies) {
            if (!e.active() || !e.touchDamage) continue;
            boolean touching = e.hitbox().intersects(pBox);
            boolean touchBody = false;
            if (!touching) {
                for (Rectangle2D r : e.blockers()) if (r.intersects(pBox)) { touchBody = true; break; }
            }
            if (touching || touchBody) {
                if (!player.isInvulnerable()) player.hit(this, e.contactDamage());
                if (touching) e.onTouchPlayer(this);
            }
        }

        for (Pickup p : pickups) {
            if (!p.dead && p.hitbox().intersects(pBox)) {
                p.dead = true;
                collect(p);
            }
        }

        for (Hostage h : hostages) {
            if (!h.rescued && h.hitbox().intersects(pBox)) {
                h.rescued = true;
                rescued++;
                Sound.play(Sfx.RESCUE);
                addScore(500, h.x, h.y - 30);
                dropRandomGood(player.centerX() + 90, player.centerY());
            }
        }
    }

    private void friendlyBulletHits(Bullet b) {
        Rectangle2D bb = b.hitbox();
        boolean hitSomething = false;
        for (Enemy e : enemies) {
            if (!e.active() || b.alreadyHit.contains(e)) continue;
            if (!bb.intersects(e.hitbox())) continue;
            if (e.blocksBullet(b)) {
                e.onBlocked(b, this);
                b.dead = true;
                sparks(b.x, b.y, new Color(150, 220, 255), 4);
                return;
            }
            hitSomething = true;
            e.damage(b.damage, this);
            sparks(b.x, b.y, new Color(255, 230, 150), 3);
            if (b.explosive) splash(b, e);
            if (b.pierce) {
                b.alreadyHit.add(e);
            } else {
                b.dead = true;
                return;
            }
        }
        if (hitSomething) return;
        // ชนส่วนที่ยิงไม่เข้า (ลำตัวงู, ตัวราชินี)
        for (Enemy e : enemies) {
            if (!e.active()) continue;
            for (Rectangle2D r : e.blockers()) {
                if (r.intersects(bb)) {
                    b.dead = true;
                    sparks(b.x, b.y, new Color(200, 200, 220), 3);
                    if (e.armorDamageRatio() > 0) e.damage(b.damage * e.armorDamageRatio(), this);
                    if (b.explosive) splash(b, null);
                    return;
                }
            }
        }
    }

    private void splash(Bullet b, Enemy direct) {
        explode(b.x, b.y, 18);
        for (Enemy o : enemies) {
            if (o != direct && o.active() && Math.hypot(o.x - b.x, o.y - b.y) < b.splashRadius) {
                o.damage(b.splashDamage, this);
            }
        }
        shake(4);
    }

    /** ปืนดรอปตอนบอสเปลี่ยนเฟส: สุ่มปืนที่ไม่ใช่ปืนที่ถืออยู่ วางในระยะที่เก็บได้ */
    private void phaseWeaponDrop() {
        Pickup.Kind[] guns = {Pickup.Kind.HEAVY, Pickup.Kind.SPREAD, Pickup.Kind.LASER, Pickup.Kind.ROCKET};
        Pickup.Kind k;
        do {
            k = guns[(int) (Math.random() * guns.length)];
        } while (player.weapon != Weapon.PISTOL && new Pickup(0, 0, k).weapon() == player.weapon);
        double x = Math.min(player.centerX() + 170, pickupReachX() - 20);
        double y = Math.max(ceiling(x) + 50, Math.min(floor(x) - 50, centerY()));
        dropPickup(x, y, k);
        sparks(x, y, new Color(255, 230, 120), 14);
        toast("WEAPON DROP!");
        Sound.play(Sfx.POWERUP);
    }

    private void dropRandomGood(double x, double y) {
        Pickup.Kind[] good = {Pickup.Kind.HEAVY, Pickup.Kind.SPREAD, Pickup.Kind.LASER, Pickup.Kind.ROCKET,
                Pickup.Kind.BOMB, Pickup.Kind.MEDKIT, Pickup.Kind.SHIELD, Pickup.Kind.EMP};
        dropPickup(x, y, good[(int) (Math.random() * good.length)]);
    }

    private void collect(Pickup p) {
        Weapon w = p.weapon();
        if (w != null) {
            player.giveWeapon(w);
            if (player.vehicle != null) {
                // อยู่ในรถถัง: กล่องปืน = เพิ่มเวลารถ (ปืนเก็บไว้ใช้หลังลงจากรถ)
                NovaSlug v = player.vehicle;
                double before = v.timeLeft;
                v.timeLeft = Math.min(Balance.SLUG_MAX_TIME, v.timeLeft + Balance.SLUG_TIME_PER_WEAPON);
                int added = (int) Math.round(v.timeLeft - before);
                toast(added > 0 ? "SLUG TIME +" + added + "s" : "SLUG TIME MAX");
                Sound.play(Sfx.POWERUP);
                sparks(v.x, v.y, new Color(230, 200, 80), 10);
            } else {
                toast(w.announce);
                Sound.play(Sfx.POWERUP);
            }
            return;
        }
        Sound.play(p.kind == Pickup.Kind.LIFE ? Sfx.ONEUP : Sfx.PICKUP, p.kind == Pickup.Kind.GEM ? 0.6f : 1f);
        switch (p.kind) {
            case BOMB:
                player.bombs = Math.min(AssaultPlayer.MAX_BOMBS, player.bombs + Balance.BOMB_PER_PICKUP);
                toast(player.bombs >= AssaultPlayer.MAX_BOMBS ? "BOMB FULL" : "BOMB +" + Balance.BOMB_PER_PICKUP);
                break;
            case MEDKIT:
                player.hp = Math.min(AssaultPlayer.MAX_HP, player.hp + 2);
                if (player.vehicle != null) player.vehicle.armor = Math.min(NovaSlug.MAX_ARMOR, player.vehicle.armor + 4);
                toast("HP +2");
                break;
            case SHIELD:
                inventory.add(ItemType.SHIELD);
                toast("SHIELD  [SPACE]");
                break;
            case EMP:
                inventory.add(ItemType.ATTACK);
                toast("EMP  [SPACE]");
                break;
            case LIFE:
                player.lives++;
                toast("1UP!");
                break;
            case GEM:
                addScore(100, p.x, p.y);
                break;
            default:
                break;
        }
    }

    /** K / คลิกขวา: Nova Bomb ทำดาเมจศัตรูทั้งจอ + ลบกระสุนศัตรู */
    private void useBomb() {
        if (!player.alive) return;
        if (bombCooldown > 0) return;
        if (player.bombs <= 0) { toast("NO BOMB"); return; }
        player.bombs--;
        Sound.play(Sfx.BOMB);
        bombCooldown = Balance.BOMB_COOLDOWN;
        bombX = player.centerX();
        bombY = player.centerY();
        bombRingT = 0;
        clearEnemyBullets();
        for (Enemy e : enemies) {
            if (!e.active()) continue;
            if (e.x < camX - 20 || e.x > camX + viewW() + 60) continue;
            e.damage(e.isBoss() ? Balance.BOMB_BOSS_DAMAGE : Balance.BOMB_DAMAGE, this);
        }
        flash(0.25);
        shake(14);
    }

    /** Space: ใช้ไอเทมชิ้นแรกในกระเป๋า (โล่ / EMP) */
    private void useItem() {
        if (!player.alive) return;
        ItemType item = inventory.pollActivatable();
        if (item == null) { toast("NO ITEM"); return; }
        if (item == ItemType.SHIELD) {
            player.shieldTimer = Config.SHIELD_TIME;
            Sound.play(Sfx.SHIELD);
            toast("SHIELD ON!");
        } else if (item == ItemType.ATTACK) {
            empTimer = Config.ATTACK_TIME;
            Sound.play(Sfx.EMP);
            flash(0.1);
            toast("EMP! ENEMIES JAMMED");
        }
    }

    // =====================================================================
    //  เปลี่ยนสถานะ
    // =====================================================================
    private void pause() {
        phaseBeforePause = phase;
        phase = Phase.PAUSED;
        input.mouseShoot = false;
        buttons.clear();
        int cx = Config.WIDTH / 2;
        buttons.add(new Button("RESUME", cx, 290, 250, 52, this::resume));
        buttons.add(new Button("STAGE SELECT", cx, 360, 250, 52, host::toStageSelect));
        buttons.add(new Button("MENU", cx, 430, 250, 52, host::toMenu));
    }

    private void resume() {
        phase = phaseBeforePause;
        buttons.clear();
    }

    private void gameOver() {
        phase = Phase.GAME_OVER;
        Sound.music(null);
        Sound.play(Sfx.GAME_OVER);
        recordScore();
        buttons.clear();
        int cx = Config.WIDTH / 2;
        buttons.add(new Button("RETRY", cx - 260, 450, 230, 52, () -> host.startStage(startRun.copy())));
        buttons.add(new Button("STAGE SELECT", cx, 450, 230, 52, host::toStageSelect));
        buttons.add(new Button("MENU", cx + 260, 450, 230, 52, host::toMenu));
    }

    private boolean isLastStage() {
        return stage.number() >= Stage.COUNT;
    }

    private void missionComplete() {
        phase = Phase.MISSION_COMPLETE;
        Sound.play(Sfx.MISSION);
        bonusRescue = rescued * 1000;
        bonusLives = Math.max(0, player.lives) * 2000;
        bonusTime = Math.max(0, (int) (300 - missionTime)) * 20;
        score += bonusRescue + bonusLives + bonusTime;
        recordScore();
        buttons.clear();
        int cx = Config.WIDTH / 2;
        if (!isLastStage()) {
            buttons.add(new Button("NEXT STAGE", cx - 260, 480, 230, 52, () -> {
                RunState next = new RunState();
                next.stage = stage.number() + 1;
                next.score = score;
                next.lives = Math.max(0, player.lives);
                next.bombs = Math.max(Balance.BOMB_START, player.bombs);
                host.startStage(next);
            }));
            buttons.add(new Button("STAGE SELECT", cx, 480, 230, 52, host::toStageSelect));
            buttons.add(new Button("MENU", cx + 260, 480, 230, 52, host::toMenu));
        } else {
            buttons.add(new Button("STAGE SELECT", cx - 130, 480, 230, 52, host::toStageSelect));
            buttons.add(new Button("MENU", cx + 130, 480, 230, 52, host::toMenu));
        }
    }

    private void recordScore() {
        int s = (int) score;
        if (s > highScore) {
            highScore = s;
            newHighScore = true;
            HighScore.save(HighScore.ASSAULT_FILE, s);
        }
    }

    // =====================================================================
    //  Mouse
    // =====================================================================
    public void mouseMove(Point p) {
        mouseScreen = p;
        for (Button b : buttons) b.setHover(p);
    }

    public boolean hasHover() {
        for (Button b : buttons) if (b.isHover()) return true;
        return false;
    }

    /** คืน true ถ้าคลิกโดนปุ่ม (จะได้ไม่นับเป็นการยิง) */
    public boolean mouseClick(Point p) {
        mouseScreen = p;
        for (Button b : new ArrayList<>(buttons)) {
            if (b.click(p)) return true;
        }
        return false;
    }

    public boolean isInMenuScreen() {
        return phase == Phase.PAUSED || phase == Phase.GAME_OVER || phase == Phase.MISSION_COMPLETE;
    }

    // =====================================================================
    //  DRAW
    // =====================================================================
    public void draw(Graphics2D g) {
        double sx = shakeAmt > 0 ? (Math.random() - 0.5) * shakeAmt : 0;
        double sy = shakeAmt > 0 ? (Math.random() - 0.5) * shakeAmt : 0;

        AffineTransform base = g.getTransform();
        g.translate(sx, sy);
        bg.drawBack(g, camX, t);

        AffineTransform screen = g.getTransform();
        g.scale(zoom, zoom);
        g.translate(-camX, 0);
        stage.drawWorld(g, this);
        for (Hostage h : hostages) h.draw(g);
        for (Pickup p : pickups) p.draw(g);
        if (slug.state == NovaSlug.State.PARKED) slug.draw(g, t);
        if (boss != null) boss.render(g, t);
        for (Enemy e : enemies) if (e != boss) e.render(g, t);
        for (Bullet b : bullets) if (!b.friendly) b.draw(g);
        player.draw(g, t);
        for (Bullet b : bullets) if (b.friendly) b.draw(g);
        for (Particle p : particles) p.draw(g);
        if (bombRingT >= 0) drawBombRing(g);
        g.setFont(Text.font(Font.BOLD, 14));
        for (FloatText f : floatTexts) {
            int a = (int) (255 * Math.min(1, f.life / 0.4));
            Text.centered(g, f.text, f.x, f.y, new Color(f.color.getRed(), f.color.getGreen(), f.color.getBlue(), a), false);
        }
        drawAimCursor(g);
        g.setTransform(screen);

        bg.drawFront(g, camX);
        stage.drawScreen(g, this);
        g.setTransform(base);

        if (empTimer > 0) drawEmpOverlay(g);
        if (flashTimer > 0) {
            g.setColor(new Color(255, 255, 255, (int) Math.min(200, flashTimer * 800)));
            g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
        }

        drawHud(g);
        drawMessages(g);

        switch (phase) {
            case PAUSED: drawPaused(g); break;
            case GAME_OVER: drawGameOver(g); break;
            case MISSION_COMPLETE: drawMissionComplete(g); break;
            default: break;
        }
        for (Button b : buttons) b.draw(g);
    }

    /** เป้าเล็งตอนขับรถถัง */
    private void drawAimCursor(Graphics2D g) {
        if (player.vehicle == null) return;
        Point2D m = mouseWorld();
        if (m == null) return;
        g.setColor(new Color(255, 230, 90, 200));
        g.setStroke(new BasicStroke(2f));
        double r = 11;
        g.draw(new Ellipse2D.Double(m.getX() - r, m.getY() - r, r * 2, r * 2));
        g.draw(new Line2D.Double(m.getX() - r - 5, m.getY(), m.getX() - r + 4, m.getY()));
        g.draw(new Line2D.Double(m.getX() + r - 4, m.getY(), m.getX() + r + 5, m.getY()));
        g.setStroke(new BasicStroke(1f));
    }

    private void drawBombRing(Graphics2D g) {
        double p = bombRingT / 0.6;
        double r = 30 + p * 700;
        g.setStroke(new BasicStroke((float) (24 * (1 - p) + 2)));
        g.setColor(new Color(150, 220, 255, (int) (220 * (1 - p))));
        g.draw(new Ellipse2D.Double(bombX - r, bombY - r, r * 2, r * 2));
        g.setStroke(new BasicStroke(1f));
    }

    private void drawEmpOverlay(Graphics2D g) {
        g.setColor(new Color(120, 200, 255, 18));
        for (int y = Config.HUD_HEIGHT; y < Config.HEIGHT; y += 4) g.drawLine(0, y, Config.WIDTH, y);
        g.setFont(Text.font(Font.BOLD, 14));
        Text.centered(g, String.format("EMP ACTIVE %.1fs", empTimer), Config.WIDTH - 110, Config.HUD_HEIGHT + 20,
                new Color(140, 220, 255), true);
    }

    private void drawHud(Graphics2D g) {
        int H = Config.HUD_HEIGHT, midY = H / 2;
        g.setColor(new Color(5, 10, 35, 190));
        g.fillRect(0, 0, Config.WIDTH, H);
        g.setColor(new Color(90, 120, 220, 120));
        g.drawLine(0, H, Config.WIDTH, H);

        // HP
        for (int i = 0; i < AssaultPlayer.MAX_HP; i++) {
            boolean full = i < player.hp && player.alive;
            Color c = player.hp <= 2 ? new Color(255, 80, 80) : new Color(90, 230, 120);
            g.setColor(full ? c : new Color(255, 255, 255, 35));
            g.fill(new RoundRectangle2D.Double(14 + i * 16, midY - 8, 12, 16, 4, 4));
        }
        g.setFont(Text.font(Font.BOLD, 15));
        g.setColor(Color.WHITE);
        g.drawString("x" + Math.max(0, player.lives), 98, midY + 5);

        if (player.vehicle != null) {
            // ข้อมูลรถถัง
            NovaSlug v = player.vehicle;
            g.setFont(Text.font(Font.BOLD, 13));
            g.setColor(new Color(230, 200, 80));
            g.drawString("SLUG", 140, midY + 5);
            int ax = 186;
            for (int i = 0; i < NovaSlug.MAX_ARMOR; i++) {
                g.setColor(i < v.armor ? new Color(230, 200, 80) : new Color(255, 255, 255, 35));
                g.fillRect(ax + i * 9, midY - 7, 7, 14);
            }
            boolean low = v.timeLeft < 5 && ((int) (t * 6)) % 2 == 0;
            g.setColor(low ? new Color(255, 90, 90) : Color.WHITE);
            g.setFont(Text.font(Font.BOLD, 15));
            g.drawString(String.format("%.0fs", Math.max(0, v.timeLeft)), ax + NovaSlug.MAX_ARMOR * 9 + 8, midY + 5);
            // ปืนที่รอใช้หลังลงจากรถ
            if (player.weapon != Weapon.PISTOL) {
                g.setFont(Text.font(Font.BOLD, 13));
                g.setColor(player.weapon.color);
                g.drawString("[" + player.weapon.letter + "]", ax + NovaSlug.MAX_ARMOR * 9 + 40, midY + 5);
            }
        } else {
            // อาวุธ
            g.setColor(player.weapon.color);
            g.setFont(Text.font(Font.BOLD, 15));
            g.drawString(player.weapon.hudName, 140, midY + 5);
            g.setColor(Color.WHITE);
            String ammo = player.weapon.ammoPerPickup == 0 ? "INF" : String.valueOf(player.ammo);
            g.drawString(ammo, 140 + g.getFontMetrics().stringWidth(player.weapon.hudName) + 8, midY + 5);

            // ระเบิด
            int bx = 300;
            g.setColor(new Color(40, 40, 55));
            g.fill(new Ellipse2D.Double(bx, midY - 8, 16, 16));
            g.setColor(new Color(255, 200, 60));
            g.fill(new Ellipse2D.Double(bx + 10, midY - 12, 5, 5));
            g.setColor(Color.WHITE);
            g.drawString("x" + player.bombs, bx + 20, midY + 5);
        }

        // กระเป๋าไอเทม
        int ix = 370;
        List<ItemType> q = inventory.queued();
        for (int i = 0; i < q.size() && i < 4; i++) {
            if (i == 0) {
                g.setColor(new Color(255, 220, 90, 90));
                g.fill(new RoundRectangle2D.Double(ix - 3, midY - 14, 28, 28, 8, 8));
            }
            q.get(i).drawIcon(g, ix, midY - 11, 22);
            ix += 30;
        }
        if (!q.isEmpty()) {
            g.setFont(Text.font(Font.PLAIN, 11));
            g.setColor(new Color(255, 220, 120));
            g.drawString("[SPACE]", ix, midY + 4);
        }

        // แผนที่ / เลือดบอส
        int mapX = 560, mapW = 210;
        if (boss != null && phase != Phase.MISSION_COMPLETE) {
            double ratio = boss.healthRatio();
            g.setFont(Text.font(Font.BOLD, 11));
            g.setColor(new Color(255, 150, 150));
            g.drawString(boss.displayName() + "  PHASE " + boss.phase(), mapX, midY - 6);
            g.setColor(new Color(255, 255, 255, 40));
            g.fill(new RoundRectangle2D.Double(mapX, midY, mapW, 9, 9, 9));
            g.setColor(new Color(255, 70, 90));
            g.fill(new RoundRectangle2D.Double(mapX, midY, mapW * ratio, 9, 9, 9));
        } else {
            g.setFont(Text.font(Font.BOLD, 11));
            g.setColor(new Color(200, 210, 240));
            g.drawString("STAGE " + stage.number() + "  " + stage.sectorName(camX), mapX, midY - 6);
            g.setColor(new Color(255, 255, 255, 50));
            g.fill(new RoundRectangle2D.Double(mapX, midY + 1, mapW, 5, 5, 5));
            g.setColor(new Color(255, 255, 255, 120));
            double[] starts = stage.sectorStarts();
            for (int i = 1; i < starts.length; i++) {
                int px = mapX + (int) (mapW * starts[i] / stage.arenaX());
                g.fillRect(px, midY - 1, 2, 9);
            }
            if (slug.state == NovaSlug.State.PARKED) {
                int px = mapX + (int) (mapW * Math.min(1, (slug.x - 400) / stage.arenaX()));
                g.setColor(new Color(230, 200, 80));
                g.fillRect(px - 3, midY, 7, 7);
            }
            double prog = Math.min(1, camX / stage.arenaX());
            g.setColor(new Color(90, 220, 255));
            g.fill(new Ellipse2D.Double(mapX + mapW * prog - 5, midY - 1, 10, 10));
            g.setColor(new Color(255, 80, 100));
            g.fill(new Ellipse2D.Double(mapX + mapW - 5, midY - 1, 10, 10));
        }

        // คะแนน
        g.setFont(Text.font(Font.BOLD, 17));
        g.setColor(Color.WHITE);
        String s = String.format("SCORE %07d", (int) score);
        g.drawString(s, Config.WIDTH - g.getFontMetrics().stringWidth(s) - 14, midY + 6);
    }

    private void drawMessages(Graphics2D g) {
        int cx = Config.WIDTH / 2;
        if (bannerTimer > 0 && banner != null) {
            boolean warn = banner.startsWith("WARNING");
            if (!warn || ((int) (bannerTimer * 5)) % 2 == 0) {
                Text.title(g, banner, cx, 200, warn ? 64 : 54, warn ? new Color(255, 80, 80) : Color.WHITE);
            }
            if (subBanner != null) {
                g.setFont(Text.font(Font.BOLD, subBanner.length() > 40 ? 15 : 22));
                Text.centered(g, subBanner, cx, 262, new Color(255, 220, 120), true);
            }
        }
        if (hintTimer > 0 && phase == Phase.PLAYING) {
            g.setFont(Text.font(Font.BOLD, 14));
            Text.centered(g, "J / CLICK = SHOOT    K / RIGHT-CLICK = BOMB    SPACE = ITEM    P = PAUSE",
                    cx, 300, new Color(220, 230, 255), true);
        }
        if (toastTimer > 0) {
            float a = (float) Math.min(1, toastTimer / 0.4);
            g.setFont(Text.font(Font.BOLD, 24));
            Text.centered(g, toast, cx, Config.HEIGHT - 48, new Color(255, 255, 255, (int) (255 * a)), true);
        }
        if (goTimer > 0 && ((int) (goTimer * 4)) % 2 == 0) {
            Text.title(g, "GO! >>", Config.WIDTH - 130, 200, 46, new Color(255, 220, 90));
        }
        if (lockX != Double.MAX_VALUE && phase == Phase.PLAYING) {
            g.setFont(Text.font(Font.BOLD, 13));
            Text.centered(g, "CLEAR THE AREA!", cx, Config.HUD_HEIGHT + 20, new Color(255, 160, 120), true);
        }
    }

    private void dim(Graphics2D g, int a) {
        g.setColor(new Color(5, 10, 30, a));
        g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
    }

    private void drawPaused(Graphics2D g) {
        dim(g, 140);
        Text.title(g, "PAUSED", Config.WIDTH / 2.0, 180, 64, Color.WHITE);
    }

    private void drawGameOver(Graphics2D g) {
        dim(g, 140);
        int cx = Config.WIDTH / 2;
        Text.title(g, "GAME OVER", cx, 160, 76, Color.WHITE);
        g.setFont(Text.font(Font.BOLD, 22));
        Text.centered(g, "SCORE " + (int) score + "    REACHED " + stage.sectorName(camX), cx, 260, Color.WHITE, true);
        Text.centered(g, "STAGE " + stage.number() + "    ENEMIES " + kills + "    RESCUED " + rescued, cx, 295,
                new Color(200, 210, 240), true);
        Text.centered(g, "HIGH SCORE " + highScore, cx, 335, new Color(255, 220, 120), true);
        if (newHighScore) Text.centered(g, "NEW HIGH SCORE!", cx, 370, new Color(255, 220, 90), true);
    }

    private void drawMissionComplete(Graphics2D g) {
        dim(g, 150);
        int cx = Config.WIDTH / 2;
        String title = isLastStage() ? "ALL STAGES CLEAR!" : "STAGE " + stage.number() + " COMPLETE!";
        Text.title(g, title, cx, 90, 54, new Color(255, 220, 90));
        g.setFont(Text.font(Font.BOLD, 20));
        int y = 165;
        int min = (int) missionTime / 60, sec = (int) missionTime % 60;
        String[][] rows = {
                {"ENEMIES DESTROYED", String.valueOf(kills)},
                {"HOSTAGES RESCUED  x1000", "+" + bonusRescue},
                {"LIVES LEFT  x2000", "+" + bonusLives},
                {String.format("TIME %d:%02d  BONUS", min, sec), "+" + bonusTime},
        };
        for (String[] r : rows) {
            g.setColor(new Color(210, 220, 245));
            g.drawString(r[0], cx - 260, y);
            g.setColor(Color.WHITE);
            g.drawString(r[1], cx + 260 - g.getFontMetrics().stringWidth(r[1]), y);
            y += 36;
        }
        g.setColor(new Color(255, 255, 255, 80));
        g.fillRect(cx - 260, y - 18, 520, 2);
        g.setFont(Text.font(Font.BOLD, 26));
        Text.centered(g, "TOTAL SCORE  " + (int) score, cx, y + 16, Color.WHITE, true);
        g.setFont(Text.font(Font.BOLD, 18));
        Text.centered(g, newHighScore ? "NEW HIGH SCORE!" : "HIGH SCORE  " + highScore, cx, y + 54,
                new Color(255, 220, 120), true);
    }

    /** ตัวเลขคะแนนลอยขึ้น */
    private static final class FloatText {
        double x, y, life = 0.9;
        final String text;
        final Color color;

        FloatText(double x, double y, String text, Color color) {
            this.x = x; this.y = y; this.text = text; this.color = color;
        }
    }
}
