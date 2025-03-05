let isMatching = false;
let matchingInterval;
let matchingStartTime;
let matchingTimeInterval;
let $matchingBtn;

function initializeMatchButton() {
  $matchingBtn = $('.matching-btn');

  $matchingBtn.on('click', async function(e) {
    e.preventDefault();

    if (!isMatching) {
      try {
        const response = await AjaxUtil.post('/match/start');
        if (response.success) {
          startMatching(response);
        }
      } catch (error) {
        console.error('매칭 시작 실패 : ', error);
      }
    } else {
      try {
        const response = await AjaxUtil.post('/match/cancel');
        if (response.success) {
          stopMatching();
        }
      } catch (error) {
        console.error('매칭 취소 실패 : ', error);
      }
    }
  });
}

function initializeMatchSubscription(stompClient) {
  stompClient.subscribe('/user/queue/match', function(message) {
    const matchResult = JSON.parse(message.body);
    if (matchResult.success) {
      handleMatchSuccess(matchResult);
    }
  });
}

function startMatching(response) {
  isMatching = true;
  matchingStartTime = new Date();

  $matchingBtn.text('매칭 취소').addClass('cancel');
  startMatchingAnimation(response);

  initializeMatchingIntervals();
}

function stopMatching() {
  isMatching = false;
  stopMatchingAnimation();
  $matchingBtn.text('매칭 시작').removeClass('cancel');
  clearMatchingIntervals();
}

function initializeMatchingIntervals() {
  matchingTimeInterval = setInterval(updateElapsedTime, 1000);

  matchingInterval = setInterval(() => {
    if (stompClient && stompClient.connected) {
      stompClient.send('/app/match/process', {}, JSON.stringify({
        type: 'MATCH_REQUEST'
      }));
    } else {
      clearInterval(matchingInterval);
    }
  }, 5000);
}

function clearMatchingIntervals() {
  if (matchingInterval) {
    clearInterval(matchingInterval);
    matchingInterval = null;
  }
  if (matchingTimeInterval) {
    clearInterval(matchingTimeInterval);
    matchingTimeInterval = null;
  }
}

function updateElapsedTime() {
  const currentTime = new Date();
  const elapsedTime = Math.floor((currentTime - matchingStartTime) / 1000);
  const minutes = Math.floor(elapsedTime / 60);
  const seconds = elapsedTime % 60;

  $('.matching-info .elapsed-time').text(
    `소요 시간: ${minutes}:${seconds.toString().padStart(2, '0')}`
  );
}

function startMatchingAnimation(matchData) {
  const $matchStatus = $('#matchStatus');
  const $statusContent = `
    <div class="loading-spinner"></div>
    <div class="matching-info">
      <p>매칭 중...</p>
      <p>현재 연승: ${matchData.winStreak}연승</p>
      <p>승률: ${matchData.winRate.toFixed(1)}%</p>
      <p class="elapsed-time">소요 시간: 00:00</p>
    </div>
  `;

  $matchStatus.html($statusContent);
  $matchStatus.show();
}

function stopMatchingAnimation() {
  const $matchStatus = $('#matchStatus');
  $matchStatus.hide();
  $matchStatus.empty();
}

function handleMatchSuccess(matchResult) {
  $matchingBtn.text('매칭 시작').removeClass('cancel');
  stopMatchingAnimation();
  clearMatchingIntervals();

  const myMemberId = $('#memberId').val();

  const gameId = matchResult.gameId;

  Swal.fire({
    title: '매칭 성공',
    html: generateMatchResultHTML(matchResult),
    icon: 'success',
    showConfirmButton: true,
    confirmButtonText: '게임 시작',
    allowOutsideClick: false,
    allowEscapeKey: false
  }).then(result => {
    if (result.isConfirmed) {
      // 게임 준비 상태 구독
      if (window.gameReadySubscription) {
        window.gameReadySubscription.unsubscribe();
      }

      window.gameReadySubscription = stompClient.subscribe(
        `/topic/game/${gameId}/ready`, 
        function(message) {
          const readyStatus = JSON.parse(message.body);
          if (readyStatus.allReady) {
            window.gameReadySubscription.unsubscribe();
            window.location.href = `/game/${gameId}`;
          }
        }
      );

      const readyData = {
        gameId: gameId,
        memberId: myMemberId
      };

      stompClient.send('/app/game/ready', {}, JSON.stringify(readyData));

      Swal.fire({
        title: '상대방 대기 중...',
        html: '<div class="loading-spinner"></div>',
        showConfirmButton: false,
        allowOutsideClick: false,
        allowEscapeKey: false,
        customClass: {
          popup: 'turn-selection-modal'
        }
      });
    }
  });
}

function generateMatchResultHTML(matchResult) {
  return `
    <div class="match-result">
      <div class="opponent-info">
        <p class="opponent-streak">
          ${matchResult.opponentStreak > 0 ? `
          <span class="streak-badge">${matchResult.opponentStreak}연승 중!</span>
          ` : '연승 없음'}
        </p>
        <p class="opponent-name">닉네임: ${matchResult.opponent}</p>
        <p class="opponent-rating">레이팅: ${matchResult.opponentTier} / ${matchResult.opponentRating}</p>
      </div>
      <div class="game-info">
        <p class="turn-into">당신은 ${matchResult.isFirstTurn ? '흑돌(선공)' : '백돌(후공)'}입니다.</p>
      </div>
    </div>
  `;
}