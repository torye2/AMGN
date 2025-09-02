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
            // 1️⃣ 이미지 슬라이드
            const slidesContainer = document.getElementById('productSlides');
            slidesContainer.innerHTML = '';
            
            if (product.photoUrls && product.photoUrls.length > 0) {
                product.photoUrls.forEach(url => {
                    const div = document.createElement('div');
                    div.className = 'swiper-slide';
                    const img = document.createElement('img');
                    img.src = url;
                    img.alt = product.title || '상품 이미지';
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

            // 2️⃣ 나머지 상품 정보
            document.getElementById('productTitle').textContent = product.title || '-';
            document.getElementById('productSeller').textContent = `판매자: ${product.sellerNickname || '-'}`;
            document.getElementById('productPrice').textContent = product.price != null ? `${product.price.toLocaleString()} 원` : '-';
            document.getElementById('productDesc').textContent = product.description || '-';
        })
        .catch(err => {
            document.getElementById('product-details').innerHTML = `<p style="color:red;">${err.message}</p>`;
            console.error(err);
        });
});
