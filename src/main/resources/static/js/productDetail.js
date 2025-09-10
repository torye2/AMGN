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

  // ---- 판매자/구매자 분기용 아이디 정리 ----
  const rawSellerId =
    product?.sellerId ??
    product?.seller?.userId ??
    product?.seller?.id ??
    null;

  const sellerId = rawSellerId != null ? String(rawSellerId) : null;
  const viewerId = me?.loggedIn && me?.userId != null ? String(me.userId) : null;
  const isSellerViewing = !!(viewerId && sellerId && viewerId === sellerId);

  // ---- 채팅 버튼 로직 ----
  const chatBtn = document.getElementById('chat-button');
  if (chatBtn) {
    if (isSellerViewing) {
      // 판매자: 해당 상품의 채팅 목록으로
      chatBtn.textContent = '대화중인 채팅';
      chatBtn.addEventListener('click', () => {
        // 목록 페이지 URL은 프로젝트 경로에 맞춰 사용
        window.location.href = `/productChatList.html?listingId=${listingId}`;
      });
    } else {
      // 구매자/타 사용자: 채팅방 생성/조회 후 해당 roomId로 이동
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
          if (!res.ok) {
            throw new Error(await res.text() || '채팅방 생성/조회 실패');
          }
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

    // Swiper 초기화 (CDN 포함되어 있어야 함)
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

  // ---- 주문 버튼 ----
  const orderButton = document.getElementById('order-button');
  if (orderButton) {
    orderButton.addEventListener('click', () => {
      window.location.href = `/order/order.html?listingId=${encodeURIComponent(listingId)}`;
    });
  }
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
