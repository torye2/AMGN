// review.js

document.addEventListener('DOMContentLoaded', async () => {

    // 2️⃣ 주문 선택 목록 불러오기 (리뷰 작성용)
    await loadUserOrders();

    // 3️⃣ 선택된 주문 리뷰 불러오기
    const orderSelect = document.getElementById('orderSelect');
    orderSelect.addEventListener('change', () => {
        const orderId = orderSelect.value;
        loadReviews(orderId);
    });

    // 4️⃣ 리뷰 작성 폼 제출
    const reviewForm = document.getElementById('review-form');
    reviewForm.addEventListener('submit', submitReview);

    // 5️⃣ 별점 클릭 이벤트 초기화
    setupStarRating();
});

// 로그인된 사용자의 주문 불러오기
async function loadUserOrders() {
    try {
        const res = await fetch('/api/reviews/mine'); // ReviewController /api/reviews/mine
        if (!res.ok) throw new Error('주문 불러오기 실패');
        const orders = await res.json();
        const select = document.getElementById('orderSelect');
        select.innerHTML = '<option value="">-- 주문 선택 --</option>';
        orders.forEach(order => {
            const option = document.createElement('option');
            option.value = order.orderId;
            option.textContent = `주문 ${order.orderId} - ${order.itemTitle || '제목 없음'}`;
            select.appendChild(option);
        });
    } catch (err) {
        console.error(err);
    }
}

// 특정 주문 리뷰 목록 불러오기
async function loadReviews(orderId) {
    if (!orderId) return;

    try {
        const res = await fetch(`/api/reviews/orders/${orderId}`);
        if (!res.ok) throw new Error('리뷰 불러오기 실패');
        const reviews = await res.json();

        const listDiv = document.getElementById('review-list');
        const ratingSpan = document.getElementById('ratingAverage');
        listDiv.innerHTML = "";

        if (!reviews || reviews.length === 0) {
            listDiv.innerHTML = "<p>리뷰가 없습니다.</p>";
            ratingSpan.textContent = "-";
            return;
        }

        let sum = 0;
        reviews.forEach(r => {
            sum += r.score;
            const div = document.createElement('div');
            div.innerHTML = `
                <p><strong>작성자:</strong> ${r.raterNickName || r.raterId}</p>
                <p><strong>평점:</strong> ${r.score}</p>
                <p><strong>댓글:</strong> ${r.rvComment || "-"}</p>
                <p><em>${new Date(r.createdAt).toLocaleString()}</em></p>
                <hr>
            `;
            listDiv.appendChild(div);
        });

        ratingSpan.textContent = (sum / reviews.length).toFixed(1);
    } catch (err) {
        console.error(err);
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
            const errData = await res.json();
            throw new Error(errData.message || '리뷰 작성 실패');
        }

        alert('리뷰 작성 완료!');
        document.getElementById('review-form').reset();
        document.getElementById('score').value = '';
        loadReviews(orderId); // 작성 후 목록 갱신
    } catch (err) {
        console.error(err);
        alert(err.message);
    }
}

// 별점 클릭 이벤트 설정
function setupStarRating() {
    const stars = document.querySelectorAll('#starRating .star');
    const scoreInput = document.getElementById('score');

    stars.forEach(star => {
        const value = parseInt(star.getAttribute('data-value'));

        star.addEventListener('click', () => {
            scoreInput.value = value;
            updateStars(value);
        });

        star.addEventListener('mouseover', () => updateStars(value));
        star.addEventListener('mouseout', () => updateStars(Number(scoreInput.value)));
    });

    function updateStars(value) {
        stars.forEach(star => {
            const starValue = parseInt(star.getAttribute('data-value'));
            star.textContent = starValue <= value ? '⭐' : '☆';
        });
    }
}


