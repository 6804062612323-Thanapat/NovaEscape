package novaescape.assault;

/** ข้อมูลที่ยกไปด่านถัดไปเมื่อกด NEXT STAGE */
public class RunState {
    public int stage = 1;
    public double score = 0;
    public int lives = 2;
    public int bombs = Balance.BOMB_START;

    public RunState copy() {
        RunState r = new RunState();
        r.stage = stage;
        r.score = score;
        r.lives = lives;
        r.bombs = bombs;
        return r;
    }

    public static RunState fresh(int stage) {
        RunState r = new RunState();
        r.stage = stage;
        return r;
    }
}
