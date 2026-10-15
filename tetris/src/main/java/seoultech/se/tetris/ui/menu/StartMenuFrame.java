package seoultech.se.tetris.ui.menu;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.ui.ScreenNavigator;
import seoultech.se.tetris.ui.settings.SettingsPanel;

/** 시작 메뉴를 담는 창. 단독 실행(main) 시에는 선택 결과를 콘솔에 출력하는 임시 내비게이터를 사용한다. */
public class StartMenuFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private final StartMenuPanel panel;

    public StartMenuFrame(ScreenNavigator navigator) {
        this(navigator, ScreenSize.MEDIUM);
    }

    public StartMenuFrame(ScreenNavigator navigator, ScreenSize size) {
        super("SeoulTech SE Tetris");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panel = new StartMenuPanel(StartMenuModel.createDefault(navigator));
        panel.setScreenSize(size);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /** 설정 저장 후 메뉴를 다시 표시할 때 새 크기를 적용한다. */
    public void setScreenSize(ScreenSize size) {
        panel.setScreenSize(size);
    }

    /** 화면을 표시한 뒤 키보드 포커스를 메뉴에 준다. */
    public void showMenu() {
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        panel.requestFocusInWindow();
    }

    /** 같은 창에서 설정 화면으로 전환한다. */
    public void showSettings(SettingsPanel settings) {
        setContentPane(settings);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StartMenuFrame(new ScreenNavigator() {
            @Override public void navigateToMenu() { System.out.println("메뉴"); }
            @Override public void navigateToGame() { System.out.println("시작 선택"); }
            @Override public void navigateToSettings() { System.out.println("설정 선택"); }
            @Override public void navigateToScoreboard() { System.out.println("스코어보드 선택"); }
            @Override public void navigateToGameOver(int finalScore) { System.out.println("게임 오버: " + finalScore); }
            @Override public void exitProgram() { System.exit(0); }
        }).showMenu());
    }
}
