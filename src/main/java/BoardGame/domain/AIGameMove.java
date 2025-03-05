package BoardGame.domain;

import lombok.Data;

@Data
public class AIGameMove {
	private Long moveId;
	private Long aiGameId;
	private Long playerId;
	private int rowPos;
	private int colPos;
	private int moveNumber;
	private Boolean isAIMove;
}
