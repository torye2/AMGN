const userId = 1; // 임시 로그인 사용자
const methodSelect = document.getElementById("methodSelect");
const meetupFields = document.getElementById("meetupFields");
const deliveryFields = document.getElementById("deliveryFields");

// 거래 방식 변경 시 입력 폼 토글
methodSelect.addEventListener("change", () => {
    if (methodSelect.value === "MEETUP") {
        meetupFields.style.display = "block";
        deliveryFields.style.display = "none";
    } else {
        meetupFields.style.display = "none";
        deliveryFields.style.display = "block";
    }
});

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
    .then(() => {
        alert('주문 등록 완료!');
        orderForm.reset();
        meetupFields.style.display = "block";
        deliveryFields.style.display = "none";
        loadOrders();
    })
    .catch(err => console.error(err));
});

// 주문 내역 불러오기
async function loadOrders() {
    try {
        const res = await fetch(`/orders?userId=${userId}`);
        if (!res.ok) throw new Error('서버 오류 발생');

        const orders = await res.json();
        const tbody = document.querySelector('#ordersTable tbody');
        tbody.innerHTML = '';

        if (!Array.isArray(orders) || orders.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6">주문 내역이 없습니다.</td></tr>`;
            return;
        }

        orders.forEach(order => {
            if (order.status === 'CANCELLED') return; // 취소 주문 제외

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

            // CREATED 상태일 때만 결제/취소 버튼 노출
            if (order.status === 'CREATED') {
                actionTd.appendChild(createButton('결제', () => payOrder(order.id)));
                actionTd.appendChild(createButton('취소', () => cancelOrder(order.id)));
            }

            // 결제 완료/판매 완료 시 버튼 대신 텍스트 표시
            if (order.status === 'PAID' || order.status === 'COMPLETED') {
                actionTd.textContent = '결제 완료';
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

// 주문 결제
function payOrder(orderId) {
    fetch(`/orders/${orderId}/pay?userId=${userId}`, { method: 'POST' })
        .then(res => res.json())
        .then(() => loadOrders())
        .catch(err => console.error(err));
}

// 주문 취소
function cancelOrder(orderId) {
    fetch(`/orders/${orderId}/cancel?userId=${userId}`, { method: 'DELETE' })
        .then(res => {
            if (!res.ok) throw new Error('취소 실패');
            // JSON 파싱 제거, 성공 시 바로 주문 리스트 갱신
            loadOrders();
			alert('주문이 취소되었습니다.');
        })
        .catch(err => console.error(err));
}

// 초기 로드
loadOrders();

