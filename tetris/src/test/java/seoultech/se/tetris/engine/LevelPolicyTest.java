package seoultech.se.tetris.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LevelPolicy")
class LevelPolicyTest {

    private LevelPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new LevelPolicy();
    }

    @Nested
    @DisplayName("level()")
    class Level {

        @Test
        @DisplayName("시작 상태는 레벨 1이다")
        void startsAtOne() {
            assertEquals(LevelPolicy.INITIAL_LEVEL, policy.level(0, 0));
        }

        @Test
        @DisplayName("줄을 10개 지우면 레벨이 1 오른다")
        void risesByLines() {
            assertEquals(1, policy.level(9, 0));
            assertEquals(2, policy.level(10, 0));
            assertEquals(3, policy.level(20, 0));
        }

        @Test
        @DisplayName("블럭을 50개 생성하면 레벨이 1 오른다")
        void risesByBlocks() {
            assertEquals(1, policy.level(0, 49));
            assertEquals(2, policy.level(0, 50));
            assertEquals(3, policy.level(0, 100));
        }

        @Test
        @DisplayName("두 조건 중 더 많이 진행된 쪽을 따른다")
        void takesHigherOfTwo() {
            // 줄 기준 +2, 블럭 기준 +1 → 레벨 3
            assertEquals(3, policy.level(20, 50));
            // 줄 기준 +1, 블럭 기준 +3 → 레벨 4
            assertEquals(4, policy.level(10, 150));
        }

        @Test
        @DisplayName("두 조건이 동시에 충족되어도 레벨이 중복으로 오르지 않는다")
        void doesNotStackConditions() {
            // 각각 +1씩이라고 해서 +2가 되면 안 된다
            assertEquals(2, policy.level(10, 50));
        }

        @Test
        @DisplayName("레벨은 1 미만이 되지 않는다")
        void neverBelowInitialLevel() {
            assertTrue(policy.level(0, 0) >= LevelPolicy.INITIAL_LEVEL);
        }

        @Test
        @DisplayName("음수를 넣으면 예외를 던진다")
        void throwsOnNegative() {
            assertThrows(IllegalArgumentException.class, () -> policy.level(-1, 0));
            assertThrows(IllegalArgumentException.class, () -> policy.level(0, -1));
        }
    }

    @Nested
    @DisplayName("dropIntervalMs()")
    class DropInterval {

        @Test
        @DisplayName("레벨 1에서는 1초다")
        void initialIntervalIsOneSecond() {
            assertEquals(LevelPolicy.INITIAL_INTERVAL_MS,
                    policy.dropIntervalMs(LevelPolicy.INITIAL_LEVEL));
        }

        @Test
        @DisplayName("레벨이 오르면 간격이 짧아진다")
        void intervalShrinksAsLevelRises() {
            int previous = policy.dropIntervalMs(LevelPolicy.INITIAL_LEVEL);

            for (int level = 2; level <= 6; level++) {
                int current = policy.dropIntervalMs(level);
                assertTrue(current < previous,
                        "레벨 " + level + "의 간격(" + current + ")이 "
                                + "이전 레벨(" + previous + ")보다 짧지 않습니다.");
                previous = current;
            }
        }

        @Test
        @DisplayName("하한 아래로는 내려가지 않는다")
        void neverGoesBelowMinimum() {
            // 하한이 없으면 0에 수렴해 게임이 성립하지 않는다
            for (int level = LevelPolicy.INITIAL_LEVEL; level <= 100; level++) {
                assertTrue(policy.dropIntervalMs(level) >= LevelPolicy.MIN_INTERVAL_MS,
                        "레벨 " + level + "에서 하한을 밑돌았습니다.");
            }
        }

        @Test
        @DisplayName("아주 높은 레벨에서는 하한값이 된다")
        void reachesMinimumAtHighLevel() {
            assertEquals(LevelPolicy.MIN_INTERVAL_MS, policy.dropIntervalMs(100));
        }

        @Test
        @DisplayName("레벨이 1 미만이면 예외를 던진다")
        void throwsOnInvalidLevel() {
            assertThrows(IllegalArgumentException.class, () -> policy.dropIntervalMs(0));
            assertThrows(IllegalArgumentException.class, () -> policy.dropIntervalMs(-1));
        }
    }

    @Nested
    @DisplayName("isAccelerated()")
    class Accelerated {

        @Test
        @DisplayName("레벨 1은 초기 속도다")
        void initialLevelIsNotAccelerated() {
            assertFalse(policy.isAccelerated(LevelPolicy.INITIAL_LEVEL));
        }

        @Test
        @DisplayName("레벨 2 이상은 초기보다 빠르다")
        void higherLevelIsAccelerated() {
            assertTrue(policy.isAccelerated(2));
            assertTrue(policy.isAccelerated(10));
        }

        @Test
        @DisplayName("레벨이 1 미만이면 예외를 던진다")
        void throwsOnInvalidLevel() {
            assertThrows(IllegalArgumentException.class, () -> policy.isAccelerated(0));
        }
    }
}
