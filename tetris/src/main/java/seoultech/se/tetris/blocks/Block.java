package seoultech.se.tetris.blocks;

import java.util.Objects;

/**
 * 보드 위에 놓인 블럭 하나의 현재 상태.
 *
 * <p>종류({@link BlockType}), 회전 상태, 보드상의 위치를 함께 가진다.
 *
 * <p><b>이 객체는 불변이다.</b> 이동이나 회전은 기존 객체를 바꾸지 않고
 * 새 객체를 만들어 반환한다. 그러므로 "움직여 보고 안 되면 되돌리기"가 필요 없고,
 * 아래처럼 "결과를 만들어 검증한 뒤 채택하기"로 처리할 수 있다.
 *
 * <pre>{@code
 * Block next = current.rotated();
 * if (board.canPlace(next)) {
 *     current = next;      // 성공
 * }                        // 실패 시 아무것도 하지 않음 — 되돌릴 것이 없다
 * }</pre>
 *
 * <p>회전 시 벽이나 기존 블럭에 걸리는 경우도 같은 방식으로 좌우로 밀어 재시도할 수 있다.
 *
 * <pre>{@code
 * for (int kick : WALL_KICKS) {
 *     Block next = current.rotated().moved(0, kick);
 *     if (board.canPlace(next)) { current = next; break; }
 * }
 * }</pre>
 */
public final class Block {

    private final BlockType type;
    private final int rotation;
    private final int row;
    private final int col;

    /**
     * @param type     블럭 종류
     * @param rotation 회전 상태. 범위를 벗어나면 0~3으로 정규화된다
     * @param row      모양 격자의 좌측 상단이 놓인 보드상의 행
     * @param col      모양 격자의 좌측 상단이 놓인 보드상의 열
     */
    public Block(BlockType type, int rotation, int row, int col) {
        this.type = Objects.requireNonNull(type, "블럭 종류는 null일 수 없습니다.");
        this.rotation = BlockType.normalize(rotation);
        this.row = row;
        this.col = col;
    }

    /** 지정한 위치에 회전하지 않은 상태로 생성한다. */
    public static Block of(BlockType type, int row, int col) {
        return new Block(type, 0, row, col);
    }

    /** 시계방향으로 90도 회전한 새 블럭을 반환 */
    public Block rotated() {
        return new Block(type, rotation + 1, row, col);
    }

    /** 반시계방향으로 90도 회전한 새 블럭을 반환 */
    public Block rotatedCounterClockwise() {
        return new Block(type, rotation - 1, row, col);
    }

    /**
     * 지정한 만큼 이동한 새 블럭 반환
     *
     * @param rowDelta 행 변화량. 아래로 한 칸은 {@code +1}
     * @param colDelta 열 변화량. 오른쪽으로 한 칸은 {@code +1}
     */
    public Block moved(int rowDelta, int colDelta) {
        return new Block(type, rotation, row + rowDelta, col + colDelta);
    }

    /**
     * 모양 격자의 (localRow, localCol) 칸이 채워져 있는지 여부를 반환
     *
     * <p>좌표는 블럭 자신의 격자 기준이며, 보드 좌표가 아니다.
     * 보드 좌표는 {@link #boardRow(int)}, {@link #boardCol(int)}로 얻는다.
     */
    public boolean isFilled(int localRow, int localCol) {
        return type.isFilled(rotation, localRow, localCol);
    }

    /** 모양 격자의 행 번호를 보드상의 행 번호로 변환한다. */
    public int boardRow(int localRow) {
        return row + localRow;
    }

    /** 모양 격자의 열 번호를 보드상의 열 번호로 변환한다. */
    public int boardCol(int localCol) {
        return col + localCol;
    }

    public BlockType type() {
        return type;
    }

    public int rotation() {
        return rotation;
    }

    /** 모양 격자의 좌측 상단이 놓인 보드상의 행. */
    public int row() {
        return row;
    }

    /** 모양 격자의 좌측 상단이 놓인 보드상의 열. */
    public int col() {
        return col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Block other)) {
            return false;
        }
        return rotation == other.rotation
                && row == other.row
                && col == other.col
                && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, rotation, row, col);
    }

    @Override
    public String toString() {
        return "Block[" + type + " rot=" + rotation + " row=" + row + " col=" + col + "]";
    }
}
