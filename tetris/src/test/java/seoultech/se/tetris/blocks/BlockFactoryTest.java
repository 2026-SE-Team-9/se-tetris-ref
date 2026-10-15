package seoultech.se.tetris.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BlockFactory")
class BlockFactoryTest {

    @Test
    @DisplayName("Random이 null이면 예외를 던진다")
    void throwsOnNullRandom() {
        assertThrows(NullPointerException.class, () -> new BlockFactory(null));
    }

    @Test
    @DisplayName("항상 블럭 종류를 반환한다")
    void alwaysReturnsType() {
        BlockFactory factory = new BlockFactory(new Random(42));

        for (int i = 0; i < 100; i++) {
            assertNotNull(factory.next());
        }
    }

    @Test
    @DisplayName("7종이 모두 등장한다")
    void producesAllTypes() {
        BlockFactory factory = new BlockFactory(new Random(42));
        Set<BlockType> seen = EnumSet.noneOf(BlockType.class);

        for (int i = 0; i < 1000; i++) {
            seen.add(factory.next());
        }

        assertEquals(BlockType.values().length, seen.size(),
                "등장하지 않은 블럭이 있습니다: " + EnumSet.complementOf(EnumSet.copyOf(seen)));
    }

    @Test
    @DisplayName("7종이 동일한 확률로 등장한다")
    void producesUniformDistribution() {
        BlockFactory factory = new BlockFactory(new Random(42));
        Map<BlockType, Integer> counts = new EnumMap<>(BlockType.class);

        int trials = 70_000;
        for (int i = 0; i < trials; i++) {
            counts.merge(factory.next(), 1, Integer::sum);
        }

        int expected = trials / BlockType.values().length;   // 10,000
        int tolerance = (int) (expected * 0.05);             // ±5%

        for (BlockType type : BlockType.values()) {
            int count = counts.getOrDefault(type, 0);
            assertTrue(Math.abs(count - expected) <= tolerance,
                    type + " 블럭이 " + count + "번 등장했습니다. "
                            + "기대값 " + expected + " ± " + tolerance + " 범위를 벗어납니다.");
        }
    }

    @Test
    @DisplayName("같은 시드면 같은 순서로 나온다")
    void sameSeedProducesSameSequence() {
        BlockFactory first = new BlockFactory(new Random(2026));
        BlockFactory second = new BlockFactory(new Random(2026));

        for (int i = 0; i < 100; i++) {
            assertEquals(first.next(), second.next());
        }
    }
}
