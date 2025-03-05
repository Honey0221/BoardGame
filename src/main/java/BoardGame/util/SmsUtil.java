package BoardGame.util;

import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;

import jakarta.annotation.PostConstruct;

@Component
public class SmsUtil {
	@Value("${coolsms.api.key}")
	private String apiKey;

	@Value("${coolsms.api.secret}")
	private String apiSecret;

	@Value("${coolsms.sender.phone}")
	private String senderPhone;

	private DefaultMessageService messageService;

	@PostConstruct
	private void init() {
		this.messageService = NurigoApp.INSTANCE
				.initialize(apiKey, apiSecret, "https://api.coolsms.co.kr");
	}

	public String generateVerificationCode() {
		Random random = new Random();
		return String.format("%06d", random.nextInt(1000000));
	}

	public boolean sendVerificationSms(String to, String verificationCode) {
		Message message = new Message();
		message.setFrom(senderPhone);
		message.setTo(to);
		message.setText("[한판해] 인증번호는 [" + verificationCode + "]입니다.");

		try {
			SingleMessageSentResponse response = this.messageService
					.sendOne(new SingleMessageSendingRequest(message));
			return response.getStatusCode().equals("2000");
		} catch (Exception e) {
			System.out.println("SMS 전송 중 오류 발생: " + e.getMessage());
			return false;
		}
	}
}
