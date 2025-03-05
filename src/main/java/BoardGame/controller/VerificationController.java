package BoardGame.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import BoardGame.service.VerificationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {
	private final VerificationService verificationService;

	@PostMapping("/send")
	public ResponseEntity<?> sendVerification(
			@RequestBody Map<String, String> request) {
		try {
			String phone = request.get("phone");
			boolean sent = verificationService.sendVerificationSms(phone);

			if (sent) {
				return ResponseEntity.ok().body(Map.of(
						"success", true,
						"message", "인증번호가 발송되었습니다."
				));
			} else {
				return ResponseEntity.badRequest().body(Map.of(
						"success", false,
						"message", "인증번호 발송에 실패했습니다."
				));
			}
		} catch (Exception e) {
			System.out.println("인증번호 발송 중 오류 발생: " + e.getMessage());
			return ResponseEntity.internalServerError().body(Map.of(
					"success", false,
					"message", "서버 오류 발생"
			));
		}
	}

	@PostMapping("/verify")
	public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
		String phone = request.get("phone");
		String code = request.get("code");

		boolean verified = verificationService.verifyCode(phone, code);

		if (verified) {
			return ResponseEntity.ok().body(Map.of(
					"success", true,
					"message", "인증이 완료되었습니다."
			));
		} else {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", "인증번호가 일치하지 않거나 만료되었습니다."
			));
		}
	}
}
