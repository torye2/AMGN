// /js/find-id.js

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

document.getElementById('findIdForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  showError('');
  if (!getCookie('XSRF-TOKEN')) await ensureCsrf();
  const xsrf = getCookie('XSRF-TOKEN');

  const userName = $('#name').value.trim();
  const birthYear = $('#birthYear').value.trim();
  const birthMonth = $('#birthMonth').value.trim();
  const birthDay = $('#birthDay').value.trim();
  const phoneNumber = $('#phone').value.trim();

  if(!userName || !birthYear || !birthMonth || !birthDay || !phoneNumber){
    showError('모든 값을 입력해 주세요.');
    return;
  }

  try {
    const resp = await fetch('/api/find-id', {
      method:'POST',
      headers: { 'Accept': 'application/json', 'X-XSRF-TOKEN': xsrf },
      body: JSON.stringify({ userName, birthYear: Number(birthYear), birthMonth: Number(birthMonth), birthDay: Number(birthDay), phoneNumber })
    });
    if(resp.ok){
      const data = await resp.json(); // { maskedUserId: 'ab****23' } or ApiResult wrapping
      const resultBox = document.getElementById('result');
      // ApiResult 대응: data.data?.maskedUserId 우선 시도
      const masked = (data && data.data && data.data.id) ? data.data.id : (data.id || '');
      resultBox.style.display='block';
      resultBox.textContent = '아이디: ' + (masked || '(없음)');
    } else if(resp.status === 404){
      showError('일치하는 회원 정보를 찾을 수 없습니다.');
    } else {
      showError('요청 처리 중 오류가 발생했습니다.');
    }
  } catch (e){
    showError('네트워크 오류가 발생했습니다.');
  }
});

async function ensureCsrf() {
  const r = await fetch('/api/csrf', {
    headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
    credentials: 'same-origin'
  });
  const j = await r.json(); // { headerName, token }
  return j; // 필요 시 헤더명도 동적으로 사용
}

function getCookie(name) {
  return document.cookie.split('; ').find(v => v.startsWith(name + '='))?.split('=')[1];
}