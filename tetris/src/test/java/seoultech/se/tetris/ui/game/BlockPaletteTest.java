package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.CellCode;
import seoultech.se.tetris.blocks.ItemType;

class BlockPaletteTest {

    @Test
    void 테트로미노_칸_번호의_색은_블럭_종류의_색과_같다() {
        for (BlockType type : BlockType.values()) {
            assertEquals(BlockPalette.colorOf(type), BlockPalette.colorOf(type.code()));
        }
    }

    @Test
    void 아이템_칸_번호마다_색이_있다() {
        for (ItemType item : ItemType.values()) {
            assertNotNull(BlockPalette.colorOf(item.code()), item.name());
        }
    }

    @Test
    void 아이템_색은_서로_다르고_기본_블럭_색과도_다르다() {
        Set<Color> colors = new HashSet<>();
        for (BlockType type : BlockType.values()) {
            colors.add(BlockPalette.colorOf(type));
        }
        int blocks = colors.size();
        for (ItemType item : ItemType.values()) {
            colors.add(BlockPalette.colorOf(item.code()));
        }

        assertEquals(blocks + ItemType.values().length, colors.size());
    }

    @Test
    void 색이_정해진_모든_번호는_표시_문자도_있다() {
        int max = BlockType.values().length + ItemType.values().length;
        for (int code = 1; code <= max; code++) {
            assertNotNull(BlockPalette.colorOf(code));
            assertTrue(CellCode.symbolOf(code) != 0);
        }
    }

    @Test
    void 빈_칸이나_정의되지_않은_번호는_색을_정할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> BlockPalette.colorOf(CellCode.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> BlockPalette.colorOf(-1));
        assertThrows(IllegalArgumentException.class,
                () -> BlockPalette.colorOf(BlockType.values().length + ItemType.values().length + 1));
    }
}
