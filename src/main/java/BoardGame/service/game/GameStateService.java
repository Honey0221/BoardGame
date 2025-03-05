package BoardGame.service.game;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BoardGame.constant.GameStatus;
import BoardGame.domain.Game;
import BoardGame.Othello.OthelloGame;
import BoardGame.mapper.GameMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GameStateService {
	private final GameMapper gameMapper;
	private final SimpMessagingTemplate messagingTemplate;
	private final GameResultService gameResultService;

	public void broadcastGameState(Long gameId, Map<String, Object> gameState) {
		try {
			messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
		} catch (Exception e) {
			System.out.println("게임 상태 브로드캐스트 실패 : " + e.getMessage());
		}
	}

	public void updateGameState(Game game, OthelloGame othelloGame) {
		game.setPlayer1Score(othelloGame.getBlackCount());
		game.setPlayer2Score(othelloGame.getWhiteCount());
		game.setIsPlayer1Turn(!game.getIsPlayer1Turn());

		if (othelloGame.isGameOver()) {
			game.setStatus(GameStatus.FINISHED);
			game.setEndedAt(LocalDateTime.now());
			gameResultService.processGameResult(game);
		}

		gameMapper.updateGame(game);
	}
}
