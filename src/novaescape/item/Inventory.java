package novaescape.item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * กระเป๋าไอเทมของผู้เล่น
 * - หัวใจ: เก็บเป็นจำนวน ใช้อัตโนมัติตอนโดนยิง
 * - โล่ / โจมตี: เข้าคิวไว้ กด Spacebar แล้วจะใช้ชิ้นที่ได้มาก่อน
 */
public class Inventory {
    private int hearts;
    private final Deque<ItemType> queue = new ArrayDeque<>();

    public void add(ItemType item) {
        if (item.activatable) queue.addLast(item);
        else if (item == ItemType.HEART) hearts++;
    }

    public int hearts() { return hearts; }

    /** ใช้หัวใจ 1 ดวง (คืน true ถ้ามีให้ใช้) */
    public boolean consumeHeart() {
        if (hearts <= 0) return false;
        hearts--;
        return true;
    }

    /** หยิบไอเทมชิ้นถัดไปที่จะใช้ด้วย Spacebar (null = ไม่มี) */
    public ItemType pollActivatable() {
        return queue.pollFirst();
    }

    public List<ItemType> queued() {
        return new ArrayList<>(queue);
    }

    public void clear() {
        hearts = 0;
        queue.clear();
    }
}
