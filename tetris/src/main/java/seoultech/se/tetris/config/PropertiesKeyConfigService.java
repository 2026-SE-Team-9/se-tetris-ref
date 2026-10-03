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
    private final Path filePath;
    private final Map<GameAction, List<Integer>> keyCodes = new EnumMap<>(GameAction.class);

    public PropertiesKeyConfigService() {
        this(Path.of(System.getProperty("user.home"), ".tetris", "keybindings.properties"));
    }

    public PropertiesKeyConfigService(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        resetToDefault();
    }

    @Override
    public List<Integer> getKeyCodes(GameAction action) {
        return keyCodes.get(Objects.requireNonNull(action, "action"));
    }

    @Override
    public void setKeyCodes(GameAction action, List<Integer> codes) {
        Objects.requireNonNull(action, "action");
        List<Integer> replacement = List.copyOf(Objects.requireNonNull(codes, "keyCodes"));
        validateKeys(replacement, new HashSet<>());
        for (GameAction other : GameAction.values()) {
            if (other != action && keyCodes.get(other).stream().anyMatch(replacement::contains)) {
                throw new IllegalArgumentException("다른 액션에서 이미 사용 중인 키 코드");
            }
        }
        keyCodes.put(action, replacement);
    }

    private static void validateKeys(List<Integer> codes, Set<Integer> used) {
        if (codes.isEmpty()) {
            throw new IllegalArgumentException("키를 하나 이상 지정해야 합니다");
        }
        for (int code : codes) {
            if (code <= KeyEvent.VK_UNDEFINED || !used.add(code)) {
                throw new IllegalArgumentException("유효하지 않거나 중복된 키 코드: " + code);
            }
        }
    }

    @Override
    public void resetToDefault() {
        keyCodes.clear();
        keyCodes.put(GameAction.MOVE_LEFT, List.of(KeyEvent.VK_LEFT));
        keyCodes.put(GameAction.MOVE_RIGHT, List.of(KeyEvent.VK_RIGHT));
        keyCodes.put(GameAction.SOFT_DROP, List.of(KeyEvent.VK_DOWN));
        keyCodes.put(GameAction.HARD_DROP, List.of(KeyEvent.VK_SPACE));
        keyCodes.put(GameAction.ROTATE, List.of(KeyEvent.VK_UP, KeyEvent.VK_X));
        keyCodes.put(GameAction.ROTATE_COUNTERCLOCKWISE, List.of(KeyEvent.VK_Z));
        keyCodes.put(GameAction.PAUSE_MENU, List.of(KeyEvent.VK_ESCAPE));
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
            Map<GameAction, List<Integer>> loaded = new EnumMap<>(GameAction.class);
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
                loaded.put(action, List.copyOf(codes));
            }
            keyCodes.clear();
            keyCodes.putAll(loaded);
        } catch (IllegalArgumentException e) {
            // 구형 PAUSE/EXIT 설정 등 필수 액션이 없거나 손상된 파일은 새 기본값으로 복구한다.
            resetToDefault();
        } catch (IOException e) {
            throw new UncheckedIOException("키 설정을 불러올 수 없습니다: " + filePath, e);
        }
    }
}
