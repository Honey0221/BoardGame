function initializeBoardTabs() {
  $('.tab-btn').on('click', function() {
    const tab = $(this).data('tab');
    $('.tab-btn').removeClass('active');
    $(this).addClass('active');
    $('.board-panel').removeClass('active');
    $(`#${tab}-panel`).addClass('active');
  });

  $('.tab-btn[data-tab="notice"]').addClass('active');
  $('#notice-panel').addClass('active');
}

async function loadRecentPosts() {
  try {
    const response = await AjaxUtil.get('/lobby/recent-posts');
    if (response.success) {
      updateBoardPanels(response);
    }
  } catch (error) {
    console.error('최근 게시글 로드 실패 : ', error);
  }
}

function updateBoardPanels(data) {
  updateBoardPanel('notice', data.recentNotices);
  updateBoardPanel('strategy', data.recentStrategies);
  updateBoardPanel('free', data.recentFrees);
}

function updateBoardPanel(type, posts) {
  const $panel = $(`#${type}-panel .board-list`);
  $panel.empty();
  
  posts.forEach(post => {
    const formattedDate = formatDate(post.createdAt);
    const $item = $(`
      <div class="board-item">
        <a href="/board/${type}/${post.postId}" class="board-title">${post.title}</a>
        <span class="board-date">${formattedDate}</span>
      </div>
    `);
    $panel.append($item);
  });
}

function formatDate(dateString) {
  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
}