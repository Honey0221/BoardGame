package BoardGame.game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import BoardGame.Othello.OthelloGame;

class OthelloGameTest {
	@Test
	@DisplayName("게임 종료 테스트")
	public void initializeGameTest() {
		OthelloGame game = new OthelloGame(1L, true);

		assertFalse(game.isGameOver(), "초기 상태에서 게임이 끝났다고 판단");

		game.getBoard()[3][3] = 1;
		game.getBoard()[3][4] = 1;
		game.getBoard()[4][3] = 1;
		game.getBoard()[4][4] = 1;

		assertTrue(game.isGameOver(), "모든 유효한 이동이 없는데 게임이 끝나지 않음");
	}
}