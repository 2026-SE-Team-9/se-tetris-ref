package seoultech.se.tetris.config;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** 키 설정을 properties 파일에 저장한다. 기본 경로는 사용자 홈의 .tetris/keybindings.properties이다. */
public class PropertiesKeyConfigService implements KeyConfigService {
    private final Path filePath;
    private final Map<GameAction, Integer> keyCodes = new EnumMap<>(GameAction.class);

    public PropertiesKeyConfigService() {
        this(Path.of(System.getProperty("user.home"), ".tetris", "keybindings.properties"));
    }

    public PropertiesKeyConfigService(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        resetToDefault();
    }

    @Override
    public int getKeyCode(GameAction action) {
        return keyCodes.get(Objects.requireNonNull(action, "action"));
    }

    @Override
    public void setKeyCode(GameAction action, int keyCode) {
        Objects.requireNonNull(action, "action");
        if (keyCode <= KeyEvent.VK_UNDEFINED) {
            throw new IllegalArgumentException("유효하지 않은 키 코드: " + keyCode);
        }
        for (GameAction other : GameAction.values()) {
            if (other != action && keyCodes.get(other) == keyCode) {
                throw new IllegalArgumentException("이미 사용 중인 키 코드: " + keyCode);
            }
        }
        keyCodes.put(action, keyCode);
    }

    @Override
    public void resetToDefault() {
        keyCodes.clear();
        keyCodes.put(GameAction.MOVE_LEFT, KeyEvent.VK_LEFT);
        keyCodes.put(GameAction.MOVE_RIGHT, KeyEvent.VK_RIGHT);
        keyCodes.put(GameAction.SOFT_DROP, KeyEvent.VK_DOWN);
        keyCodes.put(GameAction.HARD_DROP, KeyEvent.VK_SPACE);
        keyCodes.put(GameAction.ROTATE, KeyEvent.VK_UP);
        keyCodes.put(GameAction.PAUSE, KeyEvent.VK_P);
        keyCodes.put(GameAction.EXIT, KeyEvent.VK_ESCAPE);
    }

    @Override
    public void save() {
        Properties properties = new Properties();
        for (GameAction action : GameAction.values()) {
            properties.setProperty(action.name(), Integer.toString(getKeyCode(action)));
        }

        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(filePath)) {
                properties.store(writer, "Tetris key bindings");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("키 설정을 저장할 수 없습니다: " + filePath, e);
        }
    }

    @Override
    public void load() {
        if (Files.notExists(filePath)) {
            resetToDefault();
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(filePath)) {
            properties.load(reader);
            // 모든 액션의 값을 검증한 뒤 적용해 일부 설정만 복원되는 일을 막는다.
            Map<GameAction, Integer> loaded = new EnumMap<>(GameAction.class);
            for (GameAction action : GameAction.values()) {
                String value = properties.getProperty(action.name());
                if (value == null) {
                    throw new IllegalArgumentException("누락된 키 설정: " + action);
                }
                int keyCode = Integer.parseInt(value);
                if (keyCode <= KeyEvent.VK_UNDEFINED || loaded.containsValue(keyCode)) {
                    throw new IllegalArgumentException("유효하지 않은 키 코드");
                }
                loaded.put(action, keyCode);
            }
            keyCodes.clear();
            keyCodes.putAll(loaded);
        } catch (IllegalArgumentException e) {
            // 설정이 일부만 있거나 손상되었으면 모든 키를 기본값으로 되돌린다.
            resetToDefault();
        } catch (IOException e) {
            throw new UncheckedIOException("키 설정을 불러올 수 없습니다: " + filePath, e);
        }
    }
}
