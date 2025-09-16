// review.js (교체본)
document.addEventListener('DOMContentLoaded', async () => {
  const params = new URLSearchParams(window.location.search);
  const orderIdFromUrl = params.get('orderId');
  const listingIdFromUrl = params.get('listingId');

  try {
    // 1) 주문 목록 불러오기 (initialOrderId 전달하여 옵션 생성 시 선택 가능)
    await loadUserOrders(orderIdFromUrl);

    const orderSelect = document.getElementById('orderSelect');
    console.log('select options after load:', Array.from(orderSelect.options).map(o => o.value));

    // 2) URL에 orderId 있으면, 옵션에서 찾아서 선택하고 리뷰 로드
    if (orderIdFromUrl) {
      const idStr = String(orderIdFromUrl);
      const opt = Array.from(orderSelect.options).find(o => o.value === idStr);
      if (opt) {
        orderSelect.value = idStr;
        showSelectedProduct(opt, listingIdFromUrl);
        await loadReviews(idStr);
        console.log('Auto-selected order and loaded reviews for', idStr);
      } else {
        console.warn('자동 선택 실패: select 옵션에 orderId 없음 ->', idStr);
      }
    }

    // 3) select 변경 시 리뷰 로드
    orderSelect.addEventListener('change', async () => {
      const id = orderSelect.value;
      const opt = orderSelect.selectedOptions[0];
      showSelectedProduct(opt);
      await loadReviews(id);
    });

    // 4) 폼 제출, 별점 초기화
    document.getElementById('review-form').addEventListener('submit', submitReview);
    setupStarRating();

  } catch (err) {
    console.error('초기화 중 오류:', err);
  }
});

// 주문 목록 불러오기 (initialOrderId 있으면 해당 option에 selected 처리)
async function loadUserOrders(initialOrderId) {
  try {
    console.log('fetch /api/reviews/mine');
    const res = await fetch('/api/reviews/mine');
    if (!res.ok) throw new Error(`/api/reviews/mine 요청 실패: ${res.status}`);
    const orders = await res.json();
    console.log('orders from server:', orders);

    const select = document.getElementById('orderSelect');
    select.innerHTML = '<option value="">-- 주문 선택 --</option>';

    orders.forEach(order => {
      // 서버 리턴 필드명이 다르면 여기 수정 필요 (orderId vs id)
      const orderId = order.orderId ?? order.id;
      const itemTitle = order.itemTitle ?? order.listingTitle ?? '';
      const listingId = order.listingId ?? order.listing?.id ?? '';

      const option = document.createElement('option');
      option.value = String(orderId);
      option.textContent = `주문 ${orderId} - ${itemTitle || '제목 없음'}`;

      // 데이터셋에 listingId 넣어두면 나중에 확인하기 편함
      if (listingId) option.dataset.listingId = String(listingId);

      if (initialOrderId && String(initialOrderId) === String(orderId)) {
        option.selected = true;
      }

      select.appendChild(option);
    });
  } catch (err) {
    console.error('loadUserOrders 에러:', err);
    throw err;
  }
}

// 선택된 주문의 상품명(간단 표시)
function showSelectedProduct(option, expectedListingId) {
  if (!option) return;
  let display = document.getElementById('selectedItem');
  if (!display) {
    display = document.createElement('p');
    display.id = 'selectedItem';
    // 폼 위에 표시
    const form = document.getElementById('review-form');
    form.parentNode.insertBefore(display, form);
  }
  const text = option.textContent || '';
  const dsListingId = option.dataset.listingId;
  let note = '';
  if (expectedListingId && dsListingId) {
    note = dsListingId === String(expectedListingId) ? ' (listingId 일치)' : ` (listingId 불일치: option ${dsListingId} ≠ url ${expectedListingId})`;
  }
  display.textContent = `선택된 주문: ${text}${note}`;
}

// 리뷰 목록 불러오기 (디버깅 로그 포함)
async function loadReviews(orderId) {
  if (!orderId) {
    console.log('loadReviews: orderId 비어있음');
    return;
  }
  console.log('fetch /api/reviews/orders/' + orderId);
  try {
    const res = await fetch(`/api/reviews/orders/${orderId}`);
    if (!res.ok) {
      console.warn('/api/reviews/orders 응답 상태:', res.status);
      throw new Error('리뷰 조회 실패: ' + res.status);
    }
    const reviews = await res.json();
    console.log('reviews:', reviews);

    const listDiv = document.getElementById('review-list');
    const ratingSpan = document.getElementById('ratingAverage');
    if (listDiv) listDiv.innerHTML = '';

    if (!reviews || reviews.length === 0) {
      if (listDiv) listDiv.innerHTML = "<p>리뷰가 없습니다.</p>";
      if (ratingSpan) ratingSpan.textContent = "-";
      return;
    }

    let sum = 0;
    reviews.forEach(r => {
      sum += r.score;
      const div = document.createElement('div');
      div.innerHTML = `
        <p><strong>작성자:</strong> ${r.raterNickName || r.raterId || '-'}</p>
        <p><strong>평점:</strong> ${r.score}</p>
        <p><strong>댓글:</strong> ${r.rvComment || "-"}</p>
        <p><em>${r.createdAt ? new Date(r.createdAt).toLocaleString() : ''}</em></p>
        <hr>
      `;
      if (listDiv) listDiv.appendChild(div);
    });

    if (ratingSpan) ratingSpan.textContent = (sum / reviews.length).toFixed(1);
  } catch (err) {
    console.error('loadReviews 에러:', err);
  }
}

// 리뷰 작성
async function submitReview(e) {
  e.preventDefault();
  const orderId = Number(document.getElementById('orderSelect').value);
  const score = Number(document.getElementById('score').value);
  const rvComment = document.getElementById('rvCommentTextarea').value;

  if (!orderId || !score) {
    alert('주문 선택과 별점을 입력해주세요.');
    return;
  }

  try {
    const res = await fetch(`/api/reviews`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ orderId, score, rvComment })
    });
    if (!res.ok) {
      const errData = await res.json().catch(()=>({}));
      throw new Error(errData.message || '리뷰 작성 실패: ' + res.status);
    }
    alert('리뷰 작성 완료!');
    document.getElementById('review-form').reset();
    document.getElementById('score').value = '';
    await loadReviews(orderId);
  } catch (err) {
    console.error('submitReview 에러:', err);
    alert(err.message);
  }
}

// 별점
function setupStarRating() {
  const stars = document.querySelectorAll('#starRating .star');
  const scoreInput = document.getElementById('score');

  stars.forEach(star => {
    const value = parseInt(star.getAttribute('data-value'));
    // 클릭할 때만 반영
    star.addEventListener('click', () => {
      scoreInput.value = value;
      updateStars(value);
    });
  });

  // 기본 업데이트 함수 (선택된 값만 표시)
  function updateStars(value) {
    stars.forEach(star => {
      const starValue = parseInt(star.getAttribute('data-value'));
      star.textContent = starValue <= value ? '⭐' : '☆';
    });
  }
}

