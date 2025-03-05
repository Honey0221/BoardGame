package BoardGame.domain;

import lombok.Data;

@Data
public class GameMove {
	private Long moveId;
	private Long gameId;
	private Long playerId;
	private int rowPos;
	private int colPos;
	private int moveNumber;
}
