document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const listingId = urlParams.get('id');

    if (!listingId) return;

    fetch(`/product/${listingId}`)
        .then(res => {
            if (!res.ok) throw new Error('상품 정보를 불러오는데 실패했습니다.');
            return res.json();
        })
        .then(product => {
            // 상품 정보
            document.getElementById('productTitle').textContent = product.title || '-';
            document.getElementById('productSeller').textContent = `판매자: ${product.sellerNickname || '-'}`;
            document.getElementById('productPrice').textContent = product.price != null ? `${product.price.toLocaleString()} 원` : '-';
            document.getElementById('productDesc').textContent = product.description || '-';

            // 채팅 버튼
            const chatButton = document.getElementById('chat-button');
            chatButton.addEventListener('click', () => {
                window.location.href = `/chatPage.html?roomId=${listingId}&listingId=${listingId}&sellerId=${product.sellerId}`;
            });

            // 이미지 슬라이드
            const slidesContainer = document.getElementById('productSlides');
            slidesContainer.innerHTML = '';

            if (product.photoUrls && product.photoUrls.length > 0) {
                product.photoUrls.forEach(url => {
                    const div = document.createElement('div');
                    div.className = 'swiper-slide';
                    const img = document.createElement('img');
                    img.src = url.startsWith('/uploads') ? url : `/uploads/${url}`;
                    img.alt = product.title || '상품 이미지';
					img.className = 'product-img';
                    div.appendChild(img);
                    slidesContainer.appendChild(div);
                });
            } else {
                const div = document.createElement('div');
                div.className = 'swiper-slide';
                div.textContent = '사진이 없습니다.';
                slidesContainer.appendChild(div);
            }

            // Swiper 초기화
            new Swiper('.swiper', {
                loop: true,
                navigation: { nextEl: '.swiper-button-next', prevEl: '.swiper-button-prev' },
                pagination: { el: '.swiper-pagination', clickable: true },
            });
        })
        .catch(err => {
            document.getElementById('product-details').innerHTML = `<p style="color:red;">${err.message}</p>`;
            console.error(err);
        });
});

// 관련 상품 리스트
document.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const productId = params.get("id");

    // 상품 상세 불러오기
    fetch(`/product/${productId}`)
        .then(res => res.json())
        .then(product => {
            document.getElementById("productTitle").textContent = product.title;
            document.getElementById("productPrice").textContent = product.price + " 원";
            document.getElementById("productDesc").textContent = product.description;

            // 관련 상품 불러오기
            loadRelatedProducts(product.listingId);
        })
        .catch(err => console.error(err));
});

function loadRelatedProducts(productId) {
    fetch(`/product/${productId}/related`)
        .then(res => res.json())
        .then(relatedProducts => {
            const container = document.querySelector(".related-products");

            if (relatedProducts.length === 0) {
                container.innerHTML += "<p>관련 상품이 없습니다.</p>";
                return;
            }

            relatedProducts.forEach(p => {
                const item = document.createElement("div");
                item.className = "product-item";

                const imgUrl = (p.photoUrls && p.photoUrls.length > 0)
                    ? p.photoUrls[0]
                    : "https://placehold.co/300x200?text=No+Image";

                item.innerHTML = `
                    <a href="/productDetail.html?id=${p.listingId}">
                        <img src="${imgUrl}" alt="${p.title}" />
                        <p>${p.title}</p>
                        <p>${p.price} 원</p>
                    </a>
                `;
                container.appendChild(item);
            });
        })
        .catch(err => console.error(err));
}

	document.addEventListener("DOMContentLoaded", () => {
	    const orderButton = document.getElementById("order-button");
	    if (orderButton) {
	        orderButton.addEventListener("click", () => {
	            window.location.href = "order/order.html";
	        });
	    }
	});
	
	document.addEventListener("DOMContentLoaded", () => {
	    const orderButton = document.getElementById("order-button");
	    const urlParams = new URLSearchParams(window.location.search);
	    const productId = urlParams.get("id"); // productDetail?id=32 → 32 추출
	
	    if (orderButton && productId) {
	        orderButton.addEventListener("click", () => {
	            // order.html로 이동하면서 id 값 전달
	            window.location.href = `/order/order.html?listingId=${productId}`;
	        });
	    }
	});

    document.addEventListener("DOMContentLoaded", () => {
        const thecheatButton= document.getElementById("thecheat-button");
        thecheatButton.addEventListener("click", () => {
            window.location.href = "https://thecheat.co.kr/rb/?mod=_search";
        })
    })