document.addEventListener("DOMContentLoaded", async () => {
    const urlParams = new URLSearchParams(window.location.search);
    const listingId = urlParams.get("listingId"); 
    const listingInput = document.querySelector("input[name='listingId']");

    if (listingId && listingInput) {
        listingInput.value = listingId;
        listingInput.readOnly = true;
    }

    if (listingId) {
        try {
            const res = await fetch(`/product/${listingId}`);
            if (!res.ok) throw new Error("상품 정보를 가져올 수 없습니다.");
            const product = await res.json();

            document.getElementById("productTitle").textContent = product.title;
            document.getElementById("productPrice").textContent = product.price;
            document.getElementById("productSeller").textContent = product.sellerNickname;

            // 대표 사진 한 장만 표시
            const imagesDiv = document.getElementById("productImages");
            imagesDiv.innerHTML = '';
            if (product.photoUrls && product.photoUrls.length > 0) {
                const img = document.createElement('img');
                img.src = product.photoUrls[0]; // 첫 번째 사진만 사용
                img.alt = product.title;
                img.style.width = "200px";
                imagesDiv.appendChild(img);
            } else {
                imagesDiv.textContent = "이미지가 없습니다.";
            }
        } catch (err) {
            console.error(err);
            alert("상품 정보를 불러오는 중 오류가 발생했습니다.");
        }
    }

    // 거래 방식 선택 관련
    const methodSelect = document.getElementById("methodSelect");
    const meetupFields = document.getElementById("meetupFields");
    const deliveryFields = document.getElementById("deliveryFields");
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
            window.location.href = '/main.html';
        } catch (err) {
            console.error(err);
            alert('주문 등록 중 오류가 발생했습니다.');
        }
    });
	
	async function loadOrders() {
	    const tbody = document.querySelector('#ordersTable tbody');
	    tbody.innerHTML = '';

	    try {
	        const res = await fetch('/orders/buy'); // 구매 내역 API 호출
	        if (!res.ok) throw new Error('주문 내역 불러오기 실패');

	        const orders = await res.json();
	        if (!orders || orders.length === 0) {
	            tbody.innerHTML = `<tr><td colspan="8">주문 내역이 없습니다.</td></tr>`;
	            return;
	        }

	        for (const order of orders) {
	            const tr = document.createElement('tr');
	            tr.innerHTML = `
	                <td>${order.id}</td>
	                <td><a href="/productDetail.html?id=${order.listingId}">${order.listingTitle ?? '-'}</a></td>
	                <td>${order.method ?? '-'}</td>
	                <td>${order.status ?? '-'}</td>
	                <td>${order.finalPrice ?? '-'}</td>
	                <td></td>
	            `;
	            const actionTd = tr.querySelector('td:last-child');

	            // 상태별 버튼/텍스트 처리
	            if (order.status === 'CREATED') {
	                actionTd.appendChild(createButton('결제', () => payOrder(order)));
	                actionTd.appendChild(createButton('취소', () => cancelOrder(order.id, actionTd, order)));
	            } else if (order.status === 'PAID') {
	                actionTd.appendChild(createButton('주문 확정', () => completeOrder(order.id, actionTd)));
	                actionTd.appendChild(createButton('결제 취소', () => revertToCreated(order.id, actionTd, order)));
	            } else if (order.status === 'COMPLETED') {
	                actionTd.textContent = '주문 확정';
	            } else if (order.status === 'CANCELLED') {
	                actionTd.textContent = '취소됨';
	            }

	            tbody.appendChild(tr);
	        }
	    } catch (err) {
	        console.error(err);
	        tbody.innerHTML = `<tr><td colspan="8">주문 내역을 불러오는 중 오류가 발생했습니다.</td></tr>`;
	    }
	}


	loadOrders();


});
