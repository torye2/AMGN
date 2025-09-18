// productDetail.js
document.addEventListener('DOMContentLoaded', async () => {
  const params = new URLSearchParams(window.location.search);
  const listingId = params.get('id');

  if (!listingId) {
    alert('잘못된 접근: listingId 없음');
    return;
  }

  // 1) 현재 로그인 유저 정보
  let me = null;
  try {
    const meRes = await fetch('/api/user/me', { credentials: 'include' });
    if (meRes.ok) me = await meRes.json(); // { loggedIn, userId, nickname }
  } catch (e) {
    console.error('현재 사용자 조회 실패:', e);
  }

  // 2) 상품 정보
  let product = null;
  try {
    const res = await fetch(`/product/${encodeURIComponent(listingId)}`);
    if (!res.ok) throw new Error('상품 정보를 불러오는데 실패했습니다.');
    product = await res.json();
  } catch (e) {
    console.error(e);
    alert(e.message);
    return;
  }

  // ---- 화면 렌더 ----
  const titleEl   = document.getElementById('productTitle');
  const sellerEl  = document.getElementById('productSeller');
  const priceEl   = document.getElementById('productPrice');
  const descEl    = document.getElementById('productDesc');

  if (titleEl)  titleEl.textContent  = product.title ?? '-';
  if (sellerEl) sellerEl.textContent = `판매자: ${product.sellerNickname ?? '-'}`;
  if (priceEl)  priceEl.textContent  = product.price != null ? `${Number(product.price).toLocaleString()} 원` : '-';
  if (descEl)   descEl.textContent   = product.description ?? '-';

  // 상점 정보 카드 채우기 (닉네임)
  const storeNicknameEl = document.getElementById('storeSellerNickname');
  if (storeNicknameEl) storeNicknameEl.textContent = product.sellerNickname ?? '-';

  // ---- 판매자/구매자 분기용 아이디 정리 ----
  const rawSellerId =
    product?.sellerId ??
    product?.seller?.userId ??
    product?.seller?.id ??
    null;

  const sellerId = rawSellerId != null ? String(rawSellerId) : null;
  const viewerId = me?.loggedIn && me?.userId != null ? String(me.userId) : null;
  const isSellerViewing = !!(viewerId && sellerId && viewerId === sellerId);

  // 판매자 상품 그리드 로딩
  if (sellerId) {
    loadSellerProducts(sellerId);
    loadSellerReviews(sellerId);
  }

  // ---- 더치트 버튼 로직 ----
  const theCheatBtn = document.getElementById('thecheat-button');
  if (theCheatBtn) {
    theCheatBtn.addEventListener('click', (e) => {
      e.preventDefault();
      const url = 'https://thecheat.co.kr/rb/?mod=_search';
      window.open(url, '_blank', 'noopener');
    });
  }

  // ---- 채팅 버튼 로직 ----
  const chatBtn = document.getElementById('chat-button');
  if (chatBtn) {
    if (isSellerViewing) {
      // 판매자: 해당 상품의 채팅 목록으로
      chatBtn.textContent = '대화중인 채팅';
      chatBtn.addEventListener('click', () => {
        window.location.href = `/productChatList.html?listingId=${listingId}`;
      });
    } else {
      // 구매자/타 사용자: 채팅방 생성/조회 후 이동
      chatBtn.textContent = '채팅하기';
      chatBtn.addEventListener('click', async () => {
        try {
          if (!me?.loggedIn) {
            alert('로그인이 필요합니다.');
            location.href = '/login.html';
            return;
          }
          if (!sellerId) {
            alert('판매자 정보가 없어 채팅을 시작할 수 없습니다.');
            return;
          }

          const url = `/api/chat/room/open?listingId=${encodeURIComponent(listingId)}&sellerId=${encodeURIComponent(sellerId)}`;
          const res = await fetch(url, { method: 'POST', credentials: 'include' });
          if (!res.ok) throw new Error(await res.text() || '채팅방 생성/조회 실패');

          const room = await res.json();
          if (!room?.roomId) throw new Error('roomId를 받지 못했습니다.');
          window.location.href = `/chatPage.html?roomId=${room.roomId}`;
        } catch (e) {
          console.error(e);
          alert('채팅방 이동 중 오류가 발생했습니다.');
        }
      });
    }
  }

  // ---- 버튼 영역(구매 vs 수정/삭제/찜) ----
  const buttonGroup = document.querySelector('.button-group');
  const orderButton = document.getElementById('order-button');

  if (buttonGroup) {
    if (isSellerViewing) {
      // 판매자가 보면 구매 버튼 제거
      if (orderButton) orderButton.remove();

      // 수정 버튼
      const editBtn = document.createElement('button');
      editBtn.id = 'edit-button';
      editBtn.className = 'edit-button';
      editBtn.textContent = '수정하기';
      editBtn.addEventListener('click', () => {
        window.location.href = `/edit.html?id=${encodeURIComponent(listingId)}`;
      });

      // 삭제 버튼
      const deleteBtn = document.createElement('button');
      deleteBtn.id = 'delete-button';
      deleteBtn.className = 'delete-button';
      deleteBtn.textContent = '삭제하기';
      deleteBtn.addEventListener('click', async () => {
        if (!confirm('정말 삭제하시겠습니까?')) return;
        try {
          const res = await fetch(`/product/${encodeURIComponent(listingId)}`, {
            method: 'DELETE',
            credentials: 'include',
          });
          if (!res.ok) throw new Error(await res.text() || '삭제 실패');
          alert('삭제되었습니다.');
          window.location.href = '/main.html';
        } catch (e) {
          console.error(e);
          alert('삭제 중 오류가 발생했습니다.');
        }
      });

      buttonGroup.append(editBtn, deleteBtn);
    } else {
      // ★ 찜 버튼
      const wishBtn = document.createElement('button');
      wishBtn.id = 'wish-button';
      wishBtn.className = 'wish-button';
      wishBtn.type = 'button';
      wishBtn.innerHTML = '🤍 찜 <span id="wish-count">0</span>';
      buttonGroup.prepend(wishBtn); // 맨 앞에 배치 (뒤에 두려면 append)

      // 초기 상태 불러오기
      await refreshWishUI(wishBtn, listingId, typeof product?.wishCount === 'number' ? product.wishCount : 0);

      // 클릭 토글
      wishBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        try {
          if (!me?.loggedIn) { alert('로그인이 필요합니다.'); location.href = '/login.html'; return; }
          const res = await fetch(`/product/${encodeURIComponent(listingId)}/wish`, {
            method: 'POST',
            credentials: 'include'
          });
          if (!res.ok) {
            const txt = await res.text();
            throw new Error(txt || '찜 처리 실패');
          }
          const data = await res.json();
          setWishButtonUI(wishBtn, data.wished, data.count);
        } catch (err) {
          console.error(err);
          alert('찜 처리 중 오류가 발생했습니다.');
        }
      });

      // 구매자/타 사용자일 때만 주문 버튼 이벤트 부여
      if (orderButton) {
        orderButton.addEventListener('click', () => {
          window.location.href = `/order/order.html?listingId=${encodeURIComponent(listingId)}`;
        });
      }
    }
  }

  // ---- 이미지 슬라이드 ----
  const slidesContainer = document.getElementById('productSlides');
  if (slidesContainer) {
    slidesContainer.innerHTML = '';
    const photos = Array.isArray(product.photoUrls) ? product.photoUrls : [];
    if (photos.length > 0) {
      photos.forEach((url) => {
        const div = document.createElement('div');
        div.className = 'swiper-slide';
        const img = document.createElement('img');
        img.src = url.startsWith('/uploads') ? url : `/uploads/${url}`;
        img.alt = product.title ?? '상품 이미지';
        img.className = 'product-img';
        div.appendChild(img);
        slidesContainer.appendChild(div);
      });
    } else {
      const div = document.createElement('div');
      div.className = 'swiper-slide';
      div.textContent = '사진이 없습니다.';
      slidesContainer.appendChild(div);
    }

    if (typeof Swiper !== 'undefined') {
      new Swiper('.swiper', {
        loop: true,
        navigation: { nextEl: '.swiper-button-next', prevEl: '.swiper-button-prev' },
        pagination: { el: '.swiper-pagination', clickable: true },
      });
    }
  }

  // ---- 관련 상품 ----
  loadRelatedProducts(listingId);
});

// 관련 상품 로더
function loadRelatedProducts(productId) {
  fetch(`/product/${encodeURIComponent(productId)}/related`)
    .then(res => res.json())
    .then(relatedProducts => {
      const container = document.querySelector('.related-products');
      if (!container) return;
      container.innerHTML = '';
      if (!Array.isArray(relatedProducts) || relatedProducts.length === 0) {
        container.innerHTML = '<p>관련 상품이 없습니다.</p>';
        return;
      }
      relatedProducts.forEach(p => {
        const item = document.createElement('div');
        item.className = 'product-item';
        const imgUrl = (p.photoUrls && p.photoUrls.length > 0)
          ? p.photoUrls[0]
          : 'https://placehold.co/300x200?text=No+Image';
        item.innerHTML = `
          <a href="/productDetail.html?id=${p.listingId}">
            <img src="${imgUrl}" alt="${p.title ?? ''}" />
            <p>${p.title ?? ''}</p>
            <p>${p.price != null ? Number(p.price).toLocaleString() : ''} 원</p>
          </a>`;
        container.appendChild(item);
      });
    })
    .catch(err => console.error('관련 상품 불러오기 실패:', err));
}

/* ================================
   찜(위시) 보조 함수
================================ */
async function refreshWishUI(wishBtn, listingId, initialCount = 0) {
  try {
    const res = await fetch(`/product/${encodeURIComponent(listingId)}/wish`, { credentials: 'include' });
    if (!res.ok) throw new Error('찜 상태 조회 실패');
    const data = await res.json();
    setWishButtonUI(wishBtn, data.wished, data.count);
  } catch (e) {
    console.error('찜 상태 조회 실패:', e);
    // 조회 실패 시 초기값으로 세팅
    setWishButtonUI(wishBtn, false, initialCount);
  }
}

function setWishButtonUI(wishBtn, wished, count) {
  wishBtn.dataset.wished = wished ? '1' : '0';
  wishBtn.innerHTML = `${wished ? '💖' : '🤍'} 찜 <span id="wish-count">${count ?? 0}</span>`;
}

/* ================================
   판매자 상품 그리드 로더
================================ */
function loadSellerProducts(sellerId) {
  const grid = document.getElementById('storeProductsGrid');
  const storeCard = document.getElementById('storeInfoCard');
  if (!grid || !sellerId) return;

  fetch(`/product/seller/${encodeURIComponent(sellerId)}/products`)
    .then(res => {
      if (!res.ok) throw new Error('판매자 상품 조회 실패');
      return res.json();
    })
    .then(list => {
      grid.innerHTML = '';
      // 기존 더보기 링크 제거
      if (storeCard) {
        const oldMore = storeCard.querySelector('.store-more-link');
        if (oldMore) oldMore.remove();
      }

      if (!Array.isArray(list) || list.length === 0) {
        grid.innerHTML = '<p class="store-empty">판매자의 다른 상품이 없습니다.</p>';
        grid.classList.remove('two-cols');
        return;
      }

      const total = list.length;
      const toRender = list.slice(0, 6); // 최대 6개만 표시

      // 4개 이상이면 2열 바둑판
      if (total >= 4) grid.classList.add('two-cols');
      else grid.classList.remove('two-cols');

      toRender.forEach(p => {
        const raw = Array.isArray(p.photoUrls) && p.photoUrls.length ? p.photoUrls[0] : null;
        const imgUrl = raw
          ? (String(raw).startsWith('/uploads') ? String(raw) : `/uploads/${raw}`)
          : 'https://placehold.co/300x200?text=No+Image';

        const priceText = p.price != null ? `${Number(p.price).toLocaleString()} 원` : '';

        const a = document.createElement('a');
        a.className = 'store-product-tile';
        a.href = `/productDetail.html?id=${p.listingId}`;
        a.innerHTML = `
          <img src="${imgUrl}" alt="${p.title ?? ''}">
          <div class="store-price-badge">${priceText}</div>
        `;
        grid.appendChild(a);
      });

      // 판매자 상점으로 이동하는 "상품 더보기" 링크 추가 → 항상 노출
      if (storeCard) {
        const more = document.createElement('a');
        more.className = 'store-more-link';
        more.href = `/shop.html?sellerId=${encodeURIComponent(sellerId)}`;
        more.textContent = `${total}개 상품 더보기 >`;
        storeCard.appendChild(more);
      }
    })
    .catch(err => {
      console.error('판매자 상품 불러오기 실패:', err);
    });
}

/* ================================
   판매자 후기 로더 (요약 API → 폴백 지원)
================================ */
async function loadSellerReviews(sellerId) {
  const listEl   = document.getElementById('storeReviewList');
  const starFront= document.getElementById('storeStarFront');
  const scoreEl  = document.getElementById('storeRatingScore');
  const countEl  = document.getElementById('storeRatingCount');
  const moreEl   = document.getElementById('storeReviewMore');

  // "후기 더보기" 링크 (원하는 경로로 바꿔도 됨)
  if (moreEl) moreEl.href = `/shop.html?sellerId=${encodeURIComponent(sellerId)}#reviews`;

  if (!listEl || !starFront || !scoreEl || !countEl) return;

  // 1) 요약 엔드포인트 시도
  let avg = 0, total = 0, items = [];
  try {
    const res = await fetch(`/api/reviews/seller/${encodeURIComponent(sellerId)}/summary?limit=3`, { credentials: 'include' });
    if (res.ok) {
      const data = await res.json();
      avg   = typeof data?.avgRating === 'number' ? data.avgRating : 0;
      total = typeof data?.reviewCount === 'number' ? data.reviewCount : 0;
      items = Array.isArray(data?.items) ? data.items : [];
    } else {
      throw new Error('summary not ok');
    }
  } catch {
    // 2) 폴백: 전체 리스트에서 상위 3개만
    try {
      const res2 = await fetch(`/api/reviews/seller/${encodeURIComponent(sellerId)}`, { credentials: 'include' });
      if (res2.ok) {
        const arr = await res2.json(); // [{ id, score, rvComment, createdAt, ... }]
        items = (Array.isArray(arr) ? arr : []).slice(0, 3).map(r => ({
          reviewId: r.id,
          rating: r.score,
          comment: r.rvComment,
          reviewerNickname: '익명', // 백엔드 DTO에 닉네임 없으면 기본값
          createdAt: r.createdAt
        }));
        // 평균/개수 계산
        total = Array.isArray(arr) ? arr.length : 0;
        avg = total ? (arr.reduce((s, v) => s + (Number(v.score)||0), 0) / total) : 0;
      }
    } catch (e) {
      console.error('리뷰 폴백 실패:', e);
    }
  }

  // 평점 렌더
  scoreEl.textContent = avg ? Number(avg).toFixed(1) : '-';
  countEl.textContent = total ?? 0;
  starFront.style.width = Math.max(0, Math.min(100, (Number(avg)/5)*100)) + '%';

  // 리스트 렌더
  listEl.innerHTML = '';
  if (!items.length) {
    const li = document.createElement('li');
    li.className = 'store-empty';
    li.textContent = '후기가 없습니다.';
    listEl.appendChild(li);
    return;
  }

  items.forEach(r => {
    const li = document.createElement('li');
    li.className = 'store-review-item';

    const head = document.createElement('div');
    head.className = 'store-review-head';

    const left = document.createElement('div');
    left.className = 'store-review-author';
    left.textContent = r.reviewerNickname ?? '익명';

    const right = document.createElement('div');
    right.className = 'store-review-date';
    right.textContent = formatDateKST(r.createdAt);

    head.append(left, right);

    // 개별 별점
    const stars = document.createElement('div');
    stars.className = 'star-wrap';
    const back = document.createElement('div');
    back.className = 'star-back';
    back.textContent = '★★★★★';
    const front = document.createElement('div');
    front.className = 'star-front';
    front.textContent = '★★★★★';
    front.style.width = Math.max(0, Math.min(100, (Number(r.rating)/5)*100)) + '%';
    stars.append(back, front);

    const text = document.createElement('div');
    text.className = 'store-review-text';
    text.textContent = r.comment ?? '';

    li.append(head, stars, text);
    listEl.appendChild(li);
  });
}

// KST 기준 날짜 포맷 (YYYY.MM.DD)
function formatDateKST(isoOrLocal) {
  try {
    const d = new Date(isoOrLocal);
    // 단순 표기(타임존 보정이 필요하면 서버에서 ISO+Z로 내려주기 권장)
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}.${m}.${day}`;
  } catch {
    return '';
  }
}

