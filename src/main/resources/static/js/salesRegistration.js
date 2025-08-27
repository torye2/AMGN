// salesForm은 상품 등록 폼의 <form> 요소
const salesForm = document.getElementById('salesForm');
const messageDiv = document.getElementById('message');

salesForm.addEventListener('submit', async (event) => {
    event.preventDefault(); // 폼 제출의 기본 동작(페이지 새로고침) 방지
    messageDiv.textContent = "저장 중입니다...";
    messageDiv.style.color = "blue";

    try {
        // FormData 객체 생성
        const formData = new FormData(salesForm);

        // 동적 속성(attrs)을 JSON 배열로 수집합니다.
        const attrKeys = formData.getAll('attrKey[]');
        const attrValues = formData.getAll('attrValue[]');
        const attrs = [];
        for (let i = 0; i < attrKeys.length; i++) {
            if (attrKeys[i].trim() !== '' && attrValues[i].trim() !== '') {
                attrs.push({
                    attrKey: attrKeys[i],
                    attrValue: attrValues[i]
                });
            }
        }

        // 일반 텍스트 데이터를 JSON 객체로 수집합니다.
        const listingData = {
            title: formData.get('title'),
            price: parseFloat(formData.get('price')),
            negotiable: formData.get('negotiable') ? 'Y' : 'N',
            categoryId: parseInt(formData.get('categoryId')),
            itemCondition: formData.get('itemCondition'),
            description: formData.get('description'),
            tradeType: formData.get('tradeType'),
            regionId: parseInt(formData.get('region')), // 'region'을 'regionId'로 변환
            safePayYn: formData.get('safePayYn') ? 'Y' : 'N'
        };

        // 폼 데이터에 JSON 문자열을 추가하고 기존 필드는 제거
        formData.set('listingData', JSON.stringify(listingData));
        formData.set('attrs', JSON.stringify(attrs));
        formData.delete('attrKey[]');
        formData.delete('attrValue[]');

        const response = await fetch('/product/write', {
            method: 'POST',
            body: formData, // FormData 객체를 그대로 전송
        });

        const result = await response.json();

        if (response.ok) {
            messageDiv.textContent = result.message;
            messageDiv.style.color = "green";
            // ✅ 성공 후 리다이렉션 로직
            if (result.listingId) {
                window.location.href = `/productDetail.html?id=${result.listingId}`;
            } else {
                salesForm.reset();
            }
        } else {
            messageDiv.textContent = result.error || "알 수 없는 오류가 발생했습니다.";
            messageDiv.style.color = "red";
        }
    } catch (error) {
        console.error('Error submitting form:', error);
        messageDiv.textContent = "등록에 실패했습니다. 네트워크 연결을 확인해주세요.";
        messageDiv.style.color = "red";
    }
});