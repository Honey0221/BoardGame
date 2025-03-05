package BoardGame.mapper;

import org.apache.ibatis.annotations.Mapper;

import BoardGame.domain.GameChat;

@Mapper
public interface GameChatMapper {
	void insertChat(GameChat gameChat);
}
