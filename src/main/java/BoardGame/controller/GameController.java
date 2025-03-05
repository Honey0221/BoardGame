package BoardGame.controller;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import BoardGame.domain.GameChat;
import BoardGame.domain.Member;
import BoardGame.service.game.GameChatService;
import BoardGame.service.game.GameService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/game")
public class GameController {
	private final GameService gameService;
	private final GameChatService gameChatService;
	private final SimpMessagingTemplate messagingTemplate;
	private final ConcurrentHashMap<Long, Set<String>> gameReadyPlayers =
			new ConcurrentHashMap<>();

	@MessageMapping("/game/ready")
	public void handleGameReady(Map<String, Object> request) {
		try {
			Long gameId = Long.parseLong(request.get("gameId").toString());
			String memberId = request.get("memberId").toString();

			gameReadyPlayers.computeIfAbsent(gameId, k ->
							ConcurrentHashMap.newKeySet()).add(memberId);

			Set<String> readyPlayers = gameReadyPlayers.get(gameId);
			
			if (readyPlayers.size() == 2) {
				messagingTemplate.convertAndSend(
					"/topic/game/" + gameId + "/ready",
					Map.of(
							"type", "READY",
							"allReady", true,
							"gameId", gameId
					)
				);
				gameReadyPlayers.remove(gameId);
			}
		} catch (Exception e) {
			System.out.println("게임 준비 처리 중 오류 발생 : " + e.getMessage());
		}
	}

	@GetMapping("/{gameId}")
	public String getGamePage(@PathVariable Long gameId,
			@AuthenticationPrincipal Member member, Model model) {
		try {
			Map<String, Object> gameData = gameService.getGameData(gameId, member);

			model.addAttribute("gameInfo", gameData);
			model.addAttribute("board", gameData.get("board"));
			model.addAttribute("validMoves", gameData.get("validMoves"));
			model.addAttribute("moves", gameData.get("moves"));

			return "game/game";
		} catch (Exception e) {
			System.out.println("게임 페이지 로드 실패 : " + e.getMessage());
			return "redirect:/lobby";
		}
	}

	@PostMapping("/{gameId}/move")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> processMove(
			@PathVariable Long gameId,
			@AuthenticationPrincipal Member member,
			@RequestBody Map<String, Integer> moveData) {
		try {
			return gameService.processPlayerMove(member, gameId,
					moveData.get("row"), moveData.get("col"));
		} catch (Exception e) {
			System.out.println("이동 처리 실패 : " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}

	@MessageMapping("/game/chat")
	public void handleChat(GameChat message) {
		try {
			GameChat processedMessage = gameChatService.processChatMessage(message);
			messagingTemplate.convertAndSend("/topic/game/" + message.getGameId(),
					processedMessage);
		} catch (Exception e) {
			System.out.println("게임 채팅 처리 실패 : " + e.getMessage());
		}
	}

	@MessageMapping("/game/surrender")
	public void handleSurrender(Map<String, Long> request) {
		try {
			Map<String, Object> response = gameService.processSurrender(
					request.get("gameId"), request.get("playerId"));
			messagingTemplate.convertAndSend("/topic/game/" +
					request.get("gameId"), response);
		} catch (Exception e) {
			System.out.println("기권 처리 실패 : " + e.getMessage());
		}
	}

	@MessageMapping("/game/timeout")
	public void handleTimeout(Map<String, Long> request,
			@AuthenticationPrincipal Member member) {
		try {
			Map<String, Object> response = gameService.processTimeout(
					request.get("gameId"));
			messagingTemplate.convertAndSend("/topic/game/" +
					request.get("gameId"), response);
		} catch (Exception e) {
			System.out.println("타임아웃 처리 중 오류 발생 : " + e.getMessage());
		}
	}
}
