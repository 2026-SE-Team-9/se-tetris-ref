package seoultech.se.tetris.config;

import java.util.Objects;
import seoultech.se.tetris.engine.Difficulty;

/** 게임 화면과 진행 규칙에 적용할 사용자 설정. */
public record AppSettings(ScreenSize screenSize, boolean colorBlind, Difficulty difficulty) {
    public static final AppSettings DEFAULT = new AppSettings(ScreenSize.MEDIUM, false, Difficulty.DEFAULT);

    public AppSettings {
        Objects.requireNonNull(screenSize, "screenSize");
        Objects.requireNonNull(difficulty, "difficulty");
    }

    /** 설정 화면에서 선택할 수 있는 세 가지 화면 크기. */
    public enum ScreenSize {
        SMALL("작게"), MEDIUM("보통"), LARGE("크게");

        private final String label;

        ScreenSize(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
