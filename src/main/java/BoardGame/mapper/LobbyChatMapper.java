package BoardGame.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import BoardGame.domain.LobbyChat;

@Mapper
public interface LobbyChatMapper {
	void insertChat(LobbyChat chat);
	List<LobbyChat> selectRecentChats(int limit);
}
