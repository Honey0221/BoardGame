// 게임 보드 클릭 이벤트
function initializeBoardEvents() {
  $('.board-cell').on('click', async function (e) {
    e.preventDefault();
    const row = $(this).data('row');
    const col = $(this).data('col');

    if (!isMyTurn) return;

    if (!isValidMove(row, col)) return;

    await makeMove(row, col);
  });
}

// 착수 처리
async function makeMove(row, col) {
  if (!isMyTurn || !isValidMove(row, col)) return;

  try {
    hideValidMoves();
    const response = await AjaxUtil.post(`/game/${gameId}/move`, {
      row, col
    });

    if (response.success) {
      clearInterval(timer);
      await updateGameState(response);
    } else {
      showValidMoves(validMoves);
    }
  } catch (error) {
    console.error('착수 실패:', error);
    if (isMyTurn) {
      showValidMoves(validMoves);
    }
  }
}

// 턴 넘기기
function skipTurn() {
  makeMove(-1, -1);
}

// 보드 변경사항 업데이트
function updateBoardWithAnimation(oldBoard, newBoard) {
  return new Promise(resolve => {
    const changes = findBoardChanges(oldBoard, newBoard);

    if (changes.length === 0) {
      resolve();
      return;
    }

    let animationsComplete = 0;
    changes.forEach(change => {
      const cell = $(`.board-cell[data-row="${change.row}"][data-col="${change.col}"]`);
      const piece = cell.find('.piece');

      if (change.type === 'add') {
        piece.removeClass('black white')
          .addClass(change.newValue === 1 ? 'black' : 'white')
          .addClass('placing');

        setTimeout(() => {
          piece.removeClass('placing');
          animationsComplete++;
          if (animationsComplete === changes.length) {
            resolve();
          }
        }, 300);
      } else if (change.type === 'flip') {
        piece.addClass('flipping')
          .removeClass(change.oldValue === 1 ? 'black' : 'white')
          .addClass(change.newValue === 1 ? 'black' : 'white');

        setTimeout(() => {
          piece.removeClass('flipping');
          animationsComplete++;
          if (animationsComplete === changes.length) {
            resolve();
          }
        }, 500);
      }
    });
  });
}

// 보드 변경사항 찾기
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

// 마지막 착수 위치 표시
function showLastMove(lastMove) {
  $('.last-move-indicator').remove();

  if (lastMove && lastMove.rowPos >= 0 && lastMove.colPos >= 0) {
    $(`.board-cell[data-row="${lastMove.rowPos}"][data-col="${lastMove.colPos}"]`)
      .append('<div class="last-move-indicator"></div>');
  }
}

// 유효한 착수 위치 표시
function showValidMoves(moves) {
  hideValidMoves();
  if (!isMyTurn || !moves) return;

  moves.forEach(move => {
    $(`.board-cell[data-row="${move.row}"][data-col="${move.col}"]`)
      .addClass('valid-move')
      .append('<div class="valid-move-indicator"></div>');
  });
}

// 유효한 착수 위치 표시 숨김
function hideValidMoves() {
  $('.board-cell').removeClass('valid-move')
  $('.valid-move-indicator').remove();
}

// 유효한 착수 위치 확인
function isValidMove(row, col) {
  return validMoves.some(move => move.row === row && move.col === col);
} 