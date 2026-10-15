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

    /** 보드 한 칸의 크기를 기준으로 한 세 가지 화면 크기. */
    public enum ScreenSize {
        SMALL("작게", 24), MEDIUM("보통", 28), LARGE("크게", 36);

        private final String label;
        private final int cellPixels;

        ScreenSize(String label, int cellPixels) {
            this.label = label;
            this.cellPixels = cellPixels;
        }

        public int cellPixels() {
            return cellPixels;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
