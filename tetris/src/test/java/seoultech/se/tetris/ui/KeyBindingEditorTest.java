package seoultech.se.tetris.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.config.KeyConfigService.KeyBinding;
import seoultech.se.tetris.config.PropertiesKeyConfigService;

class KeyBindingEditorTest {
    @TempDir
    Path tempDir;

    @Test
    void escapeClearsMainAndBlocksSaveWithoutChangingStoredBinding() {
        var config = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        var editor = new KeyBindingEditor(config, GameAction.ROTATE);
        editor.captureMainKey(KeyEvent.VK_ESCAPE);
        assertEquals(OptionalInt.empty(), editor.getMainKeyCode());
        assertEquals(OptionalInt.of(KeyEvent.VK_X), editor.getSubKeyCode());
        assertFalse(editor.canSave());
        assertThrows(IllegalStateException.class, editor::save);
        assertEquals(new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_X), config.getKeyBinding(GameAction.ROTATE));
    }

    @Test
    void escapeClearsSubAndPersistsMainOnly() {
        Path file = tempDir.resolve("keys.properties");
        var config = new PropertiesKeyConfigService(file);
        var editor = new KeyBindingEditor(config, GameAction.ROTATE);
        editor.captureSubKey(KeyEvent.VK_ESCAPE);
        assertEquals(OptionalInt.empty(), editor.getSubKeyCode());
        assertTrue(editor.canSave());
        editor.save();
        var restored = new PropertiesKeyConfigService(file);
        restored.load();
        assertEquals(new KeyBinding(KeyEvent.VK_UP), restored.getKeyBinding(GameAction.ROTATE));
    }

    @Test
    void mainCanBeFilledAgainAndMainAndSubCanExchangePlaces() {
        var config = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        var editor = new KeyBindingEditor(config, GameAction.ROTATE);
        editor.captureMainKey(KeyEvent.VK_ESCAPE);
        editor.captureMainKey(KeyEvent.VK_X);
        editor.captureSubKey(KeyEvent.VK_UP);
        assertTrue(editor.canSave());
        editor.save();
        assertEquals(new KeyBinding(KeyEvent.VK_X, KeyEvent.VK_UP), config.getKeyBinding(GameAction.ROTATE));
    }

    @Test
    void saveFailureRestoresBothKeysAndKeepsEditsForRetry() throws IOException {
        for (KeyBinding replacement : List.of(new KeyBinding(KeyEvent.VK_R),
                new KeyBinding(KeyEvent.VK_X, KeyEvent.VK_UP))) {
            Path blockedParent = tempDir.resolve("blocked-" + replacement.mainKeyCode());
            Files.writeString(blockedParent, "A file prevents creating the settings directory");
            Path file = blockedParent.resolve("keys.properties");
            var config = new PropertiesKeyConfigService(file);
            KeyBinding previous = config.getKeyBinding(GameAction.ROTATE);
            KeyBinding other = config.getKeyBinding(GameAction.MOVE_LEFT);
            var editor = new KeyBindingEditor(config, GameAction.ROTATE);
            editor.captureMainKey(replacement.mainKeyCode());
            editor.captureSubKey(replacement.subKeyCode().orElse(KeyEvent.VK_ESCAPE));

            assertThrows(UncheckedIOException.class, editor::save);
            assertEquals(previous, config.getKeyBinding(GameAction.ROTATE));
            assertEquals(other, config.getKeyBinding(GameAction.MOVE_LEFT));
            assertEquals(OptionalInt.of(replacement.mainKeyCode()), editor.getMainKeyCode());
            assertEquals(replacement.subKeyCode(), editor.getSubKeyCode());
            assertTrue(editor.canSave());

            Files.delete(blockedParent);
            editor.save();
            var restored = new PropertiesKeyConfigService(file);
            restored.load();
            assertEquals(replacement, restored.getKeyBinding(GameAction.ROTATE));
        }
    }

    @Test
    void duplicateOrInvalidKeysCannotChangeStoredBinding() {
        var config = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        var editor = new KeyBindingEditor(config, GameAction.ROTATE);
        editor.captureSubKey(KeyEvent.VK_UP);
        assertFalse(editor.canSave());
        assertThrows(IllegalStateException.class, editor::save);
        editor.captureSubKey(KeyEvent.VK_Z);
        assertFalse(editor.canSave());
        assertThrows(IllegalStateException.class, editor::save);
        assertThrows(IllegalArgumentException.class, () -> editor.captureMainKey(KeyEvent.VK_UNDEFINED));
        assertEquals(new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_X), config.getKeyBinding(GameAction.ROTATE));
    }
}
