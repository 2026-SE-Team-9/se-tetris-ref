package seoultech.se.tetris.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.BlockFactory;
import seoultech.se.tetris.component.listener.GameEventListener;

@DisplayName("TetrisEngine")
class TetrisEngineTest {

    /** 시드를 고정해 블럭 순서를 재현 가능하게 만든다. */
    private static final int SEED = 42;

    private TetrisEngine engine;
    private RecordingListener listener;

    @BeforeEach
    void setUp() {
        engine = new TetrisEngine(new BlockFactory(new Random(SEED)));
        listener = new RecordingListener();
        engine.addListener(listener);
    }

    // ------------------------------------------------------------------
    // 테스트 보조
    // ------------------------------------------------------------------

    /** 어떤 이벤트가 몇 번 왔는지 기록하는 리스너. */
    private static class RecordingListener implements GameEventListener {
        int boardUpdated;
        int speedIncreased;
        int gameOver;
        int lastFinalScore;
        final List<int[]> linesCleared = new ArrayList<>();   // {줄 수, 얻은 점수}
        final List<Integer> intervals = new ArrayList<>();

        @Override
        public void onBoardUpdated() {
            boardUpdated++;
        }

        @Override
        public void onLinesCleared(int clearedCount, int gainedScore) {
            linesCleared.add(new int[] {clearedCount, gainedScore});
        }

        @Override
        public void onSpeedIncreased(int currentInterval) {
            speedIncreased++;
            intervals.add(currentInterval);
        }

        @Override
        public void onGameOver(int finalScore) {
            gameOver++;
            lastFinalScore = finalScore;
        }
    }

    /** 현재 블럭이 고정될 때까지 아래로 내린다. */
    private void dropCurrentBlock() {
        engine.dropToBottom();
    }

    /** 블럭을 지정한 수만큼 고정한다. */
    private void dropBlocks(int count) {
        for (int i = 0; i < count; i++) {
            engine.dropToBottom();
        }
    }

    // ------------------------------------------------------------------
    // 초기 상태
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("초기 상태")
    class InitialState {

        @Test
        @DisplayName("게임이 진행 중이다")
        void startsPlaying() {
            assertFalse(engine.isPaused());
            assertFalse(engine.isGameOver());
        }

        @Test
        @DisplayName("점수와 삭제 줄 수가 0이다")
        void startsWithZeroScore() {
            assertEquals(0, engine.getScore());
            assertEquals(0, engine.getLinesCleared());
        }

        @Test
        @DisplayName("레벨 1에서 시작한다")
        void startsAtInitialLevel() {
            assertEquals(LevelPolicy.INITIAL_LEVEL, engine.getLevel());
        }

        @Test
        @DisplayName("현재 블럭과 다음 블럭이 준비되어 있다")
        void hasCurrentAndNextBlock() {
            assertNotNull(engine.getCurrentBlock());
            assertNotNull(engine.getNextBlock());
        }

        @Test
        @DisplayName("보드가 비어 있다")
        void startsWithEmptyBoard() {
            for (int[] row : engine.getBoard()) {
                for (int cell : row) {
                    assertEquals(0, cell);
                }
            }
        }

        @Test
        @DisplayName("블럭이 보드 가운데에서 생성된다")
        void spawnsAtCenter() {
            assertEquals(TetrisEngine.SPAWN_COL, engine.getCurrentX());
            assertEquals(Board.SPAWN_ROW, engine.getCurrentY());
        }
    }

    // ------------------------------------------------------------------
    // 이동
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("이동")
    class Movement {

        @Test
        @DisplayName("좌우로 한 칸씩 움직인다")
        void movesHorizontally() {
            int startX = engine.getCurrentX();

            engine.moveLeft();
            assertEquals(startX - 1, engine.getCurrentX());

            engine.moveRight();
            assertEquals(startX, engine.getCurrentX());
        }

        @Test
        @DisplayName("왼쪽 벽에 닿으면 더 움직이지 않는다")
        void stopsAtLeftWall() {
            for (int i = 0; i < Board.WIDTH * 2; i++) {
                engine.moveLeft();
            }

            // 블럭이 실제로 차지한 칸이 왼쪽 벽(0열)에 닿아 있어야 한다
            assertEquals(0, leftmostCol(engine.getCurrentBlock()));
            // 더 눌러도 위치가 변하지 않는다
            int stuck = engine.getCurrentX();
            engine.moveLeft();
            assertEquals(stuck, engine.getCurrentX());
        }

        @Test
        @DisplayName("오른쪽 벽에 닿으면 더 움직이지 않는다")
        void stopsAtRightWall() {
            for (int i = 0; i < Board.WIDTH * 2; i++) {
                engine.moveRight();
            }

            // 블럭이 실제로 차지한 칸이 오른쪽 벽(마지막 열)에 닿아 있어야 한다
            assertEquals(Board.WIDTH - 1, rightmostCol(engine.getCurrentBlock()));
            // 더 눌러도 위치가 변하지 않는다
            int stuck = engine.getCurrentX();
            engine.moveRight();
            assertEquals(stuck, engine.getCurrentX());
        }

        /** 블럭이 실제로 차지한 칸 중 가장 왼쪽 열. 모양 격자의 빈 칸은 무시한다. */
        private int leftmostCol(Block block) {
            int min = Integer.MAX_VALUE;
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    if (block.isFilled(r, c)) {
                        min = Math.min(min, block.boardCol(c));
                    }
                }
            }
            return min;
        }

        /** 블럭이 실제로 차지한 칸 중 가장 오른쪽 열. 모양 격자의 빈 칸은 무시한다. */
        private int rightmostCol(Block block) {
            int max = Integer.MIN_VALUE;
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    if (block.isFilled(r, c)) {
                        max = Math.max(max, block.boardCol(c));
                    }
                }
            }
            return max;
        }

        @Test
        @DisplayName("아래로 내리면 행이 늘어난다")
        void movesDown() {
            int startY = engine.getCurrentY();

            engine.moveDown();

            assertEquals(startY + 1, engine.getCurrentY());
        }

        @Test
        @DisplayName("아래로 내릴 때마다 점수를 얻는다")
        void earnsScoreOnDrop() {
            engine.moveDown();

            assertEquals(LevelPolicy.INITIAL_LEVEL, engine.getScore());
        }

        @Test
        @DisplayName("이동이 성공하면 화면 갱신을 알린다")
        void notifiesOnSuccessfulMove() {
            int before = listener.boardUpdated;

            engine.moveLeft();

            assertEquals(before + 1, listener.boardUpdated);
        }

        @Test
        @DisplayName("반시계 회전은 시계 회전의 반대 방향이다")
        void rotatesCounterClockwise() {
            int before = engine.getCurrentBlock().rotation();

            engine.rotateCounterclockwise();

            assertEquals(BlockType.normalize(before - 1),
                engine.getCurrentBlock().rotation());
        }
    }

    // ------------------------------------------------------------------
    // 하드 드롭과 고정
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("하드 드롭")
    class HardDrop {

        @Test
        @DisplayName("바닥까지 내려간 뒤 새 블럭이 생성된다")
        void locksAndSpawnsNext() {
            Block before = engine.getCurrentBlock();

            engine.dropToBottom();

            assertEquals(Board.SPAWN_ROW, engine.getCurrentY());
            assertEquals(2, engine.getBlocksSpawned());
            // 새 블럭이 생성되었으므로 이전 블럭과 같은 객체가 아니다
            assertFalse(before == engine.getCurrentBlock());
        }

        @Test
        @DisplayName("보드에 블럭이 고정된다")
        void placesBlockOnBoard() {
            engine.dropToBottom();

            boolean anyFilled = false;
            for (int[] row : engine.getBoard()) {
                for (int cell : row) {
                    if (cell != 0) {
                        anyFilled = true;
                    }
                }
            }
            assertTrue(anyFilled, "고정된 블럭이 보드에 없습니다.");
        }

        @Test
        @DisplayName("떨어진 칸 수만큼 점수를 얻는다")
        void earnsScoreForDistance() {
            int ghostY = engine.getGhostY();
            int startY = engine.getCurrentY();
            int expected = (ghostY - startY) * LevelPolicy.INITIAL_LEVEL;

            engine.dropToBottom();

            assertEquals(expected, engine.getScore());
        }

        @Test
        @DisplayName("고스트 위치는 실제로 멈추는 위치와 같다")
        void ghostMatchesLandingPosition() {
            int ghostY = engine.getGhostY();
            int spawned = engine.getBlocksSpawned();

            // 한 칸씩 끝까지 내려본다
            int y = engine.getCurrentY();
            while (true) {
                engine.moveDown();
                if (engine.getBlocksSpawned() != spawned) {
                    break;          // 고정되어 새 블럭이 생성됨
                }
                y = engine.getCurrentY();
            }

            assertEquals(ghostY, y);
        }

        @Test
        @DisplayName("화면 갱신 알림 시점에 점수가 이미 반영되어 있다")
        void scoreIsUpdatedBeforeNotification() {
            List<Integer> seen = new ArrayList<>();
            engine.addListener(new GameEventListener() {
                @Override public void onBoardUpdated() { seen.add(engine.getScore()); }
                @Override public void onLinesCleared(int clearedCount, int gainedScore) { }
                @Override public void onSpeedIncreased(int currentInterval) { }
                @Override public void onGameOver(int finalScore) { }
            });

            engine.moveDown();

            // 알림은 한 번만 오고, 그 시점에 점수가 이미 반영되어 있어야 한다
            assertEquals(List.of(engine.getScore()), seen);
        }
    }

    // ------------------------------------------------------------------
    // 일시정지
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("일시정지")
    class Pause {

        @Test
        @DisplayName("일시정지와 재개를 토글한다")
        void togglesPause() {
            engine.togglePause();
            assertTrue(engine.isPaused());

            engine.togglePause();
            assertFalse(engine.isPaused());
        }

        @Test
        @DisplayName("일시정지 중에는 조작이 먹히지 않는다")
        void ignoresInputWhilePaused() {
            engine.pauseGame();
            int x = engine.getCurrentX();
            int y = engine.getCurrentY();
            int score = engine.getScore();

            engine.moveLeft();
            engine.moveRight();
            engine.moveDown();
            engine.rotate();
            engine.dropToBottom();

            assertEquals(x, engine.getCurrentX());
            assertEquals(y, engine.getCurrentY());
            assertEquals(score, engine.getScore());
        }

        @Test
        @DisplayName("재개하면 다시 조작할 수 있다")
        void resumesInput() {
            engine.pauseGame();
            engine.resumeGame();
            int x = engine.getCurrentX();

            engine.moveLeft();

            assertEquals(x - 1, engine.getCurrentX());
        }
    }

    // ------------------------------------------------------------------
    // 종료와 재시작
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("종료와 재시작")
    class EndAndRestart {

        @Test
        @DisplayName("중단하면 진행이 멈춘다")
        void exitStopsGame() {
            engine.exitGame();

            assertTrue(engine.isGameOver());
        }

        @Test
        @DisplayName("중단은 게임오버 이벤트를 발생시키지 않는다")
        void exitDoesNotFireGameOver() {
            // 사용자가 중단한 것은 스코어보드 기록 대상이 아니다
            engine.exitGame();

            assertEquals(0, listener.gameOver);
        }

        @Test
        @DisplayName("재시작하면 모든 상태가 초기화된다")
        void restartResetsEverything() {
            dropBlocks(3);
            engine.restartGame();

            assertEquals(0, engine.getScore());
            assertEquals(0, engine.getLinesCleared());
            assertEquals(LevelPolicy.INITIAL_LEVEL, engine.getLevel());
            assertEquals(1, engine.getBlocksSpawned());
            assertFalse(engine.isGameOver());

            for (int[] row : engine.getBoard()) {
                for (int cell : row) {
                    assertEquals(0, cell);
                }
            }
        }

        @Test
        @DisplayName("보드가 가득 차면 게임이 끝난다")
        void gameOverWhenBoardFills() {
            // 같은 자리에 계속 쌓으면 결국 생성 위치가 막힌다
            for (int i = 0; i < 200 && !engine.isGameOver(); i++) {
                engine.dropToBottom();
            }

            assertTrue(engine.isGameOver(), "블럭을 계속 쌓았는데 게임이 끝나지 않았습니다.");
            assertEquals(1, listener.gameOver);
            assertEquals(engine.getScore(), listener.lastFinalScore);
        }

        @Test
        @DisplayName("게임이 끝나면 조작이 먹히지 않는다")
        void ignoresInputAfterGameOver() {
            engine.exitGame();
            int x = engine.getCurrentX();

            engine.moveLeft();

            assertEquals(x, engine.getCurrentX());
        }
    }

    // ------------------------------------------------------------------
    // 리스너
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("리스너")
    class Listeners {

        @Test
        @DisplayName("여러 개를 등록하면 모두에게 알린다")
        void notifiesAllListeners() {
            RecordingListener second = new RecordingListener();
            engine.addListener(second);

            engine.moveLeft();

            assertEquals(1, second.boardUpdated);
            assertTrue(listener.boardUpdated >= 1);
        }

        @Test
        @DisplayName("해제하면 더 이상 알리지 않는다")
        void stopsNotifyingAfterRemoval() {
            engine.removeListener(listener);
            int before = listener.boardUpdated;

            engine.moveLeft();

            assertEquals(before, listener.boardUpdated);
        }
    }

    // ------------------------------------------------------------------
    // 줄 삭제·백투백·레벨
    // ------------------------------------------------------------------

    /**
     * 미리 채운 보드와 I 블럭만 주는 생성기로 줄 삭제 상황을 정확히 재현한다.
     *
     * <p>I 블럭은 생성 위치({@link TetrisEngine#SPAWN_COL} = 3)에서
     * 가로(회전 0)일 때 보드 3~6열, 세로(회전 1)일 때 보드 5열을 차지한다.
     */
    @Nested
    @DisplayName("줄 삭제·백투백·레벨")
    class LineClearAndLevel {

        /** 보드를 채울 때 쓰는 블럭 번호. 어떤 블럭이든 0이 아니면 된다. */
        private static final int FILLED = BlockType.O.code();

        /** 세로 I 블럭이 들어가는 열 */
        private static final int VERTICAL_I_COL = 5;

        private RecordingListener events;

        /** 항상 같은 종류의 블럭만 주는 생성기. */
        private BlockFactory always(BlockType type) {
            return new BlockFactory(new Random(0)) {
                @Override
                public BlockType next() {
                    return type;
                }
            };
        }

        /** fromRow~toRow 행을 채우되 emptyCols 열은 비워 둔다. */
        private void fillRows(int[][] grid, int fromRow, int toRow, int... emptyCols) {
            for (int r = fromRow; r <= toRow; r++) {
                for (int c = 0; c < Board.WIDTH; c++) {
                    grid[r][c] = FILLED;
                }
                for (int c : emptyCols) {
                    grid[r][c] = 0;
                }
            }
        }

        /** 미리 채운 보드와 I 블럭만으로 엔진을 만들고 이벤트 기록을 시작한다. */
        private TetrisEngine engineWith(int[][] grid) {
            TetrisEngine created = new TetrisEngine(always(BlockType.I), new Board(grid));
            events = new RecordingListener();
            created.addListener(events);
            return created;
        }

        /** 세로로 세운 I 블럭을 바닥까지 떨어뜨린다. */
        private void dropVerticalI(TetrisEngine target) {
            target.rotate();
            target.dropToBottom();
        }

        /** onLinesCleared로 받은 (줄 수, 얻은 점수) 목록. */
        private List<List<Integer>> clearEvents() {
            return events.linesCleared.stream()
                    .map(e -> List.of(e[0], e[1]))
                    .toList();
        }

        @Test
        @DisplayName("한 줄을 지우면 줄 삭제 점수를 얻고 이벤트를 알린다")
        void clearsSingleLine() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 19, 19, 3, 4, 5, 6);
            TetrisEngine target = engineWith(grid);

            target.dropToBottom();          // 가로 I가 18칸 떨어져 19행을 채운다

            assertEquals(1, target.getLinesCleared());
            assertEquals(List.of(List.of(1, 100)), clearEvents());
            assertEquals(18 + 100, target.getScore());
        }

        @Test
        @DisplayName("네 줄을 동시에 지우면 1000점을 얻는다")
        void clearsFourLines() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 16, 19, VERTICAL_I_COL);
            TetrisEngine target = engineWith(grid);

            dropVerticalI(target);          // 세로 I가 16칸 떨어져 16~19행을 채운다

            assertEquals(4, target.getLinesCleared());
            assertEquals(List.of(List.of(4, 1000)), clearEvents());
            assertEquals(16 + 1000, target.getScore());
        }

        @Test
        @DisplayName("네 줄 삭제가 연속되면 두 번째는 백투백으로 2배가 된다")
        void appliesBackToBack() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 12, 19, VERTICAL_I_COL);
            TetrisEngine target = engineWith(grid);

            dropVerticalI(target);
            dropVerticalI(target);

            assertEquals(List.of(List.of(4, 1000), List.of(4, 2000)), clearEvents());
            assertEquals(16 + 1000 + 16 + 2000, target.getScore());
        }

        @Test
        @DisplayName("사이에 1~3줄 삭제가 끼면 백투백이 끊긴다")
        void singleClearBreaksBackToBack() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 16, 19, VERTICAL_I_COL);   // 첫 번째 네 줄
            fillRows(grid, 12, 15, VERTICAL_I_COL);   // 세 번째 네 줄
            fillRows(grid, 11, 11, 3, 4, 5, 6);       // 두 번째 한 줄 (가로 I)
            TetrisEngine target = engineWith(grid);

            dropVerticalI(target);          // 16~19행 삭제 → 위 줄들이 4칸 내려온다
            target.dropToBottom();          // 가로 I로 한 줄 삭제 → 연속이 끊긴다
            dropVerticalI(target);          // 다시 네 줄 삭제지만 백투백이 아니다

            assertEquals(List.of(List.of(4, 1000), List.of(1, 100), List.of(4, 1000)),
                    clearEvents());
        }

        @Test
        @DisplayName("지운 줄이 10줄을 넘으면 레벨이 오르고 낙하 간격 변경을 알린다")
        void levelRisesByLines() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 8, 19, VERTICAL_I_COL);
            TetrisEngine target = engineWith(grid);

            dropVerticalI(target);          // 4줄
            dropVerticalI(target);          // 8줄
            assertEquals(LevelPolicy.INITIAL_LEVEL, target.getLevel());
            assertTrue(events.intervals.isEmpty());

            dropVerticalI(target);          // 12줄 → 레벨 2

            assertEquals(12, target.getLinesCleared());
            assertEquals(2, target.getLevel());
            assertEquals(List.of(new LevelPolicy().dropIntervalMs(2)), events.intervals);
        }

        @Test
        @DisplayName("재시작하면 레벨 1의 낙하 간격을 다시 알리고 보드를 비운다")
        void restartResetsDropInterval() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 8, 19, VERTICAL_I_COL);
            TetrisEngine target = engineWith(grid);
            dropVerticalI(target);
            dropVerticalI(target);
            dropVerticalI(target);          // 레벨 2

            target.restartGame();

            assertEquals(LevelPolicy.INITIAL_LEVEL, target.getLevel());
            assertEquals(LevelPolicy.INITIAL_INTERVAL_MS,
                    events.intervals.get(events.intervals.size() - 1));
            for (int[] row : target.getBoard()) {
                for (int cell : row) {
                    assertEquals(0, cell);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 재현성
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("재현성")
    class Reproducibility {

        @Test
        @DisplayName("같은 시드면 같은 블럭 순서가 나온다")
        void sameSeedProducesSameSequence() {
            TetrisEngine first = new TetrisEngine(new BlockFactory(new Random(SEED)));
            TetrisEngine second = new TetrisEngine(new BlockFactory(new Random(SEED)));

            for (int i = 0; i < 20; i++) {
                assertSame(first.getCurrentBlock().type(), second.getCurrentBlock().type());
                first.dropToBottom();
                second.dropToBottom();
            }

            assertEquals(first.getScore(), second.getScore());
        }
    }
}
