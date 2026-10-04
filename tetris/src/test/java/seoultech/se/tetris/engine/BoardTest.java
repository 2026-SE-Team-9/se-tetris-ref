package seoultech.se.tetris.engine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;

@DisplayName("Board")
class BoardTest {

    // ------------------------------------------------------------------
    // 테스트 보조
    // ------------------------------------------------------------------

    /**
     * 보드 아래쪽부터 채워진 상태를 만든다.
     *
     * <p>'.'은 빈 칸, 그 외 문자는 채워진 칸이다. 지정하지 않은 위쪽 줄은 모두 비어 있다.
     * 각 줄은 {@link Board#WIDTH}칸이어야 한다.
     */
    private static Board boardWithBottomRows(String... bottomRows) {
        int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
        int offset = Board.HEIGHT - bottomRows.length;

        for (int r = 0; r < bottomRows.length; r++) {
            String row = bottomRows[r];
            if (row.length() != Board.WIDTH) {
                throw new IllegalArgumentException("각 줄은 " + Board.WIDTH + "칸이어야 합니다: " + row);
            }
            for (int c = 0; c < Board.WIDTH; c++) {
                grid[offset + r][c] = row.charAt(c) == '.' ? BlockType.EMPTY_CODE : BlockType.T.code();
            }
        }
        return new Board(grid);
    }

    /** 보드의 한 줄을 문자열로 읽는다. 비교 결과를 눈으로 확인하기 위한 것이다. */
    private static String rowOf(Board board, int row) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < Board.WIDTH; c++) {
            sb.append(board.isEmpty(row, c) ? '.' : 'X');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // canPlace
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("canPlace()")
    class CanPlace {

        @Test
        @DisplayName("빈 보드의 가운데에는 놓을 수 있다")
        void emptyBoardAcceptsBlock() {
            Board board = new Board();
            Block block = Block.of(BlockType.T, 5, 3);

            assertTrue(board.canPlace(block));
        }

        @Test
        @DisplayName("왼쪽 벽을 넘으면 놓을 수 없다")
        void rejectsBeyondLeftWall() {
            Board board = new Board();
            // T 상태0은 0열부터 차 있으므로 col = -1이면 벽을 넘는다
            Block block = Block.of(BlockType.T, 5, -1);

            assertFalse(board.canPlace(block));
        }

        @Test
        @DisplayName("오른쪽 벽을 넘으면 놓을 수 없다")
        void rejectsBeyondRightWall() {
            Board board = new Board();
            // T 상태0은 2열까지 차 있으므로 col = WIDTH - 2면 오른쪽을 넘는다
            Block block = Block.of(BlockType.T, 5, Board.WIDTH - 2);

            assertFalse(board.canPlace(block));
        }

        @Test
        @DisplayName("바닥 아래로 넘으면 놓을 수 없다")
        void rejectsBelowFloor() {
            Board board = new Board();
            Block block = Block.of(BlockType.T, Board.HEIGHT - 1, 3);

            assertFalse(board.canPlace(block));
        }

        @Test
        @DisplayName("보드 위로 넘으면 놓을 수 없다")
        void rejectsAboveCeiling() {
            Board board = new Board();
            // T 상태0은 0행에 차 있으므로 row = -1이면 보드 위로 넘는다
            Block block = Block.of(BlockType.T, -1, 3);

            assertFalse(board.canPlace(block));
        }

        @Test
        @DisplayName("이미 블럭이 있는 칸과 겹치면 놓을 수 없다")
        void rejectsOverlap() {
            Board board = boardWithBottomRows("XXXXXXXXXX");
            Block block = Block.of(BlockType.T, Board.HEIGHT - 2, 3);

            assertFalse(board.canPlace(block));
        }

        @Test
        @DisplayName("모양의 빈 칸은 보드 밖에 있어도 상관없다")
        void ignoresEmptyCellsOutsideBoard() {
            Board board = new Board();
            // I 상태1은 2열에만 차 있고 나머지 3열은 비어 있다.
            // col = -2면 격자의 0, 1열이 보드 밖으로 나가지만 그 칸들은 비어 있다.
            Block block = new Block(BlockType.I, 1, 5, -2);

            assertTrue(board.canPlace(block));
        }
    }

    // ------------------------------------------------------------------
    // place
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("place()")
    class Place {

        @Test
        @DisplayName("블럭이 차지하는 칸에 종류 번호가 기록된다")
        void writesBlockCode() {
            Board board = new Board();
            Block block = Block.of(BlockType.T, 5, 3);

            board.place(block);

            // T 상태0:  .T..  /  TTT.
            assertEquals(BlockType.T.code(), board.codeAt(5, 4));
            assertEquals(BlockType.T.code(), board.codeAt(6, 3));
            assertEquals(BlockType.T.code(), board.codeAt(6, 4));
            assertEquals(BlockType.T.code(), board.codeAt(6, 5));
        }

        @Test
        @DisplayName("블럭이 차지하지 않는 칸은 그대로 비어 있다")
        void leavesOtherCellsEmpty() {
            Board board = new Board();
            board.place(Block.of(BlockType.T, 5, 3));

            assertTrue(board.isEmpty(5, 3));
            assertTrue(board.isEmpty(5, 5));
            assertTrue(board.isEmpty(7, 4));
        }

        @Test
        @DisplayName("놓을 수 없는 위치면 예외를 던진다")
        void throwsWhenCannotPlace() {
            Board board = new Board();
            Block block = Block.of(BlockType.T, 5, -1);

            assertThrows(IllegalArgumentException.class, () -> board.place(block));
        }
    }

    // ------------------------------------------------------------------
    // clearFullLines
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("clearFullLines()")
    class ClearFullLines {

        @Test
        @DisplayName("가득 찬 줄이 없으면 0을 반환한다")
        void returnsZeroWhenNoFullLine() {
            Board board = boardWithBottomRows("XXXXXXXXX.");

            assertEquals(0, board.clearFullLines());
        }

        @Test
        @DisplayName("가득 찬 한 줄을 삭제한다")
        void clearsSingleLine() {
            Board board = boardWithBottomRows("XXXXXXXXXX");

            assertEquals(1, board.clearFullLines());
            assertEquals("..........", rowOf(board, Board.HEIGHT - 1));
        }

        @Test
        @DisplayName("삭제한 줄 위의 블럭이 아래로 내려온다")
        void shiftsBlocksAboveDown() {
            Board board = boardWithBottomRows(
                    "X.........",
                    "XXXXXXXXXX"
            );

            assertEquals(1, board.clearFullLines());
            assertEquals("X.........", rowOf(board, Board.HEIGHT - 1));
            assertEquals("..........", rowOf(board, Board.HEIGHT - 2));
        }

        @Test
        @DisplayName("연속된 두 줄을 한 번에 삭제한다")
        void clearsTwoAdjacentLines() {
            Board board = boardWithBottomRows(
                    "X.........",
                    "XXXXXXXXXX",
                    "XXXXXXXXXX"
            );

            assertEquals(2, board.clearFullLines());
            assertEquals("X.........", rowOf(board, Board.HEIGHT - 1));
            assertEquals("..........", rowOf(board, Board.HEIGHT - 2));
        }

        @Test
        @DisplayName("떨어져 있는 두 줄도 모두 삭제한다")
        void clearsTwoSeparatedLines() {
            Board board = boardWithBottomRows(
                    "XXXXXXXXXX",
                    "X.........",
                    "XXXXXXXXXX"
            );

            assertEquals(2, board.clearFullLines());
            assertEquals("X.........", rowOf(board, Board.HEIGHT - 1));
            assertEquals("..........", rowOf(board, Board.HEIGHT - 2));
        }

        @Test
        @DisplayName("네 줄을 동시에 삭제한다")
        void clearsFourLines() {
            Board board = boardWithBottomRows(
                    "XXXXXXXXXX",
                    "XXXXXXXXXX",
                    "XXXXXXXXXX",
                    "XXXXXXXXXX"
            );

            assertEquals(4, board.clearFullLines());
            assertEquals("..........", rowOf(board, Board.HEIGHT - 1));
        }
    }

    // ------------------------------------------------------------------
    // 그 외
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("snapshot()")
    class Snapshot {

        @Test
        @DisplayName("화면에 보이는 줄 수만큼 반환한다")
        void returnsVisibleHeight() {
            int[][] snapshot = new Board().snapshot();

            assertEquals(Board.VISIBLE_HEIGHT, snapshot.length);
            assertEquals(Board.WIDTH, snapshot[0].length);
        }

        @Test
        @DisplayName("반환값을 수정해도 보드는 바뀌지 않는다")
        void returnsDefensiveCopy() {
            Board board = new Board();
            int[][] snapshot = board.snapshot();

            snapshot[0][0] = BlockType.I.code();

            assertTrue(board.isEmpty(0, 0));
        }

        @Test
        @DisplayName("고정된 블럭이 그대로 담긴다")
        void containsPlacedBlocks() {
            Board board = new Board();
            board.place(Block.of(BlockType.T, 5, 3));

            int[][] snapshot = board.snapshot();

            assertEquals(BlockType.T.code(), snapshot[6][4]);
        }
    }

    @Nested
    @DisplayName("isEmpty()")
    class IsEmpty {

        @Test
        @DisplayName("빈 칸이면 true")
        void trueForEmptyCell() {
            assertTrue(new Board().isEmpty(0, 0));
        }

        @Test
        @DisplayName("보드 밖은 비어 있지 않은 것으로 본다")
        void falseOutsideBoard() {
            Board board = new Board();

            assertFalse(board.isEmpty(-1, 0));
            assertFalse(board.isEmpty(Board.HEIGHT, 0));
            assertFalse(board.isEmpty(0, -1));
            assertFalse(board.isEmpty(0, Board.WIDTH));
        }
    }

    @Nested
    @DisplayName("reset()")
    class Reset {

        @Test
        @DisplayName("모든 칸을 비운다")
        void clearsEverything() {
            Board board = boardWithBottomRows(
                    "XXXXXXXXXX",
                    "XXXXXXXXXX"
            );

            board.reset();

            assertArrayEquals(new int[Board.VISIBLE_HEIGHT][Board.WIDTH], board.snapshot());
        }
    }
}
