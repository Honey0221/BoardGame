package BoardGame.service;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BoardGame.constant.BoardType;
import BoardGame.constant.Role;
import BoardGame.domain.Member;
import BoardGame.domain.Post;
import BoardGame.mapper.PostMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
	private final PostMapper postMapper;

	public List<Post> getPostsByPage(BoardType boardType, int page, int size) {
		int offset = (page - 1) * size;
		return postMapper.selectByPage(boardType, size, offset);
	}

	public int getTotalPosts(BoardType boardType) {
		return postMapper.countByBoardType(boardType);
	}

	public List<Post> getRecentPosts(BoardType boardType) {
		return postMapper.selectRecent(boardType);
	}

	@Transactional
	public Post getPost(Long postId) {
		postMapper.incrementViewCount(postId);
		return postMapper.selectById(postId);
	}

	public void insertPost(Post post) {
		try {
			postMapper.insertPost(post);
		} catch (Exception e) {
			System.out.println("게시글 등록 서비스 오류 : " + e.getMessage());
		}
	}

	@Transactional
	public boolean updatePost(Post post, @AuthenticationPrincipal Member member) {
		Post existingPost = postMapper.selectById(post.getPostId());

		if (existingPost == null) {
			return false;
		}

		if (member.getRole() == Role.ADMIN ||
				existingPost.getWriter().equals(member.getNickname())) {
			postMapper.updatePost(post);
			return true;
		}
		return false;
	}

	@Transactional
	public boolean deletePost(Long postId, @AuthenticationPrincipal Member member) {
		Post post = postMapper.selectById(postId);
		if (post == null) {
			return false;
		}

		if (member.getRole() == Role.ADMIN || post.getWriter().equals(member.getNickname())) {
			postMapper.deletePost(postId);
			return true;
		}
		return false;
	}
}
