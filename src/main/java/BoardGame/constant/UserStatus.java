package BoardGame.constant;

public enum UserStatus {
	ONLINE, IN_GAME, OFFLINE, MATCHING;

	public String getDisplayText() {
		return switch (this) {
			case ONLINE -> "접속중";
			case IN_GAME -> "게임중";
			case MATCHING -> "매칭중";
			case OFFLINE -> "오프라인";
		};
	}
}
