package BoardGame.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import BoardGame.constant.BoardType;
import BoardGame.constant.Role;
import BoardGame.domain.Member;
import BoardGame.domain.Post;
import BoardGame.service.PostService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {
	private final PostService postService;

	@GetMapping
	public String getBoard() {
		return "redirect:/board/notice";
	}

	@GetMapping("/{boardType}")
	public String getBoardList(@PathVariable String boardType,
			@AuthenticationPrincipal Member member,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "5") int size, Model model) {
		try {
			BoardType type = BoardType.valueOf(boardType.toUpperCase());
			List<Post> posts = postService.getPostsByPage(type, page, size);
			int totalPosts = postService.getTotalPosts(type);
			int totalPages = (int) Math.ceil((double) totalPosts / size);
			int startNumber = totalPosts - (page - 1) * size;

			model.addAttribute("member", member);
			model.addAttribute("posts", posts);
			model.addAttribute("boardType", type);
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", totalPages);
			model.addAttribute("startNumber", startNumber);

			return "board/board";
		} catch (Exception e) {
			System.out.println("게시판 목록 조회 실패 : " + e.getMessage());
			return "redirect:/lobby";
		}
	}

	@GetMapping("/{boardType}/{postId}")
	public String getPost(@PathVariable String boardType, Model model,
			@AuthenticationPrincipal Member member, @PathVariable Long postId) {
		try {
			BoardType type = BoardType.valueOf(boardType.toUpperCase());
			Post post = postService.getPost(postId);
			
			if (post == null || post.getBoardType() != type) {
				return "redirect:/board/" + boardType;
			}

			model.addAttribute("member", member);
			model.addAttribute("post", post);
			model.addAttribute("boardType", type);

			return "board/detail";
		} catch (Exception e) {
			System.out.println("게시글 상세 조회 실패 : " + e.getMessage());
			return "redirect:/board";
		}
	}

	@GetMapping("/{boardType}/write")
	public String getWriteForm(@PathVariable String boardType, 
							 @AuthenticationPrincipal Member member,
							 Model model) {
		try {
			BoardType type = BoardType.valueOf(boardType.toUpperCase());

			if (type == BoardType.NOTICE && member.getRole() != Role.ADMIN) {
				return "redirect:/board/" + boardType;
			}

			model.addAttribute("member", member);
			model.addAttribute("boardType", type);

			return "board/write";
		} catch (Exception e) {
			System.out.println("게시글 작성 페이지 로드 실패 " + e.getMessage());
			return "redirect:/board";
		}
	}

	@GetMapping("/{boardType}/{postId}/edit")
	public String getEditForm(@PathVariable String boardType,
							 @PathVariable Long postId,
							 @AuthenticationPrincipal Member member,
							 Model model) {
		try {
			BoardType type = BoardType.valueOf(boardType.toUpperCase());
			Post post = postService.getPost(postId);

			if (post == null) {
				return "redirect:/board/" + boardType;
			}
			
			if (member.getRole() == Role.ADMIN || post.getWriter().equals(member.getNickname())) {
				model.addAttribute("member", member);
				model.addAttribute("boardType", type);
				model.addAttribute("post", post);

				return "board/edit";
			}
			
			return "redirect:/board/" + boardType;
		} catch (Exception e) {
			System.out.println("게시글 수정 페이지 로드 실패 " + e.getMessage());
			return "redirect:/board";
		}
	}

	@PostMapping("/{boardType}")
	@ResponseBody
	public ResponseEntity<String> createPost(@PathVariable String boardType,
										   @RequestBody Post post,
										   @AuthenticationPrincipal Member member) {
		try {
			BoardType type = BoardType.valueOf(boardType.toUpperCase());

			if (type == BoardType.NOTICE && member.getRole() != Role.ADMIN) {
				return ResponseEntity.badRequest().build();
			}
			
			post.setBoardType(type);
			post.setWriter(member.getNickname());
			post.setCreatedAt(LocalDateTime.now());

			postService.insertPost(post);
			
			return ResponseEntity.ok("success");
		} catch (Exception e) {
			System.out.println("게시글 등록 실패 " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}

	@PutMapping("/{boardType}/{postId}")
	@ResponseBody
	public ResponseEntity<String> updatePost(
		@PathVariable Long postId, @RequestBody Post post,
		@AuthenticationPrincipal Member member) {
		try {
			if (postService.updatePost(post, member)) {
				return ResponseEntity.ok().body("success");
			} else {
				System.out.println("수정 권한이 없습니다.");
				return ResponseEntity.badRequest().build();
			}
		} catch (Exception e) {
			System.out.println("게시글 수정 실패 : " + e.getMessage());
			return ResponseEntity.badRequest().build();
		}
	}

	@DeleteMapping("/{boardType}/{postId}")
	@ResponseBody
	public ResponseEntity<String> deletePost(@PathVariable Long postId,
										   @AuthenticationPrincipal Member member) {
		try {
			if (postService.deletePost(postId, member)) {
				return ResponseEntity.ok().body("success");
			} else {
				System.out.println("삭제 권한이 없습니다.");
				return ResponseEntity.badRequest().build();
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}
}
