package seoultech.se.tetris.ui;

public interface ScreenNavigator {
    /** 시작 메뉴 화면으로 이동 */
    void navigateToMenu();

    /** 게임 시작 (인게임 화면으로 전환) */
    void navigateToGame();

    /** 설정 화면으로 이동 */
    void navigateToSettings();

    /** 스코어보드 화면으로 이동 */
    void navigateToScoreboard();

    /** 게임 종료 후 스코어보드/이름 입력 화면으로 이동 */
    void navigateToGameOver(int finalScore);

    /** 프로그램 전체 종료 */
    void exitProgram();
}
