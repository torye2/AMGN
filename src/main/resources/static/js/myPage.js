// ----- Endpoints -----
const ENDPOINTS = {
    // ListingController
    myProducts: (status) => `/product/my-products${status ? `?status=${status}` : ''}`,
    favorites: undefined, // 없으면 탭 숨김 권장

    // OrderController
    orders: '/orders', // 내 주문 전체(판매/구매 혼합) → 클라이언트에서 역할 추정/분리

    // ReviewController
    reviewableOrders: '/api/reviews/mine', // "받은 후기" API가 없으므로 임시로 사용

    // AuthStatusController
    meStatus: '/api/user/status', // { isLoggedIn, nickname, userId(로그인ID 문자열) }

    // (없다면 주석처리)
    notifications: undefined, // '/notifications'
    meShop: undefined, // '/me/shop'
    userProfile: '/api/user/profile',            // GET/PUT: 이메일, 연락처, 알림 설정
    verifyPassword: '/api/user/verify-password', // POST: {password}
};

// ----- Utils -----
const $ = (sel, el=document) => el.querySelector(sel);
const $$ = (sel, el=document) => Array.from(el.querySelectorAll(sel));
const toWon = (n) => (n==null || isNaN(n)) ? '-' : Number(n).toLocaleString('ko-KR') + '원';
function noAuthGuard(res){
    if(!res.ok){
        if(res.status === 401){ alert('로그인이 필요합니다.'); location.href='/login'; }
        throw new Error('요청 실패 ('+res.status+')');
    }
    return res;
}

// 로그인 상태/유저 식별자 가져오기
async function fetchMe(){
    const res = await fetch(ENDPOINTS.meStatus); noAuthGuard(res);
    // { isLoggedIn, nickname, userId(로그인ID 문자열) }
    return res.json();
}

// 주문에서 내 역할 추정(SELLER/BUYER/UNKNOWN)
function inferRole(order, me){
    // 가능하면 숫자 PK로 비교하고, 없으면 닉네임으로 비교
    if(order.sellerId != null && order.buyerId != null && me.userId != null){
        if(order.sellerId === me.userId) return 'SELLER';
        if(order.buyerId  === me.userId) return 'BUYER';
    }
    if(order.sellerName && me.nickName){
        if(order.sellerName === me.nickName) return 'SELLER';
    }
    if(order.buyerName && me.nickName){
        if(order.buyerName === me.nickName) return 'BUYER';
    }
    return 'UNKNOWN';
}

// ----- Navigation -----
function switchTab(name){
    $$('.mp-link').forEach(n=> n.classList.toggle('active', n.dataset.tab===name));
    $$('.tab').forEach(p=> p.hidden = p.id !== 'tab-'+name);
    if (name==='dashboard') loadDashboard();
    if (name==='products') loadProducts('ON_SALE');
    if (name==='favorites') loadFavorites();
    if (name==='sales') loadSales();
    if (name==='purchases') loadPurchases();
    if (name==='reviews') loadReviews();
    if (name==='alerts') loadAlerts();
    if (name==='shop') loadShopSettings();
    if (name==='account') loadAccount();
}

document.addEventListener('click', (e)=>{
    const tabBtn = e.target.closest('.mp-link[data-tab]');
    if (tabBtn){ switchTab(tabBtn.dataset.tab); }

    const subBtn = e.target.closest('#tab-products .subtabs button');
    if (subBtn){
        $$('#tab-products .subtabs button').forEach(b=> b.classList.remove('active'));
        subBtn.classList.add('active');
        loadProducts(subBtn.dataset.status);
    }
});

// ----- Renderers -----
function renderProductCard(p){
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
async function loadDashboard(){
    try{
        const [onSale, orders, me, reviewable] = await Promise.all([
            fetch(ENDPOINTS.myProducts('ON_SALE')).then(noAuthGuard).then(r=>r.json()),
            fetch(ENDPOINTS.orders).then(noAuthGuard).then(r=>r.json()),
            fetchMe(),
            ENDPOINTS.reviewableOrders ? fetch(ENDPOINTS.reviewableOrders).then(noAuthGuard).then(r=>r.json()) : Promise.resolve([])
        ]);

        // 판매/구매 분리 시도(필드가 없으면 전체 카운트 표시)
        let soldByMe = [], boughtByMe = [];
        if(Array.isArray(orders)){
            for(const o of orders){
                const role = inferRole(o, me);
                if(role==='SELLER') soldByMe.push(o);
                else if(role==='BUYER') boughtByMe.push(o);
            }
        }

        $('#statOnSale').textContent  = onSale?.length ?? 0;
        $('#statSold').textContent    = soldByMe.length || (orders?.length ?? 0); // 최소한 전체표시
        $('#statReviews').textContent = reviewable?.length ?? 0; // "작성가능 리뷰" 개수로 임시 표시
        $('#statRating').textContent  = '-'; // 받은후기 평균 API없음

        if (me?.nickname){
            $('#shopName').textContent = `상점명: ${me.nickname}`;
            $('#shopMeta').textContent = `팔로워 0 · 찜 0 · 가입일 ${me.createdAt}`;
        }
    }catch(err){ console.warn(err); }
}

async function loadProducts(status){
    const grid = $('#productsGrid');
    const empty = $('#productsEmpty');
    grid.innerHTML = '';
    empty.hidden = true;
    for(let i=0;i<6;i++){
        const sk = document.createElement('div'); sk.className='item skeleton'; sk.style.height='236px'; grid.appendChild(sk);
    }
    try{
        const res = await fetch(ENDPOINTS.myProducts(status));
        noAuthGuard(res); const list = await res.json();
        grid.innerHTML = '';
        if (!list.length){ empty.hidden=false; return; }
        list.forEach(p=> grid.appendChild(renderProductCard(p)));
    }catch(err){ grid.innerHTML = `<div class="empty">불러오기 실패: ${err.message}</div>`; }
}

async function loadFavorites(){
    const grid = $('#favGrid'); const empty = $('#favEmpty');
    grid.innerHTML=''; empty.hidden=true;
    if(!ENDPOINTS.favorites){ grid.innerHTML = `<div class='empty'>찜 기능이 아직 준비되지 않았습니다.</div>`; return; }
    try{
        const res = await fetch(ENDPOINTS.favorites); noAuthGuard(res);
        const list = await res.json();
        if(!list.length){ empty.hidden=false; return; }
        list.forEach(p=> grid.appendChild(renderProductCard(p)));
    }catch(err){ grid.innerHTML = `<div class='empty'>불러오기 실패: ${err.message}</div>`; }
}

async function loadSales(){
    const body = $('#salesBody'); body.innerHTML='';
    try{
        const [orders, me] = await Promise.all([
            fetch(ENDPOINTS.orders).then(noAuthGuard).then(r=>r.json()),
            fetchMe()
        ]);
        const rows = [];
        (orders||[]).forEach(o=>{
            const role = inferRole(o, me);
            if(role==='SELLER' || role==='UNKNOWN'){
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
    }catch(err){ body.innerHTML = `<tr><td colspan="6" class="empty">불러오기 실패: ${err.message}</td></tr>`; }
}

async function loadPurchases(){
    const body = $('#purchasesBody'); body.innerHTML='';
    try{
        const [orders, me] = await Promise.all([
            fetch(ENDPOINTS.orders).then(noAuthGuard).then(r=>r.json()),
            fetchMe()
        ]);
        const rows = [];
        (orders||[]).forEach(o=>{
            const role = inferRole(o, me);
            if(role==='BUYER' || role==='UNKNOWN'){
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
    }catch(err){ body.innerHTML = `<tr><td colspan="6" class="empty">불러오기 실패: ${err.message}</td></tr>`; }
}

async function loadReviews(){
    const body = $('#reviewsBody'); body.innerHTML='';
    if(!ENDPOINTS.reviewableOrders){ body.innerHTML = `<tr><td colspan="4" class="empty">리뷰 API가 아직 없습니다.</td></tr>`; return; }
    try{
        const list = await fetch(ENDPOINTS.reviewableOrders).then(noAuthGuard).then(r=>r.json());
        if(!list.length){ body.innerHTML = `<tr><td colspan="4" class="empty">작성 가능한 리뷰가 없습니다.</td></tr>`; return; }
        body.innerHTML = list.map(r=>`<tr>
      <td>-</td>
      <td>주문 #${r.orderId ?? '-'} 리뷰 작성 가능</td>
      <td>${r.counterpartyName ?? '-'}</td>
      <td>${r.createdAt ?? '-'}</td>
    </tr>`).join('');
    }catch(err){ body.innerHTML = `<tr><td colspan="4" class="empty">불러오기 실패: ${err.message}</td></tr>`; }
}

async function loadAlerts(){
    const listEl = $('#alertsList'); listEl.innerHTML='';
    if(!ENDPOINTS.notifications){ listEl.innerHTML = `<div class='empty'>알림 기능이 아직 준비되지 않았습니다.</div>`; return; }
    try{
        const res = await fetch(ENDPOINTS.notifications); noAuthGuard(res);
        const list = await res.json();
        if(!list.length){ listEl.innerHTML = `<div class='empty'>새 알림이 없습니다.</div>`; return; }
        listEl.innerHTML = list.map(n=>`<div class=\"mp-card\"><strong>${n.title ?? '알림'}</strong><div style=\"color:var(--muted);\">${n.body ?? ''}</div><div style=\"font-size:12px; color:var(--muted); margin-top:6px;\">${n.createdAt ?? '-'}</div></div>`).join('');
    }catch(err){ listEl.innerHTML = `<div class='empty'>불러오기 실패: ${err.message}</div>`; }
}

async function loadShopSettings(){
    // 현재 백엔드에 저장/조회 API 없음 → 폼 비활성화/안내
    const form = $('#shopForm');
    if(form){
        form.querySelectorAll('input, textarea, button[type="submit"]').forEach(el=> el.disabled = true);
        const note = document.createElement('div');
        note.className = 'empty';
        note.style.marginTop = '10px';
        note.textContent = '상점 관리 API가 아직 준비되지 않았습니다.';
        form.appendChild(note);
    }
}

async function loadAccount(){
    const res = await fetch(ENDPOINTS.userProfile);
    const { ok, data, message } = await res.json();
    if(!ok) return alert(message || '프로필 조회 실패');
}

async function verifyPassword(){
    const pw = document.getElementById('verifyPw').value;
    const res = await fetch(ENDPOINTS.verifyPassword, {
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body: JSON.stringify({ password: pw })
    });
    const j = await res.json();
    if(j.ok){
        // 읽기 전용 -> 수정 폼 전환
        document.getElementById('profileRead').style.display='none';
        document.getElementById('profileEdit').style.display='block';
    }else{
        alert(j.message || '비밀번호가 일치하지 않습니다.');
    }
}

async function saveProfile(){
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
        method:'PUT',
        headers:{'Content-Type':'application/json'},
        body: JSON.stringify(payload)
    });
    const j = await res.json();
    if(j.ok){
        alert('정보가 성공적으로 변경되었습니다.');
        loadAccount();
        document.getElementById('profileEdit').style.display='none';
        document.getElementById('profileRead').style.display='block';
    }else{
        alert(j.message || '수정 실패');
    }
}

// ----- Forms -----
$('#shopForm')?.addEventListener('submit', async (e)=>{
    e.preventDefault();
    if(!ENDPOINTS.meShop){ alert('상점 관리 API가 아직 준비되지 않았습니다.'); return; }
    const f = e.currentTarget;
    const payload = { shopName: f.shopName.value.trim(), intro: f.intro.value.trim() };
    try{
        const res = await fetch(ENDPOINTS.meShop, { method:'PUT', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) });
        noAuthGuard(res); alert('상점 정보가 저장되었습니다.');
        loadDashboard();
    }catch(err){ alert('저장 실패: '+err.message); }
});

$('#btnPreviewShop')?.addEventListener('click', ()=>{ window.open('/shop/me', '_blank'); });

$('#accountForm')?.addEventListener('submit', async (e)=>{
    e.preventDefault();
    alert('계정 관리 API가 아직 준비되지 않았습니다. (email/phone/비밀번호 변경)');
});

$('#btnDelete')?.addEventListener('click', async ()=>{
    alert('회원 탈퇴 API가 아직 준비되지 않았습니다.');
});

// ----- Boot -----
document.addEventListener('DOMContentLoaded', ()=>{
    // 알림/상점관리 탭 숨김(엔드포인트 없을 때)
    if(!ENDPOINTS.notifications){ $('[data-tab="alerts"]')?.remove(); $('#tab-alerts')?.remove(); }
    if(!ENDPOINTS.meShop){ $('[data-tab="shop"]')?.remove(); $('#tab-shop')?.remove(); }

    switchTab('dashboard');
});
