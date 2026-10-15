package seoultech.se.tetris.ui.game;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.IntConsumer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
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

        GameProgressTimer timer = new GameProgressTimer(engine, engine);
        engine.addListener(timer);
        engine.addListener(new GameEventListener() {
            @Override public void onBoardUpdated() { panel.repaint(); }
            @Override public void onLinesCleared(int clearedCount, int gainedScore) { }
            @Override public void onSpeedIncreased(int currentInterval) { }
            @Override public void onGameOver(int finalScore) {
                // 낙하 타이머는 GameProgressTimer가 멈추고, 창 전환은 이 이벤트가 끝난 뒤에 한다.
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
        return frame;
    }
}
