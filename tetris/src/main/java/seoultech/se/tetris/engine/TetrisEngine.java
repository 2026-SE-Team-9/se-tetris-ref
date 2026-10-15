package seoultech.se.tetris.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockFactory;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.GameState;

/**
 * 게임 진행을 담당한다.
 *
 * <p>{@link Board}, {@link BlockFactory}, {@link LevelPolicy}, {@link ScoreCalculator}를
 * 조립해 실제 게임을 굴린다. 계산은 각각의 component가 담당하고, 이 클래스는 순서를 정하고 상태를 보관한다.
 *
 * <p>하나의 객체가 {@link GameState}와 {@link GameController}를 모두 구현한다.
 * 같은 데이터를 보는 두 가지 창구이며, 화면 쪽에는 읽기용({@code GameState})으로,
 * 조작 쪽에는 명령용({@code GameController})으로 넘기면 각자에게 필요한 면만 보인다.
 *
 * <p><b>타이머를 갖지 않는다.</b> {@link #moveDown()}이 호출되면 반응할 뿐이며,
 * 주기적으로 부르는 것은 조작 쪽의 일이다. 덕분에 실제 시간을 기다리지 않고
 * {@code moveDown()}을 반복 호출하는 방식으로 게임 전체를 테스트할 수 있다.
 */
public class TetrisEngine implements GameState, GameController {

    /** 내부에서 미리 뽑아 두는 다음 블럭 수. 화면에 표시하는 숫자와는 무관하다. */
    public static final int NEXT_QUEUE_SIZE = 5;

    /** 블럭이 생성되는 열. 4칸 격자를 보드 가운데에 맞춘다. */
    public static final int SPAWN_COL = (Board.WIDTH - BlockType.SIZE) / 2;

    /* 각 component들의 객체 */
    private final Board board;
    private final BlockFactory blockFactory;
    private final LevelPolicy levelPolicy;
    private final ScoreCalculator scoreCalculator;

    /* 다음 블럭을 저장하는 큐 */
    private final Deque<BlockType> nextQueue = new ArrayDeque<>(NEXT_QUEUE_SIZE);
    /** 게임 이벤트를 받을 리스너 목록 */
    private final List<GameEventListener> listeners = new ArrayList<>();

    /* 현재 게임 상태 변수 */
    private Block current;
    private int score;
    private int linesCleared;
    private int blocksSpawned;
    private int level;
    private boolean lastClearWasBackToBackEligible;
    private Status status;

    /** 게임의 상태. 진행중, 일시정지, 종료 상태를 나타낸다 */
    private enum Status {
        PLAYING, PAUSED, OVER
    }

    /** 실제 게임용. 블럭의 등장 순서를 랜덤으로 설정한다. */
    public TetrisEngine() {
        this(new BlockFactory(new Random()));
    }

    /**
     * 블럭 생성기를 지정해 만든다.
     *
     * <p>시드를 고정한 {@link Random}을 넘기면 같은 순서가 재현되므로 테스트에 쓸 수 있다.
     */
    public TetrisEngine(BlockFactory blockFactory) {
        this(blockFactory, new Board());
    }

    /**
     * 테스트용. 미리 채운 보드로 시작한다.
     *
     * <p>줄 삭제·백투백·레벨 상승처럼 무작위 블럭으로는 만들기 어려운 상황을 재현하기 위한 것으로,
     * 같은 패키지의 테스트에서만 사용한다.
     */
    TetrisEngine(BlockFactory blockFactory, Board board) {
        this.board = Objects.requireNonNull(board, "Board는 null일 수 없습니다.");
        this.blockFactory = Objects.requireNonNull(blockFactory, "BlockFactory는 null일 수 없습니다.");
        this.levelPolicy = new LevelPolicy();
        this.scoreCalculator = new ScoreCalculator();
        start();
    }

    // ------------------------------------------------------------------
    // 리스너
    // ------------------------------------------------------------------

    /**
     * 게임 이벤트를 받을 대상을 등록한다.
     *
     * <p>여러 개를 등록할 수 있다. 화면 갱신은 시각화 쪽이,
     * 낙하 간격 변경({@link GameEventListener#onSpeedIncreased(int)})은 타이머를 가진
     * 조작 쪽이 받아야 하므로 한 번에 여러 개의 리스너가 필요하다.
     */
    public void addListener(GameEventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "리스너는 null일 수 없습니다."));
    }

    public void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    private void fireBoardUpdated() {
        for (GameEventListener listener : listeners) {
            listener.onBoardUpdated();
        }
    }

    private void fireLinesCleared(int count, int gainedScore) {
        for (GameEventListener listener : listeners) {
            listener.onLinesCleared(count, gainedScore);
        }
    }

    private void fireSpeedIncreased(int intervalMs) {
        for (GameEventListener listener : listeners) {
            listener.onSpeedIncreased(intervalMs);
        }
    }

    private void fireGameOver(int finalScore) {
        for (GameEventListener listener : listeners) {
            listener.onGameOver(finalScore);
        }
    }

    // ------------------------------------------------------------------
    // 진행
    // ------------------------------------------------------------------

    /**
     * 게임 상태를 초기화하고 첫 블럭을 생성한다.
     *
     * <p>보드는 비우지 않는다. 처음 만들 때는 보드가 이미 비어 있고(테스트에서는 미리 채운 보드),
     * 재시작할 때는 {@link #restartGame()}이 먼저 비운다.
     */
    private void start() {
        nextQueue.clear();
        score = 0;
        linesCleared = 0;
        blocksSpawned = 0;
        level = LevelPolicy.INITIAL_LEVEL;
        lastClearWasBackToBackEligible = false;
        status = Status.PLAYING;

        fillQueue();
        spawnNext();
    }

    private void fillQueue() {
        while (nextQueue.size() < NEXT_QUEUE_SIZE) {
            nextQueue.addLast(blockFactory.next());
        }
    }

    /** 큐에서 다음 블럭을 꺼내 생성한다. 놓을 자리가 없으면 게임 종료 */
    private void spawnNext() {
        BlockType type = nextQueue.removeFirst();
        fillQueue();

        Block spawned = Block.of(type, Board.SPAWN_ROW, SPAWN_COL);
        blocksSpawned++;

        if (!board.canPlace(spawned)) {
            current = spawned;
            status = Status.OVER;
            return;
        }

        current = spawned;
        updateLevel();
    }

    /** 레벨이 올랐으면 낙하 간격 변경을 알린다. */
    private void updateLevel() {
        int newLevel = levelPolicy.level(linesCleared, blocksSpawned);
        if (newLevel != level) {
            level = newLevel;
            fireSpeedIncreased(levelPolicy.dropIntervalMs(level));
        }
    }

    /** 현재 블럭을 보드에 고정하고 줄 삭제와 점수 처리를 한 뒤 다음 블럭을 생성한다.
     *
     * <p>백투백 조건은 직전 줄 삭제와 이번 줄 삭제가 모두 백투백 조건을 만족하는 것
     * <p>백투백은 조건을 연속으로 만족하는 경우 계속 이어질 수 있다.
     * 백투백 조건이 되는 줄 수 미만의 줄을 삭제하면 연속 백투백이 끊긴다.
     * 줄을 삭제하지 않는 동작은 백투백 조건에 영향이 없다.
     */
    private void lockCurrentBlock() {
        board.place(current);

        int cleared = board.clearFullLines();
        if (cleared > 0) {
            boolean backToBack = lastClearWasBackToBackEligible
                    && scoreCalculator.isBackToBackEligible(cleared);
            int gained = scoreCalculator.lineClearScore(cleared, level, backToBack);

            score += gained;
            linesCleared += cleared;
            lastClearWasBackToBackEligible = scoreCalculator.isBackToBackEligible(cleared);

            fireLinesCleared(cleared, gained);
        }

        spawnNext();
        fireBoardUpdated();
        if (status == Status.OVER) {
            fireGameOver(score);
        }
    }

    /** 조작을 받을 수 있는 상태인지 여부. */
    private boolean isPlayable() {
        return status == Status.PLAYING;
    }

    /** 현재 블럭을 지정한 위치로 옮긴다. 놓을 수 없으면 아무 일도 하지 않는다. */
    private boolean tryMoveTo(Block candidate) {
        if (!board.canPlace(candidate)) {
            return false;
        }
        current = candidate;
        fireBoardUpdated();
        return true;
    }

    // ------------------------------------------------------------------
    // GameController
    // ------------------------------------------------------------------

    @Override
    public void moveLeft() {
        if (isPlayable()) {
            tryMoveTo(current.moved(0, -1));
        }
    }

    @Override
    public void moveRight() {
        if (isPlayable()) {
            tryMoveTo(current.moved(0, 1));
        }
    }

    /**
     * 한 칸 아래로 내린다. 더 내려갈 수 없으면 현재 블럭을 고정한다.
     *
     * <p>자동 낙하와 수동 조작 모두 이 메서드를 쓴다. 요구사항상 점수는 둘을 구분하지 않는다.
     */
    @Override
    public void moveDown() {
        if (!isPlayable()) {
            return;
        }

        Block below = current.moved(1, 0);
        if (board.canPlace(below)) {
            current = below;
            score += scoreCalculator.dropScore(1, level);
            fireBoardUpdated();          // 위치와 점수를 모두 바꾼 뒤에 알린다
        } else {
            lockCurrentBlock();
        }
    }

    /** 더 내려갈 수 없을 때까지 내린 뒤 즉시 고정한다. */
    @Override
    public void dropToBottom() {
        if (!isPlayable()) {
            return;
        }

        int dropped = 0;
        while (board.canPlace(current.moved(1, 0))) {
            current = current.moved(1, 0);
            dropped++;
        }

        score += scoreCalculator.dropScore(dropped, level);
        lockCurrentBlock();
    }

    /**
     * 시계방향으로 90도 회전한다.
     *
     * <p>회전 결과가 벽이나 기존 블럭과 겹치면 좌우로 한 칸씩 밀어 다시 시도하고,
     * 모두 실패하면 회전을 취소한다.
     */
    @Override
    public void rotate() {
        if (!isPlayable()) {
            return;
        }

        Block rotated = current.rotated();
        for (int kick : WALL_KICKS) {
            if (tryMoveTo(rotated.moved(0, kick))) {
                return;
            }
        }
    }

    /**
     * 반시계방향으로 90도 회전한다.
     *
     * <p>회전 결과가 벽이나 기존 블럭과 겹치면 좌우로 한 칸씩 밀어 다시 시도하고,
     * 모두 실패하면 회전을 취소한다.
     */
    @Override
    public void rotateCounterclockwise() {
        if (!isPlayable()) {
            return;
        }

        Block rotated = current.rotatedCounterClockwise();
        for (int kick : WALL_KICKS) {
            if (tryMoveTo(rotated.moved(0, kick))) {
                return;
            }
        }
    }

    /** 회전이 막혔을 때 좌우로 밀어볼 거리. 0은 제자리 회전이다. */
    private static final int[] WALL_KICKS = {0, -1, 1};

    @Override
    public void togglePause() {
        if (status == Status.PLAYING) {
            pauseGame();
        } else if (status == Status.PAUSED) {
            resumeGame();
        }
    }

    @Override
    public void pauseGame() {
        if (status == Status.PLAYING) {
            status = Status.PAUSED;
            fireBoardUpdated();
        }
    }

    @Override
    public void resumeGame() {
        if (status == Status.PAUSED) {
            status = Status.PLAYING;
            fireBoardUpdated();
        }
    }

    @Override
    public void restartGame() {
        board.reset();
        start();
        fireSpeedIncreased(levelPolicy.dropIntervalMs(level));
        fireBoardUpdated();
    }

    /**
     * 게임을 중간에 중단한다.
     *
     * <p>게임 진행만 멈춘다. 화면 전환은 호출하는 쪽이
     * {@code ScreenNavigator}를 통해 처리한다.
     */
    @Override
    public void exitGame() {
        status = Status.OVER;
        fireBoardUpdated();
    }

    // ------------------------------------------------------------------
    // GameState
    // ------------------------------------------------------------------

    /**
     * 고정된 블럭들의 배치.
     *
     * <p>현재 떨어지고 있는 블럭은 포함되지 않는다.
     * 화면에 함께 그리려면 {@link #getCurrentBlock()}과 좌표를 사용한다.
     */
    @Override
    public int[][] getBoard() {
        return board.snapshot();
    }

    @Override
    public Block getCurrentBlock() {
        return current;
    }

    @Override
    public Block getNextBlock() {
        BlockType type = nextQueue.peekFirst();
        return type == null ? null : Block.of(type, 0, 0);
    }

    @Override
    public int getCurrentX() {
        return current.col();
    }

    @Override
    public int getCurrentY() {
        return current.row();
    }

    /** 지금 즉시 낙하시켰을 때 현재 블럭이 멈출 행. */
    @Override
    public int getGhostY() {
        Block ghost = current;
        while (board.canPlace(ghost.moved(1, 0))) {
            ghost = ghost.moved(1, 0);
        }
        return ghost.row();
    }

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public int getLinesCleared() {
        return linesCleared;
    }

    /** 현재 레벨. {@link LevelPolicy#INITIAL_LEVEL}부터 시작 */
    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public boolean isPaused() {
        return status == Status.PAUSED;
    }

    @Override
    public boolean isGameOver() {
        return status == Status.OVER;
    }

    /** 지금까지 생성된 블럭 수. 레벨 계산과 테스트에 쓴다. */
    public int getBlocksSpawned() {
        return blocksSpawned;
    }
}
