// /js/find-password.js

(function initFillDays(){
  const daySel = document.getElementById('birthDay');
  function fillDays(){
    const y = parseInt(document.getElementById('birthYear').value || '2000', 10);
    const m = parseInt(document.getElementById('birthMonth').value || '1', 10);
    const last = new Date(y, m, 0).getDate();
    daySel.innerHTML = '<option value=\"\">일</option>';
    for(let d=1; d<=last; d++){
      const opt = document.createElement('option');
      opt.value = String(d);
      opt.textContent = String(d);
      daySel.appendChild(opt);
    }
  }
  document.getElementById('birthYear').addEventListener('input', fillDays);
  document.getElementById('birthMonth').addEventListener('change', fillDays);
  fillDays();
})();

const $ = (s)=>document.querySelector(s);
const showError = (msg)=>{ const el=$('#formError'); el.textContent=msg||''; };
let resetToken = null;

// 1단계: 본인 확인
document.getElementById('pwCheckForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  showError('');

  const id = $('#userId').value.trim();
  const userName = $('#name').value.trim();
  const birthYear = $('#birthYear').value.trim();
  const birthMonth = $('#birthMonth').value.trim();
  const birthDay = $('#birthDay').value.trim();
  const phoneNumber = $('#phone').value.trim();

  if(!id || !userName || !birthYear || !birthMonth || !birthDay || !phoneNumber){
    showError('모든 값을 입력해 주세요.');
    return;
  }

  try {
    const resp = await fetch('/api/pw-reset/check', {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({ id, userName, birthYear:Number(birthYear), birthMonth:Number(birthMonth), birthDay:Number(birthDay), phoneNumber })
    });
    if(resp.ok){
      const data = await resp.json(); // { resetToken } or ApiResult wrapping
      resetToken = (data && data.data && data.data.token) ? data.data.token : (data.token || null);
      if(!resetToken){
        showError('토큰 발급에 실패했습니다.');
        return;
      }
      document.getElementById('pwResetForm').style.display='block';
      // 첫 폼 비활성화(선택)
      // document.getElementById('pwCheckForm').querySelector('button[type=submit]').disabled = true;
    } else if(resp.status === 404){
      showError('일치하는 회원 정보를 찾을 수 없습니다.');
    } else {
      showError('요청 처리 중 오류가 발생했습니다.');
    }
  } catch(e){
    showError('네트워크 오류가 발생했습니다.');
  }
});

// 2단계: 비밀번호 변경
document.getElementById('pwResetForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  showError('');

  const pw1 = $('#pw1').value;
  const pw2 = $('#pw2').value;
  const pattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+{}\[\]:;"'<>,.?/~`-]).{8,}$/;

  if(pw1 && !pattern.test(pw1)) {
    showError("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야합니다.");
    return;
  }

  if(pw1 !== pw2){
    showError('비밀번호가 일치하지 않습니다.');
    return;
  }
  if(!resetToken){
    showError('본인 확인을 먼저 진행해 주세요.');
    return;
  }

  try {
    const resp = await fetch('/api/pw-reset/commit', {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({ token: resetToken, newPassword: pw1 })
    });
    if(resp.ok){
      alert('비밀번호가 변경되었습니다. 다시 로그인해 주세요.');
      location.href = '/login.html';
    } else {
      showError('변경 실패. 다시 시도해 주세요.');
    }
  } catch(e){
    showError('네트워크 오류가 발생했습니다.');
  }
});
