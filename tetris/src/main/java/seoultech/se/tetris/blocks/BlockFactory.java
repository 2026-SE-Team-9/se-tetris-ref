package seoultech.se.tetris.blocks;

import java.util.Objects;
import java.util.Random;

/**
 * 다음에 등장할 블럭 종류를 무작위로 결정한다.
 *
 * <p>요구사항에 따라 7종이 <b>동일한 확률</b>로 등장해야 한다.
 *
 * <p>{@link Random}을 생성자로 주입받는다. 내부에서 직접 만들면 결과를 재현할 수 없어
 * 분포를 검증하는 테스트를 작성할 수 없기 때문이다. 테스트에서는 시드를 고정한
 * {@code new Random(42)}를 넘기고, 실제 게임에서는 {@code new Random()}을 넘긴다.
 */
public class BlockFactory {

    private static final BlockType[] TYPES = BlockType.values();

    private final Random random;

    /**
     * 위에서 나왔듯 테스트 검증을 위해 {@link Random}을 생성자로 따로 주입받는다.
     */
    public BlockFactory(Random random) {
        this.random = Objects.requireNonNull(random, "Random은 null일 수 없습니다.");
    }

    /**
     * 7종 중 하나를 동일한 확률로 반환한다.
     * 특히 요구사항에 명시되어 있는 부분이므로 확실한 검증을 요한다.
     */
    public BlockType next() {
        return TYPES[random.nextInt(TYPES.length)];
    }
}
