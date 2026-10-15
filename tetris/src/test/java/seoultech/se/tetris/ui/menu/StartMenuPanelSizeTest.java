package seoultech.se.tetris.ui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.config.AppSettings.ScreenSize;

class StartMenuPanelSizeTest {
    /** 시작 메뉴의 표시 크기가 게임 화면 설정의 세 단계에 맞춰 바뀐다. */
    @Test
    void followsConfiguredScreenSize() {
        var panel = new StartMenuPanel(new StartMenuModel(List.of(new MenuItem("시작", () -> {}))));
        panel.setScreenSize(ScreenSize.SMALL);
        int small = panel.getPreferredSize().width;
        panel.setScreenSize(ScreenSize.MEDIUM);
        int medium = panel.getPreferredSize().width;
        panel.setScreenSize(ScreenSize.LARGE);
        int large = panel.getPreferredSize().width;

        assertTrue(small < medium && medium < large);
        assertEquals(480, medium);
        assertEquals(large, panel.getPreferredSize().height);
    }
}
