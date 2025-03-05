package BoardGame.controller;

import java.time.LocalDateTime;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import BoardGame.constant.ChatType;
import BoardGame.domain.LobbyChat;

@Controller
public class ChatController {
	@MessageMapping("/chat")
	@SendTo("/topic/chat")
	public LobbyChat handleChat(LobbyChat chat) {
		chat.setType(ChatType.NORMAL);
		chat.setSentAt(LocalDateTime.now());
		return chat;
	}
}
