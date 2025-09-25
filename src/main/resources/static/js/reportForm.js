document.addEventListener("DOMContentLoaded", () => {
    const p = new URLSearchParams(location.search);
    const listingId = p.get('listingId');
    const nick = p.get('reportedNickname');
    const uid = p.get('reportedUserId');

    const f = document.getElementById('reportForm');
    const nickInput = document.getElementById('reportedNickname');
    const uidHidden = document.getElementById('reportedUserId');
    const listingHidden = document.getElementById('listingId');
    const reasonSel = document.getElementById('reason');
    const desc = document.getElementById('description');
    const evidence = document.getElementById('evidence');
    const preview = document.getElementById('preview');
    const submitBtn = document.getElementById('submitBtn');
    const descCounter = document.getElementById('descCounter');

    // 상세에서 넘어온 값 채우기
    if (nick && nickInput) { nickInput.value = nick; nickInput.readOnly = true; }
    if (uid && uidHidden) uidHidden.value = uid;
    if (listingId && listingHidden) listingHidden.value = listingId;

    // 설명 글자수 카운터
    const MAX_DESC = 1000;
    const updateCount = () => {
        const len = desc.value.length;
        descCounter.textContent = `${len} / ${MAX_DESC}`;
    };
    desc.addEventListener('input', updateCount);
    updateCount();

    let me = null;
    try {
        const meRes = fetch('/api/user/me', { credentials: 'include' });
        if (meRes.ok) me = meRes.json();
        if (me.userId == uid) submitBtn.disabled = true;
    } catch (e) {
        console.error('현재 사용자 조회 실패:', e);
    }

    // 파일 미리보기 + 검증
    const MAX_FILES = 5;
    const MAX_SIZE = 5 * 1024 * 1024; // 5MB
    const isImage = (file) => /^image\//.test(file.type);

    evidence.addEventListener('change', () => {
        preview.innerHTML = '';
        const files = Array.from(evidence.files || []);
        if (files.length > MAX_FILES) {
            alert(`이미지는 최대 ${MAX_FILES}장까지 업로드할 수 있습니다.`);
            evidence.value = '';
            return;
        }
        for (const f of files) {
            if (!isImage(f)) { alert('이미지 파일만 업로드 가능합니다.'); evidence.value = ''; return; }
            if (f.size > MAX_SIZE) { alert('각 이미지 최대 5MB까지 허용됩니다.'); evidence.value = ''; return; }
            const img = document.createElement('img');
            img.src = URL.createObjectURL(f);
            preview.appendChild(img);
        }
    });

    // XSRF 쿠키 읽기
    function getCookie(name) {
        return document.cookie.split('; ').find(c => c.startsWith(name + '='))?.split('=')[1];
    }

    // 중복 제출 방지
    let submitting = false;

    f.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (submitting) return;

        // 기본 검증
        if (!nickInput.value.trim()) { alert('닉네임을 입력해 주세요.'); return; }
        if (!reasonSel.value) { alert('신고 사유를 선택해 주세요.'); return; }
        if (!desc.value.trim()) { alert('상세 내용을 입력해 주세요.'); return; }
        if (nickInput.value.trim() && me?.nickname && nickInput.value.trim() === me.nickname) {
            alert('본인 계정은 신고할 수 없습니다.');
            return;
        }

        submitting = true;
        submitBtn.disabled = true;
        submitBtn.textContent = '제출 중...';

        try {
            // 1) 신고 생성
            const createBody = {
                reportedNickname: nickInput.value.trim(),
                listingId: listingHidden.value ? Number(listingHidden.value) : null,
                reasonCode: reasonSel.value,           // ABUSE/SCAM/INAPPROPRIATE/OTHER
                reasonText: reasonSel.selectedOptions[0]?.textContent || null,
                description: desc.value.trim()
            };

            const xsrf = getCookie('XSRF-TOKEN'); // 서버 설정에 맞게 이름 확인
            const res = await fetchJson('/api/reports', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(xsrf ? {'X-XSRF-TOKEN': xsrf} : {})
                },
                credentials: 'include',
                body: JSON.stringify(createBody)
            });

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(err.message || '신고 생성에 실패했습니다.');
            }
            const { reportId } = await res.json();

            // 2) 증거 업로드(있으면)
            const files = Array.from(evidence.files || []);
            if (files.length) {
                const form = new FormData();
                for (const f of files) form.append('files', f);
                const evRes = await fetchJson(`/api/reports/${reportId}/evidence`, {
                    method: 'POST',
                    headers: { ...(xsrf ? {'X-XSRF-TOKEN': xsrf} : {}) },
                    credentials: 'include',
                    body: form
                });
                if (!evRes.ok) {
                    // 실패해도 신고 자체는 생성됨 → 사용자에게 안내
                    console.warn('증거 업로드 실패', await evRes.text());
                    alert(err.message || '신고는 접수되었지만 증거 업로드에 실패했습니다. 고객센터로 문의해 주세요.');
                }
            }

            alert('신고가 접수되었습니다. 감사합니다.');
            // 원래 페이지로 복귀
            const back = new URLSearchParams(location.search).get('back');
            location.href = back || '/main.html';
        } catch (err) {
            console.error(err);
            alert(err.message || '처리 중 오류가 발생했습니다.');
        } finally {
            submitting = false;
            submitBtn.disabled = false;
            submitBtn.textContent = '신고 제출';
        }
    });
});

async function fetchJson(url, options = {}) {
    const res = await fetch(url, { credentials: 'include', ...options });
    if (res.ok) return res.json();

    // 에러 응답 파싱: JSON 우선, 실패하면 text
    let msg = `요청 처리 중 오류가 발생했습니다. (HTTP ${res.status})`;
    try {
        const ct = res.headers.get('content-type') || '';
        if (ct.includes('application/json')) {
            const err = await res.json();
            // 서버 표준: { success:false, code, message }
            if (err?.message) msg = err.message;
            else if (err?.error) msg = err.error;
        } else {
            const text = await res.text();
            if (text) msg = text;
        }
    } catch (_) { /* ignore */ }

    const error = new Error(msg);
    error.status = res.status;
    throw error;
}
