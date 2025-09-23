function editNotice(index) {
    window.location.href = `notice-w.html?index=${index}`;
}

function loadNotices() {
    const list = document.getElementById('notice-list');
    const notices = JSON.parse(localStorage.getItem('notices')) || [];

    if(notices.length === 0) {
        list.innerHTML = "<li>등록된 공지사항이 없습니다.</li>";
        return;
    }

    list.innerHTML = '';
    notices.forEach((notice, index) => {
        const li = document.createElement('li');
        li.innerHTML = `
                <a href="notice-v.html?index=${index}">${notice.title}
                <span class="date">${notice.date}</span>
                </a>
                    
            `;
        list.appendChild(li);
    });
}

window.onload = loadNotices;


/*찜 js*/
document.addEventListener('DOMContentLoaded', () => {
  const productsAllDiv = document.getElementById('productsAll');

  // ... 기존 /product/all 로딩 코드 그대로 ...

  // 최근 찜 사이드바 로드
  loadRecentWishes(6); // 6개 정도만 표시
});

    async function loadRecentWishes(limit = 6) {
  const box   = document.getElementById('wishSidebar');
  const empty = document.getElementById('wishSidebarEmpty');
  const guest = document.getElementById('wishSidebarGuest');
  if (!box) return;

  box.innerHTML = '<div class="text-muted small">불러오는 중...</div>';
  if (empty) empty.style.display = 'none';
  if (guest) guest.style.display = 'none';

  try {
    // 내 찜 목록 ID들 가져오기 (앞서 만든 ListingWishController의 /product/wish/my)
    const res = await fetch('/product/wish/my', { credentials: 'include' });
    if (res.status === 401) {
      // 비로그인: 안내 문구
      box.innerHTML = '';
      if (guest) guest.style.display = 'block';
      return;
    }
    if (!res.ok) throw new Error('찜 목록을 불러오지 못했습니다.');
    const data = await res.json();
    const ids = Array.isArray(data.listingIds) ? data.listingIds.slice(0, limit) : [];

    if (ids.length === 0) {
      box.innerHTML = '';
      if (empty) empty.style.display = 'block';
      return;
    }

    // 각 상품 상세 병렬 로드
    const itemPromises = ids.map(id =>
      fetch(`/product/${encodeURIComponent(id)}`)
        .then(r => r.ok ? r.json() : null)
        .catch(() => null)
    );
    const items = (await Promise.all(itemPromises)).filter(Boolean);

    // 렌더
    box.innerHTML = '';
    items.forEach(p => {
      const imgUrl =
        (p.photoUrls && p.photoUrls.length > 0)
          ? (p.photoUrls[0].startsWith('/uploads') ? p.photoUrls[0] : `/uploads/${p.photoUrls[0]}`)
          : 'https://placehold.co/64x64?text=No+Image';

      const a = document.createElement('a');
      a.href = `/productDetail.html?id=${p.listingId}`;
      a.className = 'wish-item';
      a.innerHTML = `
        <img src="${imgUrl}" alt="${(p.title ?? '').replace(/"/g, '&quot;')}" />
        <div class="title">${escapeHtml(p.title ?? '')}</div>
      `;
      box.appendChild(a);
    });
  } catch (e) {
    console.error('최근 찜 불러오기 실패:', e);
    box.innerHTML = '<div class="text-muted small">최근 찜을 불러오지 못했습니다.</div>';
  }
}

// 간단한 XSS 방지용
function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
}

document.addEventListener('DOMContentLoaded', () => {
  const card   = document.getElementById('topSellersCard');
  const listEl = document.getElementById('topSellers');
  const loading = document.getElementById('topSellersLoading');
  const empty   = document.getElementById('topSellersEmpty');

  fetch('/awards/top-sellers?metric=units&limit=3', {
    headers: { 'Accept': 'application/json' },
    credentials: 'same-origin',
    cache: 'no-store'
  })
  .then(async (res) => {
    if (res.status === 204) return [];
    const ct = res.headers.get('content-type') || '';
    const text = await res.text();
    if (!res.ok) throw new Error(text || '요청 실패');
    if (!text) return [];
    if (!ct.includes('application/json')) throw new Error('JSON이 아닌 응답입니다.');
    return JSON.parse(text);
  })
  .then(rows => {
    loading.style.display = 'none';
    if (!rows || rows.length === 0) {
      empty.classList.remove('d-none');
      return;
    }

    const medalClass = ['gold','silver','bronze'];
    const medalText  = ['1','2','3'];

    listEl.innerHTML = '';
    rows.slice(0,3).forEach((r, idx) => {
      const col = document.createElement('div');
      col.className = 'col-12 col-md-4';

      const name = r.sellerName ?? `판매자 #${r.sellerId}`;
      const units = Number(r.value ?? 0);
      const avatarUrl = r.avatarUrl || 'https://placehold.co/88x88?text=%F0%9F%8C%9F';

      // 링크: /shop.html?sellerId={id}
      const url = new URL('/shop.html', window.location.origin);
      url.searchParams.set('sellerId', String(r.sellerId));

      col.innerHTML = `
        <a href="${url.toString()}" class="text-decoration-none text-reset">
          <div class="leader-item">
            <div class="leader-medal ${medalClass[idx] || 'gold'}">${medalText[idx] || (idx+1)}</div>
            <img class="leader-avatar" src="${avatarUrl}" alt="${name}">
            <div class="flex-grow-1">
              <div class="leader-name">${name}</div>
              <div class="leader-stats">이번 달 판매수량 ${units.toLocaleString()}개</div>
            </div>
          </div>
        </a>
      `;
      listEl.appendChild(col);
    });

    card.style.display = 'block';
  })
  .catch(err => {
    console.error(err);
    loading.style.display = 'none';
    empty.classList.remove('d-none');
    empty.classList.replace('alert-light', 'alert-warning');
    empty.textContent = err.message || '리더보드를 불러오지 못했습니다.';
  });
});