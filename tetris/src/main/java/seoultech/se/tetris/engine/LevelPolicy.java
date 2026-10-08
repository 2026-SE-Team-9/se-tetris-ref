package seoultech.se.tetris.engine;

/**
 * 레벨과 낙하 속도를 결정한다.
 *
 * <p>요구사항은 "일정 수 이상 블럭이 생성되거나 줄이 삭제되면 속도가 증가한다"이다.
 * 두 조건을 각각 독립적으로 적용하면 동시에 걸릴 수 있어 일정하게 속도가 오르지 않고
 * 난이도 곡선이 들쭉날쭉해진다. 그래서 둘을 하나의 <b>레벨</b>로 묶어,
 * 둘 중 더 많이 진행된 쪽이 레벨을 올리는 방식을 사용한다.
 *
 * <pre>
 * 레벨 = max(삭제한 줄 수 / 10, 생성한 블럭 수 / 50) + INITIAL_LEVEL
 * </pre>
 *
 * <p>레벨은 <b>1부터</b> 시작한다. 시작 레벨은 상수로 저장한다.
 *
 * <p>상태를 갖지 않으므로 같은 입력에 항상 같은 결과를 돌려준다.
 */

public class LevelPolicy {

    /* 레벨 계산에 사용되는 상수들. 세부사항 변경 시 아래 변수들을 조정하여 변경 가능 */
    /** 시작 레벨 */
    public static final int INITIAL_LEVEL = 1;

    /** 레벨 1 상승에 필요한 줄 삭제 수 */
    public static final int LINES_PER_LEVEL = 10;

    /** 레벨 1 상승에 필요한 블럭 생성 수 */
    public static final int BLOCKS_PER_LEVEL = 50;

    /** 시작 레벨에서의 낙하 간격(밀리초). 요구사항상 1초에 한 칸이다. */
    public static final int INITIAL_INTERVAL_MS = 1000;

    /**
     * 낙하 간격의 하한(밀리초).
     *
     * <p>하한이 없으면 레벨이 올라갈수록 간격이 0에 수렴해 게임이 성립하지 않는다.
     */
    public static final int MIN_INTERVAL_MS = 100;

    /** 레벨이 1 오를 때마다 낙하 간격에 곱하는 비율. */
    private static final double DECAY_PER_LEVEL = 0.8;

    /**
     * 현재 레벨을 계산한다.
     *
     * @param linesCleared  지금까지 삭제한 줄 수
     * @param blocksSpawned 지금까지 생성한 블럭 수
     * @return {@link #INITIAL_LEVEL}부터 시작하는 레벨
     */
    public int level(int linesCleared, int blocksSpawned) {
        requireNotNegative(linesCleared, "삭제한 줄 수");
        requireNotNegative(blocksSpawned, "생성한 블럭 수");

        int byLines = linesCleared / LINES_PER_LEVEL;
        int byBlocks = blocksSpawned / BLOCKS_PER_LEVEL;
        return Math.max(byLines, byBlocks) + INITIAL_LEVEL;
    }

    /**
     * 해당 레벨에서의 낙하 간격(밀리초).
     *
     * <p>레벨이 오를수록 짧아지며 {@link #MIN_INTERVAL_MS} 아래로는 내려가지 않는다.
     */
    public int dropIntervalMs(int level) {
        requireValidLevel(level);

        double interval = INITIAL_INTERVAL_MS * Math.pow(DECAY_PER_LEVEL, level - INITIAL_LEVEL);
        return Math.max(MIN_INTERVAL_MS, (int) Math.round(interval));
    }

    /**
     * 초기 속도보다 빨라진 상태인지 여부.
     *
     * <p>요구사항의 "블럭이 떨어지는 속도가 초기 속도보다 빨라진 경우 추가 점수"를
     * 판정하는 데 사용한다.
     */
    public boolean isAccelerated(int level) {
        requireValidLevel(level);
        return level > INITIAL_LEVEL;
    }

    /** 값이 음수가 되지 않도록 예외를 발생 */
    private static void requireNotNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + "는 음수일 수 없습니다: " + value);
        }
    }

    /** 시작 레벨 기준으로 레벨 값이 유효한지 확인 */
    private static void requireValidLevel(int level) {
        if (level < INITIAL_LEVEL) {
            throw new IllegalArgumentException(
                    "레벨은 " + INITIAL_LEVEL + " 이상이어야 합니다: " + level);
        }
    }
}
