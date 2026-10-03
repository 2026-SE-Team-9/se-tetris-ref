package seoultech.se.tetris.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Block")
class BlockTest {

    // ------------------------------------------------------------------
    // 생성
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("of()는 회전하지 않은 상태로 만든다")
        void ofCreatesUnrotated() {
            Block block = Block.of(BlockType.T, 5, 3);

            assertEquals(BlockType.T, block.type());
            assertEquals(0, block.rotation());
            assertEquals(5, block.row());
            assertEquals(3, block.col());
        }

        @Test
        @DisplayName("회전 상태가 범위를 넘으면 정규화한다")
        void normalizesRotation() {
            assertEquals(1, new Block(BlockType.T, 5, 0, 0).rotation());
            assertEquals(3, new Block(BlockType.T, -1, 0, 0).rotation());
        }

        @Test
        @DisplayName("보드 밖의 위치도 만들 수 있다")
        void allowsPositionOutsideBoard() {
            // 유효성 판단은 Board가 한다. Block 혼자서는 보드 크기를 모른다.
            Block block = Block.of(BlockType.I, -5, -50);

            assertEquals(-5, block.row());
            assertEquals(-50, block.col());
        }

        @Test
        @DisplayName("블럭 종류가 null이면 예외를 던진다")
        void throwsOnNullType() {
            assertThrows(NullPointerException.class, () -> Block.of(null, 0, 0));
        }
    }

    // ------------------------------------------------------------------
    // 불변성
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("불변성")
    class Immutability {

        @Test
        @DisplayName("회전해도 원본은 바뀌지 않는다")
        void rotatedDoesNotMutateOriginal() {
            Block original = Block.of(BlockType.T, 5, 3);

            Block rotated = original.rotated();

            assertEquals(0, original.rotation());
            assertEquals(1, rotated.rotation());
        }

        @Test
        @DisplayName("이동해도 원본은 바뀌지 않는다")
        void movedDoesNotMutateOriginal() {
            Block original = Block.of(BlockType.T, 5, 3);

            Block moved = original.moved(1, -1);

            assertEquals(5, original.row());
            assertEquals(3, original.col());
            assertEquals(6, moved.row());
            assertEquals(2, moved.col());
        }

        @Test
        @DisplayName("회전해도 위치는 유지된다")
        void rotationKeepsPosition() {
            Block rotated = Block.of(BlockType.T, 5, 3).rotated();

            assertEquals(5, rotated.row());
            assertEquals(3, rotated.col());
        }

        @Test
        @DisplayName("이동해도 종류와 회전 상태는 유지된다")
        void moveKeepsTypeAndRotation() {
            Block moved = new Block(BlockType.S, 2, 5, 3).moved(1, 1);

            assertEquals(BlockType.S, moved.type());
            assertEquals(2, moved.rotation());
        }
    }

    // ------------------------------------------------------------------
    // 회전
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("회전")
    class Rotation {

        @Test
        @DisplayName("네 번 회전하면 원래 상태로 돌아온다")
        void fourRotationsReturnToStart() {
            Block original = Block.of(BlockType.J, 5, 3);

            Block result = original.rotated().rotated().rotated().rotated();

            assertEquals(original, result);
        }

        @Test
        @DisplayName("반시계 회전은 시계 회전의 반대다")
        void counterClockwiseIsOpposite() {
            Block original = Block.of(BlockType.L, 5, 3);

            assertEquals(original, original.rotated().rotatedCounterClockwise());
            assertEquals(3, original.rotatedCounterClockwise().rotation());
        }

        @Test
        @DisplayName("회전하면 모양이 바뀐다")
        void rotationChangesShape() {
            Block upright = Block.of(BlockType.I, 5, 3);
            Block sideways = upright.rotated();

            // I 상태0은 가로, 상태1은 세로다
            assertTrue(upright.isFilled(1, 0));
            assertFalse(sideways.isFilled(1, 0));
        }
    }

    // ------------------------------------------------------------------
    // 좌표
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("좌표 변환")
    class Coordinates {

        @Test
        @DisplayName("격자 좌표를 보드 좌표로 바꾼다")
        void convertsToBoardCoordinates() {
            Block block = Block.of(BlockType.T, 5, 3);

            assertEquals(5, block.boardRow(0));
            assertEquals(8, block.boardRow(3));
            assertEquals(3, block.boardCol(0));
            assertEquals(6, block.boardCol(3));
        }

        @Test
        @DisplayName("isFilled()는 격자 좌표를 쓴다")
        void isFilledUsesLocalCoordinates() {
            Block block = Block.of(BlockType.T, 10, 5);

            // 위치와 무관하게 T 상태0의 모양은 .T.. / TTT.
            assertTrue(block.isFilled(0, 1));
            assertTrue(block.isFilled(1, 0));
            assertFalse(block.isFilled(0, 0));
        }

        @Test
        @DisplayName("이동하면 보드 좌표가 따라 바뀐다")
        void boardCoordinatesFollowMovement() {
            Block moved = Block.of(BlockType.T, 5, 3).moved(2, 1);

            assertEquals(7, moved.boardRow(0));
            assertEquals(4, moved.boardCol(0));
        }
    }

    // ------------------------------------------------------------------
    // 동치성
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("equals() / hashCode()")
    class Equality {

        @Test
        @DisplayName("네 값이 모두 같으면 같은 블럭이다")
        void equalWhenAllFieldsMatch() {
            Block a = new Block(BlockType.Z, 2, 5, 3);
            Block b = new Block(BlockType.Z, 2, 5, 3);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("값이 하나라도 다르면 다른 블럭이다")
        void notEqualWhenAnyFieldDiffers() {
            Block base = new Block(BlockType.Z, 2, 5, 3);

            assertNotEquals(base, new Block(BlockType.S, 2, 5, 3));
            assertNotEquals(base, new Block(BlockType.Z, 1, 5, 3));
            assertNotEquals(base, new Block(BlockType.Z, 2, 6, 3));
            assertNotEquals(base, new Block(BlockType.Z, 2, 5, 4));
        }

        @Test
        @DisplayName("정규화된 회전 상태가 같으면 같은 블럭이다")
        void equalAfterRotationNormalization() {
            assertEquals(
                    new Block(BlockType.Z, 1, 5, 3),
                    new Block(BlockType.Z, 5, 5, 3));
        }
    }
}
