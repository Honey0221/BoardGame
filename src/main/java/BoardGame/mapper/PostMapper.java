package BoardGame.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import BoardGame.constant.BoardType;
import BoardGame.domain.Post;

@Mapper
public interface PostMapper {
	List<Post> selectByPage(BoardType boardType, int size,  int offset);
	int countByBoardType(BoardType boardType);
	List<Post> selectRecent(BoardType boardType);
	Post selectById(Long postId);
	void insertPost(Post post);
	void updatePost(Post post);
	void deletePost(Long postId);
	void incrementViewCount(Long postId);
}
