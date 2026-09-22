package seoultech.se.tetris.component.listener;

public interface GameEventListener {
    /** 보드 상태가 변경되어 화면을 다시 그려야 할 때 호출 */
    void onBoardUpdated();

    /** 줄이 삭제되었을 때 호출 (애니메이션, 추가 점수 팝업 등에 사용) */
    void onLinesCleared(int clearedCount, int gainedScore);

    /** 게임 속도(레벨)가 증가했을 때 호출 */
    void onSpeedIncreased(int currentInterval);

    /** 게임 오버가 발생했을 때 호출 (최종 점수 전달) */
    void onGameOver(int finalScore);
}
