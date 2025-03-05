package BoardGame.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import BoardGame.domain.Game;

@Mapper
public interface GameMapper {
	void insertGame(Game game);
	Game selectGameById(Long gameId);
	void updateGame(Game game);
	Map<String, Object> selectGameWithPlayers(Long gameId);
}
