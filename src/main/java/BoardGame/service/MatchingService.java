package BoardGame.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BoardGame.constant.GameStatus;
import BoardGame.constant.UserStatus;
import BoardGame.domain.Game;
import BoardGame.domain.Member;
import BoardGame.mapper.GameMapper;
import BoardGame.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchingService {
	private final GameMapper gameMapper;
	private final MemberMapper memberMapper;
	private final ConcurrentHashMap<String, LocalDateTime> matchingPlayers =
			new ConcurrentHashMap<>();

	@Transactional
	public void startMatching(String memberId) {
		Member member = memberMapper.selectByMemberId(memberId);
		if (member != null) {
			memberMapper.updateStatus(memberId, UserStatus.MATCHING);
			matchingPlayers.put(memberId, LocalDateTime.now());
		}
	}

	@Transactional
	public void cancelMatching(String memberId) {
		memberMapper.updateStatus(memberId, UserStatus.ONLINE);
		matchingPlayers.remove(memberId);
	}

	@Transactional
	public Map<String, Object> findMatch(String memberId) {
		Member player = memberMapper.selectByMemberId(memberId);
		if (player == null) {
			return null;
		}

		if (!matchingPlayers.containsKey(memberId)) {
			return null;
		}

		List<Member> matchingUsers = memberMapper.selectMatchingUsers();
		for (Member opponent : matchingUsers) {
			if (opponent.getMemberId().equals(memberId)) continue;

			try {
				Game game = new Game();
				boolean isPlayerWhite = player.getRatingPoint() < opponent.getRatingPoint();
				game.setPlayer1Id(isPlayerWhite ? opponent.getId() : player.getId());
				game.setPlayer2Id(isPlayerWhite ? player.getId() : opponent.getId());
				game.setStatus(GameStatus.IN_PROGRESS);
				game.setStartedAt(LocalDateTime.now());
				game.setPlayer1Score(2);
				game.setPlayer2Score(2);
				game.setIsPlayer1Turn(true);

				gameMapper.insertGame(game);

				memberMapper.updateStatus(player.getMemberId(), UserStatus.IN_GAME);
				memberMapper.updateStatus(opponent.getMemberId(), UserStatus.IN_GAME);

				matchingPlayers.remove(player.getMemberId());
				matchingPlayers.remove(opponent.getMemberId());

				Map<String, Object> matchResult = new HashMap<>();
				matchResult.put("game", game);
				matchResult.put("gameId", game.getGameId());
				matchResult.put("player1MemberId",
						isPlayerWhite ? opponent.getMemberId() : player.getMemberId());
				matchResult.put("player2MemberId",
						isPlayerWhite ? player.getMemberId() : opponent.getMemberId());
				matchResult.put("player1Nickname",
						isPlayerWhite ? opponent.getNickname() : player.getNickname());
				matchResult.put("player2Nickname",
						isPlayerWhite ? player.getNickname() : opponent.getNickname());
				matchResult.put("player1Rating",
						isPlayerWhite ? opponent.getRatingPoint() : player.getRatingPoint());
				matchResult.put("player2Rating",
						isPlayerWhite ? player.getRatingPoint() : opponent.getRatingPoint());
				matchResult.put("player1Tier",
						isPlayerWhite ? opponent.getTier() : player.getTier());
				matchResult.put("player2Tier",
						isPlayerWhite ? player.getTier() : opponent.getTier());
				matchResult.put("player1Streak",
						isPlayerWhite ? opponent.getWinStreak() : player.getWinStreak());
				matchResult.put("player2Streak",
						isPlayerWhite ? player.getWinStreak() : opponent.getWinStreak());

				return matchResult;
			} catch (Exception e) {
				System.out.println("게임 생성 중 오류 발생 : " + e.getMessage());
				memberMapper.updateStatus(player.getMemberId(), UserStatus.ONLINE);
				memberMapper.updateStatus(opponent.getMemberId(), UserStatus.ONLINE);
				throw e;
			}
		}
		return null;
	}

	public double getWinRate(Member member) {
		if (member.getTotalGames() == 0) return 0.0;
		return (double) member.getWins() / member.getTotalGames() * 100;
	}
}
