package BoardGame.domain;

import java.time.LocalDateTime;

import BoardGame.constant.GameStatus;
import lombok.Data;

@Data
public class Game {
	private Long gameId;
	private Long player1Id;
	private Long player2Id;
	private GameStatus status;
	private LocalDateTime startedAt;
	private LocalDateTime endedAt;
	private int player1Score;
	private int player2Score;
	private Boolean isPlayer1Turn;
}
