package seoultech.se.tetris.score;

import java.util.List;

public interface ScoreRepository {
    /** 상위 점수 기록 리스트 반환 (최소 10개 이상, 점수 내림차순 정렬) */
    List<ScoreRecord> getTopScores();

    /** 새 점수 기록 등록 (이름, 점수) */
    void addScore(String playerName, int score);

    /** 스코어보드 기록 전체 초기화 (설정 화면의 초기화 기능 대응) */
    void clearScores();

    /** 파일에 저장 */
    void save();

    /** 파일에서 로드 */
    void load();
}
