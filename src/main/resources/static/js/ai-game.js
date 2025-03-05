let gameStarted = false;
let isPlayerTurn = gameInfo.isPlayerTurn;
let currentBoard = [...board.map(row => [...row])];
const aiGameId = gameInfo.aiGameId;

$(document).ready(function() {
  updateScores(gameInfo);
  
  const startGameModal = ModalUtil.initializeModal('startGameModal');
  startGameModal.open();

  $('#startGameBtn').on('click', async function (e) {
    e.preventDefault();
    startGameModal.close();
    await startGame();
  });

  $('.leave-btn').on('click', async function (e) {
    e.preventDefault();
    const result = await AlertUtil.showConfirm('게임을 나가시겠습니까?');
    if (result.isConfirmed) {
      await leaveGame();
      window.location.href = '/lobby';
    }
  });
});

async function startGame() {
  gameStarted = true;

  initializeBoardEvents();

  if (isPlayerTurn) {
    showValidMoves(validMoves);
  } else {
    setTimeout(() => processMove(), 1000);
  }
}

function initializeBoardEvents() {
  $('.board-cell').off('click').removeClass('valid-move');

  $('.board-cell').on('click', async function() {
    if (!gameStarted || !isPlayerTurn) return;

    const row = $(this).data('row');
    const col = $(this).data('col');

    if (!isValidMove(row, col)) return;

    await processMove(row, col);
  });
}

async function processMove(row, col) {
  if (!gameStarted) return;

  try {
    hideValidMoves();
    let url = `/game/ai/${aiGameId}/move`;
    let data = null;

    if (row != null && col != null) {
      if (!isPlayerTurn) {
        return;
      }
      data = { row, col };
    } else {
      if (isPlayerTurn) {
        data = { row : -1, col : -1 };
      }
    }

    const response = await AjaxUtil.post(url, data);
    
    if (response && response.success) {
      if (response.skipMessage) {
        await AlertUtil.showInfo(response.skipMessage);
      }

      await updateGameState(response);
    }
  } catch (error) {
    console.error('이동 처리 실패:', error);
    if (gameStarted && isPlayerTurn) {
      showValidMoves(validMoves);
    }
  }
}

async function updateGameState(gameData) {
  await updateBoardWithAnimation(gameData.board);
  updateScores(gameData.gameInfo);

  if (gameData.lastMove) {
    showLastMove(gameData.lastMove);
  }
  
  if (gameData.gameInfo.status === 'FINISHED') {
    await handleGameOver(gameData.gameInfo);
    return;
  }

  isPlayerTurn = gameData.gameInfo.isPlayerTurn;
  validMoves = gameData.validMoves;

  if (isPlayerTurn && validMoves && validMoves.length > 0) {
    showValidMoves(validMoves);
  } else {
    setTimeout(() => processMove(), 1000);
  }
}

function updateBoardWithAnimation(newBoard) {
  return new Promise(resolve => {
    const changes = findBoardChanges(currentBoard, newBoard);

    let animationsComplete = 0;
    
    changes.forEach(change => {
      const cell = $(`.board-cell[data-row="${change.row}"][data-col="${change.col}"]`);
      const piece = cell.find('.piece');
      
      if (change.type === 'add') {
        piece.addClass(change.newValue === 1 ? 'black' : 'white');
      } else if (change.type === 'flip') {
        piece.addClass('flipping')
          .removeClass(change.oldValue === 1 ? 'black' : 'white')
          .addClass(change.newValue === 1 ? 'black' : 'white');
      }
      
      setTimeout(() => {
        piece.removeClass('flipping');
        animationsComplete++;
        if (animationsComplete === changes.length) {
          currentBoard = newBoard.map(row => [...row]);
          resolve();
        }
      }, 500);
    });
    
    if (changes.length === 0) {
      currentBoard = newBoard.map(row => [...row]);
      resolve();
    }
  });
}

function findBoardChanges(oldBoard, newBoard) {
  const changes = [];
  for (let i = 0; i < 8; i++) {
    for (let j = 0; j < 8; j++) {
      if (oldBoard[i][j] !== newBoard[i][j]) {
        changes.push({
          row: i,
          col: j,
          type: oldBoard[i][j] === 0 ? 'add' : 'flip',
          oldValue: oldBoard[i][j],
          newValue: newBoard[i][j]
        });
      }
    }
  }
  return changes;
}

function updateScores(gameInfo) {
  $('.player-score .score').text(gameInfo.playerScore);
  $('.ai-score .score').text(gameInfo.aiScore);
  $('.turn-indicator')
    .removeClass('player-turn ai-turn')
    .addClass(gameInfo.isPlayerTurn ? 'player-turn' : 'ai-turn')
    .text(gameInfo.isPlayerTurn ? '플레이어 턴' : 'AI 턴');
}

function showLastMove(lastMove) {
  $('.last-move-indicator').remove();

  if (lastMove && lastMove.rowPos >= 0 && lastMove.colPos >= 0) {
    $(`.board-cell[data-row="${lastMove.rowPos}"][data-col="${lastMove.colPos}"]`)
      .append('<div class="last-move-indicator"></div>');
  }
}

function showValidMoves(moves) {
  hideValidMoves();
  
  moves.forEach(move => {
    $(`.board-cell[data-row="${move.row}"][data-col="${move.col}"]`)
    .addClass('valid-move')
      .css('cursor', 'pointer')
      .css('pointer-events', 'auto')
      .append('<div class="valid-move-indicator"></div>');
    });
}

function hideValidMoves() {
  $('.board-cell')
    .removeClass('valid-move')
    .css('cursor', 'default')
    .css('pointer-events', 'none');
    $('.valid-move-indicator').remove();
}

function isValidMove(row, col) {
  return validMoves.some(move => move.row === row && move.col === col);
}

async function handleGameOver(gameInfo) {
  const winner = gameInfo.playerScore > gameInfo.aiScore ? '플레이어' :
                gameInfo.playerScore < gameInfo.aiScore ? 'AI' : '무승부';
  
  await AlertUtil.showInfo(
    `게임 종료!\n${winner}${winner === '무승부' ? '' : '의 승리'}\n` +
    `플레이어: ${gameInfo.playerScore} vs AI: ${gameInfo.aiScore}`
  );

  await leaveGame();
  window.location.href = '/lobby';
}

async function leaveGame() {
  try {
    await AjaxUtil.post(`/game/ai/${aiGameId}/leave`, { status: 'CANCELLED'});
  } catch (error) {
    console.error('게임 나가기 실패:', error);
  }
}