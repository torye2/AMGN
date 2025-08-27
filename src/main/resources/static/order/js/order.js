const userId = 1; // 임시: 로그인 사용자 ID


// 주문 등록
document.getElementById('orderForm').addEventListener('submit', function(e) {
	e.preventDefault();
	const formData = new FormData(this);
	const payload = {
		listingId: parseInt(formData.get('listingId')),
		method: formData.get('method'),
		recvName: formData.get('recvName'),
		recvPhone: formData.get('recvPhone'),
		recvAddr1: formData.get('recvAddr1'),
		recvAddr2: formData.get('recvAddr2'),
		recvZip: formData.get('recvZip')
	};

	fetch(`/orders?userId=${userId}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(payload)
	})
		.then(res => res.json())
		.then(data => {
			alert('주문 등록 완료!');
			loadOrders();
		})
		.catch(err => console.error(err));
});

// 주문 내역 불러오기
async function loadOrders() {
    try {
        const res = await fetch(`/orders?userId=${userId}`);

        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            console.error('서버 에러', errData);
            alert(errData.message || '서버 오류 발생');
            return;
        }

        const data = await res.json();

        const tbody = document.querySelector('#ordersTable tbody');
        tbody.innerHTML = ''; // 기존 내용 초기화

        if (!Array.isArray(data) || data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6">주문 내역이 없습니다.</td></tr>`;
            return;
        }

        data.forEach(order => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${order.id}</td>
                <td>${order.listingId}</td>
                <td>${order.method}</td>
                <td>${order.status}</td>
                <td>${order.finalPrice}</td>
                <td></td>
            `;
            const actionTd = tr.querySelector('td:last-child');

            if (order.status === 'CREATED') {
                actionTd.appendChild(createButton('결제', () => payOrder(order.id)));
            }
            if (order.status !== 'CANCELLED' && order.status !== 'COMPLETED') {
                actionTd.appendChild(createButton('취소', () => cancelOrder(order.id)));
            }
            if (order.status === 'PAID' && order.method === 'DELIVERY') {
                actionTd.appendChild(createButton('배송 입력', () => inputTracking(order.id)));
            }
            if (order.status === 'DELIVERED') {
                actionTd.appendChild(createButton('직거래 완료', () => confirmMeetup(order.id)));
            }
            if (order.status !== 'DISPUTED' && order.status !== 'CANCELLED' && order.status !== 'COMPLETED') {
                actionTd.appendChild(createButton('분쟁', () => disputeOrder(order.id)));
            }

            tbody.appendChild(tr);
        });

    } catch (err) {
        console.error('주문 내역 불러오기 중 오류', err);
        alert('주문 내역을 불러오는 중 오류가 발생했습니다.');
    }
}


// 버튼 생성 헬퍼
function createButton(text, onClick) {
	const btn = document.createElement('button');
	btn.textContent = text;
	btn.addEventListener('click', onClick);
	return btn;
}


// 결제
function payOrder(orderId) {
	const payload = { idempotencyKey: 'abc123', response: 'ok', expiryDate: new Date().toISOString() };
	fetch(`/orders/${orderId}/pay?userId=${userId}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(payload)
	})
		.then(res => res.json())
		.then(data => {
			alert('결제 완료!');
			loadOrders();
		})
		.catch(err => console.error(err));
}

// 취소
function cancelOrder(orderId) {
	fetch(`/orders/${orderId}/cancel?userId=${userId}`, { method: 'POST' })
		.then(res => res.json())
		.then(data => {
			alert('주문 취소 완료!');
			loadOrders();
		})
		.catch(err => console.error(err));
}

// 배송 입력
function inputTracking(orderId) {
	const payload = { trackingNumber: '123456' }; // 임시
	fetch(`/orders/${orderId}/tracking?userId=${userId}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(payload)
	})
		.then(res => res.json())
		.then(data => {
			alert('배송 입력 완료!');
			loadOrders();
		})
		.catch(err => console.error(err));
}

// 직거래 완료
function confirmMeetup(orderId) {
	fetch(`/orders/${orderId}/confirm-meetup?userId=${userId}`, { method: 'POST' })
		.then(res => res.json())
		.then(data => {
			alert('직거래 완료!');
			loadOrders();
		})
		.catch(err => console.error(err));
}

// 분쟁
function disputeOrder(orderId) {
	const reason = prompt("분쟁 사유를 입력해주세요:");
	if (!reason) return;
	fetch(`/orders/${orderId}/dispute?userId=${userId}&reason=${reason}`, { method: 'POST' })
		.then(res => res.json())
		.then(data => {
			alert('분쟁 처리 완료!');
			loadOrders();
		})
		.catch(err => console.error(err));
}

// 초기 로드
loadOrders();
/**
 * 
 */