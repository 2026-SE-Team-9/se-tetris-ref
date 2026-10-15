package seoultech.se.tetris.ui;

import javax.swing.SwingUtilities;

import seoultech.se.tetris.ui.game.GameFrame;
import seoultech.se.tetris.ui.menu.StartMenuFrame;
import seoultech.se.tetris.ui.settings.SettingsPanel;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.score.CsvScoreRepository;

/** 앱 진입점. 현재 구현된 시작 메뉴와 게임 화면을 연결한다. */
public final class TetrisApplication implements ScreenNavigator {

    private StartMenuFrame menu;

    private TetrisApplication() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TetrisApplication().navigateToMenu());
    }

    @Override
    public void navigateToMenu() {
        menu = new StartMenuFrame(this);
        menu.showMenu();
    }

    @Override
    public void navigateToGame() {
        menu.dispose();
        menu = null;
        GameFrame.launch();
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
        System.out.println("게임 오버: " + finalScore);
    }

    @Override
    public void exitProgram() {
        System.exit(0);
    }
}
