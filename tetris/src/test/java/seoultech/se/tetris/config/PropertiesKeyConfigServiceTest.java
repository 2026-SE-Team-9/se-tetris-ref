package seoultech.se.tetris.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.config.KeyConfigService.GameAction;

class PropertiesKeyConfigServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void changesAndResetsEveryDefaultKey() {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));

        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
        assertEquals(KeyEvent.VK_RIGHT, service.getKeyCode(GameAction.MOVE_RIGHT));
        assertEquals(KeyEvent.VK_DOWN, service.getKeyCode(GameAction.SOFT_DROP));
        assertEquals(KeyEvent.VK_SPACE, service.getKeyCode(GameAction.HARD_DROP));
        assertEquals(KeyEvent.VK_UP, service.getKeyCode(GameAction.ROTATE));
        assertEquals(KeyEvent.VK_P, service.getKeyCode(GameAction.PAUSE));
        assertEquals(KeyEvent.VK_ESCAPE, service.getKeyCode(GameAction.EXIT));

        service.setKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_A);
        assertEquals(KeyEvent.VK_A, service.getKeyCode(GameAction.MOVE_LEFT));
        service.resetToDefault();
        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
        assertThrows(IllegalArgumentException.class,
                () -> service.setKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_UNDEFINED));
    }

    @Test
    void savesAndLoadsKeysFromFile() {
        Path file = tempDir.resolve("settings").resolve("keys.properties");
        PropertiesKeyConfigService original = new PropertiesKeyConfigService(file);
        original.setKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_A);
        original.setKeyCode(GameAction.PAUSE, KeyEvent.VK_ENTER);
        original.save();

        PropertiesKeyConfigService restored = new PropertiesKeyConfigService(file);
        restored.load();

        assertEquals(KeyEvent.VK_A, restored.getKeyCode(GameAction.MOVE_LEFT));
        assertEquals(KeyEvent.VK_ENTER, restored.getKeyCode(GameAction.PAUSE));
        assertEquals(KeyEvent.VK_RIGHT, restored.getKeyCode(GameAction.MOVE_RIGHT));
    }

    @Test
    void missingFileRestoresDefaults() {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("missing.properties"));
        service.setKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_A);

        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
    }

    @Test
    void invalidFileRestoresAllDefaults() throws IOException {
        Path file = tempDir.resolve("invalid.properties");
        Files.writeString(file, "MOVE_LEFT=65\nMOVE_RIGHT=not-a-key\n");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);
        service.setKeyCode(GameAction.PAUSE, KeyEvent.VK_ENTER);

        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
        assertEquals(KeyEvent.VK_P, service.getKeyCode(GameAction.PAUSE));
    }

    @Test
    void incompleteFileRestoresDefaults() throws IOException {
        Path file = tempDir.resolve("incomplete.properties");
        Files.writeString(file, "MOVE_LEFT=65\n");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);

        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
    }
}
