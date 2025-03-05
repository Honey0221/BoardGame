package BoardGame.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import BoardGame.constant.TurnOrder;
import BoardGame.constant.UserStatus;
import BoardGame.domain.AIGame;
import BoardGame.domain.Member;
import BoardGame.mapper.MemberMapper;
import BoardGame.service.AIGameService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/game/ai")
public class AIGameController {
  private final AIGameService aiGameService;
  private final MemberMapper memberMapper;

  @PostMapping("/start")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> startAIGame(
      @AuthenticationPrincipal Member member,
      @RequestBody Map<String, String> request) {
    TurnOrder selectedOrder = TurnOrder.valueOf(request.get("order"));
    memberMapper.updateStatus(member.getMemberId(), UserStatus.IN_GAME);
    return aiGameService.createAIGame(member, selectedOrder);
  }

  @GetMapping("/{gameId}")
  public String getGamePage(@PathVariable Long gameId,
      @AuthenticationPrincipal Member member, Model model) {
    try {
      Map<String, Object> gameData = aiGameService.getGameData(gameId, member);

      model.addAllAttributes(gameData);
      return "game/ai";
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
      @RequestBody(required = false) Map<String, Integer> moveData) {
    try {
      ResponseEntity<Map<String, Object>> response;

      if (moveData == null) {
        response = aiGameService.processAIMove(member, gameId);
      } else {
        int row = moveData.get("row");
        int col = moveData.get("col");
        response = aiGameService.processPlayerMove(member, gameId, row, col);
      }

      Map<String, Object> responseBody = response.getBody();
      if (responseBody != null && responseBody.containsKey("skipMessage")) {
        return processMove(
            gameId, member, moveData == null ? new HashMap<>() : null);
      }

      return response;
    } catch (Exception e) {
      System.out.println("이동 처리 실패 : " + e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }

  @PostMapping("/{gameId}/leave")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> leaveGame(
      @PathVariable Long gameId,
      @AuthenticationPrincipal Member member,
      @RequestBody Map<String, String> request) {
    try {
      AIGame game = aiGameService.getGame(gameId);
      String status = request.get("status");
      if (status.equals("CANCELLED")) {
        aiGameService.cancelGame(game);
      } else {
        aiGameService.finishGame(game);
      }
      aiGameService.updateMemberStatusToOnline(member);
      return ResponseEntity.ok(Map.of("success", true));
    } catch (Exception e) {
      System.out.println("게임 이탈 실패 : " + e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }
}
