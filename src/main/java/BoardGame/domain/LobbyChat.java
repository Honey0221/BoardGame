package BoardGame.domain;

import java.time.LocalDateTime;

import BoardGame.constant.ChatType;
import lombok.Data;

@Data
public class LobbyChat {
	private Long chatId;
	private Long senderId;
	private String senderNickname;
	private String message;
	private ChatType type;
	private LocalDateTime sentAt;
}
