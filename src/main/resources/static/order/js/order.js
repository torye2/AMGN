// 거래 방식 선택 관련 DOM
const methodSelect = document.getElementById("methodSelect");
const meetupFields = document.getElementById("meetupFields");
const deliveryFields = document.getElementById("deliveryFields");

// 거래 방식 변경 시 입력 폼 토글
methodSelect.addEventListener("change", () => {
    meetupFields.style.display = methodSelect.value === "MEETUP" ? "block" : "none";
    deliveryFields.style.display = methodSelect.value === "DELIVERY" ? "block" : "none";
});

// 주문 등록
document.getElementById('orderForm').addEventListener('submit', async function(e) {
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

    try {
        const res = await fetch(`/orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error('주문 등록 실패');
        alert('주문 등록 완료!');
        this.reset();
        meetupFields.style.display = "block";
        deliveryFields.style.display = "none";
        loadOrders();
    } catch (err) {
        console.error(err);
        alert('주문 등록 중 오류가 발생했습니다.');
    }
});

// 주문 내역 불러오기
async function loadOrders() {
    try {
        const res = await fetch(`/orders`);
        if (!res.ok) throw new Error('주문 내역 불러오기 실패');
        const orders = await res.json();

        const tbody = document.querySelector('#ordersTable tbody');
        tbody.innerHTML = '';

        if (!Array.isArray(orders) || orders.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6">주문 내역이 없습니다.</td></tr>`;
            return;
        }

        orders.forEach(order => {
            if (order.status === 'CANCELLED') return;

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
                actionTd.appendChild(createButton('결제', () => payOrder(order)));
                actionTd.appendChild(createButton('취소', () => cancelOrder(order.id)));
            }

            if (order.status === 'PAID' || order.status === 'COMPLETED') {
                actionTd.textContent = '결제 완료';
            }

            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error(err);
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

// 결제 처리
function payOrder(order) {
    const idempotencyKey = `order_${order.id}_${Date.now()}`;
    const expiryDate = new Date(Date.now() + 3600 * 1000).toISOString();

    const payload = {
        method: order.method || "CARD",      // 주문 방식 동적 처리
        amount: order.finalPrice,
        response: "paid",
        idempotencyKey,
        expiryDate
    };

    fetch(`/orders/${order.id}/pay`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(res => {
        if (!res.ok) throw new Error('결제 실패');
        return res.json();
    })
    .then(data => {
        console.log('결제 완료:', data);
        loadOrders();
    })
    .catch(err => console.error(err));
}

// 주문 취소
function cancelOrder(orderId) {
    fetch(`/orders/${orderId}/cancel`, { method: 'DELETE' })
        .then(res => {
            if (!res.ok) throw new Error('취소 실패');
            loadOrders();
            alert('주문이 취소되었습니다.');
        })
        .catch(err => console.error(err));
}

// 초기 로드
loadOrders();