package seoultech.se.tetris.ui.game;

import java.util.function.IntConsumer;

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
        SwingUtilities.invokeLater(() -> launch());
    }

    /**
     * 게임 화면을 연다. 게임 오버 시에는 보드 위 문구만 남긴다.
     * Swing 이벤트 스레드에서 호출한다.
     */
    public static GameFrame launch() {
        return launch(null);
    }

    /**
     * 게임 화면을 연다. 게임을 더 진행할 수 없으면 {@code onGameOver}에 최종 점수를 넘긴다.
     * 콜백이 없으면 보드 위에 종료 문구만 남긴다.
     * Swing 이벤트 스레드에서 호출한다.
     */
    public static GameFrame launch(IntConsumer onGameOver) {
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
            @Override public void onGameOver(int finalScore) {
                timer.stop();
                // 창을 닫는 일은 현재 타이머 이벤트가 끝난 뒤에 한다.
                if (onGameOver != null) {
                    SwingUtilities.invokeLater(() -> onGameOver.accept(finalScore));
                    return;
                }
                panel.repaint();
            }
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
                        System.exit(0);
                    }
                }));
        input.attachTo(panel);
        frame.setVisible(true);
        panel.requestFocusInWindow();
        timer.start();
        return frame;
    }
}
