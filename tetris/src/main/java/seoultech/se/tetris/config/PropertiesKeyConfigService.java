package seoultech.se.tetris.config;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** 키 설정을 properties 파일에 저장한다. 기본 경로는 사용자 홈의 .tetris/keybindings.properties이다. */
public class PropertiesKeyConfigService implements KeyConfigService {
    private static final int MAX_KEYS_PER_ACTION = 2;
    private final Path filePath;
    private final Map<GameAction, KeyBinding> keyBindings = new EnumMap<>(GameAction.class);

    public PropertiesKeyConfigService() {
        this(Path.of(System.getProperty("user.home"), ".tetris", "keybindings.properties"));
    }

    public PropertiesKeyConfigService(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        resetToDefault();
    }

    @Override
    public KeyBinding getKeyBinding(GameAction action) {
        return keyBindings.get(Objects.requireNonNull(action, "action"));
    }

    @Override
    public void setKeyBinding(GameAction action, KeyBinding binding) {
        Objects.requireNonNull(action, "action");
        KeyBinding replacement = Objects.requireNonNull(binding, "binding");
        List<Integer> replacementCodes = replacement.keyCodes();
        validateKeys(replacementCodes, new HashSet<>());
        for (GameAction other : GameAction.values()) {
            if (other != action && keyBindings.get(other).keyCodes().stream()
                    .anyMatch(replacementCodes::contains)) {
                throw new IllegalArgumentException("다른 액션에서 이미 사용 중인 키 코드");
            }
        }
        keyBindings.put(action, replacement);
    }

    private static void validateKeys(List<Integer> codes, Set<Integer> used) {
        if (codes.isEmpty()) {
            throw new IllegalArgumentException("키를 하나 이상 지정해야 합니다");
        }
        if (codes.size() > MAX_KEYS_PER_ACTION) {
            throw new IllegalArgumentException("액션당 키는 최대 두 개까지 지정할 수 있습니다");
        }
        for (int code : codes) {
            if (code <= KeyEvent.VK_UNDEFINED || !used.add(code)) {
                throw new IllegalArgumentException("유효하지 않거나 중복된 키 코드: " + code);
            }
        }
    }

    @Override
    public void resetToDefault() {
        keyBindings.clear();
        keyBindings.put(GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_LEFT));
        keyBindings.put(GameAction.MOVE_RIGHT, new KeyBinding(KeyEvent.VK_RIGHT));
        keyBindings.put(GameAction.SOFT_DROP, new KeyBinding(KeyEvent.VK_DOWN));
        keyBindings.put(GameAction.HARD_DROP, new KeyBinding(KeyEvent.VK_SPACE));
        keyBindings.put(GameAction.ROTATE, new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_X));
        keyBindings.put(GameAction.ROTATE_COUNTERCLOCKWISE, new KeyBinding(KeyEvent.VK_Z));
        keyBindings.put(GameAction.PAUSE_MENU, new KeyBinding(KeyEvent.VK_ESCAPE));
    }

    @Override
    public void save() {
        Properties properties = new Properties();
        for (GameAction action : GameAction.values()) {
            properties.setProperty(action.name(), getKeyCodes(action).stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
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
            Map<GameAction, KeyBinding> loaded = new EnumMap<>(GameAction.class);
            Set<Integer> used = new HashSet<>();
            for (GameAction action : GameAction.values()) {
                String value = properties.getProperty(action.name());
                if (value == null) {
                    throw new IllegalArgumentException("누락된 키 설정: " + action);
                }
                List<Integer> codes = new ArrayList<>();
                for (String token : value.split(",", -1)) {
                    codes.add(Integer.parseInt(token.trim()));
                }
                validateKeys(codes, used);
                loaded.put(action, codes.size() == 1
                        ? new KeyBinding(codes.getFirst())
                        : new KeyBinding(codes.getFirst(), codes.get(1)));
            }
            keyBindings.clear();
            keyBindings.putAll(loaded);
        } catch (IllegalArgumentException e) {
            // 구형 PAUSE/EXIT 설정 등 필수 액션이 없거나 손상된 파일은 새 기본값으로 복구한다.
            resetToDefault();
        } catch (IOException e) {
            throw new UncheckedIOException("키 설정을 불러올 수 없습니다: " + filePath, e);
        }
    }
}
