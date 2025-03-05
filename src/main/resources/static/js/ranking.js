$(document).ready(function() {
  initializeRanking();
});

function initializeRanking() {
  $('.ranking-btn').on('click', async function(e) {
    e.preventDefault();
    try {
      const response = await AjaxUtil.get('/lobby/ranking/top5');
      if (response.success) {
        showRankingModal(response.ratingRankings, response.winRateRankings);
      }
    } catch (error) {
      console.error('랭킹 조회 실패 : ', error);
    }
  });
}

function showRankingModal(ratingRankings, winRateRankings) {
  Swal.fire({
    title: '랭킹',
    html: generateRankingHtml(ratingRankings, winRateRankings),
    showConfirmButton: true,
    confirmButtonText: '닫기',
    confirmButtonColor: '#3085d6',
    customClass: {
      popup: 'ranking-modal',
      container: 'ranking-container'
    }
  });
}

function generateRankingHtml(ratingRankings, winRateRankings) {
  return `
    <div class="ranking-tabs">
      <button class="ranking-tab active" data-tab="rating">점수 랭킹</button>
      <button class="ranking-tab" data-tab="winrate">승률 랭킹</button>
    </div>
    <div class="ranking-content">
      <div id="rating-panel" class="ranking-panel active">
        ${generateRankingList(ratingRankings, 'rating')}
      </div>
      <div id="winrate-panel" class="ranking-panel">
        ${generateRankingList(winRateRankings, 'winrate')}
      </div>
    </div>
  `;
}

function generateRankingList(rankings, type) {
  const sortedRankings = [...rankings].sort((a, b) => {
    if (type === 'winrate') {
      const aWinRate = a.totalGames > 0 ? (a.wins * 100.0 / a.totalGames) : 0;
      const bWinRate = b.totalGames > 0 ? (b.wins * 100.0 / b.totalGames) : 0;
      
      if (Math.abs(aWinRate - bWinRate) < 0.001) {
        return b.ratingPoint - a.ratingPoint;
      }
      return bWinRate - aWinRate;
    }
    return b.ratingPoint - a.ratingPoint;
  });

  return `
    <div class="ranking-list">
      ${sortedRankings.map((user, index) => {
        const winRate = (user.totalGames > 0 
          ? Math.round((user.wins * 100.0 / user.totalGames)) : 0);

        return `
          <div class="ranking-card rank-${index + 1}">
            <div class="ranking-medal">${getRankingMedal(index + 1)}</div>
            <div class="ranking-info">
              <div class="ranking-nickname">${user.nickname}</div>
              <div class="ranking-tier">${user.tier}</div>
              ${type === 'rating' 
                ? `<div class="ranking-rating">${user.ratingPoint}점</div>` 
                : `<div class="ranking-winrate">승률 ${winRate}%</div>`
              }
            </div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

function getRankingMedal(rank) {
  const medals = {
    1: '🥇',
    2: '🥈',
    3: '🥉',
    4: '4',
    5: '5'
  };
  return medals[rank] || rank;
}

$(document).on('click', '.ranking-tab', function() {
  const tab = $(this).data('tab');
  $('.ranking-tab').removeClass('active');
  $(this).addClass('active');
  $('.ranking-panel').removeClass('active');
  $(`#${tab}-panel`).addClass('active');
});
