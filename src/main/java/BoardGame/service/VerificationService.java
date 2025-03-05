package BoardGame.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import BoardGame.util.SmsUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationService {
	private final SmsUtil smsUtil;
	private static final Duration EXPIRE_DURATION = Duration.ofMinutes(3);

	private final Map<String, VerificationInfo> verificationMap = new ConcurrentHashMap<>();

	private static class VerificationInfo {
		private final String code;
		private final LocalDateTime expireTime;

		public VerificationInfo(String code) {
			this.code = code;
			this.expireTime = LocalDateTime.now().plus(EXPIRE_DURATION);
		}

		public boolean isValid(String inputCode) {
			return code.equals(inputCode) && LocalDateTime.now().isBefore(expireTime);
		}
	}

	public boolean sendVerificationSms(String phone) {
		String code = smsUtil.generateVerificationCode();
		boolean sent = smsUtil.sendVerificationSms(phone, code);

		if (sent) {
			verificationMap.put(phone, new VerificationInfo(code));
			return true;
		}
		return false;
	}

	public boolean verifyCode(String phone, String code) {
		VerificationInfo info = verificationMap.get(phone);
		if (info != null && info.isValid(code)) {
			verificationMap.remove(phone);
			return true;
		}
		return false;
	}
}
