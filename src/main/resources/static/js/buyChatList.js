document.addEventListener('DOMContentLoaded', async () => {
  const statusEl = document.getElementById('status');
  const listEl   = document.getElementById('chatRooms');
  const errorEl  = document.getElementById('error');
  const setBusy = (b)=>listEl.setAttribute('aria-busy', String(!!b));

  const sellerCache  = new Map(); // userId -> nickname
  const listingCache = new Map(); // listingId -> title

  const pickSellerNickname = (room) =>
    room.sellerNickname ?? room.seller_nickname ?? room.seller?.nickname ?? null;

  const pickListingTitle = (room) =>
    room.listingTitle ?? room.listing_title ?? room.listing_name ?? room.productName ?? room.listing?.title ?? null;

  // ✅ 실제 백엔드 경로 포함해서 타이틀 조회
  async function fetchListingTitle(listingId){
    if (!listingId) return null;
    const key = String(listingId);
    if (listingCache.has(key)) return listingCache.get(key);

    // 너의 백엔드에 맞춘 우선순위: /product/{id} → 그 외 후보
    const candidates = [
      `/product/${encodeURIComponent(key)}`,                 // ✅ 실제 사용
      `/api/listings/${encodeURIComponent(key)}`,
      `/api/listing/${encodeURIComponent(key)}`,
      `/api/products/${encodeURIComponent(key)}`,
      `/api/product/${encodeURIComponent(key)}`
    ];

    for (const url of candidates){
      try{
        const r = await fetch(url, {credentials:'include', cache:'no-store'});
        if (!r.ok) continue;
        const d = await r.json();
        // d가 DTO 전체일 수도, {content:...}일 수도 있으니 방어적으로…
        const src = d?.listing ?? d?.product ?? d;
        const title = src?.title ?? src?.name ?? src?.productName;
        if (title){
          listingCache.set(key, title);
          return title;
        }
      }catch(e){}
    }
    return null;
  }

  async function fetchSellerNickname(userId){
    if (!userId) return null;
    const key = String(userId);
    if (sellerCache.has(key)) return sellerCache.get(key);
    const candidates = [
      `/api/shop/${encodeURIComponent(key)}`,               // 상점 API에 userName/nickname 있을 확률 높음
      `/api/users/${encodeURIComponent(key)}`,
      `/api/user/${encodeURIComponent(key)}`,
      `/api/profile/${encodeURIComponent(key)}`
    ];
    for (const url of candidates){
      try{
        const r = await fetch(url, {credentials:'include'});
        if (!r.ok) continue;
        const d = await r.json();
        const nick = d.nickname ?? d.nickName ?? d.userName ?? d.username ?? d.name ?? d.user?.nickname ?? d.profile?.nickname;
        if (nick){ sellerCache.set(key, nick); return nick; }
      }catch(e){}
    }
    return null;
  }

  try{
    setBusy(true);

    const meRes = await fetch('/api/user/me', {cache:'no-store', credentials:'include'});
    if (!meRes.ok) throw new Error('로그인 사용자 조회 실패');
    const me = await meRes.json();

    if (!me.loggedIn){
      statusEl.textContent = '로그인이 필요합니다.';
      alert('로그인이 필요합니다.');
      location.href = '/login.html';
      return;
    }

    const myUserId = me.userId ?? me.id ?? me.user_id;
    statusEl.textContent = `구매자: ${me.nickname ?? myUserId}`;

    const roomsRes = await fetch(`/api/chat/rooms?userId=${encodeURIComponent(myUserId)}`, {credentials:'include'});
    if (!roomsRes.ok) throw new Error('채팅방 목록 조회 실패');
    const allRooms = await roomsRes.json();

    const buyerRooms = (allRooms || []).filter(r => String(r.buyerId ?? r.buyer_id) === String(myUserId));

    listEl.innerHTML = '';
    if (buyerRooms.length === 0){
      listEl.innerHTML = `<li class="empty">구매 채팅방이 없습니다.</li>`;
      return;
    }

    buyerRooms.sort((a,b)=> new Date(b.createdAt ?? b.created_at ?? 0) - new Date(a.createdAt ?? a.created_at ?? 0));

    const enriched = await Promise.all(buyerRooms.map(async (room) => {
      const sellerNickname = pickSellerNickname(room) ?? await fetchSellerNickname(room.sellerId ?? room.seller_id) ?? `판매자#${room.sellerId ?? room.seller_id}`;

      // ✅ listingId 해석을 더 넓게(스네이크/카멜 모두)
      const listingId = room.listingId ?? room.listing_id ?? room.productId ?? room.product_id;
      const listingTitle = pickListingTitle(room) ?? await fetchListingTitle(listingId);

      return {
        ...room,
        _sellerNickname: sellerNickname,
        _listingTitle: listingTitle || '(제목 없음)',
        _listingId: listingId
      };
    }));

    enriched.forEach(room => {
      const li = document.createElement('li');
      li.className = 'room';
      li.tabIndex = 0;
      li.innerHTML = `
        <div class="info">
          <div class="line1">
            <span class="chat-chip role-seller">판매자</span>
            <span class="chat-name">${escapeHtml(room._sellerNickname)}</span>
          </div>
          <div class="line2">
            <span class="chat-chip meta">상품</span>
            <span class="chat-title">${escapeHtml(room._listingTitle)}</span>
          </div>
        </div>
        <div class="go">›</div>
      `;
      const go = () => location.href = `/chatPage.html?roomId=${encodeURIComponent(room.roomId ?? room.room_id)}`;
      li.addEventListener('click', go);
      li.addEventListener('keydown', (e)=>{ if (e.key==='Enter' || e.key===' ') { e.preventDefault(); go(); } });
      listEl.appendChild(li);
    });

  }catch(err){
    console.error(err);
    errorEl.style.display = 'block';
    errorEl.textContent = `오류: ${err.message}`;
  }finally{
    setBusy(false);
  }

  function escapeHtml(s){
    return String(s ?? '')
      .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
      .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
  }
});
