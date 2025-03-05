package BoardGame.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import BoardGame.constant.Role;
import BoardGame.constant.UserStatus;
import BoardGame.constant.UserTier;
import lombok.Data;

@Data
public class Member implements UserDetails {
	private Long id;
	private String memberId;
	private String nickname;
	private String password;
	private String phone;
	private Role role;
	private UserStatus status;
	private int totalGames;
	private int wins;
	private int losses;
	private int ratingPoint;
	private UserTier tier;
	private int winStreak;
	private int maxWinStreak;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@Override
	public boolean isEnabled() {
		return status == UserStatus.OFFLINE;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
	}

	@Override
	public String getUsername() {
		return this.memberId;
	}
}
