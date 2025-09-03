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
            loadOrders();
        } catch (err) {
            console.error(err);
            alert('주문 등록 중 오류가 발생했습니다.');
        }
    });

    // 초기 주문 내역 불러오기
    loadOrders();
});
