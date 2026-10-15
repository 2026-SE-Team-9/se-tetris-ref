package seoultech.se.tetris.ui.game;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

import seoultech.se.tetris.blocks.BlockType;

/**
 * 블럭 종류별 색을 정한다. 모델은 색을 모르므로 색 결정은 여기서만 한다.
 * 기본 색은 세계 표준(Tetris Guideline)을 따르며, 색 외에 칸 안의 글자({@link BlockType#symbol()})로도 구분한다.
 */
public final class BlockPalette {

    private static final Map<BlockType, Color> STANDARD = new EnumMap<>(BlockType.class);
    private static final Map<BlockType, Color> COLOR_BLIND = new EnumMap<>(BlockType.class);

    static {
        STANDARD.put(BlockType.I, new Color(0x00, 0xF0, 0xF0));
        STANDARD.put(BlockType.O, new Color(0xF0, 0xF0, 0x00));
        STANDARD.put(BlockType.T, new Color(0xA0, 0x00, 0xF0));
        STANDARD.put(BlockType.S, new Color(0x00, 0xF0, 0x00));
        STANDARD.put(BlockType.Z, new Color(0xF0, 0x00, 0x00));
        STANDARD.put(BlockType.J, new Color(0x00, 0x00, 0xF0));
        STANDARD.put(BlockType.L, new Color(0xF0, 0xA0, 0x00));

        COLOR_BLIND.put(BlockType.I, new Color(0x56, 0xB4, 0xE9));
        COLOR_BLIND.put(BlockType.O, new Color(0xF0, 0xE4, 0x42));
        COLOR_BLIND.put(BlockType.T, new Color(0xCC, 0x79, 0xA7));
        COLOR_BLIND.put(BlockType.S, new Color(0xFF, 0xFF, 0xFF));
        COLOR_BLIND.put(BlockType.Z, new Color(0xE6, 0x9F, 0x00));
        COLOR_BLIND.put(BlockType.J, new Color(0x00, 0x72, 0xB2));
        COLOR_BLIND.put(BlockType.L, new Color(0xF4, 0x81, 0x79));
    }

    private BlockPalette() {
    }

    public static Color colorOf(BlockType type) {
        return STANDARD.get(type);
    }

    /** 색맹 모드에서도 블럭 내부 문자를 함께 표시하여 색상만으로 구분하지 않게 한다. */
    public static Color colorOf(BlockType type, boolean colorBlind) {
        return colorBlind ? COLOR_BLIND.get(type) : colorOf(type);
    }
}
