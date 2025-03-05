package BoardGame.domain;

import java.time.LocalDateTime;

import BoardGame.constant.ChatType;
import lombok.Data;

@Data
public class GameChat {
	private Long chatId;
	private Long gameId;
	private Long senderId;
	private String senderNickname;
	private String message;
	private ChatType type;
	private LocalDateTime sentAt;
}
