package BoardGame.domain;

import java.time.LocalDateTime;

import BoardGame.constant.BoardType;
import lombok.Data;

@Data
public class Post {
	private Long postId;
	private String title;
	private String content;
	private String writer;
	private BoardType boardType;
	private int viewCount;
	private LocalDateTime createdAt;
}
