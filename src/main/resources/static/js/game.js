let stompClient;
let gameId = gameInfo.gameId;
let isMyTurn = gameInfo.isMyTurn;
let isFirstTurn = gameInfo.isFirstTurn;
let timer;
let remainingTime = timeLimit;
let currentBoard = [...board];
let isConnected = false;
let pendingTimerStart = false;

$(document).ready(function () {
  initializeWebSocket();
  initializeBoardEvents();
  initializeChat();
  initializeControls();

  updateGameState(gameInfo);
});

function initializeWebSocket() {
  const sock = new SockJS('/ws/game');
  stompClient = Stomp.over(sock);
  stompClient.debug = null;

  stompClient.connect({}, () => {
    isConnected = true;

    stompClient.subscribe(`/topic/game/${gameId}`, (message) => {
      const data = JSON.parse(message.body);
      handleGameMessage(data);
    });

    if (pendingTimerStart) {
      pendingTimerStart = false;
      startTimer();
    }
  }, (error) => {
    console.error('게임 소켓 연결 실패 : ', error);
    isConnected = false;
    setTimeout(initializeWebSocket, 3000);
  });

  return stompClient;
}

function initializeControls() {
  $('.surrender-btn').on('click', async function (e) {
    e.preventDefault();
    const result = await AlertUtil.showConfirm('기권하시겠습니까?');
    if (result.isConfirmed) {
      handleSurrender();
    }
  });
}

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

async function updateGameState(data) {
  const oldBoard = currentBoard.map(row => [...row]);
  currentBoard = data.board;
  validMoves = data.validMoves;
  isMyTurn = data.isPlayer1Turn === isFirstTurn;

  await updateBoardWithAnimation(oldBoard, data.board);
  updateScores(data);

  if (data.lastMove) {
    showLastMove(data.lastMove);
  }

  if (data.status === 'FINISHED') {
    clearInterval(timer);
    handleGameEnd(data);
    return;
  }

  clearInterval(timer);

  if (isMyTurn) {
    showValidMoves(validMoves);
  } else {
    hideValidMoves();
  }

  if (isConnected) {
    startTimer();
  } else {
    pendingTimerStart = true;
  }
}

function updateBoardWithAnimation(oldBoard, newBoard) {
  const changes = findBoardChanges(oldBoard, newBoard);

  if (changes.length === 0) {
    resolve();
    return;
  }

  const animationPromises = changes.map(change => {
    return new Promise(resolve => {
      const cell = $(`.board-cell[data-row="${change.row}"][data-col="${change.col}"]`);
      const piece = cell.find('.piece');

      if (change.type === 'add') {
        piece.removeClass('black white')
          .addClass(change.newValue === 1 ? 'black' : 'white')
          .addClass('placing');

        setTimeout(() => {
          piece.removeClass('placing');
          resolve();
        }, 300);
      } else if (change.type === 'flip') {
        piece.addClass('flipping')
          .removeClass(change.oldValue === 1 ? 'black' : 'white')
          .addClass(change.newValue === 1 ? 'black' : 'white');

        setTimeout(() => {
          piece.removeClass('flipping');
          resolve();
        }, 500);
      }
    });
  });

  return Promise.all(animationPromises);
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

function startTimer() {
  clearInterval(timer);
  updateTimerDisplay();

  timer = setInterval(() => {
    remainingTime--;
    updateTimerDisplay();

    if (remainingTime <= 0) {
      clearInterval(timer);
      if (isMyTurn) {
        handleTimeout();
      }
    }
  }, 1000);
}

function updateTimerDisplay() {
  $('.timer-display .timer').text(remainingTime);

  if (isMyTurn) {
    if (remainingTime <= 10) {
      $('.timer-display .timer').addClass('urgent');
    } else {
      $('.timer-display .timer').removeClass('urgent');
    }
  } else {
    $('.timer-display .timer').removeClass('urgent');
  }
}

function updateScores(gameInfo) {
  $('.player-score:first .score').text(gameInfo.player1Score);
  $('.player-score:last .score').text(gameInfo.player2Score);

  const currentPlayerNickname = gameInfo.isPlayer1Turn ?
    gameInfo.player1Nickname : gameInfo.player2Nickname;

  $('.turn-indicator')
    .removeClass('player1-turn player2-turn')
    .addClass(gameInfo.isPlayer1Turn ? 'player1-turn' : 'player2-turn')
    .find('span')
    .text(`${currentPlayerNickname}님의 턴`);
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
  if (!isMyTurn || !moves) return;

  moves.forEach(move => {
    $(`.board-cell[data-row="${move.row}"][data-col="${move.col}"]`)
      .addClass('valid-move')
      .append('<div class="valid-move-indicator"></div>');
  });
}

function hideValidMoves() {
  $('.board-cell').removeClass('valid-move')
  $('.valid-move-indicator').remove();
}

function isValidMove(row, col) {
  return validMoves.some(move => move.row === row && move.col === col);
}

function initializeChat() {
  $('.quick-chat-btn').on('click', function (e) {
    e.preventDefault();
    const message = $(this).data('message');
    sendChatMessage(message);
  });

  $('#sendMessage').on('click', function (e) {
    e.preventDefault();
    sendInputMessage();
  });

  $('#messageInput').on('keypress', function (e) {
    if (e.which === 13) {
      e.preventDefault();
      sendInputMessage();
    }
  });
}

function sendInputMessage() {
  const message = $('#messageInput').val();
  if (message) {
    sendChatMessage(message);
    $('#messageInput').val('');
  }
}

async function sendChatMessage(message) {
  if (!stompClient || !stompClient.connected) return;

  const sendButton = $('#sendMessage');
  const messageInput = $('#messageInput');
  const quickChatBtns = $('.quick-chat-btn');

  sendButton.prop('disabled', true);
  messageInput.prop('disabled', true);
  quickChatBtns.prop('disabled', true);

  try {
    await stompClient.send('/app/game/chat', {}, JSON.stringify({
      gameId: gameId,
      senderId: gameInfo.myId,
      senderNickname: gameInfo.myNickname,
      message: message,
      type: 'NORMAL',
      sentAt: new Date().toISOString()
    }));
  } catch (error) {
    console.error('채팅 메시지 전송 실패 : ', error);
  } finally {
    sendButton.prop('disabled', false);
    messageInput.prop('disabled', false);
    quickChatBtns.prop('disabled', false);
  }
}

function addChatMessage(data) {
  const chatMessages = $('#chatMessages');
  let messageHtml = '';

  if (data.type === 'SYSTEM') {
    messageHtml = `<div class="chat-message system">${data.message}</div>`;
  } else {
    const isMyMessage = data.senderId === gameInfo.myId;
    messageHtml = `
      <div class="chat-message ${isMyMessage ? 'my-message' : 'opponent-message'}">
        <span class="sender">${data.senderNickname}</span>
        <span class="message">${data.message}</span>
      </div>
    `;
  }

  chatMessages.append(messageHtml);
  chatMessages.scrollTop(chatMessages[0].scrollHeight);
}

function handleGameMessage(data) {
  switch (data.type) {
    case 'MOVE':
      clearInterval(timer);
      remainingTime = data.remainingTime || timeLimit;

      const oldBoard = currentBoard.map(row => [...row]);
      currentBoard = data.board;
      validMoves = data.validMoves;
      isMyTurn = data.isPlayer1Turn === isFirstTurn;

      updateBoardWithAnimation(oldBoard, data.board).then(() => {
        updateTimerDisplay();
        updateScores(data);

        if (data.lastMove) {
          showLastMove(data.lastMove);
        }

        if (isMyTurn) {
          showValidMoves(validMoves);
        } else {
          hideValidMoves();
        }

        startTimer();

        if (data.skipMessage) {
          addChatMessage({
            type: 'SYSTEM',
            message: data.skipMessage
          });
        }
      });
      break;
    case 'SURRENDER':
      handleGameEnd(data);
      break;
    case 'CHAT':
      addChatMessage({
        type: data.chatType,
        gameId: data.gameId,
        senderId: data.senderId,
        senderNickname: data.senderNickname,
        message: data.message,
        sentAt: data.sentAt
      });
      break;
    case 'TIMEOUT':
      handleTimeout(data);
      break;
    case 'GAME_RESULT':
      handleGameEnd(data);
      break;
  }
}

async function handleSurrender() {
  stompClient.send('/app/game/surrender', {}, JSON.stringify({
    gameId: gameId,
    playerId: gameInfo.myId
  }));
}

async function handleTimeout() {
  if (!isMyTurn) return;

  try {
    stompClient.send('/app/game/timeout', {}, JSON.stringify({
      gameId: gameId,
      playerId: gameInfo.myId
    }));
  } catch (error) {
    console.error('타임아웃 처리 실패 : ', error);
  }
}

function disableGameControls() {
  $('.board-cell').addClass('disabled');
  $('#surrender-btn').prop('disabled', true);
  $('#messageInput').prop('disabled', true);
  $('#sendMessage').prop('disabled', true);
  $('.quick-chat-btn').prop('disabled', true);
}

async function handleGameEnd(data) {
  clearInterval(timer);
  disableGameControls();
  await showGameResult(data);
}

async function showGameResult(data) {
  await Swal.fire({
    html: generateInitialResultHtml(data),
    confirmButtonText: '다음',
    allowOutsideClick: false
  });

  const ratingData = {
    isWinner: data.winner === gameInfo.myNickname,
    startRating: data.winner === gameInfo.myNickname ? data.winnerOldRating : data.loserOldRating,
    ratingChange: data.winner === gameInfo.myNickname ? data.ratingChange : -data.ratingChange,
    oldTier: data.winner === gameInfo.myNickname ? data.winnerOldTier : data.loserOldTier,
    newTier: data.winner === gameInfo.myNickname ? data.winnerNewTier : data.loserNewTier
  };
  ratingData.endRating = ratingData.startRating + ratingData.ratingChange;

  await Swal.fire({
    title: '레이팅 변동',
    html: generateRatingAnimationHtml(),
    confirmButtonText: '로비로 이동',
    allowOutsideClick: false,
    didOpen: (modalElement) => {
      startRatingAnimation(modalElement, ratingData);
    }
  });

  window.location.href = '/lobby';
}

function generateInitialResultHtml(data) {
  const resultMessage = data.type === 'SURRENDER' ? 
        `게임 종료!<br>${data.loser}님의 기권으로 ${data.winner}님의 승리` :
        `게임 종료!<br>${data.winner}님의 승리`;

  let blackCount = 0;
  let whiteCount = 0;
  currentBoard.forEach(row => {
    row.forEach(cell => {
      if (cell === 1) blackCount++;
      if (cell === 2) whiteCount++;
    });
  });

  return `
    <div class="game-result">
      <div class="result-message">${resultMessage}</div>
      <div class="result-scores">
        <div class="player-result ${data.winner === gameInfo.player1Nickname ? 'winner' : ''}">
          <div class="player-result-name">${gameInfo.player1Nickname}</div>
          <div class="player-result-score">${blackCount}개</div>
        </div>
        <div class="vs">VS</div>
        <div class="player-result ${data.winner === gameInfo.player2Nickname ? 'winner' : ''}">
          <div class="player-result-name">${gameInfo.player2Nickname}</div>
          <div class="player-result-score">${whiteCount}개</div>
        </div>
      </div>
    </div>
  `;
}

function generateRatingAnimationHtml() {
  return `
    <div class="rating-animation">
      <div class="rating-change"></div>
      <div class="rating-bar-container">
        <div class="rating-bar"></div>
        <div class="rating-marker"></div>
        <div class="tier-range"></div>
      </div>
      <div class="tier-change" style="display: none;"></div>
    </div>
  `;
}

function startRatingAnimation(modalElement, ratingData) {
  const elements = {
    ratingDiv: modalElement.querySelector('.rating-change'),
    ratingBar: modalElement.querySelector('.rating-bar'),
    ratingMarker: modalElement.querySelector('.rating-marker'),
    tierRange: modalElement.querySelector('.tier-range'),
    tierDiv: modalElement.querySelector('.tier-change')
  };
  const tierOrder = [
    'SILVER5', 'SILVER4', 'SILVER3', 'SILVER2', 'SILVER1', 
    'GOLD5', 'GOLD4', 'GOLD3', 'GOLD2', 'GOLD1',
    'PLATINUM5', 'PLATINUM4', 'PLATINUM3', 'PLATINUM2', 'PLATINUM1'
  ];

  initializeRatingAnimation(elements, ratingData);

  animate(elements, ratingData, tierOrder);
}

function initializeRatingAnimation(elements, ratingData) {
  const { ratingDiv, ratingBar, ratingMarker, tierRange } = elements;
  const { startRating, ratingChange } = ratingData;

  ratingDiv.textContent = `${startRating} (${ratingChange >= 0 ? '+' : ''}${ratingChange}) → ${startRating}`;
  
  const initialPercentage = calculatePercentage(startRating);
  ratingBar.style.width = `${initialPercentage}%`;
  ratingMarker.style.left = `${initialPercentage}%`;
  ratingMarker.textContent = startRating;

  updateTierRange(tierRange, ratingData.oldTier, startRating);
}

function calculatePercentage(rating) {
  const baseRating = Math.floor(rating / 200) * 200;
  return ((rating - baseRating) / 199) * 100;
}

function updateTierRange(tierRange, tier, rating) {
  const minRating = Math.floor(rating / 200) * 200;
  const maxRating = minRating + 199;
  tierRange.innerHTML = `
    <span class="min-rating">${minRating}</span>
    <span class="tier-name">${tier}</span>
    <span class="max-rating">${maxRating}</span>
  `;
}

function animate(elements, ratingData, tierOrder) {
  let currentRating = ratingData.startRating;
  let currentTier = ratingData.oldTier;
  const startTime = performance.now();
  const duration = 1500;

  const animateFrame = async (currentTime) => {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    
    const easeProgress = progress < 0.5
      ? 4 * progress * progress * progress
      : 1 - Math.pow(-2 * progress + 2, 3) / 2;

    currentRating = ratingData.startRating + 
      ((ratingData.endRating - ratingData.startRating) * easeProgress);
    
    if (currentTier === 'SILVER5' && currentRating < 1000) {
      currentRating = 1000;
    }
    
    let percentage = calculatePercentage(currentRating);
    const currentTierIndex = tierOrder.indexOf(currentTier);
    const newTierIndex = tierOrder.indexOf(ratingData.newTier);

    if (percentage >= 100 && currentTierIndex < newTierIndex) {
      currentRating = await animateTierTransition(elements, currentRating, currentTier, 
        tierOrder[currentTierIndex + 1], true);
      currentTier = tierOrder[currentTierIndex + 1];
      percentage = 0;
    } else if (percentage <= 0 && currentTierIndex > newTierIndex) {
      currentRating = await animateTierTransition(elements, currentRating, currentTier, 
        tierOrder[currentTierIndex - 1], false);
      currentTier = tierOrder[currentTierIndex - 1];
      percentage = 100;
    }

    updateRatingDisplay(elements, currentRating, percentage, ratingData);

    if (shouldContinueAnimation(currentTier, ratingData.newTier, progress)) {
      requestAnimationFrame(animateFrame);
    } else {
      showTierChangeEffect(elements, ratingData.oldTier, ratingData.newTier, tierOrder);
    }
  };

  requestAnimationFrame(animateFrame);
}

async function animateTierTransition(elements, currentRating, currentTier, nextTier, isUpgrade) {
  const { ratingBar, ratingMarker, tierRange } = elements;
  const startPercentage = calculatePercentage(currentRating);
  const targetPercentage = isUpgrade ? 100 : 0;
  
  await animateFill(ratingBar, ratingMarker, startPercentage, targetPercentage);

  if (isUpgrade) {
    ratingBar.classList.add('full');
    await new Promise(resolve => setTimeout(resolve, 1000));
    ratingBar.classList.remove('full');
  } else {
    await new Promise(resolve => setTimeout(resolve, 500));
  }

  const newMinRating = Math.floor(currentRating / 200) * 200 + (isUpgrade ? 200 : 0);
  updateTierRange(tierRange, nextTier, newMinRating);

  ratingBar.style.width = isUpgrade ? '0%' : '100%';
  ratingMarker.style.left = isUpgrade ? '0%' : '100%';
  ratingMarker.textContent = newMinRating;
  
  await new Promise(resolve => setTimeout(resolve, 100));
  
  return newMinRating;
}

async function animateFill(ratingBar, ratingMarker, startPercentage, targetPercentage) {
  return new Promise(resolve => {
    const fillDuration = 500;
    const fillStartTime = performance.now();
    
    const animate = (currentTime) => {
      const elapsed = currentTime - fillStartTime;
      const progress = Math.min(elapsed / fillDuration, 1);
      const currentPercentage = startPercentage + 
        (targetPercentage - startPercentage) * progress;
      
      ratingBar.style.width = `${currentPercentage}%`;
      ratingMarker.style.left = `${currentPercentage}%`;
      
      if (progress < 1) {
        requestAnimationFrame(animate);
      } else {
        resolve();
      }
    };
    
    requestAnimationFrame(animate);
  });
}

function updateRatingDisplay(elements, currentRating, percentage, ratingData) {
  const { ratingDiv, ratingBar, ratingMarker } = elements;
  
  ratingBar.style.width = `${percentage}%`;
  ratingMarker.style.left = `${percentage}%`;
  ratingMarker.textContent = Math.round(currentRating);
  ratingDiv.textContent = `${ratingData.startRating} (${ratingData.ratingChange >= 0 ? '+' : ''}${ratingData.ratingChange}) → ${Math.round(currentRating)}`;
}

function shouldContinueAnimation(currentTier, targetTier, progress) {
  return currentTier !== targetTier || progress < 1;
}

function showTierChangeEffect(elements, oldTier, newTier, tierOrder) {
  const { tierDiv } = elements;
  if (oldTier === newTier) return;

  tierDiv.style.display = 'block';
  const isPromotion = tierOrder.indexOf(oldTier) < tierOrder.indexOf(newTier);
  
  tierDiv.innerHTML = isPromotion ? `
    <div class="tier-promotion">
      <div class="promotion-effect">승급!</div>
      <div class="new-tier">${newTier}</div>
    </div>
  ` : `
    <div class="tier-demotion">
      <div class="demotion-effect">강등</div>
      <div class="new-tier">${newTier}</div>
    </div>
  `;
}
