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

    static {
        STANDARD.put(BlockType.I, new Color(0x00, 0xF0, 0xF0));
        STANDARD.put(BlockType.O, new Color(0xF0, 0xF0, 0x00));
        STANDARD.put(BlockType.T, new Color(0xA0, 0x00, 0xF0));
        STANDARD.put(BlockType.S, new Color(0x00, 0xF0, 0x00));
        STANDARD.put(BlockType.Z, new Color(0xF0, 0x00, 0x00));
        STANDARD.put(BlockType.J, new Color(0x00, 0x00, 0xF0));
        STANDARD.put(BlockType.L, new Color(0xF0, 0xA0, 0x00));
    }

    private BlockPalette() {
    }

    public static Color colorOf(BlockType type) {
        return STANDARD.get(type);
    }
}
