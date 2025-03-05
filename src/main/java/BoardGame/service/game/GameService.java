package BoardGame.service.game;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BoardGame.constant.GameStatus;
import BoardGame.domain.Game;
import BoardGame.domain.GameMove;
import BoardGame.domain.Member;
import BoardGame.Othello.OthelloGame;
import BoardGame.mapper.GameMapper;
import BoardGame.mapper.GameMoveMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GameService {
	private final GameMapper gameMapper;
	private final GameMoveMapper gameMoveMapper;
	private final GameMoveService gameMoveService;
	private final GameResultService gameResultService;
	private final GameServiceUtils gameUtils;

	@Transactional
  public Map<String, Object> getGameData(Long gameId, Member player) {
		Map<String, Object> gameData = gameMapper.selectGameWithPlayers(gameId);
		if (gameData == null) {
			throw new IllegalArgumentException("게임을 찾을 수 없습니다.");
		}

		gameData.put("myId", player.getId());
		gameData.put("myNickname", player.getNickname());

		boolean isFirstTurn = player.getId().equals(gameData.get("player1Id"));
		gameData.put("isFirstTurn", isFirstTurn);

		boolean isMyTurn = (boolean) gameData.get("isPlayer1Turn") == isFirstTurn;
		gameData.put("isMyTurn", isMyTurn);

		OthelloGame othelloGame = gameUtils.initializeGameState(gameId,
			gameMoveMapper.selectMovesByGameId(gameId), false);

		int lastMoveNumber = gameMoveMapper.selectLastMoveNumber(gameId);
		GameMove lastMove = lastMoveNumber > 0 ?
				gameMoveMapper.selectMoveByNumber(gameId, lastMoveNumber) : null;

		Map<String, Object> response = new HashMap<>();
		response.putAll(gameData);
		response.put("board", othelloGame.getBoard());
		response.put("validMoves", othelloGame.getValidMoves(
				(Boolean) gameData.get("isPlayer1Turn") ? 1 : 2));
		response.put("moves", gameMoveMapper.selectMovesByGameId(gameId));
		response.put("lastMove", lastMove);

		return response;
	}

	@Transactional
	public ResponseEntity<Map<String, Object>> processPlayerMove(
			Member player, Long gameId, int row, int col) {
		try {
			Game game = getGame(gameId);
			validatePlayerTurn(game, player);

			if (row == -1 && col == -1) {
				return gameMoveService.processSkipTurn(game, player);
			}

			return gameMoveService.processRegularMove(game, player, row, col);
		} catch (Exception e) {
			System.out.println("이동 처리 실패 : " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}

	@Transactional
	public Map<String, Object> processSurrender(Long gameId, Long playerId) {
		try {
			Game game = getGame(gameId);
			return gameResultService.processSurrender(game, playerId);
		} catch (Exception e) {
			throw new RuntimeException("기권 처리 실패 : " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> processTimeout(Long gameId) {
		try {
			Game game = getGame(gameId);
			if (game == null || game.getStatus() == GameStatus.FINISHED) {
				return null;
			}
			return gameMoveService.processTimeoutMove(game);
		} catch (Exception e) {
			throw new RuntimeException("타임아웃 처리 실패" + e.getMessage());
		}
	}

	private Game getGame(Long gameId) {
		Game game = gameMapper.selectGameById(gameId);
		if (game == null) {
			throw new IllegalArgumentException("게임을 찾을 수 없습니다.");
		}
		return game;
	}

	private void validatePlayerTurn(Game game, Member player) {
		boolean isPlayer1Turn = game.getIsPlayer1Turn();
		if ((isPlayer1Turn && !game.getPlayer1Id().equals(player.getId())) ||
				(!isPlayer1Turn && !game.getPlayer2Id().equals(player.getId()))) {
			throw new IllegalStateException("현재 플레이어의 턴이 아닙니다.");
		}
	}
}
