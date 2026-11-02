package novaescape;

/** สถานะ (หน้าจอ) ต่าง ๆ ของเกม ตาม storyboard */
public enum GameState {
    MENU,         // หน้าเริ่มเกม (ปุ่ม START)
    MODE_SELECT,  // เลือกโหมด CLASSIC / ASSAULT
    STAGE_SELECT, // เลือกด่านของโหมด ASSAULT
    PLAYING,      // กำลังเล่น
    GAME_OVER,    // ชนเลเซอร์
    WIN,          // หลบครบเวลา -> เลือก PAUSE (หยุด) หรือ CONTINUE (เล่นต่อ)
    CHOOSE_ITEM,  // เลือกไอเทม 1 อย่างก่อนเล่นตาถัดไป
    SUMMARY,      // สรุปคะแนนหลังกด PAUSE
    ASSAULT       // โหมดยิงสู้แบบ Metal Slug (ดูแลโดย assault.AssaultGame)
}
