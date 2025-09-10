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

  // ---- 버튼 영역(구매 vs 수정/삭제) ----
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
        // 프로젝트 경로에 맞추어 수정 페이지로 이동
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
          // 삭제 후 목록/홈 등으로 이동 (경로는 프로젝트에 맞게)
          window.location.href = '/main.html';
        } catch (e) {
          console.error(e);
          alert('삭제 중 오류가 발생했습니다.');
        }
      });

      buttonGroup.append(editBtn, deleteBtn);
    } else {
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
