package seoultech.se.tetris.ui.game;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.engine.LevelPolicy;
import seoultech.se.tetris.engine.TetrisEngine;
import seoultech.se.tetris.ui.GameKeyInput;

/**
 * 게임 화면을 담는 창. 앱 진입 또는 단독 실행 시 엔진·타이머·키 입력을 임시로 연결한다.
 * 실제 타이머와 일시정지 메뉴는 조작/설정 담당 구현으로 교체한다.
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
        TetrisEngine engine = new TetrisEngine();
        GamePanel panel = new GamePanel(engine);
        GameFrame frame = new GameFrame(panel);

        Timer timer = new Timer(LevelPolicy.INITIAL_INTERVAL_MS, e -> {
            if (!engine.isPaused() && !engine.isGameOver()) {
                engine.moveDown();
            }
        });
        engine.addListener(new GameEventListener() {
            @Override public void onBoardUpdated() { panel.repaint(); }
            @Override public void onLinesCleared(int clearedCount, int gainedScore) { }
            @Override public void onSpeedIncreased(int currentInterval) { timer.setDelay(currentInterval); }
            @Override public void onGameOver(int finalScore) { timer.stop(); panel.repaint(); }
        });

        GameKeyInput input = new GameKeyInput(new PropertiesKeyConfigService(), engine, engine,
                () -> SwingUtilities.invokeLater(() -> {
                    panel.repaint();
                    int choice = JOptionPane.showConfirmDialog(frame, "계속하시겠습니까?", "일시정지",
                            JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) {
                        engine.resumeGame();
                        panel.repaint();
                        panel.requestFocusInWindow();
                    } else {
                        System.exit(0);
                    }
                }));
        input.attachTo(panel);
        frame.setVisible(true);
        panel.requestFocusInWindow();
        timer.start();
    }
}
