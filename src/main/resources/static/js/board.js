$(document).ready(function() {
  initializeBoard();
});

const BoardUtil = {
  async createPost(boardType, title, content) {
    try {
      const response = await AjaxUtil.post(`/board/${boardType}`, {
        title: title,
        content: content,
        boardType: boardType.toUpperCase()
      });

      if (response.success) {
        await AlertUtil.showSuccess('게시글이 등록되었습니다.');
        window.location.href = `/board/${boardType}`;
      }
    } catch (error) {
      console.error('게시글 등록 실패:', error);
    }
  },

  async updatePost(boardType, postId, title, content) {
    try {
      const response = await AjaxUtil.put(`/board/${boardType}/${postId}`, {
        postId: postId,
        title: title,
        boardType: boardType.toUpperCase(),
        content: content
      });

      if (response.success) {
        await AlertUtil.showSuccess('게시글이 수정되었습니다.');
        window.location.href = `/board/${boardType}/${postId}`;
      }
    } catch (error) {
      console.error('게시글 수정 실패:', error);
    }
  },

  async deletePost(boardType, postId) {
    try {
      const result = await AlertUtil.showConfirm('게시글을 삭제하시겠습니까?');
      if (result.isConfirmed) {
        const response = await AjaxUtil.delete(`/board/${boardType}/${postId}`);
        
        if (response.success) {
          await AlertUtil.showSuccess('게시글이 삭제되었습니다.');
          window.location.href = `/board/${boardType}`;
        }
      }
    } catch (error) {
      console.error('게시글 삭제 실패:', error);
    }
  }
};

function initializeBoard() {
  const path = window.location.pathname;

  if (path.includes('/write')) {
    initializeWriteForm();
  } else if (path.includes('/edit')) {
    initializeEditForm();
  } else {
    initializePostActions();
  }
}

function initializeWriteForm() {
  const $writeForm = $('#writeForm');
  if (!$writeForm.length) return;

  $writeForm.on('submit', async function(e) {
    e.preventDefault();
    
    const boardType = window.location.pathname.split('/')[2];
    const title = $('#title').val();
    const content = $('#content').val();

    if (validatePost(title, content)) {
      await BoardUtil.createPost(boardType, title, content);
    }
  });
}

function initializeEditForm() {
  const $editForm = $('#editForm');
  if (!$editForm.length) return;

  $editForm.on('submit', async function(e) {
    e.preventDefault();

    const pathParts = window.location.pathname.split('/');
    const boardType = pathParts[2];
    const postId = pathParts[3];
    const title = $('#title').val();
    const content = $('#content').val();

    if (validatePost(title, content)) {
      await BoardUtil.updatePost(boardType, postId, title, content);
    }
  });

  if ($editForm.length) {
    $('.back-btn').on('click', function() {
      const pathParts = window.location.pathname.split('/');
      const boardType = pathParts[2];
      const postId = pathParts[3];
      window.location.href = `/board/${boardType}/${postId}`;
    });
  }
}

function initializePostActions() {
  $('.back-btn').on('click', function() {
    const boardType = window.location.pathname.split('/')[2];
    window.location.href = `/board/${boardType}`;
  });

  $('.edit-btn').on('click', function() {
    const pathParts = window.location.pathname.split('/');
    const boardType = pathParts[2];
    const postId = pathParts[3];
    window.location.href = `/board/${boardType}/${postId}/edit`;
  });

  $('.delete-btn').on('click', async function() {
    const pathParts = window.location.pathname.split('/');
    const boardType = pathParts[2];
    const postId = pathParts[3];
    await BoardUtil.deletePost(boardType, postId);
  });
}

function validatePost(title, content) {
  if (!title.trim()) {
    AlertUtil.showWarning('제목을 입력해주세요.');
    return false;
  }
  if (!content.trim()) {
    AlertUtil.showWarning('내용을 입력해주세요.');
    return false;
  }
  if (title.length > 100) {
    AlertUtil.showWarning('제목은 100자를 초과할 수 없습니다.');
    return false;
  }
  return true;
}