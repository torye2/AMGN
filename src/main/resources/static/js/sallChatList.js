document.addEventListener('DOMContentLoaded', async () => {
  const statusEl = document.getElementById('status');
  const listEl   = document.getElementById('chatRooms');
  const errorEl  = document.getElementById('error');
  const setBusy = (b)=>listEl.setAttribute('aria-busy', String(!!b));

  const buyerCache   = new Map(); // userId -> nickname
  const listingCache = new Map(); // listingId -> title

  const pickBuyerNickname = (room) =>
    room.buyerNickname ?? room.buyer_nickname ?? room.buyer?.nickname ?? null;
  const pickListingTitle = (room) =>
    room.listingTitle ?? room.listing_title ?? room.listing_name ?? room.productName ?? room.listing?.title ?? null;

  async function fetchListingTitle(listingId){
    if (!listingId) return null;
    const key = String(listingId);
    if (listingCache.has(key)) return listingCache.get(key);

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

  async function fetchBuyerNickname(userId){
    if (!userId) return null;
    const key = String(userId);
    if (buyerCache.has(key)) return buyerCache.get(key);
    const candidates = [
      `/api/shop/${encodeURIComponent(key)}`,
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
        if (nick){ buyerCache.set(key, nick); return nick; }
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
    statusEl.textContent = `판매자: ${me.nickname ?? myUserId}`;

    const roomsRes = await fetch(`/api/chat/rooms?userId=${encodeURIComponent(myUserId)}`, { credentials:'include' });
    if (!roomsRes.ok) throw new Error('채팅방 목록 조회 실패');
    const allRooms = await roomsRes.json();

    const sellerRooms = (allRooms || []).filter(r => String(r.sellerId ?? r.seller_id) === String(myUserId));

    listEl.innerHTML = '';
    if (sellerRooms.length === 0){
      listEl.innerHTML = `<li class="empty">판매 채팅방이 없습니다.</li>`;
      return;
    }

    sellerRooms.sort((a,b)=> new Date(b.createdAt ?? b.created_at ?? 0) - new Date(a.createdAt ?? a.created_at ?? 0));

    const enriched = await Promise.all(sellerRooms.map(async (room) => {
      const buyerNickname = pickBuyerNickname(room) ?? await fetchBuyerNickname(room.buyerId ?? room.buyer_id) ?? `구매자#${room.buyerId ?? room.buyer_id}`;

      const listingId = room.listingId ?? room.listing_id ?? room.productId ?? room.product_id;
      const listingTitle = pickListingTitle(room) ?? await fetchListingTitle(listingId);

      return {
        ...room,
        _buyerNickname: buyerNickname,
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
            <span class="chat-chip role-buyer">구매자</span>
            <span class="chat-name">${escapeHtml(room._buyerNickname)}</span>
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
