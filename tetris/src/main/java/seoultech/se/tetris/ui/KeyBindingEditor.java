package seoultech.se.tetris.ui;

import java.awt.event.KeyEvent;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.OptionalInt;
import seoultech.se.tetris.config.KeyConfigService;
import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.config.KeyConfigService.KeyBinding;

/** 설정 화면에서 액션 하나의 키를 편집한다. 빈 메인 키는 편집 중에만 허용한다. */
public final class KeyBindingEditor {
    private final KeyConfigService config;
    private final GameAction action;
    private OptionalInt mainKeyCode;
    private OptionalInt subKeyCode;

    public KeyBindingEditor(KeyConfigService config, GameAction action) {
        this.config = Objects.requireNonNull(config, "config");
        this.action = Objects.requireNonNull(action, "action");
        KeyBinding binding = config.getKeyBinding(action);
        mainKeyCode = OptionalInt.of(binding.mainKeyCode());
        subKeyCode = binding.subKeyCode();
    }

    public OptionalInt getMainKeyCode() {
        return mainKeyCode;
    }

    public OptionalInt getSubKeyCode() {
        return subKeyCode;
    }

    public void captureMainKey(int keyCode) {
        mainKeyCode = capturedKey(keyCode);
    }

    public void captureSubKey(int keyCode) {
        subKeyCode = capturedKey(keyCode);
    }

    /** 화면의 저장 버튼 활성화 여부. 중복 키도 저장할 수 없다. */
    public boolean canSave() {
        if (mainKeyCode.isEmpty()
                || (subKeyCode.isPresent() && subKeyCode.getAsInt() == mainKeyCode.getAsInt())) {
            return false;
        }
        for (GameAction other : GameAction.values()) {
            if (other != action && config.getKeyCodes(other).stream().anyMatch(code ->
                    code == mainKeyCode.getAsInt()
                            || (subKeyCode.isPresent() && code == subKeyCode.getAsInt()))) {
                return false;
            }
        }
        return true;
    }

    public void save() {
        if (!canSave()) {
            throw new IllegalStateException("메인 키가 없거나 다른 키와 중복된 설정은 저장할 수 없습니다");
        }
        KeyBinding previous = config.getKeyBinding(action);
        applyKeys(mainKeyCode.getAsInt(), subKeyCode);
        try {
            config.save();
        } catch (UncheckedIOException e) {
            applyKeys(previous.mainKeyCode(), previous.subKeyCode());
            throw e;
        }
    }

    private void applyKeys(int main, OptionalInt sub) {
        // 적용과 복원 모두 자리 교환 시 충돌하지 않도록 먼저 서브를 비운다.
        config.setSubKeyCode(action, OptionalInt.empty());
        config.setMainKeyCode(action, main);
        config.setSubKeyCode(action, sub);
    }

    private static OptionalInt capturedKey(int keyCode) {
        if (keyCode <= KeyEvent.VK_UNDEFINED) {
            throw new IllegalArgumentException("유효하지 않은 키 코드: " + keyCode);
        }
        return keyCode == KeyEvent.VK_ESCAPE ? OptionalInt.empty() : OptionalInt.of(keyCode);
    }
}
