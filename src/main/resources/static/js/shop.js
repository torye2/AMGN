(function () {
  // sellerId 파라미터 파싱: seller_Id, sellerId, seller_id 모두 허용
  const params = new URLSearchParams(window.location.search);
  const sellerId =
    params.get('seller_Id') ||
    params.get('sellerId') ||
    params.get('seller_id');

  if (!sellerId) {
    console.warn('sellerId가 URL에 없습니다. 예: /shop.html?seller_Id=123');
    return;
  }

  fetch(`/api/shop/${encodeURIComponent(sellerId)}`)
    .then((res) => {
      if (!res.ok) throw new Error('판매자 정보를 불러오지 못했습니다.');
      return res.json();
    })
    .then((data) => {
      // 닉네임/타이틀: userName 사용
      const nicknameEl = document.querySelector('.shop-nickname');
      const titleEl = document.querySelector('.shop-title');
      if (nicknameEl) nicknameEl.textContent = data.userName ?? '';
      if (titleEl) titleEl.textContent = data.userName ?? '';

      // 상품 개수
      const miniStatsEl = document.querySelector('.mini-stats');
      if (miniStatsEl) {
        const productCount = typeof data.productCount === 'number' ? data.productCount : 0;
        // 팔로워는 요구사항에 명시되지 않아 일단 표시만 유지(0으로 표기)
        miniStatsEl.textContent = `상품 ${productCount} | 팔로워 0`;
      }

      // 상점오픈 N일 전
      const openMeta = document.querySelector('.meta .meta-item:nth-child(1)');
      if (openMeta) {
        const days = typeof data.daysSinceOpen === 'number' ? data.daysSinceOpen : null;
        if (days !== null) {
          openMeta.innerHTML = `<span class="dot"></span> 상점오픈 ${days}일 전`;
        } else if (data.createdAt) {
          openMeta.innerHTML = `<span class="dot"></span> 상점오픈일 ${data.createdAt}`;
        }
      }

      // 소개
      const descEl = document.querySelector('.shop-desc');
      if (descEl) {
        const intro = (data.intro ?? '').trim();
        descEl.textContent = intro.length ? intro : '소개가 없습니다.';
      }
    })
    .catch((err) => {
      console.error(err);
    });

  // --- 상품 목록 렌더링 ---
  const productsAll = document.getElementById('productsAll');
  if (!productsAll) return;

  // 초기 마크업 구성
  productsAll.innerHTML = `
    <div class="products-header">
      <span class="products-title">상품</span>
      <div class="products-sorts">
        <button type="button" data-sort="latest" class="sort-btn active">최신순</button>
        <button type="button" data-sort="low">저가순</button>
        <button type="button" data-sort="high">고가순</button>
      </div>
    </div>
    <div id="sellerProductsGrid" class="products-grid"></div>
  `;

  const grid = document.getElementById('sellerProductsGrid');

  const toImgUrl = (raw) => {
    if (!raw) return 'https://placehold.co/300x200?text=No+Image';
    const s = String(raw);
    return s.startsWith('/uploads') ? s : `/uploads/${s}`;
  };

  const formatPrice = (v) =>
    typeof v === 'number' ? `${Number(v).toLocaleString()} 원` : '';

  let originalList = [];

  const renderList = (list) => {
    if (!Array.isArray(list) || list.length === 0) {
      grid.innerHTML = '<p class="store-empty">등록된 상품이 없습니다.</p>';
      return;
    }
    grid.innerHTML = list
      .map((p) => {
        const img = Array.isArray(p.photoUrls) && p.photoUrls.length ? toImgUrl(p.photoUrls[0]) : toImgUrl(null);
        const price = formatPrice(p.price);
        const title = (p.title ?? '').trim();
        return `
          <a class="product-card" href="/productDetail.html?id=${encodeURIComponent(p.listingId)}" title="${title}">
            <div class="thumb-wrap">
              <img src="${img}" alt="${title}">
            </div>
            <div class="product-info">
              <div class="product-title">${title}</div>
              <div class="product-price">${price}</div>
            </div>
          </a>
        `;
      })
      .join('');
  };

  const applySort = (type) => {
    const list = [...originalList];
    switch (type) {
      case 'low':
        list.sort((a, b) => (a.price ?? 0) - (b.price ?? 0));
        break;
      case 'high':
        list.sort((a, b) => (b.price ?? 0) - (a.price ?? 0));
        break;
      case 'latest':
      default:
        // 최신순: createdAt 또는 listingId 기준 내림차순
        list.sort((a, b) => {
          const aKey = a.createdAt ? new Date(a.createdAt).getTime() : (a.listingId ?? 0);
          const bKey = b.createdAt ? new Date(b.createdAt).getTime() : (b.listingId ?? 0);
          return bKey - aKey;
        });
        break;
    }
    renderList(list);
  };

  // 정렬 버튼 이벤트
  productsAll.addEventListener('click', (e) => {
    const btn = e.target.closest('button[data-sort]');
    if (!btn) return;
    productsAll.querySelectorAll('.products-sorts .sort-btn, .products-sorts button')
      .forEach((b) => b.classList.remove('active'));
    btn.classList.add('active');
    applySort(btn.dataset.sort);
  });

  // 판매자 상품 가져오기
  fetch(`/product/seller/${encodeURIComponent(sellerId)}/products`)
    .then((res) => {
      if (!res.ok) throw new Error('판매자 상품 조회 실패');
      return res.json();
    })
    .then((list) => {
      originalList = Array.isArray(list) ? list : [];
      applySort('latest');
    })
    .catch((err) => {
      console.error('판매자 상품 불러오기 실패:', err);
      grid.innerHTML = '<p class="store-empty">상품을 불러오는 중 오류가 발생했습니다.</p>';
    });
})();
