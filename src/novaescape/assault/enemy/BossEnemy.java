package novaescape.assault.enemy;

import novaescape.assault.AssaultGame;

/**
 * คลาสแม่ของบอสทุกตัว (แต่ละด่านมีบอสของตัวเอง)
 * HUD จะโชว์ displayName(), phase() และ healthRatio()
 */
public abstract class BossEnemy extends Enemy {

    protected BossEnemy(double x, double y, double w, double h, double hp, int score) {
        super(x, y, w, h, hp, score, 0);
    }

    @Override
    public boolean isBoss() { return true; }

    public abstract String displayName();

    public abstract int phase();

    /** สัดส่วนเลือดที่เหลือ 0..1 (บอสหลายชิ้นส่วนเขียนทับได้) */
    public double healthRatio() {
        return Math.max(0, hp / maxHp);
    }

    @Override
    protected void onDeath(AssaultGame g) {
        g.enemyKilled(this);
        g.bossDefeated(this);
    }
}
