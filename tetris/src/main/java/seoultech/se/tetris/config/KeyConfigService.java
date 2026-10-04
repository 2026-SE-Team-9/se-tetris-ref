package seoultech.se.tetris.config;

import java.util.List;

public interface KeyConfigService {
    /** 조작 액션 정의 */
    enum GameAction {
        MOVE_LEFT, MOVE_RIGHT, SOFT_DROP, HARD_DROP,
        /** 시계 방향 90도 회전 */
        ROTATE,
        /** 반시계 방향 90도 회전 */
        ROTATE_COUNTERCLOCKWISE,
        /** 게임을 일시정지하고 메뉴를 연다. 직접 종료하는 액션이 아니다. */
        PAUSE_MENU
    }

    /** 특정 액션의 대표 키 코드 반환. 입력 판정에는 모든 키를 반환하는 getKeyCodes를 사용한다. */
    default int getKeyCode(GameAction action) {
        return getKeyCodes(action).getFirst();
    }

    /** 특정 액션의 기존 바인딩 전체를 키 하나로 교체 */
    default void setKeyCode(GameAction action, int keyCode) {
        setKeyCodes(action, List.of(keyCode));
    }

    /** 특정 액션에 바인딩된 모든 KeyEvent 키 코드 반환(변경 불가능한 목록) */
    List<Integer> getKeyCodes(GameAction action);

    /** 특정 액션의 바인딩 전체를 교체. 빈 목록, 중복 키, 다른 액션과 충돌하는 키는 거부한다. */
    void setKeyCodes(GameAction action, List<Integer> keyCodes);

    /** 기본값: 방향키 이동, Space 하드 드롭, ↑/X 시계 회전, Z 반시계 회전, Esc 일시정지 메뉴 */
    void resetToDefault();

    /** 현재 키 설정을 파일에 저장 */
    void save();

    /** 파일에서 키 설정을 로드 */
    void load();
}
