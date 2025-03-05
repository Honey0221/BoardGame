package BoardGame.mapper;

import org.apache.ibatis.annotations.Mapper;

import BoardGame.domain.AIGame;

@Mapper
public interface AIGameMapper {
	void insertAIGame(AIGame game);
	AIGame selectAIGameById(Long gameId);
	void updateAIGame(AIGame game);
}
