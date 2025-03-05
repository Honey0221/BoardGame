package BoardGame.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import BoardGame.domain.GameMove;

@Mapper
public interface GameMoveMapper {
	void insertMove(GameMove move);
	List<GameMove> selectMovesByGameId(Long gameId);
	int selectLastMoveNumber(Long gameId);
	GameMove selectMoveByNumber(Long gameId, int moveNumber);
}
