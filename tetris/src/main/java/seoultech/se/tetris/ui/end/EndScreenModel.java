package seoultech.se.tetris.ui.end;

import java.util.List;

import seoultech.se.tetris.ui.ScreenNavigator;
import seoultech.se.tetris.ui.menu.MenuItem;
import seoultech.se.tetris.ui.menu.StartMenuModel;

/**
 * 게임 종료 화면의 최종 점수와 선택 상태.
 *
 * <p>Swing에 의존하지 않는다. 위·아래 이동과 Enter 선택은 시작 메뉴와 같은 리스트 메뉴를 재사용한다.
 * 스코어보드와 이름 입력은 이 화면의 책임이 아니다.
 */
public final class EndScreenModel {

    private final int finalScore;
    private final StartMenuModel menu;

    /**
     * @param finalScore 이번 판의 최종 점수. 0 이상
     * @param items 종료 후 선택할 항목. 비어 있으면 안 된다
     */
    public EndScreenModel(int finalScore, List<MenuItem> items) {
        if (finalScore < 0) {
            throw new IllegalArgumentException("점수는 음수일 수 없습니다: " + finalScore);
        }
        this.finalScore = finalScore;
        this.menu = new StartMenuModel(items);
    }

    /** 메인 메뉴로 돌아가기와 프로그램 종료만 있는 종료 화면을 만든다. */
    public static EndScreenModel createDefault(int finalScore, ScreenNavigator navigator) {
        return new EndScreenModel(finalScore, List.of(
                new MenuItem("메인 메뉴", navigator::navigateToMenu),
                new MenuItem("종료", navigator::exitProgram)));
    }

    public int getFinalScore() {
        return finalScore;
    }

    public List<MenuItem> getItems() {
        return menu.getItems();
    }

    public int getSelectedIndex() {
        return menu.getSelectedIndex();
    }

    public boolean isKeyHintVisible() {
        return menu.isKeyHintVisible();
    }

    /** 위 항목으로 이동한다. 첫 항목에서는 마지막 항목으로 순환한다. */
    public void moveUp() {
        menu.moveUp();
    }

    /** 아래 항목으로 이동한다. 마지막 항목에서는 첫 항목으로 순환한다. */
    public void moveDown() {
        menu.moveDown();
    }

    /** 현재 선택된 항목의 동작을 수행한다. */
    public void select() {
        menu.select();
    }

    /** 이 화면에서 쓰지 않는 키가 들어왔을 때 사용 가능한 키 안내를 켠다. */
    public void rejectInput() {
        menu.rejectInput();
    }
}
