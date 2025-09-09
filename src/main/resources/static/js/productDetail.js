
  document.addEventListener('DOMContentLoaded', async () => {
    const params = new URLSearchParams(window.location.search);
    const listingId = params.get('id');

    if (!listingId) {
      alert('잘못된 접근: listingId 없음');
      return;
    }

    // 1) 현재 로그인 유저
    let currentUser = null;
    try {
      const meRes = await fetch('/api/currentUser', { credentials: 'include' });
      if (meRes.ok) currentUser = await meRes.json();
    } catch (e) {
      console.error('현재 사용자 조회 실패:', e);
    }

    // 2) 상품 정보
    let product = null;
    try {
      const res = await fetch(`/product/${listingId}`);
      if (!res.ok) throw new Error('상품 정보를 불러오는데 실패했습니다.');
      product = await res.json();
    } catch (e) {
      console.error(e);
      alert(e.message);
      return;
    }

    // ---- 판매자 ID 추출 (백엔드 응답 형태에 따라 대비) ----
    // 우선순위: product.sellerId -> product.seller?.userId -> product.seller?.id
    const rawSellerId =
      product?.sellerId ??
      product?.seller?.userId ??
      product?.seller?.id ??
      null;

    // 비교 안정화를 위해 문자열로 통일
    const sellerId = rawSellerId != null ? String(rawSellerId) : null;
    const viewerId = currentUser?.userId != null ? String(currentUser.userId) : null;

    console.log('[DEBUG] currentUser:', currentUser);
    console.log('[DEBUG] product.sellerId candidates:', {
      sellerId: product?.sellerId,
      sellerUserId: product?.seller?.userId,
      sellerObjId: product?.seller?.id
    });
    console.log('[DEBUG] resolved sellerId:', sellerId, 'viewerId:', viewerId);

    // ---- 화면 렌더 (제목/가격 등) ----
    document.getElementById('productTitle').textContent = product.title ?? '-';
    document.getElementById('productSeller').textContent = `판매자: ${product.sellerNickname ?? '-'}`;
    document.getElementById('productPrice').textContent = product.price != null ? `${Number(product.price).toLocaleString()} 원` : '-';
    document.getElementById('productDesc').textContent = product.description ?? '-';

    // ---- 채팅 버튼 로직 ----
    const chatBtn = document.getElementById('chat-button');

    // 내가 판매자인가?
    const isSellerViewing = !!(viewerId && sellerId && viewerId === sellerId);

    if (chatBtn) {
      if (isSellerViewing) {
        // 판매자 → “대화중인 채팅” 버튼으로 변경하여 해당 상품 채팅 목록으로
        chatBtn.textContent = '대화중인 채팅';
        chatBtn.addEventListener('click', () => {
          // 판매자용 해당 상품 채팅목록 (URL은 프로젝트에 맞게)
          window.location.href = `/newChatList.html?userId=${viewerId}&listingId=${listingId}`;
        });
      } else {
        // 구매자(또는 판매자 아닌 사용자) → 방 생성/조회 후 해당 roomId로 이동
        chatBtn.textContent = '채팅하기';
        chatBtn.addEventListener('click', async () => {
          try {
            if (!sellerId) {
              alert('판매자 정보가 없어 채팅을 시작할 수 없습니다.');
              return;
            }
            const url = `/api/chat/room/open?listingId=${listingId}&sellerId=${encodeURIComponent(sellerId)}`;
            const res = await fetch(url, { method: 'POST', credentials: 'include' });
            if (!res.ok) {
              if (res.status === 401) {
                alert('로그인이 필요합니다.');
                return;
              }
              throw new Error(await res.text() || '채팅방 생성/조회 실패');
            }
            const room = await res.json();
            if (!room?.roomId) throw new Error('roomId를 받지 못했습니다.');
            window.location.href = `/chatPage.html?roomId=${room.roomId}`;
          } catch (e) {
            console.error(e);
            alert(e.message);
          }
        });
      }
    }

    // ---- (이하: 이미지/관련상품/주문버튼 등 기존 로직 유지) ----
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

    // 관련 상품
    loadRelatedProducts(listingId);

    // 주문 버튼
    const orderButton = document.getElementById('order-button');
    if (orderButton) {
      orderButton.addEventListener('click', () => {
        window.location.href = `/order/order.html?listingId=${listingId}`;
      });
    }
  });

  function loadRelatedProducts(productId) {
    fetch(`/product/${productId}/related`)
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
              <p>${p.price != null ? p.price : ''} 원</p>
            </a>`;
          container.appendChild(item);
        });
      })
      .catch(err => console.error('관련 상품 불러오기 실패:', err));
  }
