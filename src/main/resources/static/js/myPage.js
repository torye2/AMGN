document.addEventListener('DOMContentLoaded', function() {
    const showProductsLink = document.getElementById('show-products');
    const productListContainer = document.getElementById('product-list-container');

        // 기존의 '등록한 상품' 클릭 이벤트 리스너
    showProductsLink.addEventListener('click', function(event) {
        event.preventDefault();

        fetch('/product/my-products')
            .then(response => {
                if (!response.ok) {
                    if (response.status === 401) {
                        alert("로그인이 필요합니다.");
                        window.location.href = '/login';
                    }
                    throw new Error('상품 정보를 불러오는 데 실패했습니다.');
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
                    productItem.classList.add('product-item');
                    productItem.innerHTML = `
                        <a href="/productDetail.html?id=${product.listingId}">
                            <img src="${product.photoUrl || 'https://placehold.co/300x200?text=No+Image'}" alt="${product.title}" />
                            <h4 class="product-title">${product.title}</h4>
                            <p class="product-price">${product.price.toLocaleString()} 원</p>
                        </a>
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