// 컨트롤 버튼 이벤트 초기화
function initializeControls() {
  $('.surrender-btn').on('click', async function (e) {
    e.preventDefault();
    const result = await AlertUtil.showConfirm('기권하시겠습니까?');
    if (result.isConfirmed) {
      handleSurrender();
    }
  });
}

// 기권 처리
async function handleSurrender() {
  stompClient.send('/app/game/surrender', {}, JSON.stringify({
    gameId: gameId,
    playerId: gameInfo.myId
  }));
}

// 게임 컨트롤 비활성화
function disableGameControls() {
  $('.board-cell').addClass('disabled');
  $('.surrender-btn').prop('disabled', true);
  $('#messageInput').prop('disabled', true);
  $('#sendMessage').prop('disabled', true);
  $('.quick-chat-btn').prop('disabled', true);
} 