package BoardGame.service.game;

import java.util.List;

import org.springframework.stereotype.Component;

import BoardGame.domain.Game;
import BoardGame.domain.GameMove;
import BoardGame.domain.Member;
import BoardGame.Othello.OthelloGame;
import BoardGame.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GameServiceUtils {
  private final MemberMapper memberMapper;

  public Member getMemberById(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("회원 ID가 null입니다.");
    }

    Member member = memberMapper.selectById(id);

    if (member == null) {
      throw new IllegalArgumentException("ID가 " + id + "인 회원을 찾을 수 없습니다.");
    }
    return member;
  }

  public OthelloGame initializeGameState(
      Long gameId, List<GameMove> moves, boolean isAIGame) {
    OthelloGame othelloGame = new OthelloGame(gameId, isAIGame);

    if (moves != null && !moves.isEmpty()) {
      othelloGame.applyMoves(moves);
    }
    return othelloGame;
  }

  public double calculateWinRate(Member member) {
    if (member == null || member.getTotalGames() == 0) {
      return 0.0;
    }
    return (double) member.getWins() / member.getTotalGames() * 100;
  }

  public boolean hasValidMoves(OthelloGame othelloGame, int playerNumber) {
    return !othelloGame.getValidMoves(playerNumber).isEmpty();
  }

  public int getCurrentPlayerNumber(Game game, Long playerId) {
    if (game.getPlayer1Id().equals(playerId)) {
      return 1;
    } else if (game.getPlayer2Id().equals(playerId)) {
      return 2;
    } else {
      throw new IllegalArgumentException("해당 플레이어는 이 게임에 참여하지 않았습니다.");
    }
  }
}
