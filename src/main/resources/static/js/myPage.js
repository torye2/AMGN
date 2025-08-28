document.addEventListener('DOMContentLoaded', function() {
    const showProductsLink = document.getElementById('show-products');
    const productListContainer = document.getElementById('product-list-container');

    showProductsLink.addEventListener('click', function(event) {
        event.preventDefault();

        // 로그인한 사용자의 상품만 가져오는 API 호출
        fetch('/product/my-products')
            .then(response => {
                if (!response.ok) {
                    // 로그인하지 않은 경우 401 Unauthorized 에러 처리
                    if (response.status === 401) {
                        alert("로그인이 필요합니다.");
                        window.location.href = '/login'; // 로그인 페이지로 리다이렉트
                    }
                    throw new Error('상품 정보를 불러오는데 실패했습니다.');
                }
                return response.json();
            })
            .then(data => {
                productListContainer.style.display = 'block';
                productListContainer.innerHTML = '';

                if (data.length === 0) {
                    productListContainer.innerHTML = '<p>등록된 상품이 없습니다.</p>';
                    return;
                }

                data.forEach(product => {
                    const productItem = document.createElement('div');
                    productItem.className = 'product-item';
                    productItem.innerHTML = `
                        <h3>${product.title}</h3>
                        <p>가격: ${product.price} 원</p>
                        <p>${product.description}</p>
                        <a href="/product/${product.listingId}">상세 보기</a>
                    `;
                    productListContainer.appendChild(productItem);
                });
            })
            .catch(error => {
                console.error('Error:', error);
                productListContainer.innerHTML = `<p>오류가 발생했습니다: ${error.message}</p>`;
            });
    });
});