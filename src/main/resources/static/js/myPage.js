// ----- Endpoints -----
const ENDPOINTS = {
    deleteMe: '/api/user/delete',
    oauthMe: '/api/oauth/me',
    oauthUnlink: '/api/oauth/unlink',
    myProducts: (status) => `/product/my-products${status ? `?status=${status}` : ''}`,
    favorites: undefined, // 없으면 탭 숨김 권장
    orders: '/orders', // 내 주문 전체(판매/구매 혼합) → 클라이언트에서 역할 추정/분리
    reviewableOrders: '/api/reviews/mine', // "받은 후기" API가 없으므로 임시로 사용
    meStatus: '/api/user/status', // { isLoggedIn, nickname, userId(로그인ID 문자열) }
    notifications: undefined,
    meShop: undefined,
    userProfile: '/api/user/profile',
    verifyPassword: '/api/user/verify-password',
    idAvailable: (newId) => `/api/users/exist?id=${encodeURIComponent(newId)}`,
    addresses: '/api/addresses',
    addressById: (id) => `/api/addresses/${id}`,
    addressSetDefault: (id) => `/api/addresses/${id}/default`
};

let _authState = { linkedProviders: [] };
const STATUS_MAP = { ON_SALE: 'ACTIVE', ACTIVE: 'ACTIVE', RESERVED: 'RESERVED', SOLD: 'SOLD' };
const toApiStatus = (s) => STATUS_MAP[s] || s;

// ===== Signup-style birth selectors & postcode helpers =====
function daysInMonth(year, month) {
    return new Date(year, month, 0).getDate();
}

function initBirthSelects() {
    const yearSel = document.getElementById('birthYear');
    const monthSel = document.getElementById('birthMonth');
    const daySel = document.getElementById('birthDay');
    if (!(yearSel && monthSel && daySel)) return;
    const thisYear = new Date().getFullYear();
    const startYear = 1950;
    yearSel.innerHTML = '<option value=\"\">년</option>';
    for (let y = thisYear; y >= startYear; y--) {
        yearSel.insertAdjacentHTML('beforeend', `<option value=\"${y}\">${y}</option>`);
    }
    monthSel.innerHTML = '<option value=\"\">월</option>';
    for (let m = 1; m <= 12; m++) {
        monthSel.insertAdjacentHTML('beforeend', `<option value=\"${m}\">${m}월</option>`);
    }
    daySel.innerHTML = '<option value=\"\">일</option>';

    function renderDays() {
        const yy = parseInt(yearSel.value, 10);
        const mm = parseInt(monthSel.value, 10);
        daySel.innerHTML = '<option value=\"\">일</option>';
        if (!yy || !mm) return;
        const cnt = daysInMonth(yy, mm);
        let html = '';
        for (let d = 1; d <= cnt; d++) html += `<option value=\"${d}\">${d}</option>`;
        daySel.insertAdjacentHTML('beforeend', html);
    }

    yearSel.addEventListener('change', renderDays);
    monthSel.addEventListener('change', renderDays);
    return {renderDays};
}

const tradeMethodMap = {
    MEETUP: '직거래',
    DELIVERY: '택배'
};

const orderStatusMap = {
    CREATED: '주문 등록',
    PAID: '결제 완료',
    IN_TRANSIT: '배송 중',
    DELIVERED: '배송 완료',
    MEETUP_CONFIRMED: '주문 확정',
    CANCELLED: '거래 취소',
    DISPUTED: '분쟁',
    REFUNDED: '환불됨',
    COMPLETED: '거래 완료',
    CANCEL_B_S: '거래 취소'
};


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
        '강원': '강원특별자치도',
        '충북': '충청북도',
        '충남': '충청남도',
        '전북': '전북특별자치도',
        '전남': '전라남도',
        '경북': '경상북도',
        '경남': '경상남도',
        '제주': '제주특별자치도'
    };
    return map[sido] || sido;
}

function bindPostcode() {
    const btnFindAddr = document.getElementById('btnFindAddr');
    const zipcode = document.getElementById('zipcode');
    const addr1 = document.getElementById('addr1');
    const addr2 = document.getElementById('addr2');
    const province = document.getElementById('province');
    const city = document.getElementById('city');
    const detailAddress = document.getElementById('detailAddress');
    if (!btnFindAddr) return;
    if (typeof daum !== 'undefined' && daum.Postcode) {
        btnFindAddr.addEventListener('click', function () {
            new daum.Postcode({
                oncomplete: function (data) {
                    const zip = data.zonecode || '';
                    const base = data.roadAddress || data.jibunAddress || '';
                    const sido = expandSidoName(data.sido) || '';
                    const sigungu = data.sigungu || '';
                    zipcode.value = zip;
                    addr1.value = base;
                    addr2 && addr2.focus();
                    province.value = sido;
                    city.value = sigungu;
                    let target = (sido + " " + sigungu).trim();
                    let detail = base.startsWith(target) ? base.replace(target, " ").trim() : base;
                    addr2.addEventListener('change', function () {
                        detail += addr2.value ? " " + addr2.value : " ";
                    })
                    detailAddress.value = detail.trim();
                }
            }).open();
        });
    } else {
        btnFindAddr.addEventListener('click', function () {
            alert('주소 검색 스크립트를 불러오지 못했습니다. 인터넷 연결 또는 스크립트 로드 순서를 확인해주세요.');
        });
    }
}

function bindPostcodeScoped(root, btnSel, map = {
    zipcode: '#addrZipcode',
    base: '#addrBase',
    detail2: '#addrDetail2',
    province: '#addrProvince',
    city: '#addrCity',
    final: '#addrDetailFinal'
}) {
    const btn = root.querySelector(btnSel);
    if (!btn) return;
    const zipcode = root.querySelector(map.zipcode);
    const addr1 = root.querySelector(map.base);
    const addr2 = root.querySelector(map.detail2);
    const province = root.querySelector(map.province);
    const city = root.querySelector(map.city);
    const detailAddress = root.querySelector(map.final);

    if (typeof daum !== 'undefined' && daum.Postcode) {
        btn.addEventListener('click', function () {
            new daum.Postcode({
                oncomplete: function (data) {
                    const zip = data.zonecode || '';
                    const base = data.roadAddress || data.jibunAddress || '';
                    const sido = expandSidoName(data.sido) || '';
                    const sigungu = data.sigungu || '';
                    zipcode.value = zip;
                    addr1.value = base;
                    addr2 && addr2.focus();
                    province.value = sido;
                    city.value = sigungu;
                    let target = (sido + " " + sigungu).trim();
                    let detail = base.startsWith(target) ? base.replace(target, " ").trim() : base;
                    // 상세 주소 타이핑 시 최종 주소에 붙이기
                    addr2.addEventListener('input', function () {
                        detailAddress.value = (detail + ' ' + addr2.value).trim();
                    });
                    detailAddress.value = detail.trim();
                }
            }).open();
        });
    } else {
        btn.addEventListener('click', function () {
            alert('주소 검색 스크립트를 불러오지 못했습니다.');
        });
    }
}

// ===== Addresses (CRUD) =====
function renderAddrRow(a) {
    const badge = a.isDefault ? '<span class="tag">대표</span>' : '';
    const label = a.addressType || 'OTHER';
    const namePhone = `${a.recipientName ?? '-'} / ${a.recipientPhone ?? '-'}`;
    const full = a.detailAddress ?? [a.province, a.city, a.addressLine1, a.addressLine2].filter(Boolean).join(' ');
    return `
    <tr data-id="${a.addressId}">
      <td>${label}</td>
      <td>${namePhone}</td>
      <td>${full}</td>
      <td>${a.isDefault ? 'V' : ''}</td>
      <td>
        ${a.isDefault ? '' : `<button class="btn-addr-default" type="button">대표지정</button>`}
        <button class="btn-addr-edit" type="button">수정</button>
        <button class="btn-addr-del" type="button">삭제</button>
      </td>
    </tr>`;
}

async function loadAddresses() {
    const body = document.getElementById('addrBody');
    if (!body) return;
    body.innerHTML = '<tr><td colspan="5" class="empty">불러오는 중...</td></tr>';
    try {
        const res = await fetch(ENDPOINTS.addresses, {
            headers: { 'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'}
        });
        noAuthGuard(res);
        const list = await res.json();
        if (!Array.isArray(list) || list.length === 0) {
            body.innerHTML = '<tr><td colspan="5" class="empty">저장된 주소가 없습니다.</td></tr>';
            return;
        }
        body.innerHTML = list.map(renderAddrRow).join('');
    } catch (e) {
        body.innerHTML = `<tr><td colspan="5" class="empty">불러오기 실패: ${e.message}</td></tr>`;
    }
}

function openAddrForm(a = null) {
    const form = document.getElementById('addrForm');
    if (!form) return;
    form.style.display = 'grid';
    // 초기화
    form.querySelector('#addrId').value = a?.addressId ?? '';
    form.querySelector('#addrType').value = a?.addressType ?? 'HOME';
    form.querySelector('#addrRecipient').value = a?.recipientName ?? '';
    form.querySelector('#addrPhone').value = a?.phone ?? '';
    form.querySelector('#addrZipcode').value = a?.postalCode ?? '';
    form.querySelector('#addrBase').value = a?.addressLine1 ?? '';
    form.querySelector('#addrDetail2').value = a?.addressLine2 ?? '';
    form.querySelector('#addrProvince').value = a?.province ?? '';
    form.querySelector('#addrCity').value = a?.city ?? '';
    form.querySelector('#addrDetailFinal').value = a?.detailAddress ?? '';
    form.querySelector('#addrDefault').checked = !!a?.isDefault;

    // 스코프 바인딩 (다음 주소검색)
    bindPostcodeScoped(form, '#btnAddrFind');

    // 스크롤 포커스
    form.scrollIntoView({behavior:'smooth', block:'center'});
}

function closeAddrForm() {
    const form = document.getElementById('addrForm');
    if (!form) return;
    form.reset();
    form.style.display = 'none';
}

async function saveAddress(e) {
    e?.preventDefault();
    const form = document.getElementById('addrForm');
    const id = form.querySelector('#addrId').value.trim();
    const payload = {
        addressType: form.querySelector('#addrType').value,
        recipientName: form.querySelector('#addrRecipient').value.trim(),
        recipientPhone: form.querySelector('#addrPhone').value.trim(),
        postalCode: form.querySelector('#addrZipcode').value.trim(),
        addressLine1: form.querySelector('#addrBase').value.trim(),
        addressLine2: form.querySelector('#addrDetail2').value.trim(),
        province: form.querySelector('#addrProvince').value.trim(),
        city: form.querySelector('#addrCity').value.trim(),
        detailAddress: form.querySelector('#addrDetailFinal').value.trim(),
        isDefault: form.querySelector('#addrDefault').checked
    };

    try {
        const r = await fetch(id ? ENDPOINTS.addressById(id) : ENDPOINTS.addresses, {
            method: id ? 'PUT' : 'POST',
            headers: acctHeaders(),
            body: JSON.stringify(payload)
        });
        noAuthGuard(r);
        const j = await r.json().catch(()=>({ok:true}));
        if (j?.ok === false) throw new Error(j.message || '저장 실패');
        alert('저장되었습니다.');
        closeAddrForm();
        await loadAddresses();
    } catch (err) {
        alert(err.message || '저장 실패');
    }
}

async function setDefaultAddress(id) {
    try {
        const r = await fetch(ENDPOINTS.addressSetDefault(id), {
            method:'PATCH',
            headers:acctHeaders()
        });
        noAuthGuard(r);
        await loadAddresses();
    } catch (e) { alert('대표 지정 실패: ' + e.message); }
}

async function deleteAddress(id) {
    if (!confirm('이 주소를 삭제하시겠어요?')) return;
    try {
        const r = await fetch(ENDPOINTS.addressById(id), {
            method:'DELETE',
            headers:acctHeaders()
        });
        noAuthGuard(r);
        await loadAddresses();
    } catch (e) { alert('삭제 실패: ' + e.message); }
}

// 이벤트 바인딩 (DOM ready)
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('btnAddrNew')?.addEventListener('click', () => openAddrForm(null));
    document.getElementById('btnAddrCancel')?.addEventListener('click', closeAddrForm);
    document.getElementById('addrForm')?.addEventListener('submit', saveAddress);

    // 목록 내 액션 위임
    document.getElementById('addrBody')?.addEventListener('click', async (e) => {
        const tr = e.target.closest('tr[data-id]');
        if (!tr) return;
        const id = tr.getAttribute('data-id');

        if (e.target.classList.contains('btn-addr-edit')) {
            // 단건 조회 후 편집 열기
            try {
                const r = await fetch(ENDPOINTS.addressById(id), {
                    headers: { 'Accept': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest'}
                });
                noAuthGuard(r);
                const a = await r.json();
                openAddrForm(a);
            } catch (err) { alert('조회 실패: ' + err.message); }
        }

        if (e.target.classList.contains('btn-addr-del')) {
            deleteAddress(id);
        }

        if (e.target.classList.contains('btn-addr-default')) {
            setDefaultAddress(id);
        }
    });
});

// ----- Utils -----
const $ = (sel, el = document) => el.querySelector(sel);
const $$ = (sel, el = document) => Array.from(el.querySelectorAll(sel));
const toWon = (n) => (n == null || isNaN(n)) ? '-' : Number(n).toLocaleString('ko-KR') + '원';

function noAuthGuard(res) {
    if (!res.ok) {
        if (res.status === 401) {
            alert('로그인이 필요합니다.');
            location.href = '/login';
        }
        throw new Error('요청 실패 (' + res.status + ')');
    }
    return res;
}

function normalizeMe(raw) {
    const isLoggedIn = !!(raw?.isLoggedIn ?? raw?.loggedIn);
    // 닉네임 통합
    const nickname = raw?.nickname ?? raw?.nickName ?? raw?.name ?? '';
    // 내부 숫자 PK (주문/리스트와 비교할 값)
    const idCandidate = raw?.user_id ?? raw?.id ?? raw?.userId;
    const userId = (idCandidate != null && !isNaN(Number(idCandidate))) ? Number(idCandidate) : null;
    const username = raw?.username ?? raw?.userName ?? raw?.name ?? '';
    // 문자열 로그인ID (화면표시/URL 파라미터로 쓸 값)
    const loginId = raw?.loginId ?? (typeof raw?.userId === 'string' ? raw.userId : raw?.username);
    const createdAt = raw?.createdAt ?? raw?.joinedAt;
    return { isLoggedIn, nickname, userId, loginId, createdAt };
}

// 로그인 상태/유저 식별자 가져오기
async function fetchMe() {
    const res = await fetch(ENDPOINTS.meStatus, {
        headers: { 'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'}
    });
    noAuthGuard(res);
    const j = await res.json();
    return normalizeMe(j);
}

// 주문에서 내 역할 추정(SELLER/BUYER/UNKNOWN)
function inferRole(order, me) {
    // 가능하면 숫자 PK로 비교하고, 없으면 닉네임으로 비교
    if (order.sellerId != null && order.buyerId != null && me.userId != null) {
        if (order.sellerId === me.userId) return 'SELLER';
        if (order.buyerId === me.userId) return 'BUYER';
    }
    if (order.sellerName && me.nickname) {
        if (order.sellerName === me.nickname) return 'SELLER';
    }
    if (order.buyerName && me.nickname) {
        if (order.buyerName === me.nickname) return 'BUYER';
    }
    return 'UNKNOWN';
}

// ----- Navigation -----
function switchTab(name) {
    $$('.mp-link').forEach(n => n.classList.toggle('active', n.dataset.tab === name));
    $$('.tab').forEach(p => p.hidden = p.id !== 'tab-' + name);
    if (name === 'dashboard') loadDashboard();
    if (name === 'products') loadProducts('ACTIVE');
    if (name === 'favorites') loadFavorites();
    if (name === 'sales') loadSales();
    if (name === 'purchases') loadPurchases();
    if (name === 'reviews') loadReviews();
    if (name === 'alerts') loadAlerts();
    if (name === 'shop') loadShopSettings();
    if (name === 'account') { loadAccount(); loadOauthLinks(); popFlash(); }
    if (name === 'addresses') loadAddresses();
}

document.addEventListener('click', (e) => {
    const tabBtn = e.target.closest('.mp-link[data-tab]');
    if (tabBtn) {
        switchTab(tabBtn.dataset.tab);
        history.replaceState(null, '', '#'+tabBtn.dataset.tab);
    }

    const subBtn = e.target.closest('#tab-products .subtabs button');
    if (subBtn) {
        $$('#tab-products .subtabs button').forEach(b => b.classList.remove('active'));
        subBtn.classList.add('active');
        const raw = subBtn.getAttribute('data-status'); // dataset 대신 확실히
        const status = toApiStatus((raw || '').trim().toUpperCase());
        console.log('[products] click:', raw, '=>', status);
        loadProducts(status);
    }
});

// ----- Renderers -----
function renderProductCard(p) {
    const t = document.getElementById('tplProduct').content.cloneNode(true);
    const a = t.querySelector('a');
    a.href = `/productDetail.html?id=${p.listingId}`; // 상세 페이지 이동

    const img = t.querySelector('img');
    img.src = p.photoUrls && p.photoUrls.length > 0
        ? p.photoUrls[0] // 첫 번째 사진만 보여줌
        : 'https://placehold.co/300x200?text=No+Image';
    img.alt = p.title || '상품';

    t.querySelector('.title').textContent = p.title || '-';
    t.querySelector('.price').textContent = toWon(p.price);
    t.querySelector('.meta').textContent = p.updatedAt || p.createdAt || '';
    return t;
}

// ----- Loaders -----
async function loadDashboard() {
    try {
        const [onSale, orders, me, receivedReviews] = await Promise.all([
            fetch(ENDPOINTS.myProducts('ACTIVE'), {
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
            }).then(noAuthGuard).then(r => r.json()),

            fetch(ENDPOINTS.orders, {
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
            }).then(noAuthGuard).then(r => r.json()),

            fetchMe(),

            ENDPOINTS.receivedReviews
                ? fetch(ENDPOINTS.receivedReviews, {
                    headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                }).then(noAuthGuard).then(r => r.json())
                : Promise.resolve([])
        ]);


        // 판매/구매 분리 시도(필드가 없으면 전체 카운트 표시)
        let soldByMe = [], boughtByMe = [];
        if (Array.isArray(orders)) {
            for (const o of orders) {
                const role = inferRole(o, me);
                if (role === 'SELLER') soldByMe.push(o);
                else if (role === 'BUYER') boughtByMe.push(o);
            }
        }

        $('#statOnSale').textContent = onSale?.length ?? 0;
        $('#statSold').textContent = soldByMe.length || (orders?.length ?? 0); // 최소한 전체표시
        $('#statReviews').textContent = receivedReviews?.length ?? 0; // 수정
        $('#statRating').textContent = '-'; // 받은후기 평균 API없음

        if (me?.nickname) {
           $('#shopName').textContent = `상점명: ${me.nickname}`;
           const meta = document.getElementById('shopMeta');
           if (meta) {
             const joinEl = meta.querySelector('#metaJoin');
             if (joinEl) joinEl.textContent = me.createdAt ?? '-';
           }
        }
        try {
          const r = await fetch('/product/wish/my/count', {
              headers: { 'Accept': 'application/json',
                  'X-Requested-With': 'XMLHttpRequest'},
              credentials:'include'
          });
          const { count } = r.ok ? await r.json() : { count: 0 };
          const w = document.getElementById('metaWish');
          if (w) w.textContent = String(count ?? 0);
        } catch {
          const w = document.getElementById('metaWish');
          if (w) w.textContent = '0';
        }
        // DB 기준 팔로워 수 표시 (상점 주인 = 현재 로그인 사용자)
        try {
          const sid = me && (me.userId ?? me.user_id ?? me.id);
          if (sid != null) {
            const r2 = await fetch(`/api/follows/${encodeURIComponent(sid)}/count`, {
                headers: { 'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'},
                credentials: 'include'
            });
            const j2 = r2.ok ? await r2.json() : { count: 0 };
            const followers = (j2 && (j2.count ?? j2.data?.count)) ?? 0;
            const f = document.getElementById('metaFollowers');
            if (f) f.textContent = String(followers);
          } else {
            const f = document.getElementById('metaFollowers');
            if (f) f.textContent = '0';
          }
        } catch {
          const f = document.getElementById('metaFollowers');
          if (f) f.textContent = '0';
        }

    } catch (err) {
        console.warn(err);
    }
}

let _lastLoadReqId = 0;
async function loadProducts(status) {
    const grid = $('#productsGrid');
    const empty = $('#productsEmpty');
    const reqId = ++_lastLoadReqId;

    grid.classList.add('loading');
    empty.hidden = true;
    try {
        const res = await fetch(ENDPOINTS.myProducts(status), {
            headers: { 'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'}
        });
        noAuthGuard(res);
        const list = await res.json();
        if (reqId !== _lastLoadReqId) return;

        if (!Array.isArray(list) || list.length === 0) {
            grid.replaceChildren(); // 비우기
            empty.hidden = false;
            return;
        }

        const frag = document.createDocumentFragment();
        list.forEach(p => frag.appendChild(renderProductCard(p)));
        grid.replaceChildren(frag);
    } catch (err) {
        if (reqId !== _lastLoadReqId) return;
        grid.replaceChildren();
        grid.insertAdjacentHTML('beforeend', `<div class="empty">불러오기 실패: ${err.message}</div>`);
    } finally {
        if (reqId === _lastLoadReqId) grid.classList.remove('loading');
    }
}

async function loadSales() {
    const body = document.querySelector('#salesBody');
    body.innerHTML = '';

    if (!getCookie('XSRF-TOKEN')) await ensureCsrf();
    const xsrf = getCookie('XSRF-TOKEN');

    try {
        const orders = await fetch('/orders/sell', {
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest',
                'X-XSRF-TOKEN': xsrf
            }
        }).then(noAuthGuard).then(r => r.json());

        if (!orders || orders.length === 0) {
            body.innerHTML = `<tr><td colspan="6" class="empty">판매 내역이 없습니다.</td></tr>`;
            return;
        }

        // 필터 적용: CANCELLED/DELETED는 테이블에서 제외
        const visibleOrders = orders.filter(o => o.status !== 'DELETED');

        if (visibleOrders.length === 0) {
            body.innerHTML = `<tr><td colspan="6" class="empty">판매 내역이 없습니다.</td></tr>`;
            return;
        }

        for (const o of visibleOrders) {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${o.id}</td>
                <td><a href="/productDetail.html?id=${o.listingId}">${o.listingTitle ?? '-'}</a></td>
                <td>${o.buyerNickName ?? '-'}</td>
                <td>${toWon(o.finalPrice)}</td>
                <td>${orderStatusMap[o.status] ?? o.status}</td>
                <td>${o.createdAt ?? '-'}</td>
            `;

            const actionTd = document.createElement('td');
            tr.appendChild(actionTd);

            if (o.status === 'CREATED' || o.status === 'PAID') {
                const cancelBtn = createButton('판매자 취소', async () => {
                    if (!confirm('정말 판매자가 주문을 취소하시겠습니까?')) return;
                    try {
                        const res = await fetch(`/orders/${o.id}/cancel-by-seller`, {
                            method: 'POST',
                            headers: { 'Accept': 'application/json', 'X-XSRF-TOKEN': xsrf }
                        });
                        if (!res.ok) throw new Error('취소 실패');
                        alert('판매자에 의해 주문이 취소되었습니다.');
                        await loadSales();
                    } catch (err) {
                        console.error(err);
                        alert(err.message);
                    }
                });
                actionTd.appendChild(cancelBtn);
            }

            body.appendChild(tr);
        }
    } catch (err) {
        body.innerHTML = `<tr><td colspan="6" class="empty">불러오기 실패: ${err.message}</td></tr>`;
        console.error(err);
    }
}



async function loadPurchases() {
    console.log('loadPurchases 호출됨');
    const tbody = document.querySelector('#ordersTable tbody');
    tbody.innerHTML = ''; // 초기화

    if (!getCookie('XSRF-TOKEN')) await ensureCsrf();
    const xsrf = getCookie('XSRF-TOKEN');

    try {
        const [orders, me] = await Promise.all([
            fetch('/orders/buy', {
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
            }).then(noAuthGuard).then(r => r.json()),
            fetchMe()
        ]);

        if (!orders || orders.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6">구매 내역이 없습니다.</td></tr>`;
            return;
        }

        // DELETED 상태는 숨김
        const visibleOrders = orders.filter(o => o.status !== 'DELETED');

        if (visibleOrders.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6">구매 내역이 없습니다.</td></tr>`;
            return;
        }

        for (const o of visibleOrders) {
            const role = inferRole(o, me);
            if (role !== 'BUYER' && role !== 'UNKNOWN') continue;

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${o.id}</td>
                <td><a href="/productDetail.html?id=${o.listingId}">${o.listingTitle ?? '-'}</a></td>
                <td>${tradeMethodMap[o.method] ?? o.method}</td>
                <td>${orderStatusMap[o.status] ?? o.status}</td>
                <td>${toWon(o.finalPrice)}</td>
                <td></td>
            `;
            const actionTd = tr.querySelector('td:last-child');

            // ---------------- 상태별 처리 ----------------
            if (o.status === 'CANCEL_B_S') {
                actionTd.textContent = '판매자의 거래 취소로 인해 거래가 취소되었습니다.';
            } else if (o.status === 'CREATED') {
                actionTd.appendChild(createButton('결제', () => handlePayment(o)));
                actionTd.appendChild(createButton('주문 취소', async () => {
                    if (!confirm('정말 주문을 취소하시겠습니까?')) return;
                    try {
                        const res = await fetch(`/orders/${o.id}`, {
                            method: 'DELETE',
                            headers: { 'Accept': 'application/json', 'X-XSRF-TOKEN': xsrf }
                        });
                        if (!res.ok) throw new Error('주문 취소 실패');
                        await loadPurchases();
                    } catch (err) {
                        console.error(err);
                        alert(err.message);
                    }
                }));
            } else if (o.status === 'PAID') {
                actionTd.appendChild(createButton('주문 확정', () => completeOrder(o.id, actionTd)));
                actionTd.appendChild(createButton('결제 취소', async () => {
                    if (!confirm('정말 결제를 취소하시겠습니까?')) return;
                    try {
                        const res = await fetch(`/orders/${o.id}/revert`, {
                            method: 'POST',
                            headers: { 'Accept': 'application/json', 'X-XSRF-TOKEN': xsrf }
                        });
                        if (!res.ok) throw new Error('결제 취소 실패');
                        await loadPurchases();
                    } catch (err) {
                        console.error(err);
                        alert(err.message);
                    }
                }));
            } else if (o.status === 'COMPLETED') {
                const statusText = document.createElement('span');
                statusText.textContent = '주문 확정';
                statusText.style.marginRight = '10px';
                actionTd.appendChild(statusText);

                const reviewsRes = await fetch(`/api/reviews/orders/${o.id}`);
                const reviews = await reviewsRes.json();

                if (!reviews || reviews.length === 0) {
                    const reviewBtn = createButton('리뷰 작성', () => {
                        window.location.href = `/review/review.html?orderId=${o.id}&listingId=${o.listingId}`;
                    });
                    actionTd.appendChild(reviewBtn);
                } else {
                    const review = reviews[0];
                    const editBtn = createButton('리뷰 수정', () => {
                        const url = `/review/review.html?orderId=${o.id}&listingId=${o.listingId}&reviewId=${review.id}&score=${review.score}&rvComment=${encodeURIComponent(review.rvComment)}`;
                        window.location.href = url;
                    });
                    const deleteBtn = createButton('리뷰 삭제', async () => {
                        if (!confirm('정말 리뷰를 삭제하시겠습니까?')) return;
                        try {
                            const res = await fetch(`/api/reviews/${review.id}`, {
                                method: 'DELETE',
                                headers: { 'Accept': 'application/json', 'X-XSRF-TOKEN': xsrf }
                            });
                            if (!res.ok) throw new Error('리뷰 삭제 실패');
                            await loadPurchases();
                        } catch (err) {
                            console.error(err);
                            alert(err.message);
                        }
                    });

                    actionTd.appendChild(editBtn);
                    actionTd.appendChild(deleteBtn);
                }
            }

            tbody.appendChild(tr);
        }

    } catch (err) {
        console.error(err);
        tbody.innerHTML = `<tr><td colspan="6">구매 내역을 불러오는 중 오류가 발생했습니다: ${err.message}</td></tr>`;
    }
}


// ----------------- 초기화 -----------------
const { IMP } = window;
IMP.init('imp50832616'); // 아임포트 테스트용 가맹점 코드

// PG 매핑
const pgMap = {
    'KG_INICIS': 'html5_inicis',   // KG이니시스 V2 테스트 채널
    'TOSS': 'tosspayments',     // 토스페이먼츠 테스트 채널
    'KAKAO': 'kakaopay'         // 카카오페이 테스트 채널
};

// ----------------- 공통 테스트 결제 함수 -----------------
async function handlePayment(order) {
    console.log('결제 시도:', order);

    const merchantUid = `test_${order.id}_${Date.now()}`; // idempotencyKey로 활용
    const pg = pgMap[order.paymentMethod];
    if (!pg) {
        alert('지원하지 않는 결제 수단입니다.');
        return;
    }

    let success = false;

    if (order.paymentMethod === 'KG_INICIS') {
        success = await payWithIamportTest(order, merchantUid, pg);
    } else if (order.paymentMethod === 'TOSS') {
        success = await payWithTossTest(order, merchantUid);
    } else if (order.paymentMethod === 'KAKAO') {
        success = await payWithKakaoTest(order, merchantUid);
    }

    if (success) {
        try {
            // 서버에 결제 완료 상태 전송
            const xsrf = getCookie('XSRF-TOKEN');
            const res = await fetch(`/orders/${order.id}/pay`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': xsrf
                },
                body: JSON.stringify({
                    paymentMethod: order.paymentMethod,
                    amount: order.finalPrice,
                    idempotencyKey: merchantUid // 서버 중복 결제 방지용
                })
            });

            if (!res.ok) throw new Error('서버 주문 상태 갱신 실패');
            const updatedOrder = await res.json();
            console.log('서버 주문 상태 갱신:', updatedOrder);

            // UI 갱신: 버튼 변경
            const tr = Array.from(document.querySelectorAll('#ordersTable tbody tr'))
                .find(r => r.cells[0].textContent == order.id);
            if (tr) {
                const actionTd = tr.querySelector('td:last-child');
                actionTd.innerHTML = '';
                actionTd.appendChild(createButton('주문 확정', () => completeOrder(order.id, actionTd)));
                actionTd.appendChild(createButton('결제 취소', () => revertToCreated(order.id, actionTd, updatedOrder)));
            }

        } catch (err) {
            console.error(err);
            alert(err.message);
        }
    }
}

// ----------------- 아임포트 테스트 -----------------
function payWithIamportTest(order, merchantUid, pg) {
    const buyerName = order.buyerName || '홍길동';
    const buyerEmail = order.buyerEmail || 'test@example.com';
    const buyerTel = order.buyerPhone || '01012345678';

    const testCard = {
        card_number: '4141414141414141',
        expiry: '12/25',
        birth: '970101',
        pwd_2digit: '12',
        cvc: '123'
    };

    return new Promise((resolve) => {
        IMP.request_pay({
            pg: pg,
            pay_method: 'card',
            merchant_uid: merchantUid,
            name: order.listingTitle || '테스트 주문',
            amount: order.finalPrice,
            buyer_name: buyerName,
            buyer_email: buyerEmail,
            buyer_tel: buyerTel,
            card_number: testCard.card_number,
            expiry: testCard.expiry,
            birth: testCard.birth,
            pwd_2digit: testCard.pwd_2digit,
            cvc: testCard.cvc
        }, function(rsp) {
            console.log('IMP.response (테스트):', rsp);
            if (rsp.success) {
                alert('아임포트 테스트 결제 성공! 실제 결제는 진행되지 않았습니다.');
                resolve(true);
            } else {
                alert(`결제 실패: ${rsp.error_msg}`);
                resolve(false);
            }
        });
    });
}

// ----------------- Toss 테스트 -----------------
function payWithTossTest(order, merchantUid) {
    return new Promise((resolve) => {
        const toss = new TossPayments("channel-key-f9db894a-500c-44dc-aeb5-a7ea24c2f7e5");

        const testCard = {
            cardNumber: "4100111111111111",
            cardPassword: "12",
            expiry: "12/25",
            cvc: "123"
        };

        toss.requestPayment('카드', {
            amount: order.finalPrice,
            orderId: merchantUid,
            orderName: order.listingTitle || '테스트 주문',
            cardNumber: testCard.cardNumber,
            cardPassword: testCard.cardPassword,
            expiry: testCard.expiry,
            cvc: testCard.cvc,
            successUrl: window.location.href,
            failUrl: window.location.href
        }).then(() => {
            alert('Toss 테스트 결제 성공! 실제 결제는 진행되지 않았습니다.');
            resolve(true);
        }).catch(err => {
            console.error(err);
            alert("Toss 테스트 결제 실패");
            resolve(false);
        });
    });
}

// ----------------- KakaoPay 테스트 -----------------
function payWithKakaoTest(order, merchantUid) {
    return new Promise((resolve) => {
        fetch(`/kakao/pay/ready?orderId=${order.id}&merchantUid=${merchantUid}`)
            .then(r => r.json())
            .then(data => {
                console.log('KakaoPay 테스트 준비:', data);
                if (data.next_redirect_pc_url) {
                    window.open(data.next_redirect_pc_url, '_blank', 'width=500,height=800');
                    alert("KakaoPay 테스트 결제창이 새 창으로 열렸습니다. 실제 결제는 진행되지 않습니다.");
                    resolve(true);
                } else {
                    alert("KakaoPay 테스트 결제 준비 실패: URL 없음");
                    resolve(false);
                }
            })
            .catch(err => {
                console.error(err);
                alert("KakaoPay 테스트 결제 실패");
                resolve(false);
            });
    });
}












const nicknameCache = {};

async function getNickname(userId) {
    if (nicknameCache[userId]) return nicknameCache[userId];

    try {
        const res = await fetch(`/api/user/nickname/${userId}`, {
            headers: { 'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'}
        });
        const data = await res.json();
        nicknameCache[userId] = data.nickname;
        return data.nickname;
    } catch (err) {
        console.error("닉네임 조회 실패", err);
        return userId; // fallback
    }
}

function renderStars(score) {
    if (!score) return "-";
    const filled = '★'.repeat(score);
    const empty = '☆'.repeat(5 - score);
    return filled + empty;
}

async function loadReviews() {
    const tbody = document.getElementById("reviewsBody");
    tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;">로딩 중...</td></tr>`;

    try {
        const response = await fetch("/api/reviews/received", {
            headers: { 'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'}
        });
        if (!response.ok) throw new Error("받은 후기 조회 실패");

        const reviews = await response.json();

        if (!reviews.length) {
            tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;">받은 후기가 없습니다.</td></tr>`;
            return;
        }

        tbody.innerHTML = "";

        for (const r of reviews) {
            const nickname = await getNickname(r.raterId); // 여기서 매핑

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${renderStars(r.score)}</td>
                <td>${r.rvComment || "-"}</td>
                <td>${nickname}</td>
                <td>${new Date(r.createdAt).toLocaleString()}</td>
            `;
            tbody.appendChild(tr);
        }

    } catch (err) {
        console.error(err);
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:red;">후기 로딩 중 오류 발생: ${err.message}</td></tr>`;
    }
}



async function loadAlerts() {
    const listEl = $('#alertsList');
    listEl.innerHTML = '';
    if (!ENDPOINTS.notifications) {
        listEl.innerHTML = `<div class='empty'>알림 기능이 아직 준비되지 않았습니다.</div>`;
        return;
    }
    try {
        const res = await fetch(ENDPOINTS.notifications, {
            headers: { 'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'}
        });
        noAuthGuard(res);
        const list = await res.json();
        if (!list.length) {
            listEl.innerHTML = `<div class='empty'>새 알림이 없습니다.</div>`;
            return;
        }
        listEl.innerHTML = list.map(n => `<div class=\"mp-card\"><strong>${n.title ?? '알림'}</strong><div style=\"color:var(--muted);\">${n.body ?? ''}</div><div style=\"font-size:12px; color:var(--muted); margin-top:6px;\">${n.createdAt ?? '-'}</div></div>`).join('');
    } catch (err) {
        listEl.innerHTML = `<div class='empty'>불러오기 실패: ${err.message}</div>`;
    }
}

async function loadShopSettings() {
    // 현재 백엔드에 저장/조회 API 없음 → 폼 비활성화/안내
    const form = $('#shopForm');
    if (form) {
        form.querySelectorAll('input, textarea, button[type="submit"]').forEach(el => el.disabled = true);
        const note = document.createElement('div');
        note.className = 'empty';
        note.style.marginTop = '10px';
        note.textContent = '상점 관리 API가 아직 준비되지 않았습니다.';
        form.appendChild(note);
    }
}

let _acctOriginal = null;
let _acctEditing = false;
let _acctIdChecked = true;

function acctHeaders() {
    const token = getCookie('XSRF-TOKEN');
    const h = { 'Content-Type': 'application/json' };
    if (token) h['X-XSRF-TOKEN'] = token;
    return h;
}

function acctToggle(disabled) {
    ["acc-id", "acc-email", "acc-phone", "nickName", "birthYear", "birthMonth", "birthDay", "zipcode", "addr1", "addr2", "province", "city", "detailAddress", "acc-newpw", "acc-newpw2", "btnCheckId", "btnFindAddr"].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.disabled = disabled;
    });
    const fs = document.getElementById('acc-alarms');
    if (fs) fs.disabled = disabled;
}

function acctSetViewMode() {
    _acctEditing = false;
    acctToggle(true);
    const viewBtns = document.getElementById('accountViewBtns');
    const editBtns = document.getElementById('accountEditBtns');
    if (viewBtns) {
        viewBtns.hidden = false;
        editBtns.removeAttribute('aria-hidden');
    }
    if (editBtns) {
        editBtns.hidden = true;
        editBtns.setAttribute('aria-hidden', 'true');
    }
    const save = document.getElementById('btnSave');
    if (save) save.disabled = true;
    const idHelp = document.getElementById('idHelp');
    if (idHelp) idHelp.textContent = '';
}

function acctSetEditMode() {
    _acctEditing = true;
    acctToggle(false);
    const viewBtns = document.getElementById('accountViewBtns');
    const editBtns = document.getElementById('accountEditBtns');
    if (viewBtns) {
        viewBtns.hidden = true;
        editBtns.setAttribute('aria-hidden', 'true');
    }
    if (editBtns) {
        editBtns.hidden = false;
        editBtns.removeAttribute('aria-hidden');
    }
    const idInput = document.getElementById('acc-id');
    _acctIdChecked = (idInput?.value === _acctOriginal?.id);
    const save = document.getElementById('btnSave');
    if (save) save.disabled = !_acctIdChecked;
}

async function loadAccount() {
    try {
        const res = await fetch(ENDPOINTS.userProfile, {
            headers: { 'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'}
        });
        const {ok, data, message} = await res.json();
        if (!ok) {
            alert(message || '프로필 조회 실패', data);
            return;
        }
        _acctOriginal = {
            id: data?.id ?? '',
            email: data?.email ?? '',
            phoneNumber: data?.phoneNumber ?? '',
            nickName: data?.nickName ?? '',
            birthYear: data?.birthYear ?? '',
            birthMonth: data?.birthMonth ?? '',
            birthDay: data?.birthDay ?? '',
            province: data?.province ?? '',
            city: data?.city ?? '',
            detailAddress: data?.detailAddress ?? ''
        };
        // Bind
        const idInput = document.getElementById('acc-id');
        const email = document.getElementById('acc-email');
        const phone = document.getElementById('acc-phone');
        const nick = document.getElementById('nickName');
        const by = document.getElementById('birthYear');
        const bm = document.getElementById('birthMonth');
        const bd = document.getElementById('birthDay');
        const prov = document.getElementById('province');
        const city = document.getElementById('city');
        const da = document.getElementById('detailAddress');
        const zipcode = document.getElementById('zipcode');
        const addr1 = document.getElementById('addr1');
        const addr2 = document.getElementById('addr2'); // birth init (signup-style)
        const birthHelpers = initBirthSelects();
        if (by) by.value = _acctOriginal.birthYear || '';
        if (bm) bm.value = _acctOriginal.birthMonth || '';
        if (birthHelpers && birthHelpers.renderDays) birthHelpers.renderDays();
        if (bd) bd.value = _acctOriginal.birthDay || '';
        if (idInput) idInput.value = _acctOriginal.id;
        if (email) email.value = _acctOriginal.email;
        if (phone) phone.value = _acctOriginal.phoneNumber;
        if (nick) nick.value = _acctOriginal.nickName;
        if (prov) prov.value = _acctOriginal.province;
        if (city) city.value = _acctOriginal.city;
        if (da) da.value = _acctOriginal.detailAddress; // zipcode/addr1/addr2는 서버 저장 필드는 아니고, 검색 시 보조로 표시
        if (zipcode) zipcode.value = '';
        if (addr1) addr1.value = '';
        if (addr2) addr2.value = ''; // clear password fields
        const p1 = document.getElementById('acc-newpw');
        const p2 = document.getElementById('acc-newpw2');
        if (p1) p1.value = '';
        if (p2) p2.value = ''; // postcode bind
        bindPostcode();
        acctSetViewMode();
    } catch (e) {
        console.warn(e);
        alert('프로필 조회 실패');
    }
}

function openReauth() {
    const modal = document.getElementById('reauthModal');
    if (!modal) return;
    modal.classList.add('is-open');
    modal.setAttribute('aria-hidden', 'false');
    modal.querySelector('input[name="password"]').value = '';
    modal.querySelector('input[name="password"]').focus();
}

function closeReauth() {
    const modal = document.getElementById('reauthModal');
    if (!modal) return;
    modal.classList.remove('is-open');
    modal.setAttribute('aria-hidden', 'true');
}

async function submitReauth(e) {
    e?.preventDefault();
    const form = document.getElementById('reauthForm');
    const err = document.getElementById('reauthError');
    err?.classList.remove('is-visible');
    const pw = form.password.value.trim();
    if (!pw) {
        err.textContent = '비밀번호를 입력하세요.';
        err.classList.add('is-visible');
        return;
    }
    try {
        const r = await fetch(ENDPOINTS.verifyPassword, {
            method: 'POST',
            headers: acctHeaders(),
            body: JSON.stringify({password: pw})
        });
        const j = await r.json();
        if (j.ok) {
            closeReauth();
            acctSetEditMode();
        } else {
            err.textContent = j.message || '비밀번호가 일치하지 않습니다.';
            err.classList.add('is-visible');
        }
    } catch (e) {
        err.textContent = '요청 실패';
        err.classList.add('is-visible');
    }
}

// ID check
async function accountCheckId() {
    const idInput = document.getElementById('acc-id');
    const idHelp = document.getElementById('idHelp');
    const save = document.getElementById('btnSave');
    const candidate = (idInput?.value || '').trim();
    if (!candidate) {
        idHelp.textContent = '아이디를 입력하세요.';
        _acctIdChecked = false;
        if (save) save.disabled = true;
        return;
    }
    if (candidate === _acctOriginal?.id) {
        idHelp.textContent = '현재 아이디와 동일합니다. 사용 가능합니다.';
        _acctIdChecked = true;
        if (save) save.disabled = false;
        return;
    }
    if (!/^[a-zA-Z0-9_.-]{4,20}$/.test(candidate)) {
        idHelp.textContent = '아이디 형식이 올바르지 않습니다. (영/숫자/._- 4~20자)';
        _acctIdChecked = false;
        if (save) save.disabled = true;
        return;
    }
    try {
        const r = await fetch(ENDPOINTS.idAvailable(candidate), {
            headers: { 'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'}
        });
        const j = await r.json();
        if (j.exist === false) {
            idHelp.textContent = '사용 가능한 아이디입니다.';
            _acctIdChecked = true;
            if (save) save.disabled = false;
        } else {
            idHelp.textContent = j.message || '이미 사용 중인 아이디입니다.';
            _acctIdChecked = false;
            if (save) save.disabled = true;
        }
    } catch (e) {
        idHelp.textContent = '중복확인 실패';
        _acctIdChecked = false;
        if (save) save.disabled = true;
    }
}

// input change to reset id checked
document.addEventListener('input', (e) => {
    if (e.target && e.target.id === 'acc-id' && _acctEditing) {
        const save = document.getElementById('btnSave');
        const idHelp = document.getElementById('idHelp');
        if (e.target.value.trim() === (_acctOriginal?.id || '')) {
            _acctIdChecked = true;
            idHelp.textContent = '현재 아이디와 동일합니다.';
            if (save) save.disabled = false;
        } else {
            _acctIdChecked = false;
            idHelp.textContent = '중복확인을 해주세요.';
            if (save) save.disabled = true;
        }
    }
});
// 계정 관리 탭 버튼 바인딩
document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('btnCheckId')?.addEventListener('click', accountCheckId);
    document.getElementById('btnStartEdit')?.addEventListener('click', openReauth);
    document.getElementById('btnReauthCancel')?.addEventListener('click', closeReauth);
    document.getElementById('reauthForm')?.addEventListener('submit', submitReauth);
    const chatBtn = document.getElementById('chat-btn');
    chatBtn.addEventListener('click', () => {
        window.location.href = `/chatPage.html?roomId=1&listingId=1&sellerId=2`;
    });
    try { await ensureCsrf(); } catch {}
    // "상점 정보 수정" 버튼 -> 내 상점 페이지로 이동 (?sellerId=내 ID)
    document.getElementById('btnShopEdit')?.addEventListener('click', async () => {
        try {
            const me = await fetchMe();
            const sid = (me && (me.user_id ?? me.userId ?? me.id)) ?? null;
            if (!me || me.isLoggedIn === false) {
                alert('로그인이 필요합니다.');
                location.href = '/login';
                return;
            }
            if (sid == null || String(sid).trim() === '') {
                alert('사용자 식별자를 확인할 수 없습니다.');
                return;
            }
            window.location.href = `/shop.html?sellerId=${encodeURIComponent(String(sid))}`;
        } catch (e) {
            alert('사용자 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
        }
    });
    // form submit
    document.getElementById('accountForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!_acctEditing) return;
        const id = document.getElementById('acc-id').value.trim();
        const email = document.getElementById('acc-email').value.trim();
        const phone = document.getElementById('acc-phone').value.trim();
        const newPw = document.getElementById('acc-newpw').value.trim();
        const newPw2 = document.getElementById('acc-newpw2').value.trim();
        const pattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+{}\[\]:;"'<>,.?/~`-]).{8,}$/;

        if(newPw && !pattern.test(newPw)) {
            alert("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야합니다.");
            return;
        }
        if ((newPw || newPw2) && newPw !== newPw2) {
            alert('새 비밀번호가 일치하지 않습니다.');
            return;
        }
        if (!_acctIdChecked) {
            alert('아이디 중복확인을 완료해주세요.');
            return;
        }
        const payload = {
            id,
            email,
            phoneNumber: phone,
            nickName: document.getElementById('nickName').value.trim(),
            birthYear: Number(document.getElementById('birthYear').value) || null,
            birthMonth: Number(document.getElementById('birthMonth').value) || null,
            birthDay: Number(document.getElementById('birthDay').value) || null,
        };
        if (newPw) payload.newPassword = newPw;
        try {
            const r = await fetch(ENDPOINTS.userProfile, {
                method: 'PUT',
                headers: acctHeaders(),
                body: JSON.stringify(payload)
            });
            const j = await r.json();
            if (j.ok) {
                alert('정보가 성공적으로 변경되었습니다.');
                await loadAccount();
            } else {
                alert(j.message || '수정 실패');
            }
        } catch (e) {
            alert('요청 실패');
        }
    });

    const sp = new URLSearchParams(location.search);
    const reauthOk = sp.get('reauth') === 'ok';
    const after = sp.get('after');

    if (reauthOk && after === 'delete') {
        try {
            const reason = sessionStorage.getItem('deleteReason') || '';
            await doDeleteAccount(null, reason); // 비번 대신 최근 OAuth 재인증으로 통과
            alert('탈퇴가 완료되었습니다.');
            try { localStorage.clear(); sessionStorage.clear(); } catch {}
            // 히스토리 깔끔히
            location.replace('/main.html');
        } catch (e) {
            alert(e?.message || '탈퇴 실패');
        }
    }

    // 탈퇴 버튼 → 확인 모달
    document.getElementById('btnDelete')?.addEventListener('click', openDeleteModal);
    document.getElementById('btnDeleteCancel')?.addEventListener('click', closeDeleteModal);

    // 확인 모달 제출
    document.getElementById('deleteForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const confirmText = (document.getElementById('deleteConfirm').value || '').trim();
        const err = document.getElementById('deleteError');
        if (confirmText !== '탈퇴합니다') {
            err.textContent = '정확히 "탈퇴합니다"를 입력해 주세요.';
            err.classList.add('is-visible');
            return;
        }
        err.textContent = ''; err.classList.remove('is-visible');
        closeDeleteModal();

        // 인증 방법 선택 시트 열기
        openAuthChoiceWithEnsure();

        // 비번 재인증 모달을 선택했을 때 onsubmit을 "탈퇴 확정"으로 바꿔 둔다
        const form = document.getElementById('reauthForm');
        const modal = document.getElementById('reauthModal');
        const errBox = document.getElementById('reauthError');
        const originalHandler = form?.onsubmit;

        if (form && modal) {
            form.onsubmit = async (ev) => {
                ev.preventDefault();
                try {
                    const pw = form.password.value.trim();
                    if (!pw) {
                        if (errBox) { errBox.textContent = '비밀번호를 입력하세요.'; errBox.classList.add('is-visible'); }
                        return;
                    }
                    const v = await fetch(ENDPOINTS.verifyPassword, {
                        method: 'POST', headers: acctHeaders(),
                        body: JSON.stringify({ password: pw })
                    }).then(r => r.json());
                    if (!v.ok) {
                        if (errBox) { errBox.textContent = v.message || '비밀번호가 일치하지 않습니다.'; errBox.classList.add('is-visible'); }
                        return;
                    }
                    // 비번 검증 통과 → 최종 삭제
                    await doDeleteAccount(pw, document.getElementById('deleteReason')?.value || '');
                    alert('탈퇴가 완료되었습니다.');
                    try { localStorage.clear(); sessionStorage.clear(); } catch {}
                    location.href = '/main.html';
                } catch (ex) {
                    alert(ex.message || '탈퇴 실패');
                } finally {
                    // 원상복귀
                    if (originalHandler) form.onsubmit = originalHandler;
                    modal.classList.remove('is-open'); modal.setAttribute('aria-hidden','true');
                }
            };
        }
    });
});

async function verifyPassword() {
    const pw = document.getElementById('verifyPw').value;
    const res = await fetch(ENDPOINTS.verifyPassword, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({password: pw})
    });
    const j = await res.json();
    if (j.ok) { // 읽기 전용 -> 수정 폼 전환
        document.getElementById('profileRead').style.display = 'none';
        document.getElementById('profileEdit').style.display = 'block';
    } else {
        alert(j.message || '비밀번호가 일치하지 않습니다.');
    }
}

async function saveProfile() {
    const payload = {
        id: document.getElementById('id').value,
        email: document.getElementById('email').value,
        nickName: document.getElementById('nickName').value,
        phoneNumber: document.getElementById('phoneNumber').value,
        birthYear: +document.getElementById('birthYear').value,
        birthMonth: +document.getElementById('birthMonth').value,
        birthDay: +document.getElementById('birthDay').value,
        province: document.getElementById('province').value,
        city: document.getElementById('city').value,
        detailAddress: document.getElementById('detailAddress').value
    };
    const res = await fetch(ENDPOINTS.userProfile, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
    });
    const j = await res.json();
    if (j.ok) {
        alert('정보가 성공적으로 변경되었습니다.');
        loadAccount();
        document.getElementById('profileEdit').style.display = 'none';
        document.getElementById('profileRead').style.display = 'block';
    } else {
        alert(j.message || '수정 실패');
    }
}

// ----- Forms -----
$('#shopForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!ENDPOINTS.meShop) {
        alert('상점 관리 API가 아직 준비되지 않았습니다.');
        return;
    }
    const f = e.currentTarget;
    const payload = {shopName: f.shopName.value.trim(), intro: f.intro.value.trim()};
    try {
        const res = await fetch(ENDPOINTS.meShop, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        noAuthGuard(res);
        alert('상점 정보가 저장되었습니다.');
        loadDashboard();
    } catch (err) {
        alert('저장 실패: ' + err.message);
    }
});
$('#btnPreviewShop')?.addEventListener('click', () => {
    window.open('/shop/me', '_blank');
});

// ----- Boot -----
document.addEventListener('DOMContentLoaded', () => {
    // 알림/상점관리 탭 숨김(엔드포인트 없을 때)
    if (!ENDPOINTS.notifications) {
        $('[data-tab="alerts"]')?.remove();
        $('#tab-alerts')?.remove();
    }
    if (!ENDPOINTS.meShop) {
        $('[data-tab="shop"]')?.remove();
        $('#tab-shop')?.remove();
    }
    const VALID_TABS = new Set([
        'dashboard','products','favorites','sales','purchases',
        'reviews','alerts','shop','account','addresses','support'
    ]);

    let initial = (location.hash || '').replace('#','').trim();

    if (!initial) {
        const usp = new URLSearchParams(location.search);
        initial = (usp.get('tab') || '').trim();
    }
    if (!VALID_TABS.has(initial)) initial = 'dashboard';

    // 헤더 상점명/가입일을 먼저 갱신하여 초기 렌더에서 OO가 오래 보이지 않도록 함
    fetchMe()
      .then(async (me) => {
          if (me?.nickname) {
              const shopEl = document.getElementById('shopName');
              if (shopEl) shopEl.textContent = `상점명: ${me.nickname}`;
          }
          const joinEl = document.getElementById('metaJoin');
          if (joinEl && me?.createdAt) {
              joinEl.textContent = me.createdAt;
          }

          // DB 기준 찜 수 즉시 갱신
          try {
            const r = await fetch('/product/wish/my/count', {
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                credentials: 'include'
            });
            const { count } = r.ok ? await r.json() : { count: 0 };
            const w = document.getElementById('metaWish');
            if (w) w.textContent = String(count ?? 0);
          } catch {
            const w = document.getElementById('metaWish');
            if (w) w.textContent = '0';
          }

          // DB 기준 팔로워 수 즉시 갱신 (현재 로그인 사용자 = 상점 주인)
          try {
            const sid = me && (me.userId ?? me.user_id ?? me.id);
            const f = document.getElementById('metaFollowers');
            if (sid != null) {
              const r2 = await fetch(`/api/follows/${encodeURIComponent(sid)}/count`, {
                  headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                  credentials: 'include'
              });
              const j2 = r2.ok ? await r2.json() : { count: 0 };
              const followers = (j2 && (j2.count ?? j2.data?.count)) ?? 0;
              if (f) f.textContent = String(followers);
            } else {
              if (f) f.textContent = '0';
            }
          } catch {
            const f = document.getElementById('metaFollowers');
            if (f) f.textContent = '0';
          }
      })
      .catch(() => { /* 비로그인 등 오류는 무시 */ });

    switchTab(initial);
});
document.getElementById('btnCancelEdit')?.addEventListener('click', () => {
    if (!_acctOriginal) return;
    const by = document.getElementById('birthYear');
    const bm = document.getElementById('birthMonth');
    const bd = document.getElementById('birthDay');
    document.getElementById('acc-id').value = _acctOriginal.id;
    document.getElementById('acc-email').value = _acctOriginal.email;
    document.getElementById('acc-phone').value = _acctOriginal.phoneNumber;
    document.getElementById('nickName').value = _acctOriginal.nickName;
    if (by) by.value = _acctOriginal.birthYear || '';
    if (bm) bm.value = _acctOriginal.birthMonth || '';
    const birthHelpers = initBirthSelects();
    if (birthHelpers && birthHelpers.renderDays) birthHelpers.renderDays();
    if (bd) bd.value = _acctOriginal.birthDay || '';
    document.getElementById('province').value = _acctOriginal.province;
    document.getElementById('city').value = _acctOriginal.city;
    document.getElementById('detailAddress').value = _acctOriginal.detailAddress;
    document.getElementById('zipcode').value = '';
    document.getElementById('addr1').value = '';
    document.getElementById('addr2').value = '';
    document.getElementById('acc-newpw').value = '';
    document.getElementById('acc-newpw2').value = '';
    acctSetViewMode();
});

// 주문 내역 불러오기
function createButton(text, onClick) {
    const btn = document.createElement('button');
    btn.textContent = text;
    btn.addEventListener('click', onClick);
    return btn;
}

// 결제
async function payOrder(order) {
    try {
        // 1. 서버에서 merchant_uid + 금액 가져오기
        const res = await fetch(`/api/pay/prepare/${order.id}`);
        if (!res.ok) throw new Error('결제 준비 실패');
        const data = await res.json();

        // 필수 값 체크
        const { merchantUid, amount, name } = data;
        if (!merchantUid || !amount || !name) {
            console.error('PG 정보 부족:', data);
            alert('등록된 PG 설정 정보가 없습니다. 관리자에게 문의하세요.');
            return;
        }

        const { IMP } = window;
        IMP.init("channel-key-cb0fc946-2218-494d-b06d-4609ad738145"); // 아임포트 테스트 키

        // 2. 아임포트 결제창 띄우기
        IMP.request_pay({
            pg: pg,
            pay_method: 'card',
            merchant_uid: merchantUid,
            name: name,
            amount: amount
        }, async function(rsp) {
            if (rsp.success) {
                try {
                    const confirmRes = await fetch('/api/pay/complete', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            merchantUid: rsp.merchant_uid,
                            amount: rsp.paid_amount,
                            impUid: rsp.imp_uid
                        })
                    });
                    if (!confirmRes.ok) throw new Error("결제 검증 실패");

                    alert("결제 완료!");
                    loadOrders(); // 구매 내역 갱신
                } catch (err) {
                    console.error(err);
                    alert("결제 검증 중 오류 발생");
                }
            } else {
                alert("결제 실패: " + rsp.error_msg);
            }
        });

    } catch (err) {
        console.error(err);
        alert("결제 처리 중 오류 발생: " + err.message);
    }
}


// 주문 확정
async function completeOrder(orderId, td) {
    try {
        const res = await fetch(`/orders/${orderId}/complete`, {
            method: 'POST',
            headers: acctHeaders()
        });
        if (!res.ok) throw new Error('주문 확정 실패');
        td.textContent = '주문 확정';
    } catch (err) {
        console.error(err);
        alert(err.message);
    }
}

// 결제 취소 후 CREATED 상태로 복원
function revertToCreated(orderId, td, order) {
    fetch(`/orders/${orderId}/revert`, {
        method: 'POST',
        headers: acctHeaders()
    })
        .then(res => {
            if (!res.ok) throw new Error('취소 복원 실패');
            td.innerHTML = '';
            td.appendChild(createButton('결제', () => payOrder(order)));
            td.appendChild(createButton('취소', () => cancelOrder(orderId, td, order)));
        })
        .catch(err => console.error(err));
}

// 주문 취소
// ----------------- 결제 취소 / 환불 -----------------
async function cancelOrder(orderId, actionTd) {
    if (!confirm('정말 결제를 취소하고 환불하시겠습니까?')) return;

    try {
        const xsrf = getCookie('XSRF-TOKEN');
        const res = await fetch(`/orders/${orderId}/cancel`, {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json',
                'X-XSRF-TOKEN': xsrf
            }
        });

        if (!res.ok) throw new Error('결제 취소/환불 실패');
        const updatedOrder = await res.json();
        console.log('결제 취소 후 상태:', updatedOrder);

        // UI 갱신
        actionTd.innerHTML = '';
        if (updatedOrder.status === 'CANCELLED') {
            actionTd.textContent = '취소됨';
        } else {
            // 혹시 CREATED로 돌아온 경우 버튼 재생성
            actionTd.appendChild(createButton('결제', () => handlePayment(updatedOrder)));
            actionTd.appendChild(createButton('결제 취소', () => cancelOrder(orderId, actionTd)));
        }

        alert('결제 취소 및 환불 처리 완료');

    } catch (err) {
        console.error(err);
        alert(err.message);
    }
}


// ----- 주문 내역 불러오기 -----
async function loadOrders() {
    const tbody = document.querySelector('#ordersTable tbody');
    tbody.innerHTML = '';

    try {
        const res = await fetch('/orders/buy', {
            headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
        }); // 구매 내역 API 호출
        if (!res.ok) throw new Error('주문 내역 불러오기 실패');

        const orders = await res.json();
        if (!orders || orders.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8">주문 내역이 없습니다.</td></tr>`;
            return;
        }

        for (const order of orders) {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${order.id}</td>
                <td><a href="/productDetail.html?id=${order.listingId}">${order.listingTitle ?? '-'}</a></td>
                <td>${order.method ?? '-'}</td>
                <td>${order.status ?? '-'}</td>
                <td>${order.finalPrice ?? '-'}</td>
                <td></td>
            `;
            const actionTd = tr.querySelector('td:last-child');

            // 상태별 버튼/텍스트 처리
            if (order.status === 'CREATED') {
                actionTd.appendChild(createButton('결제', () => payOrder(order)));
                actionTd.appendChild(createButton('취소', () => cancelOrder(order.id, actionTd, order)));
            } else if (order.status === 'PAID') {
                actionTd.appendChild(createButton('주문 확정', () => completeOrder(order.id, actionTd)));
                actionTd.appendChild(createButton('결제 취소', () => revertToCreated(order.id, actionTd, order)));
            } else if (order.status === 'COMPLETED') {
                actionTd.textContent = '주문 확정';
            } else if (order.status === 'CANCELLED') {
                actionTd.textContent = '취소됨';
            }

            tbody.appendChild(tr);
        }
    } catch (err) {
        console.error(err);
        tbody.innerHTML = `<tr><td colspan="8">주문 내역을 불러오는 중 오류가 발생했습니다.</td></tr>`;
    }
}






/*찜한 상품*/
document.addEventListener('DOMContentLoaded', () => {
  const favTabLink = document.querySelector('.mp-link[data-tab="favorites"]');
  if (favTabLink) {
    favTabLink.addEventListener('click', () => loadFavorites()); // 탭 클릭 시 로드
  }

  // URL에 #favorites로 진입한 경우 자동 로드
  if (location.hash === '#favorites') loadFavorites();
});

async function loadFavorites() {
  const section = document.getElementById('tab-favorites');
  if (!section || section.dataset.loaded === '1') return; // 중복 로드 방지
  section.dataset.loaded = '1';

  const grid = document.getElementById('favGrid');
  const empty = document.getElementById('favEmpty');
  grid.innerHTML = '<div class="loading">불러오는 중…</div>';
  empty.style.display = 'none';

  try {
    // 1) 내 찜 ID 목록
    const res = await fetch('/product/wish/my', {
        headers: { 'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'},
        credentials: 'include'
    });
    if (res.status === 401) {
      grid.innerHTML = '<p>로그인 후 확인할 수 있습니다.</p>';
      return;
    }
    if (!res.ok) throw new Error('찜 목록 조회 실패');
    const data = await res.json();
    const ids = Array.isArray(data.listingIds) ? data.listingIds : [];

    if (ids.length === 0) {
      grid.innerHTML = '';
      empty.style.display = 'block';
      return;
    }

    // 2) 각 상품 상세 병렬 조회
    const products = (await Promise.all(
      ids.map(id =>
        fetch(`/product/${encodeURIComponent(id)}`, {
            headers: { 'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'}
        })
          .then(r => (r.ok ? r.json() : null))
          .catch(() => null)
      )
    )).filter(Boolean);

    // 3) 렌더
    grid.innerHTML = '';
    products.forEach(p => grid.appendChild(renderFavCard(p)));

    // 전부 실패하면 빈 상태 표시
    if (!grid.children.length) empty.style.display = 'block';
  } catch (e) {
    console.error(e);
    grid.innerHTML = '<p>불러오지 못했습니다. 새로고침 해주세요.</p>';
  }
}

function renderFavCard(p) {
  const tpl = document.getElementById('tplProduct');
  const node = tpl.content.firstElementChild.cloneNode(true);

  const a = node.querySelector('a');
  const img = node.querySelector('img');
  const title = node.querySelector('.title');
  const price = node.querySelector('.price');
  const meta = node.querySelector('.meta');

  a.href = `/productDetail.html?id=${p.listingId}`;

  const imgUrl =
    (p.photoUrls && p.photoUrls.length > 0)
      ? (p.photoUrls[0].startsWith('/uploads') ? p.photoUrls[0] : `/uploads/${p.photoUrls[0]}`)
      : 'https://placehold.co/300x200?text=No+Image';
  img.src = imgUrl;
  img.alt = p.title || '상품';

  title.textContent = p.title || '';
  price.textContent = (p.price != null) ? `${Number(p.price).toLocaleString()} 원` : '';
  meta.textContent = ''; // 필요 없으면 비워둠 (원하면 `찜 ${p.wishCount ?? 0}` 등 표시)

  // (옵션) 찜 해제 버튼
  const unwishBtn = document.createElement('button');
  unwishBtn.type = 'button';
  unwishBtn.className = 'unwish-btn';
  unwishBtn.textContent = '♡ 해제';
  unwishBtn.addEventListener('click', async (e) => {
    e.preventDefault();
      if (!getCookie('XSRF-TOKEN')) await ensureCsrf();
      const xsrf = getCookie('XSRF-TOKEN');
    try {
      const r = await fetch(`/product/${encodeURIComponent(p.listingId)}/wish`, {
          method: 'DELETE',
          headers: { 'Accept': 'application/json',
              'X-Requested-With': 'XMLHttpRequest',
              'X-XSRF-TOKEN': xsrf
          },
          credentials: 'include'
      });
      if (!r.ok) throw new Error('해제 실패');
      node.remove();
      if (!document.querySelector('#favGrid .item')) {
        document.getElementById('favEmpty').style.display = 'block';
      }
    } catch (err) {
      console.error(err);
      alert('찜 해제에 실패했습니다.');
    }
  });

  node.style.position = 'relative';
  unwishBtn.style.position = 'absolute';
  unwishBtn.style.top = '8px';
  unwishBtn.style.right = '8px';
  unwishBtn.style.background = 'rgba(0,0,0,.55)';
  unwishBtn.style.color = '#fff';
  unwishBtn.style.border = '0';
  unwishBtn.style.borderRadius = '12px';
  unwishBtn.style.fontSize = '12px';
  unwishBtn.style.padding = '2px 8px';
  node.appendChild(unwishBtn);

  return node;
}
(function(){
    const box = document.querySelector('#tab-support .empty');
    const btnRow = document.querySelector('#tab-support .flex-head .btn-row');
    if (!box) return;

    function renderList(items, isAdmin) {
        if (!items || items.length === 0) {
            box.textContent = '접수한 신고/문의가 없습니다.';
            return;
        }
        const ul = document.createElement('ul');
        ul.style.listStyle = 'disc';
        ul.style.paddingLeft = '1.25rem';
        items.forEach(it => {
            const li = document.createElement('li');
            li.style.margin = '12px 0';

            const status = typeof it.status === 'string' ? it.status.trim().toUpperCase() : '';
            const isAnswered = status === 'ANSWERED';

            // 제목 + 상태 배지 래퍼
            const head = document.createElement('div');
            head.style.display = 'flex';
            head.style.alignItems = 'center';
            head.style.gap = '8px';

            const title = document.createElement('a');
            title.textContent = it.title || '(제목 없음)';
            title.style.display = 'block';
            title.href = '#';
            title.style.cursor = 'pointer';

            head.appendChild(title);

            if (isAnswered) {
                const badge = document.createElement('span');
                badge.textContent = '답변 완료';
                badge.style.display = 'inline-block';
                badge.style.padding = '2px 6px';
                badge.style.borderRadius = '10px';
                badge.style.backgroundColor = '#e6f6ea';
                badge.style.color = '#15803d';
                badge.style.fontSize = '0.8em';
                badge.style.border = '1px solid #86efac';
                head.appendChild(badge);
            }

            li.appendChild(head);

            const p = document.createElement('p');
            p.textContent = it.content || '';
            p.style.margin = '4px 0 0';
            p.style.color = '#555';
            p.style.fontSize = '0.95em';
            p.style.textAlign = 'left';
            p.style.display = 'none';
            li.appendChild(p);

            const replyBox = document.createElement('div');
            replyBox.style.display = 'none';
            replyBox.style.marginTop = '8px';
            replyBox.style.textAlign = 'left';
            li.appendChild(replyBox);

            if (isAdmin) {
                // 관리자: 답변 입력 폼 또는 상태 표시
                if (isAnswered) {
                    const done = document.createElement('div');
                    done.textContent = '답변 완료된 문의입니다.';
                    done.style.color = '#15803d';
                    done.style.fontSize = '0.9em';
                    replyBox.appendChild(done);
                } else {
                    const ta = document.createElement('textarea');
                    ta.rows = 3;
                    ta.placeholder = '답변을 입력하세요';
                    ta.style.width = '100%';
                    ta.maxLength = 2000;

                    const btnWrap = document.createElement('div');
                    btnWrap.className = 'btn-row';
                    btnWrap.style.marginTop = '6px';

                    const btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'btn btn-primary';
                    btn.textContent = '답변하기';

                    btn.addEventListener('click', async () => {
                        const content = (ta.value || '').trim();
                        if (!content) {
                            alert('답변 내용을 입력하세요.');
                            ta.focus();
                            return;
                        }
                        try {
                            btn.disabled = true;
                            const res = await fetch(`/api/inquiries/${it.inquiryId}/replies`, {
                                method: 'POST',
                                headers: acctHeaders(),
                                credentials: 'same-origin',
                                body: JSON.stringify({ content })
                            });
                            if (!res.ok) {
                                const msg = await res.text().catch(() => '');
                                throw new Error(msg || '저장에 실패했습니다.');
                            }
                            alert('답변이 등록되었습니다.');
                            ta.value = '';
                            // UI 반영: 상태를 ANSWERED로 표시하고 배지 추가, 입력 폼을 상태 안내로 교체
                            it.status = 'ANSWERED';
                            const badge = document.createElement('span');
                            badge.textContent = '답변 완료';
                            badge.style.display = 'inline-block';
                            badge.style.padding = '2px 6px';
                            badge.style.borderRadius = '10px';
                            badge.style.backgroundColor = '#e6f6ea';
                            badge.style.color = '#15803d';
                            badge.style.fontSize = '0.8em';
                            badge.style.border = '1px solid #86efac';
                            if (title.parentElement) {
                                title.parentElement.appendChild(badge);
                            }
                            replyBox.innerHTML = '';
                            const done = document.createElement('div');
                            done.textContent = '답변 완료된 문의입니다.';
                            done.style.color = '#15803d';
                            done.style.fontSize = '0.9em';
                            replyBox.appendChild(done);
                        } catch (err) {
                            console.error(err);
                            alert('답변 등록에 실패했습니다. 잠시 후 다시 시도해 주세요.');
                        } finally {
                            btn.disabled = false;
                        }
                    });

                    btnWrap.appendChild(btn);
                    replyBox.appendChild(ta);
                    replyBox.appendChild(btnWrap);
                }

                const toggle = (e) => {
                    e.preventDefault();
                    const willShow = replyBox.style.display === 'none';
                    replyBox.style.display = willShow ? 'block' : 'none';
                    p.style.display = willShow ? 'block' : 'none';
                };
                title.addEventListener('click', toggle);
                p.addEventListener('click', toggle);
            } else {
                // 일반 사용자: 답변 목록 로드/토글
                let loaded = false;
                const listWrap = document.createElement('div');
                replyBox.appendChild(listWrap);

                async function loadReplies() {
                    listWrap.textContent = '답변을 불러오는 중...';
                    try {
                        const res = await fetch(`/api/inquiries/${it.inquiryId}/replies`, {
                            headers: { 'Accept': 'application/json',
                                'X-Requested-With': 'XMLHttpRequest'},
                            credentials: 'same-origin'
                        });
                        if (res.status === 401) {
                            listWrap.textContent = '로그인이 필요합니다.';
                            return;
                        }
                        if (res.status === 403) {
                            listWrap.textContent = '접근 권한이 없습니다.';
                            return;
                        }
                        if (res.status === 404) {
                            listWrap.textContent = '문의가 존재하지 않습니다.';
                            return;
                        }
                        if (!res.ok) throw new Error('failed');
                        const replies = await res.json();
                        if (!replies || replies.length === 0) {
                            listWrap.textContent = '등록된 답변이 없습니다.';
                            return;
                        }
                        const ul2 = document.createElement('ul');
                        ul2.style.listStyle = 'circle';
                        ul2.style.paddingLeft = '1.25rem';
                        replies.forEach(r => {
                            const li2 = document.createElement('li');
                            li2.textContent = r.content || '';
                            ul2.appendChild(li2);
                        });
                        listWrap.innerHTML = '';
                        listWrap.appendChild(ul2);
                    } catch (e) {
                        console.error(e);
                        listWrap.textContent = '답변을 불러오지 못했습니다.';
                    }
                }

                const toggle = (e) => {
                    e.preventDefault();
                    const willShow = replyBox.style.display === 'none';
                    replyBox.style.display = willShow ? 'block' : 'none';
                    p.style.display = willShow ? 'block' : 'none';
                    if (willShow && !loaded) {
                        loaded = true;
                        loadReplies();
                    }
                };
                title.addEventListener('click', toggle);
                p.addEventListener('click', toggle);
            }

            ul.appendChild(li);
        });
        box.innerHTML = '';
        box.appendChild(ul);
    }

    // 사용자 상태 확인 후 관리자면 버튼 숨기고 전체 문의, 아니면 내 문의만
    fetch('/api/user/status', {
        headers: { 'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'},
        credentials: 'same-origin'
    })
        .then(res => res.ok ? res.json() : Promise.reject())
        .then(info => {
            const isAdmin = info && info.username === '관리자';
            if (isAdmin && btnRow) {
                btnRow.style.display = 'none';
            }
            const url = isAdmin ? '/api/inquiries' : '/api/inquiries/my';
            return Promise.all([Promise.resolve(isAdmin), fetch(url, {
                headers: { 'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'},
                credentials: 'same-origin'
            })]);
        })
        .then(([isAdmin, res]) => {
            if (res.status === 401) {
                box.textContent = '로그인 후 신고/문의 내역을 확인할 수 있습니다.';
                return null;
            }
            if (res.status === 403) {
                box.textContent = '접근 권한이 없습니다.';
                return null;
            }
            if (!res.ok) throw new Error('failed');
            return Promise.all([Promise.resolve(isAdmin), res.json()]);
        })
        .then(pair => {
            if (!pair) return;
            const [isAdmin, data] = pair;
            if (data) renderList(data, isAdmin);
        })
        .catch(() => {
            box.textContent = '내역을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.';
        });
})();

const ALL_PROVIDERS = ['kakao','naver','google'];
const CONNECT_URL = (p) => `/api/oauth/connect/${p}`;

async function loadOauthLinks() {
    const wrap = document.getElementById('oauthLinked');
    if (!wrap) return;
    wrap.innerHTML = '<li class="empty">불러오는 중...</li>';
    try {
        const r = await fetch(ENDPOINTS.oauthMe, {
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            credentials: 'same-origin'
        });
        if (r.status === 401) {
            wrap.innerHTML = '<li class="empty">로그인 후 이용해 주세요.</li>';
            return;
        }
        if (!r.ok) throw new Error('상태 조회 실패');

        const j = await r.json();
        const payload = j.data || j;
        const links = payload.linkedProviders || [];
        const canUnlink = !!payload.canUnlink;
        console.log("payload: " + payload);
        _authState.linkedProviders = Array.isArray(payload.linkedProviders) ? payload.linkedProviders : [];

        if (!links.length) {
            wrap.innerHTML = '<li class="empty">연결된 소셜 계정이 없습니다.</li>';
            return;
        }

        wrap.innerHTML = links.map(p => `
      <li class="provider-item" data-provider="${p}">
        <span class="provider-name">${p}</span>
        <span class="provider-actions">
          ${canUnlink ? '<button class="btn-unlink" type="button">연결 해제</button>' : ''}
        </span>
      </li>
    `).join('');
    } catch (e) {
        wrap.innerHTML = `<li class="empty">불러오기 실패: ${e.message}</li>`;
    }
}

document.addEventListener('click', async (e) => {
    const item = e.target.closest('#oauth-links .provider-item');
    if (!item) return;

    if (e.target.classList.contains('btn-unlink')) {
        if (!getCookie('XSRF-TOKEN')) await ensureCsrf();
        const xsrf = getCookie('XSRF-TOKEN');
        const provider = item.getAttribute('data-provider');
        if (!confirm(`${provider} 연결을 해제할까요?`)) return;
        try {
            const r = await fetch(ENDPOINTS.oauthUnlink, {
                method: 'POST',
                headers: {
                    'Content-Type':'application/json',
                    'X-XSRF-TOKEN': xsrf
                },
                body: JSON.stringify({ provider })
            });
            noAuthGuard(r);
            await loadOauthLinks();
            alert('해제되었습니다.');
        } catch (err) {
            alert('해제 실패: ' + (err.message || '요청 오류'));
        }
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

async function popFlash() {
    try {
        const r = await fetch('/api/flash', {
           headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
           credentials: 'same-origin'
        });
        if (!r.ok) return;
        const j = await r.json();
        if (j && j.message) {
            alert(j.message);
        }
    } catch (_) {}
}

function openDeleteModal() {
    const m = document.getElementById('deleteModal');
    if (!m) return;
    m.classList.add('is-open');
    m.setAttribute('aria-hidden', 'false');
    document.getElementById('deleteConfirm').value = '';
    document.getElementById('deleteError').textContent = '';
}
function closeDeleteModal() {
    const m = document.getElementById('deleteModal');
    if (!m) return;
    m.classList.remove('is-open');
    m.setAttribute('aria-hidden', 'true');
}

async function doDeleteAccount(password, reason) {
    await ensureCsrf();
    const payload = {
        password: password || '',
        reason: (reason || '').trim(),
        wipeConvenienceData: true
    };
    const r = await fetch(ENDPOINTS.deleteMe, {
        method: 'DELETE',
        headers: acctHeaders(),
        body: JSON.stringify(payload)
    });
    if (!r.ok) {
        const msg = await r.text().catch(()=> '');
        throw new Error(msg || '탈퇴 요청 실패');
    }
}

function startOauthReauth(provider, after = 'delete') {
    const returnUrl = `${location.origin}${location.pathname}${location.search}#account`;
    // 탈퇴 사유를 소셜 라운드트립 동안 보관
    const reason = document.getElementById('deleteReason')?.value || '';
    try { sessionStorage.setItem('deleteReason', reason); } catch {}
    // 서버는 /api/oauth/reauth/{provider}에서 재로그인 후
    // ?reauth=ok&after=delete 로 리다이렉트해 주도록 구현
    location.href = `/api/oauth/reauth/${encodeURIComponent(provider)}?return=${encodeURIComponent(returnUrl)}&after=${encodeURIComponent(after)}`;
}

function openAuthChoiceSheet() {
    // 이미 떠있으면 제거
    document.getElementById('authChoiceSheet')?.remove();

    const sheet = document.createElement('div');
    sheet.id = 'authChoiceSheet';
    sheet.style.position = 'fixed';
    sheet.style.left = 0; sheet.style.right = 0; sheet.style.bottom = 0;
    sheet.style.background = '#fff'; sheet.style.borderTop = '1px solid #eee';
    sheet.style.padding = '16px'; sheet.style.boxShadow = '0 -6px 20px rgba(0,0,0,.12)';
    sheet.style.zIndex = 9999;

    const title = document.createElement('div');
    title.textContent = '본인 확인 방법을 선택하세요';
    title.style.fontWeight = '600';
    title.style.marginBottom = '12px';
    sheet.appendChild(title);

    // ① 비밀번호로 인증
    const btnPw = document.createElement('button');
    btnPw.className = 'btn btn-primary';
    btnPw.textContent = '비밀번호로 인증';
    btnPw.style.marginRight = '8px';
    btnPw.onclick = () => {
        sheet.remove();
        openReauth(); // 이미 있는 비번 재인증 모달 열기
        // reauthForm onsubmit은 아래 deleteForm 핸들러에서 설정
    };
    sheet.appendChild(btnPw);

    // ② 소셜로 인증 (연결된 provider 버튼들)
    (_authState.linkedProviders || []).forEach(pv => {
        const b = document.createElement('button');
        b.className = 'btn';
        b.style.marginRight = '8px';
        b.textContent = `${pv}로 인증`;
        b.onclick = () => startOauthReauth(pv, 'delete');
        sheet.appendChild(b);
    });

    // 닫기
    const close = document.createElement('button');
    close.className = 'btn-secondary';
    close.style.float = 'right';
    close.textContent = '닫기';
    close.onclick = () => sheet.remove();
    sheet.appendChild(close);

    document.body.appendChild(sheet);
}

async function openAuthChoiceWithEnsure() {
    if (!_authState?.linkedProviders?.length) {
        try { await loadOauthLinks(); } catch {}
    }
    openAuthChoiceSheet();
}