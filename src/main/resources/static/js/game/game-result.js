// 게임 종료 처리
async function handleGameEnd(data) {
  clearInterval(timer);
  disableGameControls();
  await showGameResult(data);
}

// 게임 결과 표시
async function showGameResult(data) {
  // 결과 초기창
  await Swal.fire({
    html: generateInitialResultHtml(data),
    confirmButtonText: '다음',
    allowOutsideClick: false
  });

  const isWinner = data.winner === gameInfo.myNickname;

  const ratingData = {
    isWinner: isWinner,
    startRating: isWinner ? data.winnerOldRating : data.loserOldRating,
    endRating: isWinner ? data.winnerNewRating : data.loserNewRating,
    ratingChange: isWinner ? data.ratingChange : -data.ratingChange,
    oldTier: isWinner ? data.winnerOldTier : data.loserOldTier,
    newTier: isWinner ? data.winnerNewTier : data.loserNewTier
  };

  // 레이팅 변화창
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

// 게임 결과창 HTML 생성
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

// 레이팅 애니메이션 HTML 생성
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

// 레이팅 애니메이션 시작
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

// 레이팅 애니메이션 초기화
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

// 레이팅에 따른 퍼센티지 계산
function calculatePercentage(rating) {
  const baseRating = Math.floor(rating / 200) * 200;
  return ((rating - baseRating) / 199) * 100;
}

// 티어 범위 표시 업데이트
function updateTierRange(tierRange, tier, rating) {
  const minRating = Math.floor(rating / 200) * 200;
  const maxRating = minRating + 199;
  tierRange.innerHTML = `
    <span class="min-rating">${minRating}</span>
    <span class="tier-name">${tier}</span>
    <span class="max-rating">${maxRating}</span>
  `;
}

// 레이팅 애니메이션 실행
function animate(elements, ratingData, tierOrder) {
  let currentRating = ratingData.startRating;
  let currentTier = ratingData.oldTier;
  const startTime = performance.now();
  const duration = 1500;

  const animateFrame = async (currentTime) => {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    
    // 이징 함수 (부드러운 애니메이션)
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
    } 
    else if (percentage <= 0 && currentTierIndex > newTierIndex) {
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

// 티어 전환 애니메이션 실행
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

// 바 채우기/비우기 애니메이션 실행
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

// 레이팅 표시 업데이트
function updateRatingDisplay(elements, currentRating, percentage, ratingData) {
  const { ratingDiv, ratingBar, ratingMarker } = elements;
  
  ratingBar.style.width = `${percentage}%`;
  ratingMarker.style.left = `${percentage}%`;
  ratingMarker.textContent = Math.round(currentRating);
  ratingDiv.textContent = `${ratingData.startRating} (${ratingData.ratingChange >= 0 ? '+' : ''}${ratingData.ratingChange}) → ${Math.round(currentRating)}`;
}

// 애니메이션을 계속 진행해야 하는지 확인
function shouldContinueAnimation(currentTier, targetTier, progress) {
  return currentTier !== targetTier || progress < 1;
}

// 티어 변경 효과 표시
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