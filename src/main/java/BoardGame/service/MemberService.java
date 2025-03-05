package BoardGame.service;

import java.util.Random;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BoardGame.constant.Role;
import BoardGame.constant.UserStatus;
import BoardGame.constant.UserTier;
import BoardGame.domain.Member;
import BoardGame.mapper.MemberMapper;
import BoardGame.util.SmsUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {
	private final MemberMapper memberMapper;
	private final PasswordEncoder passwordEncoder;
	private final SmsUtil smsUtil;

	@Override
	public UserDetails loadUserByUsername(String username) throws
			UsernameNotFoundException {
		Member member = memberMapper.selectByMemberId(username);
		if (member == null) {
			throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
		}
		return member;
	}

	public void checkIdAvailability(String memberId) {
		if (memberMapper.selectByMemberId(memberId) != null) {
			throw new RuntimeException("이미 사용중인 아이디입니다.");
		}
	}

	@Transactional
	public void register(Member member) {
		try {
			member.setPassword(passwordEncoder.encode(member.getPassword()));
			member.setRole(Role.USER);
			member.setStatus(UserStatus.OFFLINE);
			member.setTier(UserTier.BRONZE5);
			member.setRatingPoint(1000);

			if (memberMapper.insertMember(member) <= 0) {
				throw new RuntimeException("회원가입에 실패하였습니다.");
			}
		} catch (Exception e) {
			System.out.println("회원가입 중 오류 발생: " + e.getMessage());
			throw new RuntimeException("회원가입에 실패하였습니다.");
		}
	}

	public String findMemberIdByPhone(String phone) {
		Member member = memberMapper.findMemberByPhone(phone);
		if (member == null) {
			throw new RuntimeException("해당 휴대폰 번호로 가입된 회원이 없습니다.");
		}
		return member.getMemberId();
	}

	@Transactional
	public void resetPassword(String memberId, String phone) {
		Member member = memberMapper.findMemberByIdAndPhone(memberId, phone);
		if (member == null) {
			throw new RuntimeException("일치하는 회원 정보가 없습니다.");
		}

		String tempPassword = generateTempPassword();

		member.setPassword(passwordEncoder.encode(tempPassword));
		memberMapper.updatePassword(member.getId(), member.getPassword());

		if (!smsUtil.sendVerificationSms(phone,
				"[한판해] 임시 비밀번호는 [" + tempPassword + "]입니다.")) {
			throw new RuntimeException("임시 비밀번호 발송에 실패했습니다.");
		}
	}

	private String generateTempPassword() {
		StringBuilder sb = new StringBuilder();
		Random random = new Random();

		for (int i = 0; i < 8; i++) {
			sb.append(random.nextInt(10));
		}
		return sb.toString();
	}
}
