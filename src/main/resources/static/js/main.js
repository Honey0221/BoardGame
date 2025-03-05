$(document).ready(function() {
  initializeTabManagement();
  initializeLoginForm();
  initializeRegisterForm();
  initializeModals();
});

// 탭 활성 관리
function initializeTabManagement() {
  $('.nav-item').click(function(e) {
    e.preventDefault();

    $('.nav-item.active').removeClass('active');
    $(this).addClass('active');

    const tabId = $(this).data('tab');
    $('.tab-content.active').removeClass('active');
    $(`#${tabId}-form`).addClass('active');
  });
}

// 로그인 폼 초기화
function initializeLoginForm() {
  const $loginForm = $('#login-form');
  const $username = $('#username');
  const $password = $('#password');

  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.has('error')) {
    AlertUtil.showError('아이디 또는 비밀번호가 일치하지 않습니다.');
  }

  $loginForm.submit(async function(e) {
    if (!$username.val() || !$password.val()) {
      e.preventDefault();
      await AlertUtil.showError('아이디와 비밀번호를 입력해주세요.');
      return;
    }
  });
}

// 회원가입 폼 초기화
function initializeRegisterForm() {
  const $registerForm = $('#register-form');
  let isIdVerified = false;
  let isPhoneVerified = false;
  
  // ID 중복 확인
  $('#checkId').click(async function() {
    const id = $('#registerId').val();
    if (!validateId(id)) {
      return;
    }

    try {
      const response = await AjaxUtil.get('/api/member/check-id', { id });

      if (response.success) {
        isIdVerified = true;
        await AlertUtil.showSuccess('사용 가능한 아이디입니다.');
      }
    } catch (error) {
      isIdVerified = false;
      await AlertUtil.showError('이미 사용중인 아이디입니다.');
      console.error('ID 중복 확인 실패 ', error);
    }
  });

  // 비밀번호 입력 시 강도 확인
  $('#newPassword').on('input', function() {
    const password = $(this).val();
    updatePasswordStrength(password);

    const confirmPassword = $('#confirmPassword').val();
    if (confirmPassword) {
      checkPasswordMatch(password, confirmPassword);
    }
  });

  // 비밀번호 일치 여부 확인
  $('#confirmPassword').on('input', function() {
    const password = $('#newPassword').val();
    const confirmPassword = $(this).val();
    checkPasswordMatch(password, confirmPassword);
  });

  // 휴대폰 인증번호 발송
  $('#verifyPhone').click(async function() {
    const phone = $('#phone').val();
    if (validatePhone(phone)) {
      await VerificationUtil.sendVerificationCode(phone, 'register');
    }
  });

  // 인증번호 확인
  $('#verifyCode').click(async function() {
    const phone = $('#phone').val();
    const code = $('#verificationCode').val();

    if (await VerificationUtil.verifyCode(phone, code, 'register')) {
      isPhoneVerified = true;
      VerificationUtil.disableVerificationInputs(
        $('#phone'),
        $('#verificationCode'),
        $('#verifyPhone'),
        $('#verifyCode')
      );
    }
  });
  
  // 회원가입 폼 제출
  $registerForm.submit(async function(e) {
    e.preventDefault();
    
    if (!validateRegisterForm(isIdVerified, isPhoneVerified)) {
      return;
    }

    const result = await AlertUtil.showConfirm('회원가입을 진행하시겠습니까?');
    if (result.isConfirmed) {
      try {
        const response = await AjaxUtil.post('/api/member/register', {
          id: $('#registerId').val(),
          nickname: $('#nickname').val(),
          password: $('#newPassword').val(),
          phone: $('#phone').val(),
        });           
        
        if (response.success) {
          await AlertUtil.showSuccess('회원가입이 완료되었습니다.');
          window.location.href = '/';
        }
      } catch (error) {
        console.error('회원가입 실패 ', error);
      }
    }
  });
}

// 아이디, 비밀번호 찾기 모달
function initializeModals() {
  // 아이디 찾기 모달
  const findIdModal = ModalUtil.initializeModal('findIdModal');
  let isFindIdPhoneVerified = false;

  $('#findId').click(function(e) {
    e.preventDefault();
    findIdModal.open();
  });

  // 아이디 찾기 인증번호 발송
  $('#findIdVerifyPhone').click(async function() {
    const phone = $('#findIdPhone').val();
    if (validatePhone(phone)) {
      await VerificationUtil.sendVerificationCode(phone, 'findId');
    }
  });

  // 아이디 찾기 인증번호 확인
  $('#findIdVerifyCode').click(async function() {
    const phone = $('#findIdPhone').val();
    const code = $('#findIdVerificationCode').val();
    
    if (await VerificationUtil.verifyCode(phone, code, 'findId')) {
      isFindIdPhoneVerified = true;
      VerificationUtil.disableVerificationInputs(
        $('#findIdPhone'),
        $('#findIdVerificationCode'),
        $('#findIdVerifyPhone'),
        $('#findIdVerifyCode')
      );
    }
  });

  // 아이디 찾기 폼 제출
  $('#findIdModal form').submit(async function(e) {
    e.preventDefault();

    const phone = $('#findIdPhone').val();

    if (!validatePhone(phone) || !isFindIdPhoneVerified) {
      await AlertUtil.showError('휴대폰 인증을 완료해주세요.');
      return;
    }

    try {
      const response = await AjaxUtil.post('/api/member/find-id', { phone });
      if (response.success) {
        await AlertUtil.showInfo(`고객님의 아이디는 ${response.memberId}입니다.`);
        findIdModal.close();
        isFindIdPhoneVerified = false;
        $('#findIdPhone').val('');
        $('#findIdVerificationCode').val('');
      }
    } catch (error) {
      await AlertUtil.showError('가입한 아이디가 없습니다.');
      console.error('아이디 찾기 실패 ', error);
    }
  });
  
  // 비밀번호 찾기 모달
  const findPwModal = ModalUtil.initializeModal('findPwModal');
  let isFindPwPhoneVerified = false;

  $('#findPw').click(function(e) {
    e.preventDefault();
    findPwModal.open();
  });

  // 비밀번호 찾기 인증번호 발송
  $('#findPwVerifyPhone').click(async function() {
    const phone = $('#findPwPhone').val();
    if (validatePhone(phone)) {
      await VerificationUtil.sendVerificationCode(phone, 'findPw');
    }
  });
  
  // 비밀번호 찾기 인증번호 확인
  $('#findPwVerifyCode').click(async function() {
    const phone = $('#findPwPhone').val();
    const code = $('#findPwVerificationCode').val();

    if (await VerificationUtil.verifyCode(phone, code, 'findPw')) {
      isFindPwPhoneVerified = true;
      VerificationUtil.disableVerificationInputs(
        $('#findPwPhone'),
        $('#findPwVerificationCode'),
        $('#findPwVerifyPhone'),
        $('#findPwVerifyCode')
      );
    }
  });

  // 비밀번호 찾기 폼 제출
  $('#findPwModal form').submit(async function(e) {
    e.preventDefault();

    const id = $('#findPwId').val();
    const phone = $('#findPwPhone').val();

    if (!validateId(id) || !validatePhone(phone)) {
      return;
    }

    if (!isFindPwPhoneVerified) {
      await AlertUtil.showError('휴대폰 인증을 완료해주세요.');
      return;
    }

    try {
      const response = await AjaxUtil.post('/api/member/find-pw', {
        id: id,
        phone: phone,
      });

      if (response.success) {
        await AlertUtil.showSuccess('임시 비밀번호가 발송되었습니다.');
        findPwModal.close();
        isFindPwPhoneVerified = false;
        $('#findPwId').val('');
        $('#findPwPhone').val('');
        $('#findPwVerificationCode').val('');
      }
    } catch (error) {
      await AlertUtil.showError('아이디와 휴대폰 번호가 일치하지 않습니다.');
      console.error('비밀번호 찾기 실패 ', error);
    }
  });
}

// 유효성 검사 함수
function validateId(id) {
  if (!id) {
    AlertUtil.showError('아이디를 입력해주세요.');
    return false;
  }
  if (!/^[a-zA-Z0-9]{4,20}$/.test(id)) {
    AlertUtil.showError('아이디는 4~20자의 영문자와 숫자만 사용 가능합니다.');
    return false;
  }
  return true;
}

function validatePassword(password) {
  if (!password) {
    AlertUtil.showError('비밀번호를 입력해주세요.');
    return false;
  }

  if (password.length < 8) {
    AlertUtil.showError('비밀번호는 최소 8자 이상 입력해주세요.');
    return;
  }
  return true;
}

function checkPasswordMatch(password, confirmPassword) {
  const $matchDiv = $('.password-match');

  if (!confirmPassword) {
    $matchDiv.text('');
    return false;
  }

  if (password === confirmPassword) {
    $matchDiv.text('비밀번호가 일치합니다.').css('color', 'green');
    return true;
  } else {
    $matchDiv.text('비밀번호가 일치하지 않습니다.').css('color', 'red');
    return false;
  }
}

function validateNickname(nickname) {
  if (!nickname) {
    AlertUtil.showError('닉네임을 입력해주세요.');
    return false;
  }
  if (!/^[a-zA-Z0-9가-힣]{2,10}$/.test(nickname)) {
    AlertUtil.showError('닉네임은 2~10자의 영문자, 숫자, 한글만 사용 가능합니다.');
    return false;
  }
  return true;
}

function validatePhone(phone) {
  if (!phone) {
    AlertUtil.showError('휴대폰 번호를 입력해주세요.');
    return false;
  }
  if (!/^01[016789]\d{7,8}$/.test(phone)) {
    AlertUtil.showError('올바른 휴대폰 번호를 입력해주세요.');
    return false;
  }
  return true;
}

function validateRegisterForm(isIdVerified, isPhoneVerified) {
  if (!isIdVerified) {
    AlertUtil.showError('아이디 중복 확인을 진행해주세요.');
    return false;
  }
  
  const nickname = $('#nickname').val();
  if (!validateNickname(nickname)) {
    return false;
  }

  const password = $('#newPassword').val();
  const confirmPassword = $('#confirmPassword').val();
  if (!validatePassword(password) || !checkPasswordMatch(password, confirmPassword)) {
    return false;
  }

  if (!isPhoneVerified) {
    AlertUtil.showError('휴대폰 인증을 완료해주세요.');
    return false;
  }

  return true;
}

function updatePasswordStrength(password) {
  const $strengthDiv = $('.password-strength');

  if (!password) {
    $strengthDiv.text('');
    return;
  }

  if (password.length < 8) {
    $strengthDiv.text('비밀번호는 최소 8자 이상이어야 합니다.').css('color', 'red');
    return;
  }

  const hasNumber = /\d/.test(password);
  const hasLower = /[a-z]/.test(password);
  const hasUpper = /[A-Z]/.test(password);
  const hasSpecial = /[@$!%*?&]/.test(password);

  let score = 0;
  if (password.length >= 12) score++;
  if (hasNumber) score++;
  if (hasLower && hasUpper) score++;
  if (hasSpecial) score++;

  const strengthText = {
    1: { text: '낮음', color: '#FF0000'},
    2: { text: '보통', color: '#FFA500'},
    3: { text: '높음', color: '#2E8B57'},
    4: { text: '강력', color: '#008000'},
  };

  const strength = strengthText[score] || strengthText[1];
  $strengthDiv.text(`비밀번호 강도: ${strength.text}`).css('color', strength.color);
}
