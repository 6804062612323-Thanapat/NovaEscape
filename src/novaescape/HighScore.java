package novaescape;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * บันทึก/โหลดคะแนนสูงสุดลงไฟล์ (อยู่ในโฟลเดอร์ที่รันเกม)
 * โหมด CLASSIC ใช้ highscore.dat, โหมด ASSAULT ใช้ highscore_assault.dat
 */
public final class HighScore {
    public static final String CLASSIC_FILE = "highscore.dat";
    public static final String ASSAULT_FILE = "highscore_assault.dat";

    private HighScore() {}

    public static int load() {
        return load(CLASSIC_FILE);
    }

    public static void save(int score) {
        save(CLASSIC_FILE, score);
    }

    public static int load(String fileName) {
        Path file = Paths.get(fileName);
        try {
            if (Files.exists(file)) {
                String s = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
                return Integer.parseInt(s);
            }
        } catch (IOException | NumberFormatException ignored) {
            // ไฟล์เสียหรืออ่านไม่ได้ ถือว่าเริ่มที่ 0
        }
        return 0;
    }

    public static void save(String fileName, int score) {
        try {
            Files.write(Paths.get(fileName), String.valueOf(score).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // บันทึกไม่ได้ก็เล่นต่อได้ตามปกติ
        }
    }
}
