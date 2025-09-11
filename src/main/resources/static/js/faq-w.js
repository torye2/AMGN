(function () {
    const submitBtn = document.querySelector('.submit-btn');
    const adminHelp = document.getElementById('adminHelp');

    function setSubmitVisible(visible) {
        if (!submitBtn) return;
        if (visible) {
            submitBtn.classList.remove('hidden');
            if (adminHelp) adminHelp.textContent = '';
        } else {
            submitBtn.classList.add('hidden');
            if (adminHelp) adminHelp.textContent = '관리자만 FAQ를 등록할 수 있습니다.';
        }
    }

    function checkAdmin() {
        fetch('/api/user/status', { credentials: 'same-origin' })
            .then(res => res.ok ? res.json() : null)
            .then(info => {
                const isAdmin = info && info.username === '관리자';
                setSubmitVisible(!!isAdmin);
            })
            .catch(() => setSubmitVisible(false));
    }

    window.saveFaq = function (e) {
        e.preventDefault();
        const qEl = document.getElementById('question');
        const aEl = document.getElementById('answer');
        const question = (qEl?.value || '').trim();
        const answer = (aEl?.value || '').trim();
        if (!question || !answer) {
            alert('질문과 답변을 모두 입력해주세요.');
            (qEl && !question ? qEl : aEl).focus();
            return;
        }
        if (submitBtn) submitBtn.disabled = true;

        // CSRF 토큰을 쿠키(XSRF-TOKEN)에서 읽어 헤더로 전송
        const headers = { 'Content-Type': 'application/json' };
        const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
        if (m && m[1]) {
            headers['X-XSRF-TOKEN'] = decodeURIComponent(m[1]);
        }

        fetch('/api/faqs', {
            method: 'POST',
            headers,
            credentials: 'same-origin',
            body: JSON.stringify({ question, answer })
        })
            .then(async res => {
                const raw = await res.text().catch(() => '');
                let data = null;
                try { data = raw ? JSON.parse(raw) : null; } catch (_) {}

                if (res.status === 401) throw new Error('로그인이 필요합니다.');
                if (res.status === 403) throw new Error('관리자만 등록할 수 있습니다.');

                if (!res.ok || (data && data.ok === false)) {
                    const msg = (data && (data.message || data.error)) || raw || '등록에 실패했습니다.';
                    // 상태/본문 로깅
                    console.error('FAQ 등록 오류:', { status: res.status, body: raw });
                    throw new Error(msg);
                }
                return data || {};
            })
            .then(() => {
                alert('등록되었습니다.');
                location.href = 'faq.html';
            })
            .catch(err => {
                alert(err.message || '등록에 실패했습니다. 잠시 후 다시 시도해주세요.');
            })
            .finally(() => {
                if (submitBtn) submitBtn.disabled = false;
            });
    };

    checkAdmin();
})();
