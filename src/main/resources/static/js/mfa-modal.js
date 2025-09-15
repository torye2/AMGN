
(() => {
    // 1) 모달이 없으면 자동 생성 (idempotent)
    function ensureModal() {
        if (document.getElementById('mfa-modal')) return;
        const wrap = document.createElement('div');
        wrap.innerHTML = `
<div id="mfa-modal" class="mfa-modal hidden" aria-hidden="true">
  <div class="mfa-backdrop"></div>
  <div class="mfa-dialog" role="dialog" aria-modal="true" aria-labelledby="mfa-title">
    <h2 id="mfa-title">추가 인증</h2>
    <p class="mfa-desc">인증앱의 6자리 코드 또는 백업코드를 입력하세요.</p>
    <form id="mfa-form">
      <input type="text" id="mfa-code" inputmode="numeric" autocomplete="one-time-code"
             placeholder="6자리 코드 또는 백업코드" maxlength="16" required />
      <div class="mfa-actions">
        <button type="submit" id="mfa-submit">인증</button>
        <button type="button" id="mfa-cancel">취소</button>
      </div>
      <p id="mfa-error" class="mfa-error" role="alert" style="display:none;"></p>
    </form>
  </div>
</div>
<style>
.mfa-modal.hidden{display:none}
.mfa-modal{position:fixed;inset:0;z-index:9999}
.mfa-backdrop{position:absolute;inset:0;background:rgba(0,0,0,.35)}
.mfa-dialog{position:relative;margin:10vh auto 0;max-width:360px;background:#fff;border-radius:12px;padding:20px;box-shadow:0 10px 30px rgba(0,0,0,.2)}
#mfa-code{width:100%;font-size:18px;padding:10px 12px;border:1px solid #ddd;border-radius:8px}
.mfa-actions{display:flex;gap:8px;justify-content:flex-end;margin-top:12px}
.mfa-actions button{padding:8px 12px;border-radius:8px;border:0;cursor:pointer}
#mfa-submit{background:#111;color:#fff}
#mfa-cancel{background:#eee}
.mfa-error{color:#c00;margin-top:8px}
</style>`;
        document.body.appendChild(wrap);
    }

    // 2) 모달/검증 로직 + 401 재시도 래퍼
    let pendingRetry = null;
    function openModal() {
        ensureModal();
        const m = document.getElementById('mfa-modal');
        const input = document.getElementById('mfa-code');
        const err = document.getElementById('mfa-error');
        err.style.display='none'; err.textContent='';
        input.value='';
        m.classList.remove('hidden');
        m.setAttribute('aria-hidden','false');
        setTimeout(()=>input.focus(),0);
    }
    function closeModal() {
        const m = document.getElementById('mfa-modal');
        m.classList.add('hidden');
        m.setAttribute('aria-hidden','true');
    }

    async function verifyMfa(code) {
        const res = await fetch('/api/mfa/totp/verify', {
            method:'POST',
            headers:{'Content-Type':'application/json','X-Requested-With':'XMLHttpRequest'},
            body: JSON.stringify(code)
        });
        if (!res.ok) return false;
        return (await res.text()).trim().toUpperCase()==='OK';
    }

    // 이벤트 바인딩(한 번만)
    document.addEventListener('click', function initOnce(){
        ensureModal();
        const form = document.getElementById('mfa-form');
        const input= document.getElementById('mfa-code');
        const err  = document.getElementById('mfa-error');
        const cancel = document.getElementById('mfa-cancel');
        form.addEventListener('submit', async e=>{
            e.preventDefault();
            const code = input.value.trim();
            if (!code) return;
            const ok = await verifyMfa(code);
            if (!ok){ err.textContent='인증 실패. 코드를 확인하세요.'; err.style.display='block'; input.select(); return; }
            closeModal();
            if (pendingRetry){ try{ await pendingRetry(); } finally { pendingRetry=null; } }
            window.dispatchEvent(new CustomEvent('mfa:retried',{detail:{ok:true}}));
        }, {once:false});
        cancel.addEventListener('click', ()=>{ pendingRetry=null; closeModal(); window.dispatchEvent(new CustomEvent('mfa:cancelled')); });
        document.removeEventListener('click', initOnce);
    });

    async function fetchWithMfa(requestFn) {
        if (typeof requestFn !== 'function') {
            throw new Error('fetchWithMfa: requestFn must be a function');
        }
        let res = await requestFn();
        if (res.status !== 401) return res;

        // 1) 헤더 신호
        const need = res.headers.get('X-MFA-Required');
        // 2) 바디 신호(보조)
        let bodyErr = null;
        try { bodyErr = (await res.clone().json())?.error; } catch {}

        const isMfaRequired = (need && need.toUpperCase() === 'TOTP') || bodyErr === 'MFA_REQUIRED';
        if (!isMfaRequired) return res;

        // 모달 열고 사용자 입력 대기
        openModal();
        const outcome = await new Promise(resolve => {
            const ok = () => { cleanup(); resolve('ok'); };
            const cancel = () => { cleanup(); resolve('cancel'); };
            function cleanup() {
                window.removeEventListener('mfa:retried', ok);
                window.removeEventListener('mfa:cancelled', cancel);
            }
            window.addEventListener('mfa:retried', ok, {once:true});
            window.addEventListener('mfa:cancelled', cancel, {once:true});
        });

        if (outcome === 'cancel') {
            const e = new Error('MFA_CANCELLED'); e.mfaCancelled = true; throw e;
        }
        // 인증 성공 → 원요청 재시도
        return await requestFn();
    }

    window.fetchWithMfa = fetchWithMfa;
})();
