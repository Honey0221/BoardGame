// 채팅 기능 초기화
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

// 입력 메시지 전송
function sendInputMessage() {
  const message = $('#messageInput').val();
  if (message) {
    sendChatMessage(message);
    $('#messageInput').val('');
  }
}

// 채팅 메시지 전송
async function sendChatMessage(message) {
  if (!stompClient || !stompClient.connected) return;

  const sendButton = $('#sendMessage');
  const messageInput = $('#messageInput');
  const quickChatBtns = $('.quick-chat-btn');

  // 중복 전송 방지
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
  }

  setTimeout(() => {
    sendButton.prop('disabled', false);
    messageInput.prop('disabled', false);
    quickChatBtns.prop('disabled', false);
  }, 500);
}

// 채팅 메시지 추가
function addChatMessage(data) {
  const chatMessages = $('#chatMessages');
  let messageHtml = '';

  if (data.type === 'SYSTEM') {
    // 시스템 메시지
    messageHtml = `<div class="chat-message system">${data.message}</div>`;
  } else {
    // 일반 메시지 (내 메시지/상대방 메시지 구분)
    const isMyMessage = data.senderId === gameInfo.myId;
    messageHtml = `
      <div class="chat-message ${isMyMessage ? 'my-message' : 'opponent-message'}">
        <span class="sender">${data.senderNickname}</span>
        <span class="message">${data.message}</span>
      </div>
    `;
  }

  chatMessages.append(messageHtml);

  // 스크롤을 최하단으로 이동
  setTimeout(() => {
    chatMessages.scrollTop(chatMessages[0].scrollHeight);
  }, 0);
} 