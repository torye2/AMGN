document.addEventListener("DOMContentLoaded", async () => {

    // ----- 상품 정보 불러오기 -----
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

            const imagesDiv = document.getElementById("productImages");
            imagesDiv.innerHTML = '';
            if (product.photoUrls?.length > 0) {
                const img = document.createElement('img');
                img.src = product.photoUrls[0];
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

    // ----- 거래 방식 선택 -----
    const methodSelect = document.getElementById("methodSelect");
    const meetupFields = document.getElementById("meetupFields");
    const deliveryFields = document.getElementById("deliveryFields");
    methodSelect.addEventListener("change", () => {
        meetupFields.style.display = methodSelect.value === "MEETUP" ? "block" : "none";
        deliveryFields.style.display = methodSelect.value === "DELIVERY" ? "block" : "none";
        const selectedCard = document.querySelector('.addr-card.selected');
        if (selectedCard) {
            const addrId = selectedCard.dataset.addrId;
            fillFormWithAddress(addressMap[addrId]);
        }
    });

    // ----- 주소 불러오기 -----
    const addressMap = {}; // addrId -> 주소 객체
    async function loadAddresses() {
        const addressCardsDiv = document.getElementById("addressCards");
        addressCardsDiv.innerHTML = '<p>불러오는 중...</p>';

        try {
            const res = await fetch('/api/addresses');
            if (!res.ok) throw new Error('주소 정보를 가져올 수 없습니다.');
            const userAddresses = await res.json();

            if (!Array.isArray(userAddresses) || userAddresses.length === 0) {
                addressCardsDiv.innerHTML = '<p>저장된 주소가 없습니다.</p>';
                return;
            }

            addressCardsDiv.innerHTML = '';
            userAddresses.forEach(addr => {
                addressMap[addr.addressId] = addr;
                const card = document.createElement('div');
                card.classList.add('addr-card');
                card.dataset.addrId = addr.addressId;
                card.textContent = addr.addressType; // 집, 회사, 기타
                card.addEventListener('click', () => {
                    selectAddressCard(addr.addressId);
                    fillFormWithAddress(addr);
                });
                addressCardsDiv.appendChild(card);
            });

            // 기본 주소 자동 선택
            const defaultAddr = userAddresses.find(a => a.isDefault) || userAddresses[0];
            if (defaultAddr) {
                selectAddressCard(defaultAddr.addressId);
                fillFormWithAddress(defaultAddr);
            }

        } catch (err) {
            console.error(err);
            addressCardsDiv.innerHTML = `<p>주소 정보를 가져올 수 없습니다: ${err.message}</p>`;
        }
    }

    function selectAddressCard(addrId) {
        document.querySelectorAll('.addr-card').forEach(card => {
            card.classList.toggle('selected', card.dataset.addrId == addrId);
        });
    }

    // ----- 주소 구성 -----
    function buildFullAddress(addr) {
        const parts = [addr.province, addr.city, addr.addressLine1, addr.addressLine2]
            .filter(part => part && part.trim() !== '');
        const uniqueParts = [...new Set(parts)];
        return uniqueParts.join(' ');
    }

    function fillFormWithAddress(addr) {
        if (!addr) return;
        if (methodSelect.value === 'MEETUP') {
            document.querySelector("input[name='recvAddr1']").value = '';
        } else if (methodSelect.value === 'DELIVERY') {
            document.querySelector("input[name='recvAddr2']").value = buildFullAddress(addr);
            document.querySelector("input[name='recvZip']").value = addr.postalCode || '';
        }
        document.querySelector("input[name='recvName']").value = addr.recipientName || '';
        document.querySelector("input[name='recvPhone']").value = addr.recipientPhone || '';
    }

    // ----- 주문 등록 -----
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
            methodSelect.value = "MEETUP";
            meetupFields.style.display = "block";
            deliveryFields.style.display = "none";
            await loadAddresses(); // 기본 주소 재선택
            window.location.href = '/main.html';
        } catch (err) {
            console.error(err);
            alert('주문 등록 중 오류가 발생했습니다.');
        }
    });

    // 초기 주소 불러오기
    await loadAddresses();

});
