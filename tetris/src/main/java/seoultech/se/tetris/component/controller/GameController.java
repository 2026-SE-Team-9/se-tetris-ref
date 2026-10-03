package seoultech.se.tetris.component.controller;

public interface GameController {
    /** 블록을 왼쪽으로 1칸 이동 */
    void moveLeft();

    /** 블록을 오른쪽으로 1칸 이동 */
    void moveRight();

    /** 블록을 아래로 1칸 이동 (소프트 드롭) */
    void moveDown();

    /** 블록을 바닥까지 즉시 낙하 및 고정 (하드 드롭) */
    void dropToBottom();

    /** 블록을 시계방향으로 90도 회전 */
    void rotate();

    /** 블록을 반시계방향으로 90도 회전 */
    void rotateCounterclockwise();

    /** 게임 일시정지 / 재개 토글 */
    void togglePause();

    /** 게임 일시정지. Esc 입력 시 호출하고 일시정지 메뉴는 화면 계층에서 표시한다. */
    void pauseGame();

    /** 게임 재개 */
    void resumeGame();

    /** 게임 재시작 */
    void restartGame();

    /** 게임 도중 종료 (메뉴 복귀 등) */
    void exitGame();
}
