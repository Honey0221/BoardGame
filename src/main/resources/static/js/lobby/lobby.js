window.stompClient = null;
window.memberNickname = null;

$(document).ready(function() {
  window.memberNickname = $('#memberNickname').val();
  window.stompClient = initializeSocket();

  initializeBanner();
  initializePageEvents();
  initializeBoardTabs();
  loadRecentPosts();
  initializeChat();
  updateTime();
  setInterval(updateTime, 60000);
  initializeMatchButton();
  initializeAIGame();
  initializeRanking();
  initializeLogout();
});

function initializeBanner() {
  const swiper = new Swiper('.banner-swiper', {
    loop: true,
    speed: 800,
    slidesPerView: 1,
    centeredSlides: true,
    effect: 'slide',
    autoplay: {
      delay: 3000,
      disableOnInteraction: false,
      pauseOnMouseEnter: true
    },
    pagination: {
      el: '.swiper-pagination',
      clickable: true
    }
  });

  return swiper;
}

function updateTime() {
  const timeElement = $('#currentTime');
  const now = new Date();
  const hours = now.getHours();
  const minutes = now.getMinutes();
  const ampm = hours >= 12 ? '오후' : '오전';
  let displayHours = hours % 12 || 12;
  const displayMinutes = minutes < 10 ? '0' + minutes : minutes;

  timeElement.text(`${ampm} ${displayHours}:${displayMinutes}`);
}

function initializeSocket() {
  const sock = new SockJS("/ws/lobby");
  const stompClient = Stomp.over(sock);
  stompClient.debug = null;

  stompClient.connect({}, function() {
    // 채팅 메시지 구독
    stompClient.subscribe('/topic/chat', function(message) {
      const data = JSON.parse(message.body);
      appendMessage(data);
    });

    // 유저 목록 구독
    stompClient.subscribe('/topic/users', function(message) {
      const data = JSON.parse(message.body);
      updateUserList(data.users);
      $('#userCount').text(data.count);
    });

    requestUserListUpdate();
    initializeMatchSubscription(stompClient);
  });

  return stompClient;
}

function initializePageEvents() {
  document.addEventListener('visibilitychange', function() {
    if (!document.hidden && stompClient && stompClient.readyState === SockJS.OPEN) {
      requestUserListUpdate();
    }
  });

  window.addEventListener('focus', function() {
    if (stompClient && stompClient.readyState === SockJS.OPEN) {
      requestUserListUpdate();
    }
  });
}

function requestUserListUpdate() {
  if (stompClient && stompClient.readyState === SockJS.OPEN) {
    stompClient.send('/app/users/update', {}, JSON.stringify({
      type: 'REQUEST_USERS_UPDATE'
    }));
  }
}

function initializeChat() {
  const $chatInput = $('#chatInput');
  const $sendButton = $('#sendMessage');

  // 메시지 전송
  function sendMessage() {
    const message = $chatInput.val();
    if (message) {
      stompClient.send('/app/chat', {}, JSON.stringify({
        type: 'NORMAL',
        senderNickname: memberNickname,
        message: message
      }));
      $chatInput.val('');
    }
  }

  $chatInput.on('keypress', function(e) {
    if (e.which === 13) {
      e.preventDefault();
      sendMessage();
    }
  });

  $sendButton.on('click', sendMessage);
}

function appendMessage(data) {
  if (!data || !data.message) return;

  let messageClass = 'system-message';
  let nicknameHtml = '';

  if (data.senderNickname) {
    messageClass = data.senderNickname === memberNickname ? 'my-message' : 'other-message';
    nicknameHtml = `<span class="message-nickname">${data.senderNickname}</span>`;
  }

  const messageDiv = $(`
    <div class="chat-message ${messageClass}">
      ${nicknameHtml}
      <span class="message-content">${data.message}</span>
    </div>
  `);
    
  const $chatMessages = $('#chatMessages');
  $chatMessages.append(messageDiv);
  $chatMessages.scrollTop($chatMessages[0].scrollHeight);
}

function updateUserList(users) {
  const userList = $('#userList');
  userList.empty();

  users.forEach(user => {
    const userItem = $(`
      <div class="user-item">
        <span class="user-nickname">${user.nickname}</span>
        <span class="user-status">${user.status}</span>
      </div>
    `);
    userList.append(userItem);
  });
}

function initializeLogout() {
  $('.logout-btn').on('click', async function(e) {
    e.preventDefault();

    const result = await AlertUtil.showConfirm('로그아웃 하시겠습니까?');
    if (result.isConfirmed) {
      try {
        const response = await AjaxUtil.post('/logout');
        if (response.success) {
          await AlertUtil.showSuccess('로그아웃 되었습니다.');
          window.location.href = '/';
        }
      } catch (error) {
        console.error('로그아웃 실패', error);
      }
    }
  });
}