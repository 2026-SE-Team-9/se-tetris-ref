package seoultech.se.tetris.blocks;

import java.util.Objects;
import java.util.Random;

/**
 * 다음에 등장할 블럭 종류를 무작위로 결정한다.
 *
 * <p>기본 난이도에서는 7종이 동일한 확률로 등장하고, 다른 난이도에서는
 * I형 블럭에 지정한 가중치를 적용한다.
 *
 * <p>{@link Random}을 생성자로 주입받는다. 내부에서 직접 만들면 결과를 재현할 수 없어
 * 분포를 검증하는 테스트를 작성할 수 없기 때문이다. 테스트에서는 시드를 고정한
 * {@code new Random(42)}를 넘기고, 실제 게임에서는 {@code new Random()}을 넘긴다.
 */
public class BlockFactory {

    private static final BlockType[] TYPES = BlockType.values();

    private final Random random;
    private final int iWeight;

    /**
     * 위에서 나왔듯 테스트 검증을 위해 {@link Random}을 생성자로 따로 주입받는다.
     */
    public BlockFactory(Random random) {
        this(random, 10);
    }

    /** I형 블럭의 가중치는 8·10·12 중 하나이며 다른 블럭의 가중치는 10이다. */
    public BlockFactory(Random random, int iWeight) {
        this.random = Objects.requireNonNull(random, "Random은 null일 수 없습니다.");
        if (iWeight != 8 && iWeight != 10 && iWeight != 12) {
            throw new IllegalArgumentException("I형 블럭 가중치는 8, 10, 12 중 하나여야 합니다");
        }
        this.iWeight = iWeight;
    }

    /** 기본 난이도에서는 동일 확률로, 다른 난이도에서는 I형 가중치에 따라 선택한다. */
    public BlockType next() {
        if (iWeight == 10) {
            return TYPES[random.nextInt(TYPES.length)];
        }
        int choice = random.nextInt(iWeight + (TYPES.length - 1) * 10);
        if (choice < iWeight) {
            return BlockType.I;
        }
        choice -= iWeight;
        for (BlockType type : TYPES) {
            if (type != BlockType.I) {
                if (choice < 10) {
                    return type;
                }
                choice -= 10;
            }
        }
        throw new IllegalStateException("블럭 가중치 범위를 벗어났습니다");
    }
}
