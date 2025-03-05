package BoardGame.service.game;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BoardGame.constant.GameStatus;
import BoardGame.constant.UserStatus;
import BoardGame.constant.UserTier;
import BoardGame.domain.Game;
import BoardGame.domain.GameHistory;
import BoardGame.domain.Member;
import BoardGame.Othello.OthelloGame;
import BoardGame.mapper.GameHistoryMapper;
import BoardGame.mapper.GameMapper;
import BoardGame.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GameResultService {
	private final GameMapper gameMapper;
	private final MemberMapper memberMapper;
	private final GameHistoryMapper gameHistoryMapper;
	private final SimpMessagingTemplate messagingTemplate;
	private final GameChatService gameChatService;
	private final GameServiceUtils gameUtils;

	public Map<String, Object> processSurrender(Game game, Long playerId) {
		game.setStatus(GameStatus.FINISHED);
		game.setEndedAt(LocalDateTime.now());

		Member winner = gameUtils.getMemberById(playerId.equals(game.getPlayer1Id()) ?
				game.getPlayer2Id() : game.getPlayer1Id());
		Member loser = gameUtils.getMemberById(playerId);

		winner.setWins(winner.getWins() + 1);
		winner.setTotalGames(winner.getTotalGames() + 1);
		loser.setLosses(loser.getLosses() + 1);
		loser.setTotalGames(loser.getTotalGames() + 1);

		winner.setWinStreak(winner.getWinStreak() + 1);
		if (winner.getWinStreak() > winner.getMaxWinStreak()) {
			winner.setMaxWinStreak(winner.getWinStreak());
		}
		loser.setWinStreak(0);

		int points = calculatePoints(game, winner, loser);
		UserTier oldWinnerTier = UserTier.getTierByRating(
				winner.getRatingPoint());
		UserTier oldLoserTier = UserTier.getTierByRating(loser.getRatingPoint());

		int winnerOldRating = winner.getRatingPoint();
		int loserOldRating = loser.getRatingPoint();

		int winnerNewRating = winnerOldRating + points;
		int loserNewRating;

		if (oldLoserTier.ordinal() >= UserTier.SILVER5.ordinal()) {
			loserNewRating = Math.max(loserOldRating - points, UserTier.SILVER5.getMinRating());
		} else {
			loserNewRating = Math.max(0, loserOldRating - points);
		}

		winner.setRatingPoint(winnerNewRating);
		loser.setRatingPoint(loserNewRating);

		winner.setTier(UserTier.getTierByRating(winnerNewRating));
		loser.setTier(UserTier.getTierByRating(loserNewRating));

		memberMapper.updateMember(winner);
		memberMapper.updateMember(loser);
		updateGameState(game, new OthelloGame(game.getGameId(), false));

		GameHistory history = new GameHistory();
		history.setGameId(game.getGameId());
		history.setWinnerId(winner.getId());
		history.setLoserId(loser.getId());
		history.setRatingChange(points);
		gameHistoryMapper.insertHistory(history);
		updatePlayerStatuses(game);

		Map<String, Object> response = new HashMap<>();
		response.put("type", "SURRENDER");
		response.put("winner", winner.getNickname());
		response.put("loser", loser.getNickname());
		response.put("player1Score", game.getPlayer1Score());
		response.put("player2Score", game.getPlayer2Score());
		response.put("ratingChange", points);
		response.put("winnerOldRating", winnerOldRating);
		response.put("loserOldRating", loserOldRating);
		response.put("winnerNewRating", winnerNewRating);
		response.put("loserNewRating", loserNewRating);
		response.put("winnerOldTier", oldWinnerTier);
		response.put("winnerNewTier", winner.getTier());
		response.put("loserOldTier", oldLoserTier);
		response.put("loserNewTier", loser.getTier());
		response.put("winnerCurrentStreak", winner.getWinStreak());
		response.put("winnerMaxStreak", winner.getMaxWinStreak());
		response.put("winnerWinRate", gameUtils.calculateWinRate(winner));
		response.put("loserWinRate", gameUtils.calculateWinRate(loser));
		response.put("message", loser.getNickname() + "님이 기권하셨습니다.");

		gameChatService.sendSystemMessage(game.getGameId(),
				loser.getNickname() + "님이 기권하셨습니다.");
		return response;
	}

	public void processGameResult(Game game) {
		Member player1 = gameUtils.getMemberById(game.getPlayer1Id());
		Member player2 = gameUtils.getMemberById(game.getPlayer2Id());

		boolean isPlayer1Winner = game.getPlayer1Score() > game.getPlayer2Score();
		Member winner = isPlayer1Winner ? player1 : player2;
		Member loser = isPlayer1Winner ? player2 : player1;

		game.setStatus(GameStatus.FINISHED);
		game.setEndedAt(LocalDateTime.now());

		updatePlayerStatuses(game);

		winner.setWins(winner.getWins() + 1);
		winner.setTotalGames(winner.getTotalGames() + 1);
		loser.setLosses(loser.getLosses() + 1);
		loser.setTotalGames(loser.getTotalGames() + 1);

		winner.setWinStreak(winner.getWinStreak() + 1);
		if (winner.getWinStreak() > winner.getMaxWinStreak()) {
			winner.setMaxWinStreak(winner.getWinStreak());
		}
		loser.setWinStreak(0);

		int points = calculatePoints(game, winner, loser);

		UserTier oldWinnerTier = UserTier.getTierByRating(winner.getRatingPoint());
		UserTier oldLoserTier = UserTier.getTierByRating(loser.getRatingPoint());

		int winnerOldRating = winner.getRatingPoint();
		int loserOldRating = loser.getRatingPoint();

		int winnerNewRating = winnerOldRating + points;
		int loserNewRating;

		if (oldLoserTier.ordinal() >= UserTier.SILVER5.ordinal()) {
			loserNewRating =
					Math.max(loserOldRating - points, UserTier.SILVER5.getMinRating());
		} else {
			loserNewRating = Math.max(0, loserOldRating - points);
		}

		winner.setRatingPoint(winnerNewRating);
		loser.setRatingPoint(loserNewRating);

		winner.setTier(UserTier.getTierByRating(winnerNewRating));
		loser.setTier(UserTier.getTierByRating(loserNewRating));

		GameHistory history = new GameHistory();
		history.setGameId(game.getGameId());
		history.setWinnerId(winner.getId());
		history.setLoserId(loser.getId());
		history.setRatingChange(points);

		gameHistoryMapper.insertHistory(history);
		memberMapper.updateMember(winner);
		memberMapper.updateMember(loser);

		Map<String, Object> resultData = new HashMap<>();
		resultData.put("type", "GAME_RESULT");
		resultData.put("winner", winner.getNickname());
		resultData.put("loser", loser.getNickname());
		resultData.put("player1Score", game.getPlayer1Score());
		resultData.put("player2Score", game.getPlayer2Score());
		resultData.put("winnerOldRating", winnerOldRating);
		resultData.put("loserOldRating", loserOldRating);
		resultData.put("winnerNewRating", winnerNewRating);
		resultData.put("loserNewRating", loserNewRating);
		resultData.put("winnerOldTier", oldWinnerTier);
		resultData.put("winnerNewTier", winner.getTier());
		resultData.put("loserOldTier", oldLoserTier);
		resultData.put("loserNewTier", loser.getTier());
		resultData.put("ratingChange", points);
		resultData.put("winnerCurrentStreak", winner.getWinStreak());
		resultData.put("winnerMaxStreak", winner.getMaxWinStreak());
		resultData.put("winnerWinRate", gameUtils.calculateWinRate(winner));
		resultData.put("loserWinRate", gameUtils.calculateWinRate(loser));

		messagingTemplate.convertAndSend(
				"/topic/game/" + game.getGameId(), resultData);
	}

	private int calculatePoints(Game game, Member winner, Member loser) {
		int ratingDiff = Math.abs(winner.getRatingPoint() - loser.getRatingPoint());
		int scoreDiff = Math.abs(game.getPlayer1Score() - game.getPlayer2Score());
		int basePoints = GameConstants.BASE_RATING_POINTS;

		double ratingMultiplier;
		if (winner.getRatingPoint() > loser.getRatingPoint()) {
			ratingMultiplier = Math.max(GameConstants.MIN_RATING_DIFF_MULTIPLIER,
					1.0 - (ratingDiff / 1200.0));
		} else {
			ratingMultiplier = Math.min(GameConstants.MAX_RATING_DIFF_MULTIPLIER,
					1.0 + (ratingDiff / 800.0));
		}

		double scoreMultiplier = 1.0 + (scoreDiff / 32.0) * 0.2;
		double streakMultiplier = Math.min(GameConstants.MAX_STREAK_MULTIPLIER,
				1.0 + (winner.getWinStreak() * 0.05));

		return (int) Math.round(basePoints * ratingMultiplier *
				scoreMultiplier * streakMultiplier);
	}

	private void updatePlayerStatuses(Game game) {
		Member player1 = gameUtils.getMemberById(game.getPlayer1Id());
		Member player2 = gameUtils.getMemberById(game.getPlayer2Id());

		memberMapper.updateStatus(player1.getMemberId(), UserStatus.ONLINE);
		memberMapper.updateStatus(player2.getMemberId(), UserStatus.ONLINE);
	}

	private void updateGameState(Game game, OthelloGame othelloGame) {
		game.setPlayer1Score(othelloGame.getBlackCount());
		game.setPlayer2Score(othelloGame.getWhiteCount());
		gameMapper.updateGame(game);
	}
}
