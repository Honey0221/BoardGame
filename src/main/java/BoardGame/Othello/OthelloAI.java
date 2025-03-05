package BoardGame.Othello;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OthelloAI {
	private static final int[][] WEIGHTS = {
			{120, -20, 20, 5, 5, 20, -20, 120},
			{-20, -40, -5, -5, -5, -5, -40, -20},
			{20, -5, 15, 3, 3, 15, -5, 20},
			{5, -5, 3, 3, 3, 3, -5, 5},
			{5, -5, 3, 3, 3, 3, -5, 5},
			{20, -5, 15, 3, 3, 15, -5, 20},
			{-20, -40, -5, -5, -5, -5, -40, -20},
			{120, -20, 20, 5, 5, 20, -20, 120}
	};
	private static final int MOBILITY_WEIGHT = 10;
	private static final int STABILITY_WEIGHT = 15;
	private static final int CORNER_BOUNES = 25;

	public OthelloGame.Move findBestMove(OthelloGame game) {
		List<OthelloGame.Move> validMoves = game.getValidMoves(2);

		OthelloGame.Move bestMove = null;
		int bestScore = Integer.MIN_VALUE;
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;
		int depth = calculateDynamicDepth(game);

		for (OthelloGame.Move move : validMoves) {
			OthelloGame tempGame = cloneGame(game);
			tempGame.makeMove(move.getRow(), move.getCol(), 2);

			int score = minimax(tempGame, depth, false, alpha, beta);

			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
			}
		}
		return bestMove;
	}

	private int calculateDynamicDepth(OthelloGame game) {
		int emptySquares = countEmptySquares(game.getBoard());
		if (emptySquares <= 8) return 8;
		if (emptySquares <= 16) return 6;
		return 5;
	}

	private int minimax(OthelloGame game, int depth,
			boolean isMaximizing, int alpha, int beta) {
		if (depth == 0 || game.isGameOver()) {
			return evaluatePosition(game);
		}

		if (isMaximizing) {
			List<OthelloGame.Move> validMoves = game.getValidMoves(2);
			if (validMoves.isEmpty()) {
				return minimax(game, depth - 1, false, alpha, beta);
			}

			int maxScore = Integer.MIN_VALUE;
			for (OthelloGame.Move move : validMoves) {
				OthelloGame tempGame = cloneGame(game);
				tempGame.makeMove(move.getRow(), move.getCol(), 2);
				int score = minimax(tempGame, depth - 1, false, alpha, beta);
				maxScore = Math.max(maxScore, score);
				alpha = Math.max(alpha, score);
				if (beta <= alpha) break;
			}
			return maxScore;
		} else {
			List<OthelloGame.Move> validMoves = game.getValidMoves(1);

			if (validMoves.isEmpty()) {
				return minimax(game, depth - 1, true, alpha, beta);
			}

			int minScore = Integer.MAX_VALUE;
			for (OthelloGame.Move move : validMoves) {
				OthelloGame tempGame = cloneGame(game);
				tempGame.makeMove(move.getRow(), move.getCol(), 1);
				int score = minimax(tempGame, depth - 1, true, alpha, beta);
				minScore = Math.min(minScore, score);
				beta = Math.min(beta, score);
				if (beta <= alpha) break;
			}
			return minScore;
		}
	}

	private int evaluatePosition(OthelloGame game) {
		int[][] board = game.getBoard();
		int score = 0;
		// 1. 기본 가중치 평가
		score += evaluateWeights(board);
		// 2. 모빌리티(이동 가능성) 평가
		score += evaluateMobility(game) * MOBILITY_WEIGHT;
		// 3. 안정성 평가
		score += evaluateStability(board) * STABILITY_WEIGHT;
		// 4. 코너 점유 평가
		score += evaluateCorners(board) * CORNER_BOUNES;
		// 5. 게임 종료 상태 평가
		if (game.isGameOver()) {
			score += evaluateEndGame(game) * 1000;
		}
		return score;
	}

	private int evaluateWeights(int[][] board) {
		int score = 0;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (board[i][j] == 2) score += WEIGHTS[i][j];
				else if (board[i][j] == 1) score -= WEIGHTS[i][j];
			}
		}
		return score;
	}

	private int evaluateMobility(OthelloGame game) {
		return game.getValidMoves(2).size() - game.getValidMoves(1).size();
	}

	private int evaluateStability(int[][] board) {
		int stableAI = 0, stablePlayer = 0;
		boolean[][] stable = new boolean[8][8];

		if (board[0][0] != 0) markStableFromCorner(board, stable, 0, 0);
		if (board[0][7] != 0) markStableFromCorner(board, stable, 0, 7);
		if (board[7][0] != 0) markStableFromCorner(board, stable, 7, 0);
		if (board[7][7] != 0) markStableFromCorner(board, stable, 7, 7);

		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (stable[i][j]) {
					if (board[i][j] == 2) stableAI++;
					else if (board[i][j] == 1) stablePlayer++;
				}
			}
		}
		return stableAI - stablePlayer;
	}

	private void markStableFromCorner(int[][] board, boolean[][] stable, int row, int col) {
		int player = board[row][col];
		stable[row][col] = true;

		markStableLine(board, stable, player, row, col, 0, 1);
		markStableLine(board, stable, player, row, col, 1, 0);
		markStableLine(board, stable, player, row, col, 1, 1);
	}

	private void markStableLine(int[][] board, boolean[][] stable, int player,
			int startRow, int startCol, int dRow, int dCol) {
		int row = startRow + dRow;
		int col = startCol + dCol;

		while (row >= 0 && row < 8 && col >= 0 && col < 8
				&& board[row][col] == player) {
			stable[row][col] = true;
			row += dRow;
			col += dCol;
		}
	}

	private int evaluateCorners(int[][] board) {
		int aiCorners = 0, playerCorners = 0;
		int[][] corners = {{0, 0}, {0, 7}, {7, 0}, {7, 7}};

		for (int[] corner : corners) {
			if (board[corner[0]][corner[1]] == 2) aiCorners++;
			else if (board[corner[0]][corner[1]] == 1) playerCorners++;
		}
		return aiCorners - playerCorners;
	}

	private int evaluateEndGame(OthelloGame game) {
		return game.getWhiteCount() - game.getBlackCount();
	}

	private int countEmptySquares(int[][] board) {
		int count = 0;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (board[i][j] == 0) count++;
			}
		}
		return count;
	}

	private OthelloGame cloneGame(OthelloGame original) {
		OthelloGame clone = new OthelloGame(
				original.getGameId(),
				original.isAIGame()
		);

		int[][] originalBoard = original.getBoard();
		int[][] newBoard = clone.getBoard();

		for (int i = 0; i < 8; i++) {
			System.arraycopy(originalBoard[i], 0, newBoard[i], 0, 8);
		}
		clone.updateScore();
		return clone;
	}
}
