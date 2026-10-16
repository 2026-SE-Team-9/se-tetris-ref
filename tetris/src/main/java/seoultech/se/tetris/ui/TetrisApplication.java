package seoultech.se.tetris.ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import seoultech.se.tetris.ui.end.EndScreenFrame;
import seoultech.se.tetris.ui.game.GameFrame;
import seoultech.se.tetris.ui.menu.StartMenuFrame;
import seoultech.se.tetris.ui.settings.SettingsPanel;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.CsvScoreRepository;

/** 앱 진입점. 시작 메뉴, 게임 화면, 종료 화면을 한 번에 하나만 연다. */
public final class TetrisApplication implements ScreenNavigator {

    private StartMenuFrame menu;
    private GameFrame game;
    private EndScreenFrame endScreen;

    private TetrisApplication() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TetrisApplication().navigateToMenu());
    }

    @Override
    public void navigateToMenu() {
        closeGame();
        closeEndScreen();
        closeMenu();
        menu = new StartMenuFrame(this);
        menu.showMenu();
    }

    @Override
    public void navigateToGame() {
        closeMenu();
        closeEndScreen();
        closeGame();
        game = GameFrame.launch(this::navigateToGameOver);
    }

    @Override
    public void navigateToSettings() {
        PropertiesAppSettingsService settings = new PropertiesAppSettingsService();
        settings.load();
        PropertiesKeyConfigService keys = new PropertiesKeyConfigService();
        keys.load();
        menu.showSettings(new SettingsPanel(settings, keys, new CsvScoreRepository(),
                true, () -> {}, () -> menu.showMenu()));
    }

    @Override
    public void navigateToScoreboard() {
        System.out.println("스코어보드 선택");
    }

    @Override
    public void navigateToGameOver(int finalScore) {
        closeGame();
        closeEndScreen();
        PropertiesAppSettingsService settings = new PropertiesAppSettingsService();
        settings.load();
        // 모드 선택 기능이 생기기 전까지는 일반 모드로 기록한다. 저장소는 판마다 새로 만들어 파일에서 다시 읽는다.
        endScreen = new EndScreenFrame(this, finalScore, GameMode.NORMAL,
                settings.current().difficulty(), new CsvScoreRepository());
        endScreen.showScreen();
    }

    /** 열려 있는 창을 닫는다. 이미 닫혀 있으면 아무 것도 하지 않는다. */
    private static void close(JFrame frame) {
        if (frame != null) {
            frame.dispose();
        }
    }

    private void closeMenu() {
        close(menu);
        menu = null;
    }

    private void closeGame() {
        close(game);
        game = null;
    }

    private void closeEndScreen() {
        close(endScreen);
        endScreen = null;
    }

    @Override
    public void exitProgram() {
        System.exit(0);
    }
}
