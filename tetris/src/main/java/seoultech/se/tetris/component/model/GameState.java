package seoultech.se.tetris.component.model;

import seoultech.se.tetris.blocks.Block;

public interface GameState {
    /** 현재 20x10 보드 상태 반환 (블록 번호 또는 색상 코드) */
    int[][] getBoard();

    /** 현재 떨어지고 있는 블록 객체 반환 */
    Block getCurrentBlock();

    /** 현재 블록의 보드 상 X 좌표 */
    int getCurrentX();

    /** 현재 블록의 보드 상 Y 좌표 */
    int getCurrentY();

    /** 다음에 등장할 블록 객체 반환 */
    Block getNextBlock();

    /** 실시간 점수 반환 */
    int getScore();

    /** 현재 삭제한 총 라인 수 반환 */
    int getLinesCleared();

    /** 게임 일시정지 상태 여부 */
    boolean isPaused();

    /** 게임 오버 상태 여부 */
    boolean isGameOver();

    /** (추가 기능) 하드 드롭 착지 예상 Y 좌표 (고스트 피스용) */
    int getGhostY();
}
