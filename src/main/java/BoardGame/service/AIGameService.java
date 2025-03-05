package BoardGame.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BoardGame.constant.GameStatus;
import BoardGame.constant.TurnOrder;
import BoardGame.constant.UserStatus;
import BoardGame.domain.AIGame;
import BoardGame.domain.AIGameMove;
import BoardGame.domain.Member;
import BoardGame.Othello.OthelloAI;
import BoardGame.Othello.OthelloGame;
import BoardGame.mapper.AIGameMapper;
import BoardGame.mapper.AIGameMoveMapper;
import BoardGame.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AIGameService {
	private final AIGameMapper aiGameMapper;
	private final AIGameMoveMapper aiGameMoveMapper;
	private final MemberMapper memberMapper;
	
	@Transactional
	public ResponseEntity<Map<String, Object>> createAIGame(Member player, TurnOrder order) {
		AIGame game = new AIGame();
		game.setPlayerId(player.getId());
		game.setStatus(GameStatus.IN_PROGRESS);
		game.setPlayerOrder(order);
		game.setStartedAt(LocalDateTime.now());
		game.setPlayerScore(2);
		game.setAiScore(2);
		game.setIsPlayerTurn(order == TurnOrder.FIRST);

		aiGameMapper.insertAIGame(game);
		memberMapper.updateStatus(player.getMemberId(), UserStatus.IN_GAME);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("gameId", game.getAiGameId());
		return ResponseEntity.ok(response);
	}

	public Map<String, Object> getGameData(Long gameId, Member player) {
		AIGame game = getGame(gameId);
		validateGame(game, player);

		OthelloGame othelloGame = new OthelloGame(game.getAiGameId(), true);
		List<AIGameMove> moves = aiGameMoveMapper.selectMovesByAIGameId(gameId);

		int lastMoveNumber = aiGameMoveMapper.selectLastMoveNumber(gameId);
		AIGameMove lastMove = lastMoveNumber > 0 ? 
			aiGameMoveMapper.selectAIMoveByNumber(gameId, lastMoveNumber) : null;

		if (moves != null && !moves.isEmpty()) {
			othelloGame.applyAIMoves(moves);
		}
		
		Map<String, Object> gameData = new HashMap<>();
		gameData.put("gameInfo", game);
		gameData.put("board", othelloGame.getBoard());
		gameData.put("validMoves",
				othelloGame.getValidMoves(game.getIsPlayerTurn() ? 1 : 2));
		gameData.put("moves", moves);
		gameData.put("lastMove", lastMove);

		return gameData;
	}

	@Transactional
	public ResponseEntity<Map<String, Object>> processPlayerMove(
			Member player, Long gameId, int row, int col) {
		try {
			AIGame game = getGame(gameId);
			validateGame(game, player);

			if (!game.getIsPlayerTurn()) {
				throw new IllegalStateException("AI의 턴입니다.");
			}

			OthelloGame othelloGame = new OthelloGame(game.getAiGameId(), true);
			List<AIGameMove> moves = aiGameMoveMapper.selectMovesByAIGameId(gameId);

			if (moves != null && !moves.isEmpty()) {
				othelloGame.applyAIMoves(moves);
			}

      List<OthelloGame.Move> validMoves = othelloGame.getValidMoves(1);
			if (validMoves.isEmpty()) {
				game.setIsPlayerTurn(false);
        game.setPlayerScore(othelloGame.getBlackCount());
        game.setAiScore(othelloGame.getWhiteCount());
        aiGameMapper.updateAIGame(game);
				Map<String, Object> response = getGameData(gameId, player);
				response.put("success", true);
				response.put("skipMessage", "플레이어의 유효한 이동이 없어 턴을 넘깁니다.");
				return ResponseEntity.ok(response);
			}

			if (!othelloGame.makeMove(row, col, 1)) {
				throw new IllegalStateException("유효하지 않은 이동입니다.");
			}

      if (moves == null) {
        moves = new ArrayList<>();
      }

			AIGameMove move = othelloGame.createAIGameMove(row, col, player.getId(),
					false, moves.size() + 1);
			aiGameMoveMapper.insertAIGameMove(move);
			
      game.setPlayerScore(othelloGame.getBlackCount());
      game.setAiScore(othelloGame.getWhiteCount());
			game.setIsPlayerTurn(false);

			if (othelloGame.isGameOver()) {
        finishGame(game);
			}

			aiGameMapper.updateAIGame(game);
			Map<String, Object> response = getGameData(gameId, player);
			response.put("success", true);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			System.out.println("플레이어 이동 처리 실패 : " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}

	@Transactional
	public ResponseEntity<Map<String, Object>> processAIMove(Member player, Long gameId) {
		try {
			AIGame game = getGame(gameId);
			validateGame(game, player);

			if (game.getIsPlayerTurn()) {
				throw new IllegalStateException("플레이어의 턴입니다.");
			}

			OthelloGame othelloGame = new OthelloGame(game.getAiGameId(), true);
			List<AIGameMove> moves = aiGameMoveMapper.selectMovesByAIGameId(gameId);

			if (moves != null && !moves.isEmpty()) {
				othelloGame.applyAIMoves(moves);
			}

      List<OthelloGame.Move> validMoves = othelloGame.getValidMoves(2);
			if (validMoves.isEmpty()) {
				game.setIsPlayerTurn(true);
        game.setPlayerScore(othelloGame.getBlackCount());
        game.setAiScore(othelloGame.getWhiteCount());
        aiGameMapper.updateAIGame(game);
				Map<String, Object> response = getGameData(gameId, player);
				response.put("success", true);
				response.put("skipMessage", "AI의 유효한 이동이 없어 턴을 넘깁니다.");
				return ResponseEntity.ok(response);
			}

			OthelloAI ai = new OthelloAI();
			OthelloGame.Move aiMove = ai.findBestMove(othelloGame);

			if (!othelloGame.makeMove(aiMove.getRow(), aiMove.getCol(), 2)) {
				throw new IllegalStateException("유효하지 않은 AI 이동입니다.");
			}

      if (moves == null) {
        moves = new ArrayList<>();
      }
      
			AIGameMove move = othelloGame.createAIGameMove(aiMove.getRow(), aiMove.getCol(),
					player.getId(), true, moves.size() + 1);
			aiGameMoveMapper.insertAIGameMove(move);

      game.setPlayerScore(othelloGame.getBlackCount());
      game.setAiScore(othelloGame.getWhiteCount());
			game.setIsPlayerTurn(true);

			if (othelloGame.isGameOver()) {
				finishGame(game);
			}

			aiGameMapper.updateAIGame(game);
			Map<String, Object> response = getGameData(gameId, player);
			response.put("success", true);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			System.out.println("AI 이동 처리 실패 : " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}

	public AIGame getGame(Long gameId) {
		AIGame game = aiGameMapper.selectAIGameById(gameId);
		if (game == null) {
			throw new IllegalArgumentException("게임이 존재하지 않습니다.");
		}
		return game;
	}

	private void validateGame(AIGame game, Member player) {
		if (!game.getPlayerId().equals(player.getId())) {
			throw new IllegalArgumentException("해당 게임에 참여할 수 없습니다.");
		}
	}

	@Transactional
	public void updateMemberStatusToOnline(Member player) {
		memberMapper.updateStatus(player.getMemberId(), UserStatus.ONLINE);
	}

	@Transactional
	public void cancelGame(AIGame game) {
		game.setStatus(GameStatus.CANCELLED);
		game.setEndedAt(LocalDateTime.now());
		aiGameMapper.updateAIGame(game);
	}

	@Transactional
	public void finishGame(AIGame game) {
		game.setStatus(GameStatus.FINISHED);
		game.setEndedAt(LocalDateTime.now());	
		aiGameMapper.updateAIGame(game);
	}
}
