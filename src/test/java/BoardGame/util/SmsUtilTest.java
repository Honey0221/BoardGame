package BoardGame.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmsUtilTest {
	@Autowired
	private SmsUtil smsUtil;

	@Test
	@DisplayName("문자 발송 테스트")
	public void sendSmsTest() {
		String code = smsUtil.generateVerificationCode();
		boolean result = smsUtil.sendVerificationSms("01033679402", code);
		assertTrue(result);
	}
}