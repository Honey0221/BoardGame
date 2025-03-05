// 전역 변수
let stompClient; // 웹소켓 통신을 위한 STOMP 클라이언트
let gameId = gameInfo.gameId;
let isMyTurn = gameInfo.isMyTurn;
let isFirstTurn = gameInfo.isFirstTurn;
let timer; // 타이머 인터벌 ID
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

// 웹소켓 연결 초기화
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

// 게임 메시지 처리
function handleGameMessage(data) {
  switch (data.type) {
    case 'MOVE':
      handleMoveMessage(data);
      break;
    case 'SURRENDER':
      handleGameEnd(data);
      break;
    case 'CHAT':
      addChatMessage(data);
      break;
    case 'TIMEOUT':
      handleTimeout(data);
      break;
    case 'GAME_RESULT':
      handleGameEnd(data);
      break;
  }
}

// 착수 메시지 처리
function handleMoveMessage(data) {
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
}

// 게임 상태 업데이트
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
    if (validMoves && validMoves.length > 0) {
      showValidMoves(validMoves);
    } else {
      addChatMessage({
        type: 'SYSTEM',
        message: '유효한 이동이 없어 턴을 넘깁니다.'
      });

      setTimeout(() => {
        skipTurn();
      }, 1000);
    }
  } else {
    hideValidMoves();
  }

  if (isConnected) {
    startTimer();
  } else {
    pendingTimerStart = true;
  }
}

// 게임 점수 업데이트
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