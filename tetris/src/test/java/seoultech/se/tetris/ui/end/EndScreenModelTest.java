package seoultech.se.tetris.ui.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.ui.ScreenNavigator;

class EndScreenModelTest {

    @Test
    void 최종_점수를_보관하고_메인_메뉴가_기본_선택이다() {
        EndScreenModel model = EndScreenModel.createDefault(1200, navigator());

        assertEquals(1200, model.getFinalScore());
        assertEquals(0, model.getSelectedIndex());
        assertEquals(List.of("메인 메뉴", "종료"),
                model.getItems().stream().map(item -> item.label()).toList());
    }

    @Test
    void 점수_0도_종료_화면을_만들_수_있다() {
        EndScreenModel model = EndScreenModel.createDefault(0, navigator());

        assertEquals(0, model.getFinalScore());
    }

    @Test
    void 음수_점수는_종료_화면을_만들_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new EndScreenModel(-1, List.of()));
    }

    @Test
    void 메인_메뉴를_선택하면_시작_메뉴로_돌아간다() {
        List<String> calls = new ArrayList<>();
        EndScreenModel model = EndScreenModel.createDefault(10, navigator(calls));

        model.select();

        assertEquals(List.of("menu"), calls);
    }

    @Test
    void 종료를_선택하면_프로그램을_종료한다() {
        List<String> calls = new ArrayList<>();
        EndScreenModel model = EndScreenModel.createDefault(10, navigator(calls));

        model.moveDown();
        model.select();

        assertEquals(List.of("exit"), calls);
    }

    @Test
    void 마지막_항목_아래에서는_첫_항목으로_순환한다() {
        EndScreenModel model = EndScreenModel.createDefault(10, navigator());

        model.moveDown();
        model.moveDown();

        assertEquals(0, model.getSelectedIndex());
    }

    @Test
    void 허용되지_않은_키는_안내를_표시하고_이동하면_사라진다() {
        EndScreenModel model = EndScreenModel.createDefault(10, navigator());

        assertFalse(model.isKeyHintVisible());
        model.rejectInput();
        assertTrue(model.isKeyHintVisible());
        model.moveUp();
        assertFalse(model.isKeyHintVisible());
    }

    private static ScreenNavigator navigator() {
        return navigator(new ArrayList<>());
    }

    private static ScreenNavigator navigator(List<String> calls) {
        return new ScreenNavigator() {
            @Override public void navigateToMenu() { calls.add("menu"); }
            @Override public void navigateToGame() { calls.add("game"); }
            @Override public void navigateToSettings() { calls.add("settings"); }
            @Override public void navigateToScoreboard() { calls.add("scoreboard"); }
            @Override public void navigateToGameOver(int finalScore) { calls.add("gameOver"); }
            @Override public void exitProgram() { calls.add("exit"); }
        };
    }
}
