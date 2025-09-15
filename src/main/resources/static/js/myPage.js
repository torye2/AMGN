// ----- Endpoints -----
const ENDPOINTS = {
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
        const res = await fetch(ENDPOINTS.addresses);
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
            headers: {'Content-Type': 'application/json'},
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
        const r = await fetch(ENDPOINTS.addressSetDefault(id), {method:'PATCH'});
        noAuthGuard(r);
        await loadAddresses();
    } catch (e) { alert('대표 지정 실패: ' + e.message); }
}

async function deleteAddress(id) {
    if (!confirm('이 주소를 삭제하시겠어요?')) return;
    try {
        const r = await fetch(ENDPOINTS.addressById(id), {method:'DELETE'});
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
                const r = await fetch(ENDPOINTS.addressById(id));
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

// 로그인 상태/유저 식별자 가져오기
async function fetchMe() {
    const res = await fetch(ENDPOINTS.meStatus);
    noAuthGuard(res);
    // { isLoggedIn, nickname, userId(로그인ID 문자열) }
    return res.json();
}

// 주문에서 내 역할 추정(SELLER/BUYER/UNKNOWN)
function inferRole(order, me) {
    // 가능하면 숫자 PK로 비교하고, 없으면 닉네임으로 비교
    if (order.sellerId != null && order.buyerId != null && me.userId != null) {
        if (order.sellerId === me.userId) return 'SELLER';
        if (order.buyerId === me.userId) return 'BUYER';
    }
    if (order.sellerName && me.nickName) {
        if (order.sellerName === me.nickName) return 'SELLER';
    }
    if (order.buyerName && me.nickName) {
        if (order.buyerName === me.nickName) return 'BUYER';
    }
    return 'UNKNOWN';
}

// ----- Navigation -----
function switchTab(name) {
    $$('.mp-link').forEach(n => n.classList.toggle('active', n.dataset.tab === name));
    $$('.tab').forEach(p => p.hidden = p.id !== 'tab-' + name);
    if (name === 'dashboard') loadDashboard();
    if (name === 'products') loadProducts('ON_SALE');
    if (name === 'favorites') loadFavorites();
    if (name === 'sales') loadSales();
    if (name === 'purchases') loadPurchases();
    if (name === 'reviews') loadReviews();
    if (name === 'alerts') loadAlerts();
    if (name === 'shop') loadShopSettings();
    if (name === 'account') { loadAccount(); loadOauthLinks(); }
    if (name === 'addresses') loadAddresses();
}

document.addEventListener('click', (e) => {
    const tabBtn = e.target.closest('.mp-link[data-tab]');
    if (tabBtn) {
        switchTab(tabBtn.dataset.tab);
    }

    const subBtn = e.target.closest('#tab-products .subtabs button');
    if (subBtn) {
        $$('#tab-products .subtabs button').forEach(b => b.classList.remove('active'));
        subBtn.classList.add('active');
        loadProducts(subBtn.dataset.status);
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
        const [onSale, orders, me, reviewable] = await Promise.all([
            fetch(ENDPOINTS.myProducts('ON_SALE')).then(noAuthGuard).then(r => r.json()),
            fetch(ENDPOINTS.orders).then(noAuthGuard).then(r => r.json()),
            fetchMe(),
            ENDPOINTS.reviewableOrders ? fetch(ENDPOINTS.reviewableOrders).then(noAuthGuard).then(r => r.json()) : Promise.resolve([])
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
        $('#statReviews').textContent = reviewable?.length ?? 0; // "작성가능 리뷰" 개수로 임시 표시
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
          const r = await fetch('/product/wish/my/count', { credentials:'include' });
          const { count } = r.ok ? await r.json() : { count: 0 };
          const w = document.getElementById('metaWish');
          if (w) w.textContent = String(count ?? 0);
        } catch {
          const w = document.getElementById('metaWish');
          if (w) w.textContent = '0';
        }

    } catch (err) {
        console.warn(err);
    }
}

async function loadProducts(status) {
    const grid = $('#productsGrid');
    const empty = $('#productsEmpty');
    grid.innerHTML = '';
    empty.hidden = true;
    for (let i = 0; i < 6; i++) {
        const sk = document.createElement('div');
        sk.className = 'item skeleton';
        sk.style.height = '236px';
        grid.appendChild(sk);
    }
    try {
        const res = await fetch(ENDPOINTS.myProducts(status));
        noAuthGuard(res);
        const list = await res.json();
        grid.innerHTML = '';
        if (!list.length) {
            empty.hidden = false;
            return;
        }
        list.forEach(p => grid.appendChild(renderProductCard(p)));
    } catch (err) {
        grid.innerHTML = `<div class="empty">불러오기 실패: ${err.message}</div>`;
    }
}

async function loadFavorites() {
    const grid = $('#favGrid');
    const empty = $('#favEmpty');
    grid.innerHTML = '';
    empty.hidden = true;
    if (!ENDPOINTS.favorites) {
        grid.innerHTML = `<div class='empty'>찜 기능이 아직 준비되지 않았습니다.</div>`;
        return;
    }
    try {
        const res = await fetch(ENDPOINTS.favorites);
        noAuthGuard(res);
        const list = await res.json();
        if (!list.length) {
            empty.hidden = false;
            return;
        }
        list.forEach(p => grid.appendChild(renderProductCard(p)));
    } catch (err) {
        grid.innerHTML = `<div class='empty'>불러오기 실패: ${err.message}</div>`;
    }
}

async function loadSales() {
    const body = $('#salesBody');
    body.innerHTML = '';
    try {
        const [orders, me] = await Promise.all([
            fetch(ENDPOINTS.orders).then(noAuthGuard).then(r => r.json()),
            fetchMe()
        ]);
        const rows = [];
        (orders || []).forEach(o => {
            const role = inferRole(o, me);
            if (role === 'SELLER' || role === 'UNKNOWN') {
                rows.push(`<tr>
          <td>${o.orderId}</td>
          <td><a href="/productDetail.html?id=${o.listingId}">${o.title ?? '-'}</a></td>
          <td>${o.buyerName ?? '-'}</td>
          <td>${toWon(o.price)}</td>
          <td>${o.status ?? '-'}</td>
          <td>${o.orderedAt ?? '-'}</td>
        </tr>`);
            }
        });
        body.innerHTML = rows.length ? rows.join('') : `<tr><td colspan="6" class="empty">판매 내역이 없습니다.</td></tr>`;
    } catch (err) {
        body.innerHTML = `<tr><td colspan="6" class="empty">불러오기 실패: ${err.message}</td></tr>`;
    }
}

async function loadPurchases() {
    const body = $('#purchasesBody');
    body.innerHTML = '';
    try {
        const [orders, me] = await Promise.all([
            fetch(ENDPOINTS.orders).then(noAuthGuard).then(r => r.json()),
            fetchMe()
        ]);
        const rows = [];
        (orders || []).forEach(o => {
            const role = inferRole(o, me);
            if (role === 'BUYER' || role === 'UNKNOWN') {
                rows.push(`<tr>
          <td>${o.orderId}</td>
          <td><a href="/productDetail.html?id=${o.listingId}">${o.title ?? '-'}</a></td>
          <td>${o.sellerName ?? '-'}</td>
          <td>${toWon(o.price)}</td>
          <td>${o.status ?? '-'}</td>
          <td>${o.orderedAt ?? '-'}</td>
        </tr>`);
            }
        });
        body.innerHTML = rows.length ? rows.join('') : `<tr><td colspan="6" class="empty">구매 내역이 없습니다.</td></tr>`;
    } catch (err) {
        body.innerHTML = `<tr><td colspan="6" class="empty">불러오기 실패: ${err.message}</td></tr>`;
    }
}

async function loadReviews() {
    const body = $('#reviewsBody');
    body.innerHTML = '';
    if (!ENDPOINTS.reviewableOrders) {
        body.innerHTML = `<tr><td colspan="4" class="empty">리뷰 API가 아직 없습니다.</td></tr>`;
        return;
    }
    try {
        const list = await fetch(ENDPOINTS.reviewableOrders).then(noAuthGuard).then(r => r.json());
        if (!list.length) {
            body.innerHTML = `<tr><td colspan="4" class="empty">작성 가능한 리뷰가 없습니다.</td></tr>`;
            return;
        }
        body.innerHTML = list.map(r => `<tr>
      <td>-</td>
      <td>주문 #${r.orderId ?? '-'} 리뷰 작성 가능</td>
      <td>${r.counterpartyName ?? '-'}</td>
      <td>${r.createdAt ?? '-'}</td>
    </tr>`).join('');
    } catch (err) {
        body.innerHTML = `<tr><td colspan="4" class="empty">불러오기 실패: ${err.message}</td></tr>`;
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
        const res = await fetch(ENDPOINTS.notifications);
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
    const t = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const h = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const m = {'Content-Type': 'application/json'};
    if (t && h) m[h] = t;
    return m;
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
        const res = await fetch(ENDPOINTS.userProfile);
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
        const r = await fetch(ENDPOINTS.idAvailable(candidate));
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
// Bind buttons on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('btnCheckId')?.addEventListener('click', accountCheckId);
    document.getElementById('btnStartEdit')?.addEventListener('click', openReauth);
    document.getElementById('btnReauthCancel')?.addEventListener('click', closeReauth);
    document.getElementById('reauthForm')?.addEventListener('submit', submitReauth);
    const chatBtn = document.getElementById('chat-btn');
    chatBtn.addEventListener('click', () => {
        window.location.href = `/chatPage.html?roomId=1&listingId=1&sellerId=2`;
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
            province: document.getElementById('province').value.trim(),
            city: document.getElementById('city').value.trim(),
            detailAddress: document.getElementById('detailAddress').value.trim()
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
$('#btnDelete')?.addEventListener('click', async () => {
    alert('회원 탈퇴 API가 아직 준비되지 않았습니다.');
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
    const initial = (location.hash === '#favorites') ? 'favorites' : 'dashboard';
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
        // 실제 결제 구현 없으므로 빈 객체 전송
        const res = await fetch(`/orders/${order.id}/pay`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({})
        });

        // 결제 완료 후 버튼 업데이트
        const tr = Array.from(document.querySelectorAll('#ordersTable tbody tr'))
            .find(r => r.querySelector('td').textContent === String(order.id));
        if (tr) {
            const td = tr.querySelector('td:last-child');
            td.innerHTML = '';
            td.appendChild(createButton('주문 확정', () => completeOrder(order.id, td)));
            td.appendChild(createButton('결제 취소', () => revertToCreated(order.id, td, order)));
        }

        alert('결제가 완료되었습니다.');
    } catch (err) {
        console.error(err);
        alert('결제 처리 중 오류가 발생했습니다: ' + err.message);
    }
}

// 주문 확정
async function completeOrder(orderId, td) {
    try {
        const res = await fetch(`/orders/${orderId}/complete`, {method: 'POST'});
        if (!res.ok) throw new Error('주문 확정 실패');
        td.textContent = '주문 확정';
    } catch (err) {
        console.error(err);
        alert(err.message);
    }
}

// 결제 취소 후 CREATED 상태로 복원
function revertToCreated(orderId, td, order) {
    fetch(`/orders/${orderId}/revert`, {method: 'POST'})
        .then(res => {
            if (!res.ok) throw new Error('취소 복원 실패');
            td.innerHTML = '';
            td.appendChild(createButton('결제', () => payOrder(order)));
            td.appendChild(createButton('취소', () => cancelOrder(orderId, td, order)));
        })
        .catch(err => console.error(err));
}

// 주문 취소
function cancelOrder(orderId, td, order) {
    fetch(`/orders/${orderId}/cancel`, {method: 'DELETE'})
        .then(res => {
            if (!res.ok) throw new Error('취소 실패');
            alert('주문이 취소되었습니다.');
            td.innerHTML = '';
            td.appendChild(createButton('결제', () => payOrder(order)));
            td.appendChild(createButton('취소', () => cancelOrder(orderId, td, order)));
        })
        .catch(err => console.error(err));
}

// ----- 주문 내역 불러오기 -----
async function loadOrders() {
    const tbody = document.querySelector('#ordersTable tbody');
    tbody.innerHTML = '';

    try {
        const res = await fetch('/orders/buy'); // 구매 내역 API 호출
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


loadOrders();


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
    const res = await fetch('/product/wish/my', { credentials: 'include' });
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
        fetch(`/product/${encodeURIComponent(id)}`)
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
    try {
      const r = await fetch(`/product/${encodeURIComponent(p.listingId)}/wish`, {
        method: 'DELETE',
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
                                headers: { 'Content-Type': 'application/json' },
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
                        const res = await fetch(`/api/inquiries/${it.inquiryId}/replies`, { credentials: 'same-origin' });
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
    fetch('/api/user/status', { credentials: 'same-origin' })
        .then(res => res.ok ? res.json() : Promise.reject())
        .then(info => {
            const isAdmin = info && info.username === '관리자';
            if (isAdmin && btnRow) {
                btnRow.style.display = 'none';
            }
            const url = isAdmin ? '/api/inquiries' : '/api/inquiries/my';
            return Promise.all([Promise.resolve(isAdmin), fetch(url, { credentials: 'same-origin' })]);
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

async function loadOauthLinks() {
    const wrap = document.getElementById('oauthLinked');
    if (!wrap) return;
    wrap.innerHTML = '<li class="empty">불러오는 중...</li>';
    try {
        const r = await fetch(ENDPOINTS.oauthMe);
        noAuthGuard(r);
        const j = await r.json();
        const payload = j.data || j; // ApiResult 래핑 호환
        const links = payload.linkedProviders || [];
        const canUnlink = !!payload.canUnlink;

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
        const provider = item.getAttribute('data-provider');
        if (!confirm(`${provider} 연결을 해제할까요?`)) return;
        try {
            const r = await fetch(ENDPOINTS.oauthUnlink, {
                method: 'POST',
                headers: {'Content-Type':'application/json'},
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
