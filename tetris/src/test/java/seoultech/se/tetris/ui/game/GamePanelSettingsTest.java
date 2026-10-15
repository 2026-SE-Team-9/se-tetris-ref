package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.config.AppSettings;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.TetrisEngine;

class GamePanelSettingsTest {
    @TempDir
    Path tempDir;

    /** 파일에 저장한 크기를 새 게임 화면에서 읽어 실제 너비에 반영한다. */
    @Test
    void savedScreenSizeChangesNewGamePanelWidth() {
        Path file = tempDir.resolve("settings.properties");
        var service = new PropertiesAppSettingsService(file);
        service.save(new AppSettings(ScreenSize.LARGE, false, Difficulty.NORMAL));
        var loaded = new PropertiesAppSettingsService(file);
        loaded.load();
        int savedWidth = new GamePanel(new TetrisEngine(), loaded.current()).getPreferredSize().width;
        int defaultWidth = new GamePanel(new TetrisEngine(), AppSettings.DEFAULT).getPreferredSize().width;
        assertTrue(savedWidth > defaultWidth);
    }
    /** 저장된 화면 크기 선택에 따라 게임 창의 실제 크기가 달라진다. */
    @Test
    void screenSizeChangesPreferredDimensions() {
        TetrisEngine engine = new TetrisEngine();
        int small = new GamePanel(engine, new AppSettings(ScreenSize.SMALL, false, Difficulty.NORMAL))
                .getPreferredSize().width;
        int medium = new GamePanel(engine, AppSettings.DEFAULT).getPreferredSize().width;
        int large = new GamePanel(engine, new AppSettings(ScreenSize.LARGE, false, Difficulty.NORMAL))
                .getPreferredSize().width;
        assertTrue(small < medium && medium < large);
    }

    /** 색맹 모드가 일반 팔레트와 다른 색을 제공한다. */
    @Test
    void colorBlindPaletteChangesBlockColor() {
        assertNotEquals(BlockPalette.colorOf(BlockType.Z), BlockPalette.colorOf(BlockType.Z, true));
    }
}
