package BoardGame.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import BoardGame.domain.Game;
import BoardGame.domain.Member;
import BoardGame.service.MatchingService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/match")
public class MatchController {
	private final MatchingService matchingService;
	private final SimpMessagingTemplate messagingTemplate;

	@PostMapping("/start")
	@ResponseBody
	public Map<String, Object> startMatching(
			@AuthenticationPrincipal Member member) {
		try {
			matchingService.startMatching(member.getMemberId());
			return Map.of(
					"success", true,
					"message", "매칭이 시작되었습니다.",
					"winStreak", member.getWinStreak(),
					"winRate", matchingService.getWinRate(member)
			);
		} catch (Exception e) {
			System.out.println("매칭 시작 실패 : " + e.getMessage());
			return Map.of("success", false);
		}
	}

	@PostMapping("/cancel")
	@ResponseBody
	public Map<String, Object> cancelMatching(
			@AuthenticationPrincipal Member member) {
		try {
			matchingService.cancelMatching(member.getMemberId());
			return Map.of(
					"success", true,
					"message", "매칭이 취소되었습니다."
			);
		} catch (Exception e) {
			System.out.println("매칭 취소 실패 : " + e.getMessage());
			return Map.of("success", false);
		}
	}

	@MessageMapping("/match/process")
	public void processMatch(Principal principal) {
		try {
			String memberId = principal.getName();
			Map<String, Object> matchResult = matchingService.findMatch(memberId);

			if (matchResult != null) {
				Game game = (Game) matchResult.get("game");
				boolean isPlayer1 =
						matchResult.get("player1MemberId").toString().equals(memberId);

				messagingTemplate.convertAndSendToUser(
					memberId, "/queue/match",
					Map.of(
						"success", true,
						"gameId", game.getGameId(),
						"isFirstTurn", isPlayer1,
						"opponent", isPlayer1 ? matchResult.get("player2Nickname") :
									matchResult.get("player1Nickname"),
						"opponentRating", isPlayer1 ? matchResult.get("player2Rating") :
									matchResult.get("player1Rating"),
						"opponentTier", isPlayer1 ? matchResult.get("player2Tier") :
									matchResult.get("player1Tier"),
						"opponentStreak", isPlayer1 ? matchResult.get("player2Streak") :
									matchResult.get("player1Streak")
					)
				);
				
				String opponentId = isPlayer1 ? 
					matchResult.get("player2MemberId").toString() : 
					matchResult.get("player1MemberId").toString();
					
				messagingTemplate.convertAndSendToUser(
					opponentId, "/queue/match",
					Map.of(
						"success", true,
						"gameId", game.getGameId(),
						"isFirstTurn", !isPlayer1,
						"opponent", isPlayer1 ? matchResult.get("player1Nickname") :
									matchResult.get("player2Nickname"),
						"opponentRating", isPlayer1 ? matchResult.get("player1Rating") :
									matchResult.get("player2Rating"),
						"opponentTier", isPlayer1 ? matchResult.get("player1Tier") :
									matchResult.get("player2Tier"),
						"opponentStreak", isPlayer1 ? matchResult.get("player1Streak") :
									matchResult.get("player2Streak")
					)
				);
			} else {
				messagingTemplate.convertAndSendToUser(
					memberId, "/queue/match",
					Map.of(
						"success", false,
						"message", "매칭 실패"
					)
				);
			}
		} catch (Exception e) {
			System.out.println("매칭 처리 중 오류 발생 : " + e.getMessage());
		}
	}
}
