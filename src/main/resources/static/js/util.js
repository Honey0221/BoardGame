const ModalUtil = {
  initializeModal: function(modalId) {
    const $modal = $(`#${modalId}`);
    if (!$modal) {
      return null;
    }

    // 모달 열기
    const openModal = function() {
      $modal.show();
    };

    // 모달 닫기
    const closeModal = function() {
      $modal.hide();
    };

    // 닫기 버튼 이벤트
    $modal.find('.close').on('click', function() {
      closeModal();
    });

    return {
      open: openModal,
      close: closeModal
    };
  }
};

const AlertUtil = {
  showSuccess: function(message) {
    return Swal.fire({
      icon: 'success',
      title: message,
      confirmButtonColor: '#3085d6'
    });
  },

  showInfo: function(message) {
    return Swal.fire({
      icon: 'info',
      title: message,
      confirmButtonColor: '#3085d6'
    });
  },

  showError: function(message) {
    return Swal.fire({
      icon: 'error',
      title: message,
      confirmButtonColor: '#d33'
    });
  },

  showWarning: function(message) {
    return Swal.fire({
      icon: 'warning',
      title: message,
      confirmButtonColor: '#f8bb86'
    });
  },

  showConfirm: function(message) {
    return Swal.fire({
      icon: 'question',
      title: message,
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: '확인',
      cancelButtonText: '취소'
    });
  },

  showAlert: function(message) {
    return Swal.fire({
      text: message,
      confirmButtonColor: '#3085d6'
    });
  }
};

const AjaxUtil = {
  async call(url, method = 'GET', data = null, showLoading = true) {
    try {
      if (showLoading) {
        Swal.fire({
          title: '처리중...',
          allowOutsideClick: false,
          didOpen: () => {
            Swal.showLoading();
          }
        });
      }

      const csrfToken = document.querySelector('meta[name="_csrf"]').content;
      const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

      const options = {
        method: method,
        headers: {
          'Content-Type': 'application/json',
          [csrfHeader]: csrfToken
        },
      };

      if (data) {
        options.body = JSON.stringify(data);
      }

      const response = await fetch(url, options);

      const contentType = response.headers.get('content-type');
      let responseData;

      if (contentType && contentType.includes('application/json')) {
        responseData = await response.json();
      } else {
        responseData = { success: response.ok };
      }

      if (response.ok) {
        return responseData;
      } else {
        throw new Error('요청이 실패했습니다.');
      }
    } catch (error) {
      console.error('API 호출 실패 ', error);
      throw error;
    } finally {
      if (showLoading) {
        Swal.close();
      }
    }
  },
  async get(url, params = null, showLoading = true) {
    if (params) {
      const queryString = new URLSearchParams(params).toString();
      url = `${url}?${queryString}`;
    }
    return this.call(url, 'GET', null, showLoading);
  },
  async post(url, data, showLoading = true) {
    return this.call(url, 'POST', data, showLoading);
  },
  async put(url, data, showLoading = true) {
    return this.call(url, 'PUT', data, showLoading);
  },
  async delete(url, showLoading = true) {
    return this.call(url, 'DELETE', null, showLoading);
  },
  showLoading() {
    return Swal.fire({
      title: '처리중...',
      allowOutsideClick: false,
      showConfirmButton: false,
      willOpen: () => {
        Swal.showLoading();
      }
    });
  }
};

const VerificationUtil = {
  async sendVerificationCode(phone, type = 'register') {
    try {
      if (!this.validatePhone(phone)) {
        return false;
      }

      const response = await AjaxUtil.post('/api/verification/send', {
        phone,
        type
      });

      if (response.success) {
        await AlertUtil.showSuccess('인증번호가 발송되었습니다');
        return true;
      }
      return false;
    } catch (error) {
      await AlertUtil.showError('인증번호 발송이 실패하였습니다');
      console.error('인증번호 발송 실패 ', error);
      return false;
    }
  },
  async verifyCode(phone, code, type = 'register') {
    try {
      if (!code) {
        await AlertUtil.showError('인증번호를 입력해주세요');
        return false;
      }

      const response = await AjaxUtil.post('/api/verification/verify', {
        phone,
        code,
        type
      });

      if (response.success) {
        await AlertUtil.showSuccess('인증이 완료되었습니다');
        return true;
      }
      return false;
    } catch (error) {
      await AlertUtil.showError('인증이 실패하였습니다');
      console.error('인증 실패 ', error);
      return false;
    }
  },

  validatePhone(phone) {
    if (!phone) {
      AlertUtil.showError('휴대폰 번호를 입력해주세요.');
      return false;
    }
    if (!/^01[016789]\d{7,8}$/.test(phone)) {
      AlertUtil.showError('올바른 휴대폰 번호를 입력해주세요.');
      return false;
    }
    return true;
  },

  // 인증 입력 비활성화
  disableVerificationInputs($phoneInput, $codeInput, $sendBtn, $verifyBtn) {
    $phoneInput.prop('disabled', true);
    $codeInput.prop('disabled', true);
    $sendBtn.prop('disabled', true);
    $verifyBtn.prop('disabled', true);
  }
};