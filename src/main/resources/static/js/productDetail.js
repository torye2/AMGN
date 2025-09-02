document.addEventListener('DOMContentLoaded', () => {
    // 현재 URL에서 상품 ID를 추출합니다.
    const urlParams = new URLSearchParams(window.location.search);
    const listingId = urlParams.get('id');

    if (listingId) {
        // 비동기 통신을 사용하여 백엔드 API에서 상품 정보를 가져옵니다.
        fetch(`/product/${listingId}`)
            .then(response => {
                if (!response.ok) {
                    // HTTP 상태 코드가 200번대가 아닐 경우 에러 처리
                    throw new Error('상품 정보를 불러오는데 실패했습니다.');
                }
                return response.json(); // 응답을 JSON 형식으로 변환
            })
            .then(product => {
                // 받아온 상품 정보를 HTML에 동적으로 삽입합니다.
                const productDetailsDiv = document.getElementById('product-details');
                productDetailsDiv.innerHTML = `
              <img src="${product.photoUrl}" alt="${product.title}" class="product-img" />
              <h1 class="product-name">상품명: ${product.title}</h1><br>
              <p class="product-nickname">판매자: ${product.sellerNickname}</p>
              <p class="product-price">가격: ${product.price.toLocaleString()} 원</p>
              <p class="product-introduct">${product.description}</p>
              <p class="introduct">상세정보</p>
              <hr />
            `;
            })
            .catch(error => {
                const productDetailsDiv = document.getElementById('product-details');
                productDetailsDiv.innerHTML = `<p style="color: red;">${error.message}</p>`;
                console.error('Error fetching product details:', error);
            });
    }
});