package BoardGame.constant;

public enum BoardType {
	NOTICE, STRATEGY, FREE;

	public String getDisplayText() {
		return switch (this) {
			case NOTICE -> "공지사항";
			case STRATEGY -> "공략" + " " + "게시판";
			case FREE -> "자유" + " " + "게시판";
		};
	}
}
