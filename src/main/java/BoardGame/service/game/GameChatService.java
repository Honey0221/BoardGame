package BoardGame.service.game;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BoardGame.constant.ChatType;
import BoardGame.domain.GameChat;
import BoardGame.mapper.GameChatMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GameChatService {
	private final GameChatMapper gameChatMapper;
	private final SimpMessagingTemplate messagingTemplate;

	public GameChat processChatMessage(GameChat chat) {
		try {
			chat.setType(ChatType.NORMAL);
			chat.setSentAt(LocalDateTime.now());
			gameChatMapper.insertChat(chat);

			Map<String, Object> response = new HashMap<>();
			response.put("type", "CHAT");
			response.put("gameId", chat.getGameId());
			response.put("senderId", chat.getSenderId());
			response.put("senderNickname", chat.getSenderNickname());
			response.put("message", chat.getMessage());
			response.put("chatType", chat.getType());
			response.put("sentAt", chat.getSentAt());

			messagingTemplate.convertAndSend("/topic/game/" +
					chat.getGameId(), response);

			return chat;
		} catch (Exception e) {
			System.out.println("채팅 메시지 전송 실패 : " + e.getMessage());
			return null;
		}
	}

	public void sendSystemMessage(Long gameId, String content) {
		GameChat systemChat = new GameChat();
		systemChat.setGameId(gameId);
		systemChat.setSenderId(GameConstants.SYSTEM_SENDER_ID);
		systemChat.setSenderNickname(GameConstants.SYSTEM_SENDER_NICKNAME);
		systemChat.setType(ChatType.SYSTEM);
		systemChat.setMessage(content);
		systemChat.setSentAt(LocalDateTime.now());

		Map<String, Object> response = new HashMap<>();
		response.put("type", "CHAT");
		response.put("gameId", gameId);
		response.put("senderId", systemChat.getSenderId());
		response.put("senderNickname", systemChat.getSenderNickname());
		response.put("message", content);
		response.put("chatType", ChatType.SYSTEM);
		response.put("sentAt", systemChat.getSentAt());

		messagingTemplate.convertAndSend("/topic/game/" + gameId, response);
		gameChatMapper.insertChat(systemChat);
	}
}
