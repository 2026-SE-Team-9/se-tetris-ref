package seoultech.se.tetris.ui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartMenuModelTest {

    private final List<String> calls = new ArrayList<>();
    private StartMenuModel model;

    @BeforeEach
    void setUp() {
        model = new StartMenuModel(List.of(
                new MenuItem("시작", () -> calls.add("start")),
                new MenuItem("설정", () -> calls.add("settings")),
                new MenuItem("스코어보드", () -> calls.add("scoreboard")),
                new MenuItem("종료", () -> calls.add("exit"))));
    }

    @Test
    void 첫_항목이_기본으로_선택된다() {
        assertEquals(0, model.getSelectedIndex());
    }

    @Test
    void 아래로_이동하고_끝에서는_처음으로_순환한다() {
        model.moveDown();
        assertEquals(1, model.getSelectedIndex());
        model.moveDown();
        model.moveDown();
        model.moveDown();
        assertEquals(0, model.getSelectedIndex());
    }

    @Test
    void 위로_이동하고_처음에서는_끝으로_순환한다() {
        model.moveUp();
        assertEquals(3, model.getSelectedIndex());
    }

    @Test
    void 선택하면_해당_항목의_동작이_수행된다() {
        model.moveDown();
        model.moveDown();
        model.select();
        assertEquals(List.of("scoreboard"), calls);
    }

    @Test
    void 허용되지_않은_키는_안내를_표시하고_이동하면_사라진다() {
        assertFalse(model.isKeyHintVisible());
        model.rejectInput();
        assertTrue(model.isKeyHintVisible());
        model.moveDown();
        assertFalse(model.isKeyHintVisible());
    }

    @Test
    void 빈_메뉴는_만들_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new StartMenuModel(List.of()));
    }
}
