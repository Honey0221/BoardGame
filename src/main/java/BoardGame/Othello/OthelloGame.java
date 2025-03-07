package BoardGame.Othello;

import java.util.ArrayList;
import java.util.List;

import BoardGame.domain.AIGameMove;
import BoardGame.domain.GameMove;
import lombok.Getter;

@Getter
public class OthelloGame {
	private static final int BOARD_SIZE = 8;
	private static final int[][] DIRECTIONS = {
			{-1, -1}, {-1, 0}, {-1, 1},
			{0, -1},           {0, 1},
			{1, -1}, {1, 0}, {1, 1}
	};
	private int[][] board;
	private int blackCount;
	private int whiteCount;
	private final Long gameId;
	private final boolean isAIGame;

	public OthelloGame(Long gameId, boolean isAIGame) {
		this.gameId = gameId;
		this.isAIGame = isAIGame;
		this.board = new int[BOARD_SIZE][BOARD_SIZE];
		initializeBoard();
	}

	private void initializeBoard() {
		for (int i = 0; i < BOARD_SIZE; i++) {
			for (int j = 0; j < BOARD_SIZE; j++) {
				board[i][j] = 0;
			}
		}
		int center = BOARD_SIZE / 2;
		board[center - 1][center - 1] = 2;
		board[center - 1][center] = 1;
		board[center][center - 1] = 1;
		board[center][center] = 2;
		updateScore();
	}

	public void updateScore() {
		blackCount = 0;
		whiteCount = 0;
		for (int i = 0; i < BOARD_SIZE; i++) {
			for (int j = 0; j < BOARD_SIZE; j++) {
				if (board[i][j] == 1) blackCount++;
				else if (board[i][j] == 2) whiteCount++;
			}
		}
	}

  public boolean isValidMove(int row, int col, int player) {
		if (row < 0 || row >= BOARD_SIZE || col < 0 ||
				col >= BOARD_SIZE || board[row][col] != 0) {
			return false;
		}

		for (int[] dir : DIRECTIONS) {
			if (wouldFlip(row, col, player, dir[0], dir[1])) {
				return true;
			}
		}
		return false;
	}

	private boolean wouldFlip(int row, int col, int player, int dRow, int dCol) {
		int opponent = player == 1 ? 2 : 1;
		int r = row + dRow;
		int c = col + dCol;
		boolean hasOpponent = false;

		while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
			if (board[r][c] == opponent) {
				hasOpponent = true;
			} else if (board[r][c] == player && hasOpponent) {
				return true;
			} else {
				break;
			}
			r += dRow;
			c += dCol;
		}
		return false;
	}

	public boolean makeMove(int row, int col, int player) {
		if (!isValidMove(row, col, player)) {
			return false;
		}

		board[row][col] = player;
		for (int[] dir : DIRECTIONS) {
			flipPieces(row, col, player, dir[0], dir[1]);
		}
		updateScore();
		return true;
	}

	private void flipPieces(int row, int col, int player, int dRow, int dCol) {
		if (!wouldFlip(row, col, player, dRow, dCol)) return;

		int r = row + dRow;
		int c = col + dCol;

		while (board[r][c] != player) {
			board[r][c] = player;
			r += dRow;
			c += dCol;
		}
	}

	public GameMove createGameMove(int row, int col, Long playerId, int moveNumber) {
		GameMove move = new GameMove();
		move.setGameId(this.gameId);
		move.setPlayerId(playerId);
		move.setRowPos(row);
		move.setColPos(col);
		move.setMoveNumber(moveNumber);

		return move;
	}

	public AIGameMove createAIGameMove(int row, int col, Long playerId,
			boolean isAIMove, int moveNumber) {
		AIGameMove move = new AIGameMove();
		move.setAiGameId(this.gameId);
		move.setPlayerId(playerId);
		move.setRowPos(row);
		move.setColPos(col);	
		move.setMoveNumber(moveNumber);
		move.setIsAIMove(isAIMove);

		return move;
	}

	public List<Move> getValidMoves(int player) {
		List<Move> moves = new ArrayList<>();
    
    for (int i = 0; i < BOARD_SIZE; i++) {
			for (int j = 0; j < BOARD_SIZE; j++) {
				if (isValidMove(i, j, player)) {
          moves.add(new Move(i, j));
				}
			}
    }
		return moves;
	}

	public void applyMoves(List<GameMove> moves) {
		for (GameMove move : moves) {
			int player;
			if (isAIGame) {
				player = move.getPlayerId() % 2 == 1 ? 1 : 2;
			} else {
				player = move.getMoveNumber() % 2 == 1 ? 1 : 2;
			}
			makeMove(move.getRowPos(), move.getColPos(), player);
		}
	}

	public void applyAIMoves(List<AIGameMove> moves) {
		for (AIGameMove move : moves) {
			int player = move.getIsAIMove() ? 2 : 1;
			makeMove(move.getRowPos(), move.getColPos(), player);
		}
	}
	
	public boolean isGameOver() {
		return getValidMoves(1).isEmpty() && getValidMoves(2).isEmpty();
	}

	@Getter
	public static class Move {
		private final int row;
		private final int col;

		public Move(int row, int col) {
			this.row = row;
			this.col = col;
		}
	}
}
