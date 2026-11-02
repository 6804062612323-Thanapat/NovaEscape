package novaescape;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * เก็บสถานะปุ่มคีย์บอร์ด/เมาส์
 * WASD = บังคับยาน, Spacebar = ใช้ไอเทม
 * (โหมด ASSAULT) J / คลิกซ้าย = ยิง, K / คลิกขวา = ระเบิด, P / ESC = หยุดเกม
 */
public class Input extends KeyAdapter {
    public boolean up, down, left, right;
    public boolean shootKey;      // ปุ่ม J ค้าง
    public boolean mouseShoot;    // คลิกซ้ายค้าง (GamePanel เป็นคนตั้งค่า)
    private boolean spaceQueued;
    private boolean bombQueued;
    private boolean pauseQueued;
    private int numberQueued; // ปุ่ม 1-3 สำหรับเลือกไอเทม

    @Override
    public void keyPressed(KeyEvent e) {
        set(e.getKeyCode(), true);
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_SPACE) spaceQueued = true;
        if (code == KeyEvent.VK_K) bombQueued = true;
        if (code == KeyEvent.VK_P || code == KeyEvent.VK_ESCAPE) pauseQueued = true;
        if (code == KeyEvent.VK_M) novaescape.audio.Sound.toggleMute();   // M = เปิด/ปิดเสียง
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_3) {
            numberQueued = code - KeyEvent.VK_0;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        set(e.getKeyCode(), false);
    }

    private void set(int code, boolean pressed) {
        switch (code) {
            case KeyEvent.VK_W: case KeyEvent.VK_UP:    up = pressed; break;
            case KeyEvent.VK_S: case KeyEvent.VK_DOWN:  down = pressed; break;
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  left = pressed; break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: right = pressed; break;
            case KeyEvent.VK_J: shootKey = pressed; break;
            default: break;
        }
    }

    public boolean isShooting() {
        return shootKey || mouseShoot;
    }

    public void queueBomb() {
        bombQueued = true;
    }

    /** คืนค่า true ครั้งเดียวต่อการกด Spacebar 1 ครั้ง */
    public boolean consumeSpace() {
        boolean v = spaceQueued;
        spaceQueued = false;
        return v;
    }

    public boolean consumeBomb() {
        boolean v = bombQueued;
        bombQueued = false;
        return v;
    }

    public boolean consumePause() {
        boolean v = pauseQueued;
        pauseQueued = false;
        return v;
    }

    /** คืนค่าเลข 1-3 ที่เพิ่งกด (0 = ไม่ได้กด) */
    public int consumeNumber() {
        int v = numberQueued;
        numberQueued = 0;
        return v;
    }

    public void clear() {
        up = down = left = right = false;
        shootKey = mouseShoot = false;
        spaceQueued = bombQueued = pauseQueued = false;
        numberQueued = 0;
    }
}
