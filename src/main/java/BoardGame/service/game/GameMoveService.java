package BoardGame.service.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class GameMoveService {
	private final GameMapper gameMapper;
	private final GameMoveMapper gameMoveMapper;
	private final GameStateService gameStateService;
	private final GameChatService gameChatService;
	private final GameServiceUtils gameUtils;

	public ResponseEntity<Map<String, Object>> processSkipTurn(Game game, Member player) {
		switchTurn(game);

		OthelloGame othelloGame = gameUtils.initializeGameState(game.getGameId(),
			gameMoveMapper.selectMovesByGameId(game.getGameId()), false);

		int nextPlayer = game.getIsPlayer1Turn() ? 1 : 2;
		List<OthelloGame.Move> nextValidMoves = othelloGame.getValidMoves(nextPlayer);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("type", "MOVE");
		response.put("board", othelloGame.getBoard());
		response.put("validMoves", nextValidMoves);
		response.put("player1Score", game.getPlayer1Score());
		response.put("player2Score", game.getPlayer2Score());
		response.put("isPlayer1Turn", game.getIsPlayer1Turn());
		response.put("remainingTime", GameConstants.TIME_LIMIT);
		response.put("player1Nickname",
				gameUtils.getMemberById(game.getPlayer1Id()).getNickname());
		response.put("player2Nickname",
				gameUtils.getMemberById(game.getPlayer2Id()).getNickname());
		response.put("skipMessage",
				player.getNickname() + "님이 유효한 이동이 없어 턴을 넘깁니다.");

		gameChatService.sendSystemMessage(game.getGameId(),
				player.getNickname() + "님이 유효한 이동이 없어 턴을 넘깁니다.");
		gameStateService.broadcastGameState(game.getGameId(), response);
		return ResponseEntity.ok(response);
	}

	public ResponseEntity<Map<String, Object>> processRegularMove(
			Game game, Member player, int row, int col) {
		OthelloGame othelloGame = gameUtils.initializeGameState(game.getGameId(),
			gameMoveMapper.selectMovesByGameId(game.getGameId()), false);

		int currentPlayer = gameUtils.getCurrentPlayerNumber(game, player.getId());

		List<OthelloGame.Move> validMoves = othelloGame.getValidMoves(currentPlayer);

		boolean isValidMove = validMoves.stream()
				.anyMatch(move -> move.getRow() == row && move.getCol() == col);

		if (!isValidMove) {
			return ResponseEntity.badRequest().body(Map.of(
					"error", "유효하지 않은 이동입니다.",
					"validMoves", validMoves
			));
		}

		othelloGame.makeMove(row, col, currentPlayer);

		List<GameMove> moves = gameMoveMapper.selectMovesByGameId(game.getGameId());
		if (moves == null) {
			moves = new ArrayList<>();
		}

		GameMove move = othelloGame.createGameMove(row, col, player.getId(),
				moves.size() + 1);
		gameMoveMapper.insertMove(move);

		int nextPlayer = (currentPlayer == 1) ? 2 : 1;
		List<OthelloGame.Move> nextValidMoves = othelloGame.getValidMoves(nextPlayer);

		gameStateService.updateGameState(game, othelloGame);
		gameMapper.updateGame(game);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("type", "MOVE");
		response.put("board", othelloGame.getBoard());
		response.put("validMoves", nextValidMoves);
		response.put("lastMove", Map.of("rowPos", row, "colPos", col));
		response.put("player1Score", game.getPlayer1Score());
		response.put("player2Score", game.getPlayer2Score());
		response.put("isPlayer1Turn", game.getIsPlayer1Turn());
		response.put("remainingTime", GameConstants.TIME_LIMIT);
		response.put("player1Nickname",
				gameUtils.getMemberById(game.getPlayer1Id()).getNickname());
		response.put("player2Nickname",
				gameUtils.getMemberById(game.getPlayer2Id()).getNickname());

		gameStateService.broadcastGameState(game.getGameId(), response);
		return ResponseEntity.ok(response);
	}

	public Map<String, Object> processTimeoutMove(Game game) {
		OthelloGame othelloGame = gameUtils.initializeGameState(game.getGameId(),
			gameMoveMapper.selectMovesByGameId(game.getGameId()), false);

		boolean isPlayer1Turn = game.getIsPlayer1Turn();
		int currentPlayerNumber = isPlayer1Turn ? 1 : 2;
		Member currentPlayer = gameUtils.getMemberById(
				isPlayer1Turn ? game.getPlayer1Id() : game.getPlayer2Id());

		List<OthelloGame.Move> validMoves =
				othelloGame.getValidMoves(currentPlayerNumber);

		Map<String, Object> response = new HashMap<>();
		response.put("type", "MOVE");
		response.put("remainingTime", GameConstants.TIME_LIMIT);
		response.put("board", othelloGame.getBoard());
		response.put("player1Nickname",
				gameUtils.getMemberById(game.getPlayer1Id()).getNickname());
		response.put("player2Nickname",
				gameUtils.getMemberById(game.getPlayer2Id()).getNickname());

		int randomIndex = (int)(Math.random() * validMoves.size());
		OthelloGame.Move randomMove = validMoves.get(randomIndex);

		othelloGame.makeMove(randomMove.getRow(), randomMove.getCol(),
				currentPlayerNumber);

		List<GameMove> moves = gameMoveMapper.selectMovesByGameId(game.getGameId());
		if (moves == null) {
			moves = new ArrayList<>();
		}

		GameMove move = othelloGame.createGameMove(randomMove.getRow(),
				randomMove.getCol(), currentPlayer.getId(), moves.size() + 1);
		gameMoveMapper.insertMove(move);

		gameStateService.updateGameState(game, othelloGame);

		response.put("lastMove", new HashMap<String, Integer>() {{
			put("rowPos", randomMove.getRow());
			put("colPos", randomMove.getCol());
		}});
		response.put("validMoves",
				othelloGame.getValidMoves(game.getIsPlayer1Turn() ? 1 : 2));
		response.put("player1Score", game.getPlayer1Score());
		response.put("player2Score", game.getPlayer2Score());
		response.put("isPlayer1Turn", game.getIsPlayer1Turn());

		gameChatService.sendSystemMessage(game.getGameId(),
				currentPlayer.getNickname() + "님의 시간 초과! 랜덤으로 착수합니다.");
		return response;
	}

	private void switchTurn(Game game) {
		game.setIsPlayer1Turn(!game.getIsPlayer1Turn());
		gameMapper.updateGame(game);
	}
}
