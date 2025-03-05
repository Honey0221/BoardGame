package BoardGame.service.game;

public class GameConstants {
	// 게임 시간 제한
	public static final int TIME_LIMIT = 30;

	// 기본 점수 변동 포인트
	public static final int BASE_RATING_POINTS = 30;

	// 최대 연승 보너스 배율
	public static final double MAX_STREAK_MULTIPLIER = 1.3;

	// 최대 레이팅 차이 보너스 배율
	public static final double MAX_RATING_DIFF_MULTIPLIER = 1.5;

	// 최소 레이팅 차이 보너스 배율
	public static final double MIN_RATING_DIFF_MULTIPLIER = 0.5;

	// 시스템 메시지 발신자 ID
	public static final Long SYSTEM_SENDER_ID = -1L;

	// 시스템 메시지 발신자 닉네임
	public static final String SYSTEM_SENDER_NICKNAME = "SYSTEM";

	private GameConstants() {
		// 인스턴스화 방지
	}
}
