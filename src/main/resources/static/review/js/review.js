document.addEventListener('DOMContentLoaded', async () => {
    const params = new URLSearchParams(window.location.search);
    const orderId = params.get('orderId');
    const reviewId = params.get('reviewId'); // 수정 모드
    const listingId = params.get('listingId'); // 상품 ID

    if (!orderId) {
        alert('주문 정보가 없습니다.');
        return;
    }

    try {
        // ----- 상품 정보 불러오기 -----
        if (listingId) {
            const res = await fetch(`/product/${listingId}`);
            if (!res.ok) throw new Error('상품 정보를 가져올 수 없습니다.');
            const product = await res.json();

            document.getElementById('productTitle').textContent = product.title || '-';
            document.getElementById('productPrice').textContent = product.price ? product.price.toLocaleString() + '원' : '-';
            document.getElementById('productSeller').textContent = product.sellerNickname || '-';

            const imagesDiv = document.getElementById('productImages');
            imagesDiv.innerHTML = '';
            if (product.photoUrls?.length > 0) {
                const img = document.createElement('img');
                img.src = product.photoUrls[0];
                img.alt = product.title;
                img.style.width = '200px';
                imagesDiv.appendChild(img);
            } else {
                imagesDiv.textContent = '이미지가 없습니다.';
            }
        }

        // ----- 주문 정보 불러오기 -----
        const orderRes = await fetch(`/orders/buy`);
        if (!orderRes.ok) throw new Error('주문 정보 조회 실패');
        const orders = await orderRes.json();
        const order = orders.find(o => String(o.id) === String(orderId));
        if (!order) throw new Error('해당 주문을 찾을 수 없습니다.');

        // 선택된 상품 표시


        // ----- 리뷰 수정 모드 진입 -----
        if (reviewId) {
            const res = await fetch(`/api/reviews/orders/${orderId}`);
            if (!res.ok) throw new Error('리뷰 조회 실패');
            const reviews = await res.json();
            const review = reviews.find(r => String(r.id) === String(reviewId));
            if (review) {
                document.getElementById('score').value = review.score;
                document.getElementById('rvCommentTextarea').value = review.rvComment || '';
                updateStars(review.score);

                const hidden = document.createElement('input');
                hidden.type = 'hidden';
                hidden.id = 'reviewIdHidden';
                hidden.value = review.id;
                document.getElementById('review-form').appendChild(hidden);
            }
        }

        // 리뷰 작성/수정 이벤트
        document.getElementById('review-form').addEventListener('submit', e => submitReview(e, orderId));

        // 별점 세팅
        setupStarRating();

    } catch (err) {
        console.error(err);
        alert(err.message);
    }
});

// 리뷰 작성/수정
async function submitReview(e, orderId) {
    e.preventDefault();
    const score = Number(document.getElementById('score').value);
    const rvComment = document.getElementById('rvCommentTextarea').value;
    const reviewId = document.getElementById('reviewIdHidden')?.value;

    if (!score) { alert('별점을 선택해주세요'); return; }

    try {
        let res;
        if (reviewId) {
            res = await fetch(`/api/reviews/${reviewId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ score, rvComment })
            });
        } else {
            res = await fetch(`/api/reviews`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderId: Number(orderId), score, rvComment })
            });
        }

        if (!res.ok) throw new Error('리뷰 처리 실패');

        // 완료 후 마이페이지 구매내역으로 이동
        window.location.href = '/mypage/mypage.html?tab=purchases';

    } catch (err) {
        console.error(err);
        alert(err.message);
    }
}

// 별점
function setupStarRating() {
    const stars = document.querySelectorAll('#starRating .star');
    const scoreInput = document.getElementById('score');

    stars.forEach(star => {
        const value = parseInt(star.getAttribute('data-value'));
        star.addEventListener('click', () => {
            scoreInput.value = value;
            updateStars(value);
        });
    });
}

function updateStars(value) {
    const stars = document.querySelectorAll('#starRating .star');
    stars.forEach(star => {
        star.textContent = parseInt(star.getAttribute('data-value')) <= value ? '⭐' : '☆';
    });
}
