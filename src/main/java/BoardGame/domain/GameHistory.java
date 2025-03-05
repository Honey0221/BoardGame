package BoardGame.domain;

import lombok.Data;

@Data
public class GameHistory {
	private Long historyId;
	private Long winnerId;
	private Long loserId;
	private Long gameId;
	private int ratingChange;
}
