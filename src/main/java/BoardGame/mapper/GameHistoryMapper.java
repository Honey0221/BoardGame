package BoardGame.mapper;

import org.apache.ibatis.annotations.Mapper;

import BoardGame.domain.GameHistory;

@Mapper
public interface GameHistoryMapper {
	void insertHistory(GameHistory history);
}
