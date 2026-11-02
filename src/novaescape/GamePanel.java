package novaescape;

import novaescape.assault.AssaultGame;
import novaescape.assault.AssaultHost;
import novaescape.assault.RunState;
import novaescape.assault.stage.Stage;
import novaescape.audio.Music;
import novaescape.audio.Sfx;
import novaescape.audio.Sound;
import novaescape.entity.AlienShip;
import novaescape.entity.Laser;
import novaescape.entity.Particle;
import novaescape.entity.PlayerShip;
import novaescape.item.Inventory;
import novaescape.item.ItemType;
import novaescape.ui.Background;
import novaescape.ui.Button;
import novaescape.ui.Text;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * หัวใจของเกม: game loop, สถานะเกม, การชน, และการวาดทุกหน้าจอ
 *
 * ลำดับการเล่น (ตาม storyboard):
 * โหมด ASSAULT (ยิงสู้แบบ Metal Slug) อยู่ในแพ็กเกจ novaescape.assault — GamePanel แค่ส่งต่อให้ AssaultGame
 *
 * MENU -> PLAYING -> (ชนเลเซอร์) GAME_OVER
 *                  -> (หลบครบเวลา) WIN -> PAUSE = SUMMARY (จบเกม)
 *                                       -> CONTINUE = CHOOSE_ITEM -> PLAYING ตาถัดไป (ยากขึ้น)
 */
public class GamePanel extends JPanel {

    // ---------- สถานะเกม ----------
    private GameState state = GameState.MENU;
    private int round = 1;
    private double score = 0;
    private int highScore = HighScore.load();
    private boolean newHighScore = false;
    private double roundTimer;
    private double readyTimer;
    private double time;              // เวลารวมตั้งแต่เปิดเกม (ใช้ทำแอนิเมชัน)
    private double hitFlash;          // จอแวบขาวตอนโดนยิง
    private String toast = "";        // ข้อความแจ้งเตือนสั้น ๆ
    private double toastTimer;
    private int lastBonus;            // โบนัสผ่านตาล่าสุด (โชว์ในหน้า WIN)

    // ---------- วัตถุในเกม ----------
    private final Input input = new Input();
    private final PlayerShip player = new PlayerShip();
    private final AlienShip alien = new AlienShip();
    private final List<Laser> lasers = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Background background = new Background();
    private final Inventory inventory = new Inventory();
    private AssaultGame assault;      // โหมด ASSAULT (null = ไม่ได้เล่นโหมดนี้)
    private int assaultHighScore = HighScore.load(HighScore.ASSAULT_FILE);

    // ---------- UI ----------
    private final List<Button> buttons = new ArrayList<>();
    private Point mouse;
    private int hoverCard = -1;

    private final Timer loop;
    private long lastNanos;

    public GamePanel() {
        setPreferredSize(new Dimension(Config.WIDTH, Config.HEIGHT));
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(input);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { onMouseMove(e.getPoint()); }
            @Override public void mouseDragged(MouseEvent e) { onMouseMove(e.getPoint()); }
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (state == GameState.ASSAULT && assault != null) {
                    // คลิกโดนปุ่ม = กดปุ่ม, ไม่โดน = ซ้ายยิง / ขวาระเบิด
                    if (assault.mouseClick(e.getPoint())) return;
                    if (assault.isInMenuScreen()) return;
                    if (SwingUtilities.isLeftMouseButton(e)) input.mouseShoot = true;
                    if (SwingUtilities.isRightMouseButton(e)) input.queueBomb();
                    return;
                }
                onMouseClick(e.getPoint());
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) input.mouseShoot = false;
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        loop = new Timer(Config.FRAME_MS, e -> tick());
        goToMenu();
    }

    public void start() {
        lastNanos = System.nanoTime();
        loop.start();
        requestFocusInWindow();
    }

    // =====================================================================
    //  เปลี่ยนหน้าจอ / สถานะ
    // =====================================================================

    private void goToMenu() {
        state = GameState.MENU;
        Sound.music(Music.MENU);
        assault = null;
        input.mouseShoot = false;
        assaultHighScore = HighScore.load(HighScore.ASSAULT_FILE);
        player.reset();
        alien.reset();
        lasers.clear();
        buttons.clear();
        int cx = Config.WIDTH / 2;
        buttons.add(new Button("START", cx, 350, 240, 58, this::goToModeSelect));
    }

    /** หน้าเลือกโหมด: CLASSIC (หลบเลเซอร์) / ASSAULT (ยิงสู้) */
    private void goToModeSelect() {
        state = GameState.MODE_SELECT;
        Sound.music(Music.MENU);
        assault = null;
        input.mouseShoot = false;
        buttons.clear();
        int cx = Config.WIDTH / 2;
        buttons.add(new Button("CLASSIC", cx - 170, 270, 280, 70, this::newGame));
        buttons.add(new Button("ASSAULT", cx + 170, 270, 280, 70, this::goToStageSelect));
        buttons.add(new Button("BACK", cx, 470, 180, 46, this::goToMenu));
    }

    /** หน้าเลือกด่านของโหมด ASSAULT */
    private void goToStageSelect() {
        state = GameState.STAGE_SELECT;
        Sound.music(Music.MENU);
        assault = null;
        input.mouseShoot = false;
        assaultHighScore = HighScore.load(HighScore.ASSAULT_FILE);
        buttons.clear();
        buttons.add(new Button("BACK", Config.WIDTH / 2, 503, 180, 44, this::goToModeSelect));
        setCursor(Cursor.getDefaultCursor());
    }

    /** เริ่มเล่นด่านในโหมด ASSAULT */
    private void startAssault(RunState run) {
        state = GameState.ASSAULT;
        buttons.clear();
        input.consumeSpace();
        input.consumeBomb();
        input.consumePause();
        input.mouseShoot = false;
        assault = new AssaultGame(input, run, assaultHost);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    private final AssaultHost assaultHost = new AssaultHost() {
        @Override public void toMenu() { goToMenu(); }
        @Override public void toStageSelect() { goToStageSelect(); }
        @Override public void startStage(RunState run) { startAssault(run); }
    };

    private void newGame() {
        Sound.music(Music.CLASSIC);
        round = 1;
        score = 0;
        newHighScore = false;
        inventory.clear();
        startRound();
    }

    private void startRound() {
        state = GameState.PLAYING;
        buttons.clear();
        player.reset();
        alien.reset();
        lasers.clear();
        roundTimer = Config.ROUND_TIME;
        readyTimer = Config.READY_TIME;
        input.consumeSpace();
    }

    private void winRound() {
        state = GameState.WIN;
        Sound.play(Sfx.WIN);
        int bonus = Config.ROUND_CLEAR_BONUS * round;
        score += bonus;
        lastBonus = bonus;
        for (Laser l : lasers) Particle.explode(particles, l.x, l.y, 4);
        lasers.clear();
        player.shieldTimer = 0;
        buttons.clear();
        int cx = Config.WIDTH / 2;
        buttons.add(new Button("PAUSE", cx, 330, 230, 54, this::stopGame));
        buttons.add(new Button("CONTINUE", cx, 410, 230, 54, () -> {
            state = GameState.CHOOSE_ITEM;
            buttons.clear();
        }));
    }

    private void gameOver() {
        state = GameState.GAME_OVER;
        player.alive = false;
        Sound.music(null);
        Sound.play(Sfx.BIG_EXPLOSION);
        Sound.play(Sfx.GAME_OVER);
        Particle.explode(particles, player.centerX(), player.centerY(), 70);
        hitFlash = 0.35;
        recordScore();
        buttons.clear();
        int cx = Config.WIDTH / 2;
        buttons.add(new Button("PLAY AGAIN", cx - 130, 420, 230, 52, this::newGame));
        buttons.add(new Button("MENU", cx + 130, 420, 230, 52, this::goToMenu));
    }

    /** กด PAUSE หลังชนะ = หยุดเกมและสรุปคะแนน */
    private void stopGame() {
        state = GameState.SUMMARY;
        recordScore();
        buttons.clear();
        int cx = Config.WIDTH / 2;
        buttons.add(new Button("PLAY AGAIN", cx - 130, 430, 230, 52, this::newGame));
        buttons.add(new Button("MENU", cx + 130, 430, 230, 52, this::goToMenu));
    }

    private void chooseItem(ItemType item) {
        Sound.play(Sfx.POWERUP);
        inventory.add(item);
        round++;
        showToast(item.label + " ADDED");
        startRound();
    }

    private void recordScore() {
        int s = (int) score;
        if (s > highScore) {
            highScore = s;
            newHighScore = true;
            HighScore.save(s);
        }
    }

    // =====================================================================
    //  Game loop
    // =====================================================================

    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - lastNanos) / 1_000_000_000.0);
        lastNanos = now;
        update(dt);
        repaint();
    }

    private void update(double dt) {
        time += dt;
        if (state == GameState.ASSAULT && assault != null) {
            assault.update(dt);
            return;
        }
        background.update(dt, state == GameState.PLAYING ? 170 : 45);
        if (hitFlash > 0) hitFlash -= dt;
        if (toastTimer > 0) toastTimer -= dt;

        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            p.update(dt);
            if (p.isDead()) it.remove();
        }

        Difficulty diff = new Difficulty(round);

        switch (state) {
            case PLAYING:
                updatePlaying(dt, diff);
                break;
            case CHOOSE_ITEM: {
                alien.update(dt, diff, player, lasers, false);
                int n = input.consumeNumber();
                if (n >= 1 && n <= ItemType.values().length) chooseItem(ItemType.values()[n - 1]);
                break;
            }
            default:
                alien.update(dt, diff, player, lasers, false);
                updateLasers(dt);
                input.consumeSpace();
                input.consumeNumber();
                break;
        }
    }

    private void updatePlaying(double dt, Difficulty diff) {
        player.update(dt, input);

        boolean ready = readyTimer > 0;
        if (ready) {
            readyTimer -= dt;
        } else {
            roundTimer -= dt;
            score += dt * Config.SCORE_PER_SECOND * round;
        }

        alien.update(dt, diff, player, lasers, !ready);
        updateLasers(dt);

        if (input.consumeSpace()) useItem();

        // ตรวจการชน
        Rectangle2D hb = player.hitbox();
        for (Iterator<Laser> it = lasers.iterator(); it.hasNext(); ) {
            Laser l = it.next();
            if (!l.hitbox().intersects(hb)) continue;

            if (player.shieldTimer > 0) {
                // โล่สลายเลเซอร์
                Particle.explode(particles, l.x, l.y, 8);
                it.remove();
            } else if (player.invulnTimer > 0) {
                // อมตะชั่วคราว เลเซอร์ทะลุผ่าน
            } else if (inventory.consumeHeart()) {
                it.remove();
                player.invulnTimer = Config.REVIVE_INVULN;
                hitFlash = 0.25;
                Particle.explode(particles, player.centerX(), player.centerY(), 25);
                showToast("REVIVED!");
                Sound.play(Sfx.HURT);
                Sound.play(Sfx.ONEUP);
            } else {
                gameOver();
                return;
            }
        }

        if (roundTimer <= 0) winRound();
    }

    private void updateLasers(double dt) {
        for (Iterator<Laser> it = lasers.iterator(); it.hasNext(); ) {
            Laser l = it.next();
            l.update(dt);
            if (l.isOffScreen()) it.remove();
        }
    }

    /** กด Spacebar = ใช้ไอเทมชิ้นถัดไปในคิว */
    private void useItem() {
        ItemType item = inventory.pollActivatable();
        if (item == null) {
            showToast("NO ITEM");
            return;
        }
        switch (item) {
            case SHIELD:
                player.shieldTimer = Config.SHIELD_TIME;
                showToast("SHIELD ON!");
                Sound.play(Sfx.SHIELD);
                break;
            case ATTACK:
                alien.disabledTimer = Config.ATTACK_TIME;
                Particle.explode(particles, Config.WIDTH - 90, alien.centerY, 40);
                showToast("ENEMY DISABLED!");
                Sound.play(Sfx.EMP);
                break;
            default:
                break;
        }
    }

    private void showToast(String msg) {
        toast = msg;
        toastTimer = 1.6;
    }

    // =====================================================================
    //  Mouse
    // =====================================================================

    private void onMouseMove(Point p) {
        mouse = p;
        if (state == GameState.ASSAULT && assault != null) {
            assault.mouseMove(p);
            setCursor(Cursor.getPredefinedCursor(assault.hasHover() ? Cursor.HAND_CURSOR : Cursor.CROSSHAIR_CURSOR));
            return;
        }
        for (Button b : buttons) b.setHover(p);
        hoverCard = -1;
        if (state == GameState.STAGE_SELECT) {
            for (int i = 0; i < Stage.COUNT; i++) {
                if (stageCard(i).contains(p)) hoverCard = i;
            }
        }
        if (state == GameState.CHOOSE_ITEM) {
            for (int i = 0; i < ItemType.values().length; i++) {
                if (itemCard(i).contains(p)) hoverCard = i;
            }
        }
        boolean hand = hoverCard >= 0 || buttons.stream().anyMatch(Button::isHover);
        setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void onMouseClick(Point p) {
        if (state == GameState.STAGE_SELECT) {
            for (int i = 0; i < Stage.COUNT; i++) {
                if (stageCard(i).contains(p)) {
                    startAssault(RunState.fresh(i + 1));
                    return;
                }
            }
        }
        if (state == GameState.CHOOSE_ITEM) {
            for (int i = 0; i < ItemType.values().length; i++) {
                if (itemCard(i).contains(p)) {
                    chooseItem(ItemType.values()[i]);
                    return;
                }
            }
        }
        for (Button b : new ArrayList<>(buttons)) {
            if (b.click(p)) break;
        }
        onMouseMove(p);
    }

    private static final int PANEL_W = 600, PANEL_H = 320;

    private Rectangle stageCard(int i) {
        int cardW = 270, cardH = 300, gap = 24;
        int total = Stage.COUNT * cardW + (Stage.COUNT - 1) * gap;
        int x0 = Config.WIDTH / 2 - total / 2;
        return new Rectangle(x0 + i * (cardW + gap), 140, cardW, cardH);
    }

    private Rectangle itemCard(int i) {
        int n = ItemType.values().length;
        int cardW = 160, cardH = 190, gap = 24;
        int total = n * cardW + (n - 1) * gap;
        int x0 = Config.WIDTH / 2 - total / 2;
        int y0 = (Config.HEIGHT - PANEL_H) / 2 + 95;
        return new Rectangle(x0 + i * (cardW + gap), y0, cardW, cardH);
    }

    // =====================================================================
    //  วาดหน้าจอ
    // =====================================================================

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (state == GameState.ASSAULT && assault != null) {
            assault.draw(g);
            drawSoundIndicator(g);
            g.dispose();
            return;
        }

        background.draw(g, time);
        alien.draw(g, time);
        for (Laser l : lasers) l.draw(g);
        player.draw(g, time);
        for (Particle p : particles) p.draw(g);

        boolean menuLike = state == GameState.MENU || state == GameState.MODE_SELECT || state == GameState.STAGE_SELECT;
        if (!menuLike) drawHud(g);

        switch (state) {
            case MENU:        drawMenu(g); break;
            case MODE_SELECT: drawModeSelect(g); break;
            case STAGE_SELECT: drawStageSelect(g); break;
            case PLAYING:     drawPlayingOverlay(g); break;
            case GAME_OVER:   drawGameOver(g); break;
            case WIN:         drawWin(g); break;
            case CHOOSE_ITEM: drawChooseItem(g); break;
            case SUMMARY:     drawSummary(g); break;
            default: break;
        }

        for (Button b : buttons) b.draw(g);

        if (hitFlash > 0) {
            g.setColor(new Color(255, 255, 255, (int) Math.min(200, hitFlash * 600)));
            g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
        }
        drawSoundIndicator(g);
        g.dispose();
    }

    /** ป้ายมุมขวาล่างเมื่อปิดเสียง */
    private void drawSoundIndicator(Graphics2D g) {
        if (!Sound.isMuted()) return;
        g.setFont(Text.font(Font.BOLD, 13));
        String s = "SOUND OFF [M]";
        int w = g.getFontMetrics().stringWidth(s);
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(Config.WIDTH - w - 22, Config.HEIGHT - 30, w + 14, 22, 10, 10);
        g.setColor(new Color(255, 200, 120));
        g.drawString(s, Config.WIDTH - w - 15, Config.HEIGHT - 14);
    }

    private void dim(Graphics2D g, int alpha) {
        g.setColor(new Color(5, 10, 30, alpha));
        g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
    }

    private void drawMenu(Graphics2D g) {
        int cx = Config.WIDTH / 2;
        float bob = (float) Math.sin(time * 2) * 4;
        Text.title(g, "NOVA", cx, 125 + bob, 80, Color.WHITE);
        Text.title(g, "ESCAPE", cx, 210 + bob, 80, Color.WHITE);

        g.setFont(Text.font(Font.BOLD, 16));
        Text.centered(g, "CLASSIC BEST " + highScore + "      ASSAULT BEST " + assaultHighScore,
                cx, 430, new Color(255, 220, 120), true);

        g.setFont(Text.font(Font.PLAIN, 15));
        Text.centered(g, "WASD = Move    SPACE = Use item    Mouse = Buttons    M = Sound on/off",
                cx, Config.HEIGHT - 30, new Color(200, 210, 240), true);
    }

    private void drawModeSelect(Graphics2D g) {
        dim(g, 90);
        int cx = Config.WIDTH / 2;
        Text.title(g, "SELECT MODE", cx, 120, 56, Color.WHITE);
        g.setFont(Text.font(Font.BOLD, 15));
        Text.centered(g, "Dodge the alien lasers", cx - 170, 330, new Color(210, 225, 255), true);
        Text.centered(g, "(the original game)", cx - 170, 352, new Color(170, 185, 220), true);
        Text.centered(g, "Shoot & fight, Metal Slug style", cx + 170, 330, new Color(210, 225, 255), true);
        Text.centered(g, "3 stages · 3 bosses · tank", cx + 170, 352, new Color(170, 185, 220), true);
    }

    private void drawStageSelect(Graphics2D g) {
        dim(g, 120);
        int cx = Config.WIDTH / 2;
        Text.title(g, "ASSAULT - SELECT STAGE", cx, 80, 44, Color.WHITE);
        for (int i = 0; i < Stage.COUNT; i++) {
            Stage st = Stage.create(i + 1);
            Stage.Theme th = st.theme();
            Rectangle r = stageCard(i);
            boolean hov = i == hoverCard;
            int lift = hov ? 6 : 0;
            RoundRectangle2D card = new RoundRectangle2D.Double(r.x, r.y - lift, r.width, r.height, 22, 22);
            g.setPaint(new GradientPaint(0, r.y, th.skyTop, 0, r.y + r.height, th.skyBottom));
            g.fill(card);
            // ลายประจำด่านเล็ก ๆ
            g.setColor(new Color(th.accent.getRed(), th.accent.getGreen(), th.accent.getBlue(), 60));
            g.fill(new java.awt.geom.Ellipse2D.Double(r.x + r.width - 120, r.y + 30 - lift, 150, 150));
            g.setStroke(new BasicStroke(hov ? 3f : 2f));
            g.setColor(hov ? new Color(255, 230, 120) : new Color(th.accent.getRed(), th.accent.getGreen(), th.accent.getBlue(), 200));
            g.draw(card);
            g.setStroke(new BasicStroke(1f));

            Text.title(g, "STAGE " + (i + 1), r.getCenterX(), r.y + 40 - lift, 30, Color.WHITE);
            g.setFont(Text.font(Font.BOLD, 22));
            Text.centered(g, st.name(), r.getCenterX(), r.y + 95 - lift, th.accent, true);
            g.setFont(Text.font(Font.PLAIN, 12));
            Text.centered(g, st.tagline(), r.getCenterX(), r.y + 125 - lift, new Color(230, 235, 250), true);
            String[] sec = st.sectorNames();
            int step = sec.length > 4 ? 15 : 20;
            g.setFont(Text.font(Font.BOLD, sec.length > 4 ? 11 : 13));
            for (int k = 0; k < sec.length; k++) {
                Text.centered(g, (k + 1) + ". " + sec[k], r.getCenterX(), r.y + 150 + k * step - lift,
                        new Color(200, 210, 235), false);
            }
            int bossY = sec.length > 4 ? 252 : 240;
            g.setFont(Text.font(Font.BOLD, 12));
            Text.centered(g, "BOSS", r.getCenterX(), r.y + bossY - lift, new Color(255, 150, 150), false);
            g.setFont(Text.font(Font.BOLD, 18));
            Text.centered(g, st.bossName(), r.getCenterX(), r.y + bossY + 22 - lift, Color.WHITE, true);
        }
        g.setFont(Text.font(Font.BOLD, 15));
        Text.centered(g, "ASSAULT BEST " + assaultHighScore, cx, 462, new Color(255, 220, 120), true);
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(5, 10, 35, 170));
        g.fillRect(0, 0, Config.WIDTH, Config.HUD_HEIGHT);
        g.setColor(new Color(90, 120, 220, 120));
        g.drawLine(0, Config.HUD_HEIGHT, Config.WIDTH, Config.HUD_HEIGHT);

        int midY = Config.HUD_HEIGHT / 2;

        // ตา
        g.setFont(Text.font(Font.BOLD, 18));
        g.setColor(Color.WHITE);
        g.drawString("ROUND " + round, 16, midY + 6);

        // ไอเทม
        int ix = 150;
        ItemType.HEART.drawIcon(g, ix, midY - 12, 24);
        g.setFont(Text.font(Font.BOLD, 16));
        g.setColor(Color.WHITE);
        g.drawString("x" + inventory.hearts(), ix + 28, midY + 6);
        ix += 70;
        List<ItemType> q = inventory.queued();
        for (int i = 0; i < q.size() && i < 6; i++) {
            if (i == 0) {
                g.setColor(new Color(255, 220, 90, 90));
                g.fill(new RoundRectangle2D.Double(ix - 3, midY - 15, 30, 30, 8, 8));
            }
            q.get(i).drawIcon(g, ix, midY - 12, 24);
            ix += 32;
        }
        if (!q.isEmpty()) {
            g.setFont(Text.font(Font.PLAIN, 12));
            g.setColor(new Color(255, 220, 120));
            g.drawString("[SPACE]", ix + 2, midY + 5);
        }

        // แถบเวลา
        int barW = 200, barH = 12;
        int barX = Config.WIDTH / 2 + 40, barY = midY - barH / 2;
        double ratio = Math.max(0, Math.min(1, roundTimer / Config.ROUND_TIME));
        if (state != GameState.PLAYING) ratio = state == GameState.WIN ? 0 : ratio;
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(new RoundRectangle2D.Double(barX, barY, barW, barH, barH, barH));
        g.setColor(new Color(90, 220, 255));
        g.fill(new RoundRectangle2D.Double(barX, barY, barW * ratio, barH, barH, barH));
        g.setFont(Text.font(Font.BOLD, 14));
        g.setColor(Color.WHITE);
        g.drawString(String.format("%4.1fs", Math.max(0, roundTimer)), barX + barW + 8, midY + 5);

        // คะแนน
        g.setFont(Text.font(Font.BOLD, 18));
        String s = String.format("SCORE %06d", (int) score);
        int sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, Config.WIDTH - sw - 16, midY + 6);
    }

    private void drawPlayingOverlay(Graphics2D g) {
        int cx = Config.WIDTH / 2, cy = Config.HEIGHT / 2;
        if (readyTimer > 0) {
            Text.title(g, "ROUND " + round, cx, cy - 30, 56, Color.WHITE);
            g.setFont(Text.font(Font.BOLD, 24));
            Text.centered(g, readyTimer > 0.5 ? "GET READY" : "GO!", cx, cy + 30,
                    new Color(255, 220, 120), true);
        }
        int y = Config.HUD_HEIGHT + 22;
        g.setFont(Text.font(Font.BOLD, 15));
        if (player.shieldTimer > 0) {
            Text.centered(g, String.format("SHIELD %.1fs", player.shieldTimer), 110, y,
                    new Color(140, 235, 255), true);
        }
        if (alien.isDisabled()) {
            Text.centered(g, String.format("ENEMY OFFLINE %.1fs", alien.disabledTimer),
                    Config.WIDTH - 150, y, new Color(255, 190, 90), true);
        }
        drawToast(g);
    }

    private void drawToast(Graphics2D g) {
        if (toastTimer <= 0) return;
        float a = (float) Math.min(1, toastTimer / 0.4);
        g.setFont(Text.font(Font.BOLD, 22));
        Text.centered(g, toast, Config.WIDTH / 2.0, Config.HEIGHT - 45,
                new Color(255, 255, 255, (int) (255 * a)), true);
    }

    private void drawGameOver(Graphics2D g) {
        dim(g, 110);
        int cx = Config.WIDTH / 2;
        Text.title(g, "GAME", cx, 170, 80, Color.WHITE);
        Text.title(g, "OVER", cx, 255, 80, Color.WHITE);
        g.setFont(Text.font(Font.BOLD, 22));
        Text.centered(g, "SCORE " + (int) score + "    ROUND " + round, cx, 330, Color.WHITE, true);
        if (newHighScore) {
            Text.centered(g, "NEW HIGH SCORE!", cx, 362, new Color(255, 220, 90), true);
        }
    }

    private void drawWin(Graphics2D g) {
        dim(g, 80);
        int cx = Config.WIDTH / 2;
        Text.title(g, "WIN", cx, 170, 96, Color.WHITE);
        g.setFont(Text.font(Font.BOLD, 20));
        Text.centered(g, "ROUND " + round + " CLEAR  (+" + lastBonus + ")    SCORE " + (int) score,
                cx, 248, Color.WHITE, true);
        g.setFont(Text.font(Font.PLAIN, 15));
        Text.centered(g, "PAUSE = stop & keep score     CONTINUE = pick an item, next round is harder",
                cx, 475, new Color(200, 210, 240), true);
    }

    private void drawChooseItem(Graphics2D g) {
        dim(g, 90);
        int px = (Config.WIDTH - PANEL_W) / 2, py = (Config.HEIGHT - PANEL_H) / 2;
        g.setColor(new Color(20, 70, 160, 235));
        g.fill(new RoundRectangle2D.Double(px, py, PANEL_W, PANEL_H, 26, 26));
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(110, 170, 255));
        g.draw(new RoundRectangle2D.Double(px, py, PANEL_W, PANEL_H, 26, 26));
        g.setStroke(new BasicStroke(1f));

        Text.title(g, "CHOOSE 1 ITEM", Config.WIDTH / 2.0, py + 50, 40, Color.WHITE);

        ItemType[] items = ItemType.values();
        for (int i = 0; i < items.length; i++) {
            Rectangle r = itemCard(i);
            boolean hov = i == hoverCard;
            g.setColor(hov ? new Color(60, 130, 240) : new Color(25, 85, 185));
            g.fill(new RoundRectangle2D.Double(r.x, r.y - (hov ? 4 : 0), r.width, r.height, 18, 18));
            if (hov) {
                g.setStroke(new BasicStroke(2.5f));
                g.setColor(new Color(255, 230, 120));
                g.draw(new RoundRectangle2D.Double(r.x, r.y - 4, r.width, r.height, 18, 18));
                g.setStroke(new BasicStroke(1f));
            }
            int lift = hov ? 4 : 0;
            double iconSize = 78;
            items[i].drawIcon(g, r.getCenterX() - iconSize / 2, r.y + 18 - lift, iconSize);
            g.setFont(Text.font(Font.BOLD, 18));
            Text.centered(g, items[i].label, r.getCenterX(), r.y + 122 - lift, Color.WHITE, true);
            g.setFont(Text.font(Font.PLAIN, 11));
            Text.centered(g, items[i].description, r.getCenterX(), r.y + 148 - lift,
                    new Color(210, 225, 255), false);
            g.setFont(Text.font(Font.BOLD, 12));
            Text.centered(g, "[" + (i + 1) + "]", r.getCenterX(), r.y + 172 - lift,
                    new Color(255, 220, 120), false);
        }
    }

    private void drawSummary(Graphics2D g) {
        dim(g, 140);
        int cx = Config.WIDTH / 2;
        Text.title(g, "GAME SUMMARY", cx, 130, 56, Color.WHITE);
        g.setFont(Text.font(Font.BOLD, 24));
        Text.centered(g, "ROUNDS CLEARED  " + round, cx, 215, Color.WHITE, true);
        Text.centered(g, "FINAL SCORE  " + (int) score, cx, 260, Color.WHITE, true);
        Text.centered(g, "HIGH SCORE  " + highScore, cx, 305, new Color(255, 220, 120), true);
        if (newHighScore) {
            g.setFont(Text.font(Font.BOLD, 20));
            Text.centered(g, "NEW HIGH SCORE!", cx, 350, new Color(255, 220, 90), true);
        }
    }
}
