package novaescape.assault;

/** สิ่งที่โหมด ASSAULT ขอให้ GamePanel ทำ (เปลี่ยนหน้าจอ) */
public interface AssaultHost {
    void toMenu();

    void toStageSelect();

    void startStage(RunState run);
}
