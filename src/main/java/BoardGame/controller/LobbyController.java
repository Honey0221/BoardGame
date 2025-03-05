package BoardGame.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import BoardGame.constant.BoardType;
import BoardGame.constant.UserStatus;
import BoardGame.domain.LobbyChat;
import BoardGame.domain.Member;
import BoardGame.domain.Post;
import BoardGame.mapper.MemberMapper;
import BoardGame.service.LobbyChatService;
import BoardGame.service.PostService;
import BoardGame.websocket.LobbyWebSocketHandler;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/lobby")
public class LobbyController {
	private final MemberMapper memberMapper;
	private final LobbyWebSocketHandler lobbyWebSocketHandler;
	private final LobbyChatService lobbyChatService;
	private final PostService postService;
	
	@GetMapping
	public String getLobby(@AuthenticationPrincipal Member member, Model model) {
		try {
			Member currentMember = memberMapper.selectByMemberId(member.getMemberId());
      
			if (currentMember != null && currentMember.getStatus() == UserStatus.IN_GAME) {
        memberMapper.updateStatus(currentMember.getMemberId(), UserStatus.ONLINE);
			}
      
      List<Member> onlineUsers = memberMapper.selectAllOnlineUsers();
      List<LobbyChat> recentChats = lobbyChatService.getRecentChats(50);
      
			model.addAttribute("member", currentMember);
			model.addAttribute("onlineUsers", onlineUsers);
			model.addAttribute("userCount", onlineUsers.size());
			model.addAttribute("recentChats", recentChats);
      
      lobbyWebSocketHandler.sendUserListToAll();

			return "lobby";
		} catch (Exception e) {
			System.out.println("로비 페이지 로드 실패" + e.getMessage());
			return "redirect:/";
		}
	}

	@GetMapping("/recent-posts")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getRecentPosts() {
		try {
			List<Post> recentNotices = postService.getRecentPosts(BoardType.NOTICE);
			List<Post> recentStrategies = postService.getRecentPosts(BoardType.STRATEGY);
			List<Post> recentFrees = postService.getRecentPosts(BoardType.FREE);

			return ResponseEntity.ok(Map.of(
					"success", true,
					"recentNotices", recentNotices,
					"recentStrategies", recentStrategies,
					"recentFrees", recentFrees
			));
		} catch (Exception e) {
			System.out.println("최근 게시글 조회 실패 : " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}

  @GetMapping("/ranking/top5")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> getTop5Rankings() {
    try {
      List<Member> ratingRankings = memberMapper.selectTop5ByRatingPoint();
      List<Member> winRateRankings = memberMapper.selectTop5ByWinRate();

      return ResponseEntity.ok(Map.of(
        "success", true,
        "ratingRankings", ratingRankings,
        "winRateRankings", winRateRankings
      ));
    } catch (Exception e) {
      System.out.println("랭킹 조회 실패 : " + e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }
}
