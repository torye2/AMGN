document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(location.search);
    const msg = params.get("message");
    if (msg) {
        alert(decodeURIComponent(msg));
        // 또는 document.getElementById("login-msg").innerText = msg;
    }

    const errorMsg   = params.get('error');       // 에러 메시지

    // 폼 상단 공통 에러 박스에 표시
    if (errorMsg) {
        if (errorMsg == 'locked') alert("정지/휴면 상태인 계정입니다.");
        if (errorMsg == 'user_not_found') alert("일치하는 유저 정보를 찾을 수 없습니다.");
        if (errorMsg == 'account_disabled') alert("사용할 수 없는 계정입니다.");
        if (errorMsg == 'bad_credentials') alert("일치하는 유저 정보를 찾을 수 없습니다.");

        const formError = document.getElementById('formError');
        if (formError) {
            formError.textContent = decodeURIComponent(errorMsg);
            formError.style.display = 'block';
        }
    }

    const next = params.get('next');
    const $next = document.getElementById('nextHidden');
    if ($next && next) $next.value = next;

    propageNext(params);
});

function propageNext(params) {
    const next = params.get('next');
    if (!next) return;

    const hidden = document.getElementById('nextHidden');
    if (hidden) hidden.value = next;

    document.querySelectorAll('.sns-login a, .social-buttons a').forEach(a => {
        try {
            const u = new URL(a.getAttribute('href'), location.origin);
            u.searchParams.set('next', next);
            // 상대경로 유지
            a.setAttribute('href', u.pathname + '?' + u.searchParams.toString());
        } catch (e) {
        }
    });
}

function getCookie(name) {
    const m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : null;
}
async function ensureCsrfHidden() {
    // 쿠키에서 먼저 시도
    let token = getCookie('XSRF-TOKEN');
    if (!token) {
        // 쿠키가 아직 없으면 API로 한 번 받아오고, 서버 필터가 쿠키도 내려줌
        const res = await fetch('/api/csrf', {
            headers: { 'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'},
            credentials: 'include'
        });
        const json = await res.json();
        token = json.token;
    }
    document.getElementById('csrfHidden').value = token;
}
document.addEventListener('DOMContentLoaded', ensureCsrfHidden);