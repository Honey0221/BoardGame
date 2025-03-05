package BoardGame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.lang.NonNull;

@Configuration
@EnableWebSocketMessageBroker // 웹소켓 메시지 브로커 활성화
// 웹소켓 메시지 브로커 : 메시지를 중간에서 전달하는 역할
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	@Override
	public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
		// 구독 주제에 대한 prefix 설정
		config.enableSimpleBroker("/topic", "/queue");
		// 클라이언트에서 보낸 메시지를 받을 prefix 설정
		config.setApplicationDestinationPrefixes("/app");
		// 사용자별 구독을 위한 prefix 설정
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
		// 웹소켓 엔드포인트 등록
		registry.addEndpoint("/ws/lobby", "/ws/game", "/ws")
				// 허용된 원본 패턴 설정
				.setAllowedOriginPatterns("http://localhost:[*]")
				// SockJS 사용 설정
				.withSockJS();
	}
}
