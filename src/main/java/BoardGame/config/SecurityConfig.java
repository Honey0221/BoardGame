package BoardGame.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import BoardGame.constant.UserStatus;
import BoardGame.domain.Member;
import BoardGame.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Configuration
@EnableWebSecurity
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {
	private final MemberMapper memberMapper;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
			UserDetailsService userDetailsService) throws Exception {
		http
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/css/**", "/js/**", "/api/**", "/ws/**", "/logout")
				.ignoringRequestMatchers("/match/**", "/game/**"))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/css/**", "/js/**", "/favicon.ico").permitAll()
				.requestMatchers("/", "/api/**", "/ws/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/board", "/board/**").authenticated()
				.requestMatchers(HttpMethod.POST, "/board/**").authenticated()
				.requestMatchers(HttpMethod.PUT, "/board/**").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/board/**").authenticated()
				.requestMatchers("/board/*/write", "/board/*/edit").authenticated()
				.requestMatchers("/board/notice/write", "/board/notice/edit").hasRole("ADMIN")
				.requestMatchers(HttpMethod.POST, "/board/notice/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/board/notice/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/board/notice/**").hasRole("ADMIN")
				.requestMatchers("/match/**").permitAll()
				.requestMatchers("/lobby/**", "/game/**").authenticated()
				.anyRequest().authenticated())
			.formLogin(formLogin -> formLogin
				.loginPage("/")
				.usernameParameter("username")
				.passwordParameter("password")
				.loginProcessingUrl("/login")
				.defaultSuccessUrl("/lobby", true)
				.failureUrl("/?error")
				.permitAll())
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/")
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID")
				.permitAll())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				.sessionFixation().newSession()
				.maximumSessions(10)
				.maxSessionsPreventsLogin(false)
				.expiredUrl("/"))
			.userDetailsService(userDetailsService);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// 인증 관리자 빈 설정
	@Bean
	public AuthenticationManager authenticationManager(
			AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	// 세션 이벤트 리스너 빈 설정
	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	// 인증 성공 이벤트 리스너 빈 설정
	@EventListener
	public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
		try {
			Authentication authentication = event.getAuthentication();
			if (authentication != null && authentication.getPrincipal() instanceof Member) {
				Member member = (Member) authentication.getPrincipal();

				WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
				log.info("새로운 로그인: user={}, sessionId={}, remoteAddress={}", 
					member.getUsername(), 
					details.getSessionId(),
					details.getRemoteAddress());

				memberMapper.updateStatus(member.getMemberId(), UserStatus.ONLINE);
			}
		} catch (Exception e) {
			log.error("로그인 처리 중 오류: {}", e.getMessage());
		}
	}

	// 인증 실패 이벤트 리스너 빈 설정
	@EventListener
	public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
		try {
			String username = event.getAuthentication().getName();
			log.error("로그인 실패: user={}", username);
			memberMapper.updateStatus(username, UserStatus.OFFLINE);
		} catch (Exception e) {
			log.error("로그인 실패 처리 중 오류: {}", e.getMessage());
		}
	}

	// 로그아웃 성공 이벤트 리스너 빈 설정
	@EventListener
	public void onLogoutSuccess(LogoutSuccessEvent event) {
		try {
			Authentication authentication = event.getAuthentication();
			if (authentication != null && authentication.getPrincipal() instanceof Member) {
				Member member = (Member) authentication.getPrincipal();
				memberMapper.updateStatus(member.getMemberId(), UserStatus.OFFLINE);
				
				WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
				log.info("로그아웃: user={}, sessionId={}", 
					member.getUsername(), details.getSessionId());
			}
		} catch (Exception e) {
			log.error("로그아웃 처리 중 오류: {}", e.getMessage());
		}
	}
}
