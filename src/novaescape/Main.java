package novaescape;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * จุดเริ่มต้นของเกม Nova Escape
 * สร้างหน้าต่างเกม แล้วใส่ GamePanel (ตัวเกมจริง) ลงไป
 */
public class Main {
    public static void main(String[] args) {
        novaescape.audio.Sound.init();   // เตรียมเสียง (ทำงานเบื้องหลัง)
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Nova Escape");
            GamePanel panel = new GamePanel();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.start();
        });
    }
}
