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

document.addEventListener("DOMContentLoaded", async () => {
  const res = await fetch("/api/categories");
  const categories = await res.json();

  // path 기준으로 대분류/중분류/소분류 나누기
  const categoryMap = {};
  categories.forEach(cat => {
    const parts = cat.path.split(" > ");
    if (parts.length === 1) {
      if (!categoryMap[parts[0]]) categoryMap[parts[0]] = {};
    } else if (parts.length === 2) {
      if (!categoryMap[parts[0]]) categoryMap[parts[0]] = {};
      if (!categoryMap[parts[0]][parts[1]]) categoryMap[parts[0]][parts[1]] = {};
    } else if (parts.length === 3) {
      if (!categoryMap[parts[0]]) categoryMap[parts[0]] = {};
      if (!categoryMap[parts[0]][parts[1]]) categoryMap[parts[0]][parts[1]] = {};
      categoryMap[parts[0]][parts[1]][parts[2]] = cat.categoryId;
    }
  });

  const category1 = document.getElementById("category1");
  const category2 = document.getElementById("category2");
  const category3 = document.getElementById("category3");
  const hiddenInput = document.getElementById("categoryId");

  // 대분류 채우기
  Object.keys(categoryMap).forEach(c1 => {
    const opt = document.createElement("option");
    opt.value = c1;
    opt.textContent = c1;
    category1.appendChild(opt);
  });

  // 대분류 선택 이벤트
  category1.addEventListener("change", () => {
    category2.innerHTML = `<option value="">-- 중분류 선택 --</option>`;
    category3.innerHTML = `<option value="">-- 소분류 선택 --</option>`;
    category2.disabled = true;
    category3.disabled = true;
    hiddenInput.value = "";

    if (category1.value) {
      Object.keys(categoryMap[category1.value]).forEach(c2 => {
        const opt = document.createElement("option");
        opt.value = c2;
        opt.textContent = c2;
        category2.appendChild(opt);
      });
      category2.disabled = false;
    }
  });

  // 중분류 선택 이벤트
  category2.addEventListener("change", () => {
    category3.innerHTML = `<option value="">-- 소분류 선택 --</option>`;
    category3.disabled = true;
    hiddenInput.value = "";

    if (category2.value) {
      const sub = categoryMap[category1.value][category2.value];
      Object.keys(sub).forEach(c3 => {
        const opt = document.createElement("option");
        opt.value = c3;
        opt.textContent = c3;
        category3.appendChild(opt);
      });

      // 중분류만 있고 소분류 없는 경우 → 중분류의 categoryId 필요
      if (Object.keys(sub).length === 0) {
        // categories에서 parent=category2 찾기
        const match = categories.find(c => c.path === `${category1.value} > ${category2.value}`);
        if (match) hiddenInput.value = match.categoryId;
      } else {
        category3.disabled = false;
      }
    }
  });

  // 소분류 선택 이벤트
  category3.addEventListener("change", () => {
    hiddenInput.value = "";
    if (category3.value) {
      const id = categoryMap[category1.value][category2.value][category3.value];
      hiddenInput.value = id;
    }
  });
});
