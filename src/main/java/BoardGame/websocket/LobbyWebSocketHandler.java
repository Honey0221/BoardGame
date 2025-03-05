package BoardGame.websocket;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import BoardGame.constant.UserStatus;
import BoardGame.domain.LobbyChat;
import BoardGame.domain.Member;
import BoardGame.mapper.MemberMapper;
import BoardGame.service.LobbyChatService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LobbyWebSocketHandler extends TextWebSocketHandler {
	private final ObjectMapper objectMapper;
	private final MemberMapper memberMapper;
	private final LobbyChatService lobbyChatService;
	private static final Map<String, WebSocketSession> SESSIONS =
			new ConcurrentHashMap<>();
	private static final Map<String, Member> CONNECTED_USERS =
			new ConcurrentHashMap<>();

	@Override
	public void afterConnectionEstablished(@NonNull WebSocketSession session)
			throws Exception {
		SESSIONS.put(session.getId(), session);
		lobbyChatService.addSession(session);

    Principal principal = session.getPrincipal();
    Authentication auth = (Authentication) principal;
    
    if (principal == null) {
      throw new IllegalStateException("Principal is null");
    }
    
    if (auth == null) {
      throw new IllegalStateException("Authentication is null");
    }
    
    Member currentUser = memberMapper.selectByMemberId(auth.getName());
    if (currentUser != null) {
      CONNECTED_USERS.put(session.getId(), currentUser);
			sendUserListToAll();
    }
	}

	@Override
	public void afterConnectionClosed(@NonNull WebSocketSession session,
			@NonNull CloseStatus status) throws Exception {
		Member disconnectedUser = CONNECTED_USERS.remove(session.getId());
		SESSIONS.remove(session.getId());
		lobbyChatService.removeSession(session);

		if (disconnectedUser != null) {
			try {
				Member updatedUser = memberMapper.selectByMemberId(
						disconnectedUser.getMemberId());
				if (updatedUser != null && updatedUser.getStatus() != UserStatus.IN_GAME) {
					memberMapper.updateStatus(disconnectedUser.getMemberId(), UserStatus.OFFLINE);
				}
			} catch (Exception e) {
				System.out.println("오프라인 상태 변경 중 오류 발생: " + e.getMessage());
			}
		}
		sendUserListToAll();
	}

	@Override
	public void handleTransportError(@NonNull WebSocketSession session,
			@NonNull Throwable exception) {
		Member errorUser = CONNECTED_USERS.remove(session.getId());
		SESSIONS.remove(session.getId());

		if (errorUser != null) {
			try {
				Member updatedUser = memberMapper.selectByMemberId(errorUser.getMemberId());
				if (updatedUser != null && updatedUser.getStatus() != UserStatus.IN_GAME) {
					memberMapper.updateStatus(errorUser.getMemberId(), UserStatus.OFFLINE);
				}
			} catch (Exception e) {
				System.out.println("오프라인 상태 변경 중 오류 발생: " + e.getMessage());
			}
		}
	}

	@Override
	protected void handleTextMessage(@NonNull WebSocketSession session,
			@NonNull TextMessage message) throws Exception {
		Map<String, Object> payload = objectMapper.readValue(
				message.getPayload(), new TypeReference<>() {} );
		String type = (String) payload.get("type");

		switch (type) {
      case "CHAT" -> handleChatMessage(session, payload);
      case "REQUEST_USERS_UPDATE" -> sendUserListToAll();
      default -> System.out.println("Unknown message type : " + type);
		}
	}

	public void sendUserListToAll() throws Exception {
		List<Member> onlineUsers = new ArrayList<>(CONNECTED_USERS.values());

		Map<String, Object> message = Map.of(
		"type", "USERS_UPDATE",
		"users", onlineUsers,
		"count", onlineUsers.size()
		);

		String json = objectMapper.writeValueAsString(message);
		TextMessage textMessage = new TextMessage(json);

		for (WebSocketSession session : SESSIONS.values()) {
			if (session.isOpen()) {
				session.sendMessage(textMessage);
			}
		}
	}

	private void handleChatMessage(WebSocketSession session, Map<String, Object> payload) {
		try {
			Principal principal = session.getPrincipal();
      Authentication auth = (Authentication) principal;
      
      if (principal == null) {
        throw new IllegalStateException("Principal is null");
      }
      
      if (auth == null) {
        throw new IllegalStateException("Authentication is null");
      }

      Member sender = memberMapper.selectByMemberId(auth.getName());
      if (sender != null) {
        LobbyChat chat = new LobbyChat();
        chat.setSenderId(sender.getId());
        chat.setSenderNickname(sender.getNickname());
        chat.setMessage((String) payload.get("message"));
        lobbyChatService.broadcastMessage(chat);
      }
		} catch (Exception e) {
			System.out.println("채팅 메시지 전송 중 오류 발생: " + e.getMessage());
		}
	}
}
