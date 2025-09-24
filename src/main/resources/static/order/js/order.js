document.addEventListener("DOMContentLoaded", async () => {

    const urlParams = new URLSearchParams(window.location.search);
    const listingId = urlParams.get("listingId");
    const listingInput = document.querySelector("input[name='listingId']");
    const methodSelect = document.getElementById("methodSelect");
    const meetupFields = document.getElementById("meetupFields");
    const deliveryFields = document.getElementById("deliveryFields");

    if (listingId && listingInput) {
        listingInput.value = listingId;
        listingInput.readOnly = true;
    }

    // ----- 거래 방식 옵션 초기화 -----
    function initMethodSelect(availableMethods) {
        methodSelect.innerHTML = '';

        if (availableMethods.length === 0) {
            // 거래 방식 없음 → 기본 직거래
            availableMethods = ["MEETUP"];
        }

        if (availableMethods.length === 1) {
            // 한 가지 방식만 허용 → select 숨김, 해당 필드만 보이기
            methodSelect.style.display = 'none';
            showFieldsByMethod(availableMethods[0]);
        } else {
            // 둘 다 가능 → 드롭다운 표시
            methodSelect.style.display = 'block';
            availableMethods.forEach(method => {
                const option = document.createElement('option');
                option.value = method;
                option.textContent = method === 'MEETUP' ? '직거래' : '택배 거래';
                methodSelect.appendChild(option);
            });
            methodSelect.value = availableMethods[0];
            showFieldsByMethod(methodSelect.value);
        }
    }

    // ----- 거래 방식 필드 표시 -----
    function showFieldsByMethod(method) {
        meetupFields.style.display = method === "MEETUP" ? "block" : "none";
        deliveryFields.style.display = method === "DELIVERY" ? "block" : "none";
    }

    methodSelect.addEventListener("change", () => {
        showFieldsByMethod(methodSelect.value);
        const selectedCard = document.querySelector('.addr-card.selected');
        if (selectedCard) {
            const addrId = selectedCard.dataset.addrId;
            fillFormWithAddress(addressMap[addrId]);
        }
    });

    // ----- 상품 정보 불러오기 -----
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

            // tradeType 기반으로 availableMethods 배열 생성
            let availableMethods = [];
            switch (product.tradeType) {
                case "MEETUP":
                    availableMethods = ["MEETUP"];
                    break;
                case "DELIVERY":
                    availableMethods = ["DELIVERY"];
                    break;
                case "BOTH":
                    availableMethods = ["MEETUP", "DELIVERY"];
                    break;
                default:
                    availableMethods = ["MEETUP"];
            }

            initMethodSelect(availableMethods);

        } catch (err) {
            console.error(err);
            alert("상품 정보를 불러오는 중 오류가 발생했습니다.");
        }
    }

    // ----- 주소 불러오기 -----
    const addressMap = {};
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
                card.textContent = addr.addressType;
                card.addEventListener('click', () => {
                    selectAddressCard(addr.addressId);
                    fillFormWithAddress(addr);
                });
                addressCardsDiv.appendChild(card);
            });

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

    function buildFullAddress(addr) {
        const parts = [addr.province, addr.city, addr.addressLine1, addr.addressLine2]
            .filter(part => part && part.trim() !== '');
        return [...new Set(parts)].join(' ');
    }

    function fillFormWithAddress(addr) {
        if (!addr) return;
        if (meetupFields.style.display === 'block') {
            document.querySelector("input[name='recvAddr1']").value = '';
        } else if (deliveryFields.style.display === 'block') {
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
            method: meetupFields.style.display === 'block' ? 'MEETUP' : 'DELIVERY',
            recvName: formData.get('recvName'),
            recvPhone: formData.get('recvPhone'),
            recvAddr1: formData.get('recvAddr1'),
            recvAddr2: formData.get('recvAddr2'),
            recvZip: formData.get('recvZip'),
            paymentMethod: 'KG_INICIS' // 기본 결제 수단
        };
        if (!getCookie('XSRF-TOKEN')) await ensureCsrf();
        const xsrf = getCookie('XSRF-TOKEN');

        try {
            const res = await fetch(`/orders`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': xsrf
                },
                body: JSON.stringify(payload),
                credentials: 'include'
            });
            if (!res.ok) throw new Error('주문 등록 실패');
            alert('주문 등록 완료!');
            this.reset();
            await loadAddresses();
            window.location.href = '/main.html';
        } catch (err) {
            console.error(err);
            alert('주문 등록 중 오류가 발생했습니다.');
        }
    });

    // 초기 주소 불러오기
    await loadAddresses();

});

async function ensureCsrf() {
    const r = await fetch('/api/csrf', {
        headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
        credentials: 'same-origin'
    });
    const j = await r.json(); // { headerName, token }
    return j; // 필요 시 헤더명도 동적으로 사용
}

function getCookie(name) {
    return document.cookie.split('; ').find(v => v.startsWith(name + '='))?.split('=')[1];
}