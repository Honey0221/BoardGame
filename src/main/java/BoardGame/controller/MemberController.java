package BoardGame.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import BoardGame.domain.Member;
import BoardGame.service.MemberService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
	private final MemberService memberService;

	@GetMapping("/check-id")
	public ResponseEntity<Void> checkId(@RequestParam String id) {
		try {
			memberService.checkIdAvailability(id);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			System.out.println("중복 체크 중 오류 발생: " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}

	@PostMapping("/register")
	public ResponseEntity<Void> register(@RequestBody Map<String, String> request) {
		try {
			Member member = new Member();
			member.setMemberId(request.get("id"));
			member.setPassword(request.get("password"));
			member.setNickname(request.get("nickname"));
			member.setPhone(request.get("phone"));

			memberService.register(member);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			System.out.println("회원가입 중 오류 발생: " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}

	@PostMapping("/find-id")
	public ResponseEntity<Map<String, String>> findId(
			@RequestBody Map<String, String> request) {
		try {
			String memberId = memberService.findMemberIdByPhone(request.get("phone"));
			return ResponseEntity.ok(Map.of(
					"success", "true",
					"memberId", memberId
			));
		} catch (Exception e) {
			System.out.println("아이디 찾기 중 오류 발생: " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}

	@PostMapping("/find-pw")
	public ResponseEntity<Map<String, String>> findPassword(
			@RequestBody Map<String, String> request) {
		try {
			memberService.resetPassword(request.get("id"), request.get("phone"));
			return ResponseEntity.ok(Map.of("success", "true"));
		} catch (Exception e) {
			System.out.println("비밀번호 찾기 중 오류 발생: " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}
}
