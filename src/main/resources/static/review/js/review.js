//리뷰 페이지 초기화
document.addEventListener('DOMContentLoaded', async () => {
	//1.헤더 로드
	try {
		const res = await fetch('../header.html');
		const html = await res.text();
		document.getElementById('header').innerHTML = html;
	} catch(err) {
		console.error('헤더 불러오기 실패', err);
	}
	
	//2. 주문 불러오기
	await fetchOrders();
	
	//3. 사용자 리뷰 불러오기
	loadUserReviews();
});

//별점 클릭 이벤트
(function setupStarRating() {
	const stars = document.querySelectorAll('#starRating .star');
	const scoreInput = document.getElementById('score');
	
	stars.forEach(star => {
		star.addEventListener('click', () => {
			const value = parseInt(star.getAttribute('data-value'));
			scoreInput.value = value;
			updateStarStyle(value);
		});
		
		star.addEventListener('mouseover', () => {
			const value = parseInt(star.getAttribute('data-value'));
			updateStarStyle(value);
		});
		
		star.addEventListener('mouseout', () => {
			updateStarStyle(parseInt(scoreInput.value));
		});
	});
	
	function updateStarStyle(value) {
		stars.forEach(star => {
			const starValue = parseInt(star.getAttribute('data-value'));
			star.textContent = starValue <= value ? '⭐' : '☆';
		});
	}
})();

//주문 선택 목록 불러오기 (리뷰 작성용)
async function fetchOrders() {
	try {
		const res = await fetch('/api/orders/mine');
		if(!res.ok) return;
		
		const orders = await res.json();
		const select = document.getElementById('orderSelect');
		select.innerHTML = '<option value = "">-- 주문 선택 --</option>'; // 초기화
		
		orders.forEach(order => {
			const option = document.createElement('option');
			option.value = order.id;
			option.textContent = `주문 ${order.id} - ${oreder.itemTitle || '제목 없음'}`;
			select.appendChild(option);
		});
	} catch (err) {
		console.error('주문 불러오기 실패', err);
	}
}

//특정 주문의 리뷰 목록 불러오기
async function loadOrderReviews(orderId) {
	if (!orderId) return;
	
	try {
		const res = await fetch(`/api/reviews/orders/${orderId}`);
		if (!res.ok) return;
		
		const review = await res.json();
		renderReviewList(reviews);
	} catch (err) {
		console.error('리뷰 불러오기 실패', err);
	}
}

// 현재 로그인한 사용자의 리뷰 불러오기
async function loadUserReviews() {
	const userId = 123; //예시
	const limit = 20;
	
	try {
		const res = await fetch(`/api/reviews/users/${userId}?limit=${limit}`);
		if (!res.ok) return;
		
		const reviews = await res.json();
		renderReviewList(reviews)
	} catch (err) {
		alert('리뷰 불러오기 실패 : ' + err.message);
	}
}

//리뷰 리스트 + 평균 평점 랜더링 함수
function renderReviewList(reviews) {
	const listDiv = document.getElementById("review-list");
	const ratingSpan = document.getElementById("ratingAverage");
	listDiv.innerHTML = "";
	
	if (!reviews || reviews.length === 0) {
		listDiv.innerHTML = "<p>리뷰가 없습니다.</p>";
		ratingSpan.textContent = "-";
		return;
	}
	
	let sum = 0;
	reviews.forEach(r => {
		sum += r.score;
		const item = document.createElement("div");
		item.innerHTML = `
			<p><strong>작성자 : </strong> ${r.rateNickName || r.raterId}</p>
			<p><strong>평점 : </strong> ${r.score}</p>
			<p><strong>댓글 : </strong> ${r.rvComment || "-"}</p>
			<p><em>${new Date(r.createdAt).toLocaleString()}</em></p>
			<hr>
		`;
		listDiv.appendChild(item);
	});
	
	const avg = (sum / review.length).toFixed(1);
	ratingSpan.textContent = avg;
}

//리뷰 작성 이벤트 핸들러
document.getElementById("review-form").addEventListener("submit", async (e) => {
	e.preventDefault();
	
	const raterId = 123; //로그인된 사용자 ID
 	const payload = {
		orderId : Number(document.getElementById("orderSelect").value),
		score : Number(document.getElementById("score").value),
		rvComment : document.getElementById("rvComment").value
	};
	
	try {
		const res = await fetch(`/api/reviews?uid=${raterId}`, {
			method : "POST",
			headers : { "Content-Type" : "application/json"},
			body : JSON.stringify(payload)
		});
		
		if (!res.ok) {
			const errData = await res.json();
			throw new Error(errData.message || "리뷰 작성 중 오류 발생");
		}
		
		alert("리뷰 작성 완료!");
		document.getElementById("review-form").reset();
		loadUserReviews(); // 작성 후 리뷰 목록 갱신
	} catch(err) {
		alert(err.message);
	}
});