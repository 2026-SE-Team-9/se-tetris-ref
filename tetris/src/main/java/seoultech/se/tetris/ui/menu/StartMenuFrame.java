package seoultech.se.tetris.ui.menu;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import seoultech.se.tetris.ui.ScreenNavigator;

/** 시작 메뉴를 담는 창. 단독 실행(main) 시에는 선택 결과를 콘솔에 출력하는 임시 내비게이터를 사용한다. */
public class StartMenuFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public StartMenuFrame(ScreenNavigator navigator) {
        super("SeoulTech SE Tetris");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        StartMenuPanel panel = new StartMenuPanel(StartMenuModel.createDefault(navigator));
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        panel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StartMenuFrame(new ScreenNavigator() {
            @Override public void navigateToMenu() { System.out.println("메뉴"); }
            @Override public void navigateToGame() { System.out.println("시작 선택"); }
            @Override public void navigateToSettings() { System.out.println("설정 선택"); }
            @Override public void navigateToScoreboard() { System.out.println("스코어보드 선택"); }
            @Override public void navigateToGameOver(int finalScore) { System.out.println("게임 오버: " + finalScore); }
            @Override public void exitProgram() { System.exit(0); }
        }).setVisible(true));
    }
}
