function initializeAIGame() {
  $('.ai-btn').on('click', function() {
    showTurnSelectionModal();
  });
}

function showTurnSelectionModal() {
  const cards = [
    { order: 'FIRST', text: '선공' },
    { order: 'SECOND', text: '후공' }
  ].sort(() => Math.random() - 0.5);

  Swal.fire({
    title: '순서 선택',
    html: generateTurnSelectionHTML(cards),
    showConfirmButton: false,
    allowOutsideClick: true,
    showCloseButton: true,
    customClass: {
      popup: 'turn-selection-modal',
      closeButton: 'turn-selection-close-button',
      title: 'turn-selection-title'
    }
  });

  initializeTurnCardEvents();
}

function generateTurnSelectionHTML(cards) {
  return `
    <div class="turn-selection">
      <div class="turn-cards">
        ${cards.map((card) => `
          <div class="turn-card" data-order="${card.order}">
            <div class="card-inner">
              <div class="card-front">?</div>
              <div class="card-back">${card.text}</div>
            </div>
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

function initializeTurnCardEvents() {
  let isSelecting = false;

  $('.turn-card').on('click', async function(e) {
    e.preventDefault();

    if (isSelecting) return;
    isSelecting = true;

    const $card = $(this);
    const selectedOrder = $card.data('order');

    try {
      await handleTurnSelection($card, selectedOrder);
    } catch (error) {
      console.error('게임 생성 실패 : ', error);
      resetTurnSelection($card);
      isSelecting = false;
    }
  });
}

async function handleTurnSelection($card, selectedOrder) {
  $card.find('.card-inner').addClass('flipped');
  $('.turn-card').not($card).addClass('not-selected');

  await new Promise(resolve => setTimeout(resolve, 800));

  const response = await AjaxUtil.post('/game/ai/start', {
    order: selectedOrder
  });

  if (response.success) {
    window.location.href = `/game/ai/${response.gameId}`;
  }
}

function resetTurnSelection($card) {
  $card.find('.card-inner').removeClass('flipped');
  $('.turn-card').removeClass('not-selected');
}