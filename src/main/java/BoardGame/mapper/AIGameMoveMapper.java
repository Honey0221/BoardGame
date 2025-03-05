package BoardGame.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import BoardGame.domain.AIGameMove;

@Mapper
public interface AIGameMoveMapper {
  void insertAIGameMove(AIGameMove aiGameMove);
  List<AIGameMove> selectMovesByAIGameId(Long aiGameId);
	int selectLastMoveNumber(Long aiGameId);
	AIGameMove selectAIMoveByNumber(Long aiGameId, int moveNumber);
}
