package seoultech.se.tetris.config;

public interface KeyConfigService {
    /** 조작 액션 정의 */
    enum GameAction {
        MOVE_LEFT, MOVE_RIGHT, SOFT_DROP, HARD_DROP, ROTATE, PAUSE, EXIT
    }

    /** 특정 액션에 바인딩된 KeyEvent 키 코드 반환 */
    int getKeyCode(GameAction action);

    /** 특정 액션의 키 코드 변경 */
    void setKeyCode(GameAction action, int keyCode);

    /** 모든 키 설정을 기본값(방향키, 스페이스바, P, ESC 등)으로 초기화 */
    void resetToDefault();

    /** 현재 키 설정을 파일에 저장 */
    void save();

    /** 파일에서 키 설정을 로드 */
    void load();
}
