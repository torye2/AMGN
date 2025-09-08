(function () {
  // URLSearchParams로 categoryId 가져오기
  const params = new URLSearchParams(location.search);
  const categoryId = params.get('id');

  if (!categoryId) {
    console.error('categoryId가 없습니다.');
    document.getElementById('empty').style.display = 'block';
    document.getElementById('empty').textContent = '잘못된 접근입니다.';
    return;
  }

  const API_BASE = '/product'; // 컨트롤러 @RequestMapping("/product")
  const gridEl = document.getElementById('productGrid');
  const emptyEl = document.getElementById('empty');
  const countEl = document.getElementById('count');
  const breadcrumbEl = document.getElementById('breadcrumb');
  const sortSelect = document.getElementById('sortSelect');

  // 카테고리명 표시(선택) – 이미 /api/categories가 있다면 거기서 이름도 가져와 breadcrumb 갱신
  fetch('/api/categories')
    .then(r => r.ok ? r.json() : [])
    .then(cats => {
      const cat = Array.isArray(cats) ? cats.find(c => String(c.categoryId) === String(categoryId)) : null;
      if (cat) breadcrumbEl.textContent = `카테고리: ${cat.name}`;
    })
    .catch(() => {/* 무시: 이름 없으면 기본 텍스트 유지 */});

  // 정렬 함수
  function sortProducts(list, key) {
    const cloned = list.slice();
    switch (key) {
      case 'priceAsc':
        cloned.sort((a, b) => (a.price ?? 0) - (b.price ?? 0));
        break;
      case 'priceDesc':
        cloned.sort((a, b) => (b.price ?? 0) - (a.price ?? 0));
        break;
      case 'recent':
      default:
        // 최신순: listingId가 클수록 최근이라고 가정(생성일이 없다면)
        cloned.sort((a, b) => (b.listingId ?? 0) - (a.listingId ?? 0));
        break;
    }
    return cloned;
  }

  function formatPrice(won) {
    if (won == null) return '-';
    try {
      // price가 BigDecimal 매핑이면 숫자로 내려오거나 문자열일 수 있음
      const num = typeof won === 'string' ? Number(won) : won;
      return num.toLocaleString('ko-KR') + '원';
    } catch {
      return String(won) + '원';
    }
  }

  function productCard(item) {
    // DB 값
    const CONDITION_LABEL = {
      NEW: '새상품',
      LIKE_NEW: '사용감 거의 없음',
      GOOD: '상태 좋음',
      FAIR: '보통',
      POOR: '사용감 많음',
    };

    const TRADE_LABEL = {
      MEETUP: '직거래',
      DELIVERY: '택배',
      BOTH: '직거래/택배',
    };

    // 상태
    const STATUS_LABEL = {
      ACTIVE: '판매중',
      RESERVED: '예약중',
      SOLD: '판매완료',
      HIDDEN: '숨김',
      DELETED: '삭제됨',
    };
    const YESNO = { Y: '예', N: '아니오' };

    // 대소문자/공백/널 안전한 라벨러
    function label(v, map) {
      if (v == null) return '';
      const key = String(v).trim().toUpperCase();
      return map[key] ?? v;  // 매핑 없으면 원문 그대로
    }


    const photo = (item.photoUrls && item.photoUrls.length > 0) ? item.photoUrls[0] : null;
    const imgSrc = photo ? photo : '/images/placeholder.png';

    const a = document.createElement('a');
    a.href = `/product-detail.html?id=${encodeURIComponent(item.listingId)}`;
    a.className = 'card';
    a.innerHTML = `
      <img class="thumb" src="${imgSrc}" alt="${item.title ? item.title.replace(/"/g, '&quot;') : '상품 이미지'}" onerror="this.src='/images/placeholder.png'"/>
      <div class="body">
        <h3 class="title">${item.title ?? ''}</h3>
        <div class="price">${formatPrice(item.price)}</div>
        <div class="meta">
          ${label(item.itemCondition, CONDITION_LABEL)}
          ${item.itemCondition && item.tradeType ? ' · ' : ''}
          ${label(item.tradeType, TRADE_LABEL)}
        </div>
      </div>
    `;
    return a;
  }

  async function loadProducts() {
    try {
      const res = await fetch(`${API_BASE}/category/${encodeURIComponent(categoryId)}`);
      if (!res.ok) throw new Error('상품 요청 실패');
      const data = await res.json();

      const sorted = sortProducts(data, sortSelect.value);

      gridEl.innerHTML = '';
      if (!sorted || sorted.length === 0) {
        emptyEl.style.display = 'block';
        countEl.textContent = '0개 상품';
        return;
      }

      emptyEl.style.display = 'none';
      countEl.textContent = `${sorted.length}개 상품`;

      sorted.forEach(item => {
        gridEl.appendChild(productCard(item));
      });
    } catch (e) {
      console.error(e);
      emptyEl.style.display = 'block';
      emptyEl.textContent = '상품을 불러오지 못했습니다.';
    }
  }

  sortSelect.addEventListener('change', loadProducts);
  loadProducts();
})();
