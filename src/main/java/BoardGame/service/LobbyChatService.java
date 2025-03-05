package BoardGame.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import BoardGame.constant.ChatType;
import BoardGame.domain.LobbyChat;
import BoardGame.mapper.LobbyChatMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LobbyChatService {
	private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
	private final ObjectMapper objectMapper;
	private final LobbyChatMapper lobbyChatMapper;

	public void addSession(WebSocketSession session) {
		sessions.add(session);
	}

	public void removeSession(WebSocketSession session) {
		sessions.remove(session);
	}

	public void saveChat(LobbyChat chat) {
		lobbyChatMapper.insertChat(chat);
	}

	public List<LobbyChat> getRecentChats(int limit) {
		return lobbyChatMapper.selectRecentChats(limit);
	}

	public void broadcastMessage(LobbyChat chat) {
		chat.setSentAt(LocalDateTime.now());
		chat.setType(ChatType.NORMAL);

		saveChat(chat);

		try {
			String message = objectMapper.writeValueAsString(chat);
			TextMessage textMessage = new TextMessage(message);

			for (WebSocketSession session : sessions) {
				if (session.isOpen()) {
					session.sendMessage(textMessage);
				}
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
