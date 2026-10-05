package seoultech.se.tetris.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.config.PropertiesKeyConfigService;

class GameKeyInputTest {
    @TempDir
    Path tempDir;

    @Test
    void routesDefaultKeysAndIgnoresUnmappedKeys() {
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        GameKeyInput input = new GameKeyInput(config(), controller, state, () -> {});
        JPanel panel = new JPanel();
        input.attachTo(panel);

        press(input, panel, KeyEvent.VK_LEFT);
        press(input, panel, KeyEvent.VK_RIGHT);
        press(input, panel, KeyEvent.VK_DOWN);
        press(input, panel, KeyEvent.VK_SPACE);
        press(input, panel, KeyEvent.VK_UP);
        press(input, panel, KeyEvent.VK_X);
        press(input, panel, KeyEvent.VK_Z);
        press(input, panel, KeyEvent.VK_P);

        assertEquals(List.of("left", "right", "down", "drop", "rotate", "rotate", "counterclockwise"),
                controller.calls);
    }

    @Test
    void appliesChangedBindingsAfterRefresh() {
        PropertiesKeyConfigService config = config();
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        GameKeyInput input = new GameKeyInput(config, controller, state, () -> {});
        JPanel panel = new JPanel();
        input.attachTo(panel);

        config.setKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_A);
        press(input, panel, KeyEvent.VK_LEFT);
        input.refreshBindings();
        press(input, panel, KeyEvent.VK_LEFT);
        press(input, panel, KeyEvent.VK_A);

        assertEquals(List.of("left", "left"), controller.calls);
    }

    @Test
    void blocksMovementWhilePausedOrGameOver() {
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        GameKeyInput input = new GameKeyInput(config(), controller, state, () -> {});
        JPanel panel = new JPanel();
        input.attachTo(panel);

        state.paused = true;
        press(input, panel, KeyEvent.VK_LEFT);
        press(input, panel, KeyEvent.VK_ESCAPE);
        state.paused = false;
        state.gameOver = true;
        press(input, panel, KeyEvent.VK_SPACE);
        press(input, panel, KeyEvent.VK_ESCAPE);

        assertEquals(List.of(), controller.calls);
    }

    @Test
    void escapePausesAndRequestsMenuWithoutExiting() {
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        int[] menuRequests = {0};
        GameKeyInput input = new GameKeyInput(config(), controller, state, () -> menuRequests[0]++);
        JPanel panel = new JPanel();
        input.attachTo(panel);

        press(input, panel, KeyEvent.VK_ESCAPE);
        press(input, panel, KeyEvent.VK_ESCAPE);

        assertEquals(List.of("pause"), controller.calls);
        assertEquals(1, menuRequests[0]);
    }

    @Test
    void attachingAgainDoesNotDuplicateListenerAndDetachingStopsInput() {
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        GameKeyInput input = new GameKeyInput(config(), controller, state, () -> {});
        FocusTrackingPanel first = new FocusTrackingPanel();
        JPanel second = new JPanel();

        input.attachTo(first);
        input.attachTo(first);
        assertEquals(1, first.getKeyListeners().length);
        assertEquals(2, first.focusRequests);
        input.attachTo(second);
        assertEquals(0, first.getKeyListeners().length);
        assertEquals(1, second.getKeyListeners().length);
        press(input, first, KeyEvent.VK_LEFT);
        press(input, second, KeyEvent.VK_LEFT);
        input.detach();
        press(input, second, KeyEvent.VK_LEFT);
        assertEquals(0, second.getKeyListeners().length);
        assertEquals(List.of("left"), controller.calls);
    }

    private PropertiesKeyConfigService config() {
        return new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
    }

    private static void press(GameKeyInput input, JPanel panel, int keyCode) {
        input.keyPressed(new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED));
    }

    private static final class FocusTrackingPanel extends JPanel {
        int focusRequests;

        @Override
        public boolean requestFocusInWindow() {
            focusRequests++;
            return true;
        }
    }

    private static final class TestState implements GameState {
        boolean paused;
        boolean gameOver;

        @Override public int[][] getBoard() { return new int[0][0]; }
        @Override public Block getCurrentBlock() { return null; }
        @Override public int getCurrentX() { return 0; }
        @Override public int getCurrentY() { return 0; }
        @Override public Block getNextBlock() { return null; }
        @Override public int getScore() { return 0; }
        @Override public int getLevel() { return 0; }
        @Override public int getLinesCleared() { return 0; }
        @Override public boolean isPaused() { return paused; }
        @Override public boolean isGameOver() { return gameOver; }
        @Override public int getGhostY() { return 0; }
    }

    private static final class RecordingController implements GameController {
        final List<String> calls = new ArrayList<>();
        final TestState state;

        RecordingController(TestState state) { this.state = state; }

        @Override public void moveLeft() { calls.add("left"); }
        @Override public void moveRight() { calls.add("right"); }
        @Override public void moveDown() { calls.add("down"); }
        @Override public void dropToBottom() { calls.add("drop"); }
        @Override public void rotate() { calls.add("rotate"); }
        @Override public void rotateCounterclockwise() { calls.add("counterclockwise"); }
        @Override public void togglePause() { calls.add("toggle"); }
        @Override public void pauseGame() { calls.add("pause"); state.paused = true; }
        @Override public void resumeGame() { calls.add("resume"); state.paused = false; }
        @Override public void restartGame() { calls.add("restart"); }
        @Override public void exitGame() { calls.add("exit"); }
    }
}
