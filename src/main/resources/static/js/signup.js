document.addEventListener('DOMContentLoaded', function () {
    console.log('[SIGNUP] DOM ready');
    document.addEventListener('click', (e) => {
        console.log('[DEBUG] clicked target:', e.target);
    });
    const yearSel = document.getElementById('birthYear');
    const monthSel = document.getElementById('birthMonth');
    const daySel = document.getElementById('birthDay');
    const btnFindAddr = document.getElementById('btnFindAddr');
    const zipcode = document.getElementById('zipcode');
    const addr1 = document.getElementById('addr1');
    const addr2 = document.getElementById('addr2');
    const pw = document.getElementById('id-password');
    const pw2 = document.getElementById('id-password2');
    const phone = document.getElementById('id-phone');
    const idInput = document.getElementById('id-account');
    const idCheckBtn = document.getElementById('idCheck');
    const form = document.querySelector('form');
    const province = document.getElementById('province');
    const city = document.getElementById('city');
    const detailAddress = document.getElementById('detailAddress');
    const submitBtn = document.getElementById('submitBtn');
    let idCheckPassed = false;
    let lastCheckedId = '';
    const params = new URLSearchParams(location.search);
    const msg   = params.get('error');       // 에러 메시지

    // 폼 상단 공통 에러 박스에 표시
    if (msg) {
        const formError = document.getElementById('formError');
        if (formError) {
            formError.textContent = decodeURIComponent(msg);
            formError.style.display = 'block';
        }
    }

    // URL 깨끗이 (뒤로가기 해도 메시지 안 남도록)
    if (window.history && history.replaceState) {
        const cleanUrl = location.pathname; // /signup.html
        history.replaceState({}, document.title, cleanUrl);
    }

    // ===== 생년월일 드롭다운 초기화 =====
    if (yearSel && monthSel && daySel) {
        const thisYear = new Date().getFullYear();
        const startYear = 1950;

        // 연도
        yearSel.innerHTML = '<option value="">년</option>';
        for (let y = thisYear; y >= startYear; y--) {
            yearSel.insertAdjacentHTML('beforeend', `<option value="${y}">${y}</option>`);
        }
        // 월
        monthSel.innerHTML = '<option value="">월</option>';
        for (let m = 1; m <= 12; m++) {
            monthSel.insertAdjacentHTML('beforeend', `<option value="${m}">${m}월</option>`);
        }
        // 일 (연/월 선택 시 동적 생성)
        daySel.innerHTML = '<option value="">일</option>';

        function daysInMonth(year, month) {
            return new Date(year, month, 0).getDate();
        }
        function renderDays() {
            const yy = parseInt(yearSel.value, 10);
            const mm = parseInt(monthSel.value, 10);
            daySel.innerHTML = '<option value="">일</option>';
            if (!yy || !mm) return;
            const cnt = daysInMonth(yy, mm);
            let html = '';
            for (let d = 1; d <= cnt; d++) html += `<option value="${d}">${d}</option>`;
            daySel.insertAdjacentHTML('beforeend', html);
        }
        yearSel.addEventListener('change', renderDays);
        monthSel.addEventListener('change', renderDays);
    }

    // ===== 카카오 우편번호 검색 =====
    if (btnFindAddr && zipcode && addr1 && typeof daum !== 'undefined' && daum.Postcode) {
        btnFindAddr.addEventListener('click', function () {
            new daum.Postcode({
                oncomplete: function (data) {
                    const zip = data.zonecode || '';
                    const base = data.roadAddress || data.jibunAddress || '';
                    const sido = expandSidoName(data.sido) || '';        // 시/도
                    const sigungu = data.sigungu || '';  // 시/군/구
                    zipcode.value = zip;
                    addr1.value = base;
                    addr2 && addr2.focus();
                    province.value = sido;
                    city.value = sigungu;
                    let target = (sido + " " + sigungu).trim();
                    let detail = base.startsWith(target) ? base.replace(target, "").trim() : base;
                    detail += addr2.value ? " " + addr2.value : "";
                    detailAddress.value = detail.trim();
                }
            }).open();
        });
    } else if (btnFindAddr) {
        // daum 라이브러리를 못 찾은 경우(로드 순서/경로 문제)
        btnFindAddr.addEventListener('click', function () {
            alert('주소 검색 스크립트를 불러오지 못했습니다. 인터넷 연결 또는 스크립트 로드 순서를 확인해주세요.');
        });
    }

    // ===== 비밀번호 일치 검증 =====
    if (pw && pw2) {
        function passwordsMatch() {
            return pw.value && pw.value === pw2.value;
        }
        pw.addEventListener('input', function () {
           const pattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+{}\[\]:;"'<>,.?/~`-]).{8,}$/;

           if(!pattern.test(pw.value)) {
               pw.setCustomValidity("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야합니다.");
           } else {
               pw.setCustomValidity("");
           }
        });
        pw2.addEventListener('input', () => {
            pw2.setCustomValidity(passwordsMatch() ? '' : '비밀번호가 일치하지 않습니다.');
        });
        form && form.addEventListener('submit', (e) => {
            if (!passwordsMatch()) {
                e.preventDefault();
                pw2.reportValidity();
            }
        });
    }

    // ===== 전화번호 포맷 =====
    function formatKRPhone(v) {
        const d = (v || '').replace(/\D/g, '');
        if (!d) return '';

        // 대표번호(1588/1600/1666/1800 등)
        if (/^(15|16|18)\d{2}/.test(d)) {
            if (d.length <= 4) return d;
            return d.slice(0, 4) + '-' + d.slice(4, 8);
        }

        // 서울(02)
        if (d.startsWith('02')) {
            if (d.length <= 2) return d;
            if (d.length <= 5) return d.slice(0, 2) + '-' + d.slice(2);
            if (d.length <= 9) return d.slice(0, 2) + '-' + d.slice(2, 5) + '-' + d.slice(5);
            return d.slice(0, 2) + '-' + d.slice(2, 6) + '-' + d.slice(6, 10);
        }

        // 010/070/050/지역 국번(3자리)
        const ac3 = d.slice(0, 3);
        if (/^(010|070|050|051|052|053|054|055|031|032|033|041|042|043|044|061|062|063|064)$/.test(ac3)) {
            if (d.length <= 3) return d;
            if (d.length <= 7) return d.slice(0, 3) + '-' + d.slice(3);
            if (d.length === 10) return d.slice(0, 3) + '-' + d.slice(3, 6) + '-' + d.slice(6);
            return d.slice(0, 3) + '-' + d.slice(3, 7) + '-' + d.slice(7, 11);
        }

        // 그 외 일반: 3-3/4-4
        if (d.length <= 3) return d;
        if (d.length <= 7) return d.slice(0, 3) + '-' + d.slice(3);
        if (d.length === 10) return d.slice(0, 3) + '-' + d.slice(3, 6) + '-' + d.slice(6);
        return d.slice(0, 3) + '-' + d.slice(3, 7) + '-' + d.slice(7, 11);
    }

    // 입력 중 자동 포맷 + 캐럿 유지
    phone.addEventListener('input', () => {
        const cur = phone.value;
        const start = phone.selectionStart;
        const beforeDigits = (cur.slice(0, start) || '').replace(/\D/g, '').length;

        const next = formatKRPhone(cur);
        if (cur !== next) {
            phone.value = next;

            // 새 문자열에서 같은 "숫자 인덱스" 위치로 캐럿 복원
            let seen = 0, pos = 0;
            while (pos < next.length && seen < beforeDigits) {
                if (/\d/.test(next[pos])) seen++;
                pos++;
            }
            phone.setSelectionRange(pos, pos);
        }
    });

    // 붙여넣기 방어 (숫자만 유지)
    phone.addEventListener('paste', (e) => {
        e.preventDefault();
        const text = (e.clipboardData || window.clipboardData).getData('text') || '';
        const digits = text.replace(/\D/g, '');
        phone.value = formatKRPhone(digits);
    });

    // blur 시 최종 정리(있어도 무방)
    phone.addEventListener('blur', () => {
        phone.value = formatKRPhone(phone.value);
    });

    // ===== 아이디 중복 확인 =====
    if (idCheckBtn && idInput) {
        idCheckBtn.addEventListener('click', async () => {
            console.log('[SIGNUP] idCheck clicked');
            const id = idInput.value.trim();
            if (!id) { alert('아이디를 입력해주세요.'); return; }
            try {
                const res = await fetch(`/api/users/exist?id=${encodeURIComponent(id)}`, {
                    headers: { 'Accept': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest'},
                    credentials: 'same-origin'
                });
                if (!res.ok) throw new Error('중복 확인 API 호출에 실패했습니다.');
                const data = await res.json();
                if (data.exist) {
                    idCheckPassed = false;
                    lastCheckedId = '';
                    alert('이미 사용 중인 아이디입니다.');
                } else {
                    idCheckPassed = true;
                    lastCheckedId = id;
                    alert('사용 가능한 아이디입니다.');
                }
            } catch (e) {
                alert('중복 확인 중 오류가 발생했습니다.');
            }
        });
    }

    idInput.addEventListener('input', () => {
        idCheckPassed = false;
        lastCheckedId = '';
    });

    form.addEventListener('submit', (e) => {
        const digits = (phone.value || '').replace(/\D/g, '');
        phone.value = digits;
        const currentId = idInput.value.trim();
        console.log('[DEBUG] idCheckPassed: ' + idCheckPassed);
        console.log('[DEBUG] lastCheckedId: ' + lastCheckedId);
        console.log('[DEBUG] currentId: ' + currentId);
        if (!idCheckPassed || currentId !== lastCheckedId) {
            e.preventDefault();
            alert('아이디 중복 확인을 완료해주세요.');
            return;
        }
    });
});

function expandSidoName(sido) {
    const map = {
        '서울': '서울특별시',
        '부산': '부산광역시',
        '대구': '대구광역시',
        '인천': '인천광역시',
        '광주': '광주광역시',
        '대전': '대전광역시',
        '울산': '울산광역시',
        '세종': '세종특별자치시',
        '경기': '경기도',
        '강원': '강원특별자치도',   // 2023 변경
        '충북': '충청북도',
        '충남': '충청남도',
        '전북': '전북특별자치도',   // 2023 변경
        '전남': '전라남도',
        '경북': '경상북도',
        '경남': '경상남도',
        '제주': '제주특별자치도'
    };
    return map[sido] || sido;
}