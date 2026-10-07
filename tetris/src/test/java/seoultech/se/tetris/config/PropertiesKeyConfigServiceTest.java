package seoultech.se.tetris.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.config.KeyConfigService.KeyBinding;

class PropertiesKeyConfigServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void providesDefaultsAndResetsChangedKey() {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));

        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
        assertEquals(KeyEvent.VK_RIGHT, service.getKeyCode(GameAction.MOVE_RIGHT));
        assertEquals(KeyEvent.VK_DOWN, service.getKeyCode(GameAction.SOFT_DROP));
        assertEquals(KeyEvent.VK_SPACE, service.getKeyCode(GameAction.HARD_DROP));
        assertEquals(KeyEvent.VK_UP, service.getKeyCode(GameAction.ROTATE));
        assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_X), service.getKeyCodes(GameAction.ROTATE));
        assertEquals(new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_X), service.getKeyBinding(GameAction.ROTATE));
        assertEquals(OptionalInt.empty(), service.getKeyBinding(GameAction.MOVE_LEFT).subKeyCode());
        assertEquals(KeyEvent.VK_ESCAPE, service.getKeyCode(GameAction.PAUSE_MENU));
        assertEquals(KeyEvent.VK_Z, service.getKeyCode(GameAction.ROTATE_COUNTERCLOCKWISE));

        service.setMainKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_A);
        assertEquals(KeyEvent.VK_A, service.getKeyCode(GameAction.MOVE_LEFT));
        service.resetToDefault();
        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
        assertThrows(IllegalArgumentException.class,
                () -> service.setMainKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_UNDEFINED));
        assertThrows(IllegalArgumentException.class,
                () -> service.setMainKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_RIGHT));
    }

    @Test
    void savesAndLoadsKeysFromFile() {
        Path file = tempDir.resolve("settings").resolve("keys.properties");
        PropertiesKeyConfigService original = new PropertiesKeyConfigService(file);
        original.setMainKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_A);
        original.setMainKeyCode(GameAction.PAUSE_MENU, KeyEvent.VK_ENTER);
        original.setSubKeyCode(GameAction.ROTATE, OptionalInt.of(KeyEvent.VK_R));
        original.save();

        PropertiesKeyConfigService restored = new PropertiesKeyConfigService(file);
        restored.load();

        assertEquals(KeyEvent.VK_A, restored.getKeyCode(GameAction.MOVE_LEFT));
        assertEquals(KeyEvent.VK_ENTER, restored.getKeyCode(GameAction.PAUSE_MENU));
        assertEquals(KeyEvent.VK_RIGHT, restored.getKeyCode(GameAction.MOVE_RIGHT));
        assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_R), restored.getKeyCodes(GameAction.ROTATE));
        assertEquals(new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_R), restored.getKeyBinding(GameAction.ROTATE));
        assertEquals(KeyEvent.VK_Z, restored.getKeyCode(GameAction.ROTATE_COUNTERCLOCKWISE));
    }

    @Test
    void missingFileRestoresDefaults() {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("missing.properties"));
        service.setMainKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_A);

        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
    }

    @Test
    void invalidFileRestoresAllDefaults() throws IOException {
        Path file = tempDir.resolve("invalid.properties");
        Files.writeString(file, "MOVE_LEFT=65\nMOVE_RIGHT=not-a-key\n");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);
        service.setMainKeyCode(GameAction.PAUSE_MENU, KeyEvent.VK_ENTER);

        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
        assertEquals(KeyEvent.VK_ESCAPE, service.getKeyCode(GameAction.PAUSE_MENU));
    }

    @Test
    void incompleteFileRestoresDefaults() throws IOException {
        Path file = tempDir.resolve("incomplete.properties");
        Files.writeString(file, "MOVE_LEFT=65\n");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);

        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
    }

    @Test
    void duplicateKeyInFileRestoresDefaults() throws IOException {
        Path file = tempDir.resolve("duplicate.properties");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);
        service.save();
        rewriteProperty(file, "MOVE_LEFT", Integer.toString(KeyEvent.VK_X));

        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
        assertEquals(KeyEvent.VK_RIGHT, service.getKeyCode(GameAction.MOVE_RIGHT));
    }

    @Test
    void rejectsInvalidBindingsWithoutChangingExistingKeys() {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        List<Integer> original = service.getKeyCodes(GameAction.ROTATE);

        assertThrows(IllegalArgumentException.class,
                () -> service.setMainKeyCode(GameAction.ROTATE, KeyEvent.VK_UNDEFINED));
        assertThrows(IllegalArgumentException.class,
                () -> service.setMainKeyCode(GameAction.ROTATE, KeyEvent.VK_X));
        assertThrows(IllegalArgumentException.class,
                () -> service.setSubKeyCode(GameAction.ROTATE, OptionalInt.of(KeyEvent.VK_UP)));
        assertThrows(IllegalArgumentException.class,
                () -> service.setSubKeyCode(GameAction.ROTATE, OptionalInt.of(KeyEvent.VK_UNDEFINED)));
        assertThrows(IllegalArgumentException.class,
                () -> service.setSubKeyCode(GameAction.ROTATE, OptionalInt.of(KeyEvent.VK_Z)));
        assertThrows(IllegalArgumentException.class,
                () -> service.setMainKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_X));
        assertEquals(original, service.getKeyCodes(GameAction.ROTATE));
        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
    }

    @Test
    void bindingsCannotBeModifiedOutsideServiceAndSingleKeyReplacesAliases() {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        service.setSubKeyCode(GameAction.ROTATE, OptionalInt.of(KeyEvent.VK_R));
        assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_R), service.getKeyCodes(GameAction.ROTATE));
        assertEquals(KeyEvent.VK_UP, service.getKeyCode(GameAction.ROTATE));
        assertThrows(UnsupportedOperationException.class,
                () -> service.getKeyCodes(GameAction.ROTATE).clear());

        service.setSubKeyCode(GameAction.ROTATE, OptionalInt.empty());
        service.setMainKeyCode(GameAction.ROTATE, KeyEvent.VK_X);
        assertEquals(List.of(KeyEvent.VK_X), service.getKeyCodes(GameAction.ROTATE));
        service.setMainKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_UP);
        service.resetToDefault();
        assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_X), service.getKeyCodes(GameAction.ROTATE));
    }

    @Test
    void editsMainAndSubKeysIndependently() {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));

        service.setMainKeyCode(GameAction.ROTATE, KeyEvent.VK_R);
        assertEquals(new KeyBinding(KeyEvent.VK_R, KeyEvent.VK_X), service.getKeyBinding(GameAction.ROTATE));

        service.setSubKeyCode(GameAction.ROTATE, OptionalInt.empty());
        assertEquals(new KeyBinding(KeyEvent.VK_R), service.getKeyBinding(GameAction.ROTATE));
        assertEquals(List.of(KeyEvent.VK_R), service.getKeyCodes(GameAction.ROTATE));

        service.setSubKeyCode(GameAction.ROTATE, OptionalInt.of(KeyEvent.VK_UP));
        assertEquals(new KeyBinding(KeyEvent.VK_R, KeyEvent.VK_UP), service.getKeyBinding(GameAction.ROTATE));

        assertThrows(IllegalArgumentException.class,
                () -> service.setSubKeyCode(GameAction.ROTATE, OptionalInt.of(KeyEvent.VK_R)));
        assertEquals(new KeyBinding(KeyEvent.VK_R, KeyEvent.VK_UP), service.getKeyBinding(GameAction.ROTATE));
    }

    @Test
    void invalidMultipleKeysRestoreAllDefaults() throws IOException {
        Path file = tempDir.resolve("keys.properties");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);
        for (String value : List.of("38,38", "38,", "", "38,0", "38,not-a-key", "38,82,84")) {
            service.setMainKeyCode(GameAction.MOVE_LEFT, KeyEvent.VK_A);
            service.save();
            rewriteProperty(file, "ROTATE", value);
            service.load();
            assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
            assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_X), service.getKeyCodes(GameAction.ROTATE));
        }
    }

    @Test
    void oldPauseAndExitFormatRestoresNewDefaults() throws IOException {
        Path file = tempDir.resolve("old.properties");
        Files.writeString(file, "MOVE_LEFT=65\nMOVE_RIGHT=39\nSOFT_DROP=40\nHARD_DROP=32\n"
                + "ROTATE=38\nPAUSE=80\nEXIT=27\n");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);
        service.load();
        assertEquals(KeyEvent.VK_LEFT, service.getKeyCode(GameAction.MOVE_LEFT));
        assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_X), service.getKeyCodes(GameAction.ROTATE));
        assertEquals(KeyEvent.VK_Z, service.getKeyCode(GameAction.ROTATE_COUNTERCLOCKWISE));
        assertEquals(KeyEvent.VK_ESCAPE, service.getKeyCode(GameAction.PAUSE_MENU));
    }

    private static void rewriteProperty(Path file, String name, String value) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        properties.setProperty(name, value);
        try (Writer writer = Files.newBufferedWriter(file)) {
            properties.store(writer, "Test key bindings");
        }
    }
}
