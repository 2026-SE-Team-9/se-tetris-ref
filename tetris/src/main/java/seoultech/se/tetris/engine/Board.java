package seoultech.se.tetris.engine;

import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;

/**
 * 블럭이 쌓이는 20줄 × 10칸의 보드.
 *
 * <p>고정된 블럭들만 보관한다. 현재 떨어지고 있는 블럭은 이 안에 없으며,
 * 바닥이나 다른 블럭에 닿아 더 내려갈 수 없을 때 {@link #place(Block)}로 들어온다.
 *
 * <p>각 칸에는 {@link BlockType#code()}가 저장되고, 빈 칸은 {@link BlockType#EMPTY_CODE}다.
 * <p>계층 규칙에 따라 색을 저장하지 않으므로 색맹 모드 전환은 이 클래스에 영향을 주지 않는다.
 * <p>(계층 규칙 2 — 모델은 색을 모른다)
 */

public class Board {
    /** 화면에 보이는 줄 수. 20줄 */
    public static final int VISIBLE_HEIGHT = 20;

    /**
     * 보드 위쪽의 숨은 줄 수.
     *
     * <p>블럭은 내부 보드의 0행에서 생성되며 보드 위로 넘어갈 수 없다.
     * <p>표준 테트리스처럼 숨은 생성 영역이 필요해지면 이 값만 늘리고
     * {@link #snapshot()}이 보이는 영역만 잘라 반환하도록 하면 된다.
     */
    public static final int BUFFER_HEIGHT = 0;

    /** 보드 전체 줄 수. 내부 보드의 격자의 크기 */
    public static final int HEIGHT = VISIBLE_HEIGHT + BUFFER_HEIGHT;

    /** 보드의 칸 수 */
    public static final int WIDTH = 10;

    /** 새 블럭이 생성되는 행. 숨은 영역이 있으면 그 위쪽에서 시작한다. */
    public static final int SPAWN_ROW = 0;

    /** [행][열] 순서. 값은 블럭 종류 번호이며 0은 빈 칸이다. */
    private final int[][] grid;

    public Board() {
        this.grid = new int[HEIGHT][WIDTH];
    }


    /**
     * * 테스트용 코드 *
     *
     * <p>지정한 상태로 보드를 만든다.
     * <p>전달받은 배열을 복사하므로 이후 바깥에서 수정해도 보드는 바뀌지 않는다.
     */
    public Board(int[][] initial) {
        if (initial.length != HEIGHT) {
            throw new IllegalArgumentException("보드는 " + HEIGHT + "줄이어야 합니다.");
        }
        this.grid = new int[HEIGHT][WIDTH];
        for (int r = 0; r < HEIGHT; r++) {
            if (initial[r].length != WIDTH) {
                throw new IllegalArgumentException("각 줄은 " + WIDTH + "칸이어야 합니다.");
            }
            System.arraycopy(initial[r], 0, grid[r], 0, WIDTH);
        }
    }

    /**
     * 블럭을 이 위치에 놓을 수 있는지 판단한다.
     *
     * <p>보드 범위를 벗어나거나 이미 고정된 블럭과 겹치면 놓을 수 없다.
     * <p>이동, 회전, 새 블럭 생성의 가능 판정을 모두 이 메소드로 통일한다.
     */
    public boolean canPlace(Block block) {
        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (!block.isFilled(r, c)) continue;    // 채워지지 않은 칸은 검사 제외

                int boardRow = block.boardRow(r);
                int boardCol = block.boardCol(c);

                if (boardRow < 0 || boardRow >= HEIGHT) return false; // 보드 위아래 범위
                if (boardCol < 0 || boardCol >= WIDTH) return false; // 보드 좌우 범위
                if (grid[boardRow][boardCol] != BlockType.EMPTY_CODE) return false; // 이미 고정된 블럭이 존재
            }
        }
        return true;
    }

    /**
     * 블럭을 보드에 고정한다.
     *
     * <p>호출 전에 {@link #canPlace(Block)}로 확인해야 한다.
     * 놓을 수 없는 위치에 호출하면 예외를 던진다.
     */
    public void place(Block block) {
        if (!canPlace(block)) {
            throw new IllegalArgumentException("놓을 수 없는 위치입니다: " + block);
        }

        int code = block.type().code();
        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (block.isFilled(r, c)) {
                    grid[block.boardRow(r)][block.boardCol(c)] = code;
                }
            }
        }
    }

    /**
     * 가득 찬 줄을 모두 삭제하고 위의 줄들을 아래로 내린다.
     *
     * @return 삭제한 줄의 수
     */
    public int clearFullLines() {
        int cleared = 0;

        // 보드를 아래에서 위로 훑는다. 줄을 삭제한 경우 같은 행을 다시 확인해야 하므로 행 번호를 올리지 않는다.
        for (int row = HEIGHT - 1; row >= 0; ) {
            if (isFullLine(row)) {
                removeLine(row);
                cleared++;
            } else {
                row--;
            }
        }

        return cleared;
    }

    /**
     * 지정한 칸이 비어 있는지 여부. 범위를 벗어나면 비어 있지 않은 것으로 본다.
     */
    public boolean isEmpty(int row, int col) {
        if (row < 0 || row >= HEIGHT || col < 0 || col >= WIDTH) {
            return false;
        }
        return grid[row][col] == BlockType.EMPTY_CODE;
    }

    /** 지정한 칸의 블럭 종류 번호. 빈 칸이면 {@link BlockType#EMPTY_CODE}. */
    public int codeAt(int row, int col) {
        return grid[row][col];
    }

    /**
     * 화면에 보이는 영역의 복사본.
     *
     * <p>숨은 줄은 포함하지 않으므로 항상 {@link #VISIBLE_HEIGHT}줄을 반환한다.
     * <p>받은 쪽에서 수정해도 보드는 바뀌지 않는다.
     */
    public int[][] snapshot() {
        int[][] copy = new int[VISIBLE_HEIGHT][WIDTH];
        for (int r = 0; r < VISIBLE_HEIGHT; r++) {
            System.arraycopy(grid[r + BUFFER_HEIGHT], 0, copy[r], 0, WIDTH);
        }
        return copy;
    }

    /** 보드를 모두 비운다. */
    public void reset() {
        for (int r = 0; r < HEIGHT; r++) {
            java.util.Arrays.fill(grid[r], BlockType.EMPTY_CODE);
        }
    }

    private boolean isFullLine(int row) {
        for (int c = 0; c < WIDTH; c++) {
            if (grid[row][c] == BlockType.EMPTY_CODE) {
                return false;
            }
        }
        return true;
    }

    /** 한 줄을 지우고 그 위의 줄들을 한 칸씩 아래로 내린다. */
    private void removeLine(int row) {
        for (int r = row; r > 0; r--) {
            System.arraycopy(grid[r - 1], 0, grid[r], 0, WIDTH);
        }
        java.util.Arrays.fill(grid[0], BlockType.EMPTY_CODE);
    }
}
