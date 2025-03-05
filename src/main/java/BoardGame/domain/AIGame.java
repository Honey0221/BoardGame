package BoardGame.domain;

import java.time.LocalDateTime;

import BoardGame.constant.GameStatus;
import BoardGame.constant.TurnOrder;
import lombok.Data;

@Data
public class AIGame {
	private Long aiGameId;
	private Long playerId;
	private GameStatus status;
	private TurnOrder playerOrder;
	private LocalDateTime startedAt;
	private LocalDateTime endedAt;
	private int playerScore;
	private int aiScore;
	private Boolean isPlayerTurn;
}
