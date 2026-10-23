package seoultech.se.tetris.ui.game;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.CellCode;
import seoultech.se.tetris.blocks.ItemType;

/**
 * 블럭 종류와 아이템별 색을 정한다. 모델은 색을 모르므로 색 결정은 여기서만 한다.
 * 기본 색은 세계 표준(Tetris Guideline)을 따르며, 색 외에 칸 안의 글자({@link BlockType#symbol()})로도 구분한다.
 */
public final class BlockPalette {

    private static final Map<BlockType, Color> STANDARD = new EnumMap<>(BlockType.class);
    private static final Map<ItemType, Color> ITEMS = new EnumMap<>(ItemType.class);

    static {
        STANDARD.put(BlockType.I, new Color(0x00, 0xF0, 0xF0));
        STANDARD.put(BlockType.O, new Color(0xF0, 0xF0, 0x00));
        STANDARD.put(BlockType.T, new Color(0xA0, 0x00, 0xF0));
        STANDARD.put(BlockType.S, new Color(0x00, 0xF0, 0x00));
        STANDARD.put(BlockType.Z, new Color(0xF0, 0x00, 0x00));
        STANDARD.put(BlockType.J, new Color(0x00, 0x00, 0xF0));
        STANDARD.put(BlockType.L, new Color(0xF0, 0xA0, 0x00));

        // 아이템은 칸 안의 글자로 구분하고, 색은 기본 블럭 7종과 겹치지 않게 따로 정한다.
        // 포함형(L, D)은 붙은 칸만 이 색이므로 같은 블럭의 나머지 칸과 구분된다.
        ITEMS.put(ItemType.LINE_CLEAR, new Color(0xFF, 0x69, 0xB4));
        ITEMS.put(ItemType.WEIGHT, new Color(0xC0, 0xC0, 0xC0));
        ITEMS.put(ItemType.DOUBLE_OR_NOTHING, new Color(0xDD, 0xA0, 0xDD));
        ITEMS.put(ItemType.TALL_BOY, new Color(0xA0, 0x72, 0x4A));
        ITEMS.put(ItemType.CRUSHER, new Color(0x78, 0x90, 0x9C));
    }

    private BlockPalette() {
    }

    public static Color colorOf(BlockType type) {
        return STANDARD.get(type);
    }

    /**
     * 칸 번호에 해당하는 색. 테트로미노 칸과 아이템 칸을 모두 다룬다.
     *
     * <p>번호의 종류는 {@link CellCode}로 구분하므로 번호 범위가 바뀌어도 이 메서드는 바뀌지 않는다.
     *
     * @throws IllegalArgumentException 빈 칸이거나 정의되지 않은 번호인 경우
     */
    public static Color colorOf(int code) {
        BlockType type = CellCode.blockTypeOf(code);
        if (type != null) {
            return colorOf(type);
        }
        ItemType item = CellCode.itemOf(code);
        if (item != null) {
            return ITEMS.get(item);
        }
        throw new IllegalArgumentException("색이 없는 칸 번호입니다: " + code);
    }
}
