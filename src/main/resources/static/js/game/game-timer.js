// 타이머 시작
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

// 타이머 표시 업데이트
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

// 시간 초과 처리
async function handleTimeout() {
  if (!isMyTurn) return;

  try {
    stompClient.send('/app/game/timeout', {}, JSON.stringify({
      gameId: gameId,
      playerId: gameInfo.myId
    }));
  } catch (error) {
    console.error('시간 초과 처리 실패 : ', error);
  }
}