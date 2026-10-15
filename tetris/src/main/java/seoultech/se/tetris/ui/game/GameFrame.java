package seoultech.se.tetris.ui.game;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.engine.TetrisEngine;
import seoultech.se.tetris.ui.GameKeyInput;

/**
 * 게임 화면을 담는 창. 앱 진입 또는 단독 실행 시 엔진·자동 낙하·키 입력을 연결한다.
 * 일시정지 메뉴는 조작/설정 담당 구현으로 교체한다.
 */
public class GameFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public GameFrame(GamePanel panel) {
        super("SeoulTech SE Tetris");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::launch);
    }

    /** 게임 화면을 연다. Swing 이벤트 스레드에서 호출한다. */
    public static void launch() {
        PropertiesAppSettingsService settingsService = new PropertiesAppSettingsService();
        settingsService.load();
        var settings = settingsService.current();
        TetrisEngine engine = new TetrisEngine(settings.difficulty());
        GamePanel panel = new GamePanel(engine, settings);
        GameFrame frame = new GameFrame(panel);

        GameProgressTimer timer = new GameProgressTimer(engine, engine);
        engine.addListener(timer);
        engine.addListener(new GameEventListener() {
            @Override public void onBoardUpdated() { panel.repaint(); }
            @Override public void onLinesCleared(int clearedCount, int gainedScore) { }
            @Override public void onSpeedIncreased(int currentInterval) { }
            @Override public void onGameOver(int finalScore) { panel.repaint(); }
        });

        PropertiesKeyConfigService keyConfig = new PropertiesKeyConfigService();
        keyConfig.load();
        GameKeyInput input = new GameKeyInput(keyConfig, engine, engine,
                () -> SwingUtilities.invokeLater(() -> {
                    panel.repaint();
                    int choice = JOptionPane.showConfirmDialog(frame, "계속하시겠습니까?", "일시정지",
                            JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) {
                        engine.resumeGame();
                        panel.repaint();
                        panel.requestFocusInWindow();
                    } else {
                        engine.exitGame();
                        frame.dispose();
                    }
                }));
        input.attachTo(panel);
        frame.addWindowListener(new WindowAdapter() {
            private void releaseGame() {
                timer.stop();
                engine.removeListener(timer);
            }

            @Override public void windowClosing(WindowEvent event) { releaseGame(); }
            @Override public void windowClosed(WindowEvent event) { releaseGame(); }
        });
        frame.setVisible(true);
        panel.requestFocusInWindow();
        timer.start();
    }
}
