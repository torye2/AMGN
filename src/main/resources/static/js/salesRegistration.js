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

document.addEventListener("DOMContentLoaded", async () => {
  const region1 = document.getElementById("region1");
  const region2 = document.getElementById("region2");
  const region3 = document.getElementById("region3");
  const regionHidden = document.getElementById("regionId");

  try {
    const rres = await fetch("/api/regions");
    if (!rres.ok) throw new Error("지역 목록을 불러오지 못했습니다.");
    const regions = await rres.json();

    // regionMap 예시 구조:
    // { "서울특별시": { "강남구": { "개포동": 123, ... , _id: 45(강남구 자체 id) }, ... } }
    const regionMap = {};

    regions.forEach(r => {
      const parts = (r.path || "").split("/").map(s => s.trim()).filter(Boolean);
      if (parts.length === 1) {
        if (!regionMap[parts[0]]) regionMap[parts[0]] = {};
      } else if (parts.length === 2) {
        if (!regionMap[parts[0]]) regionMap[parts[0]] = {};
        if (!regionMap[parts[0]][parts[1]]) regionMap[parts[0]][parts[1]] = {};
        // 시·군·구 자체 선택 시 사용할 ID 보관
        regionMap[parts[0]][parts[1]]._id = r.regionId;
      } else if (parts.length === 3) {
        if (!regionMap[parts[0]]) regionMap[parts[0]] = {};
        if (!regionMap[parts[0]][parts[1]]) regionMap[parts[0]][parts[1]] = {};
        regionMap[parts[0]][parts[1]][parts[2]] = r.regionId;
      }
    });

    // 시/도 채우기
    Object.keys(regionMap)
      .sort((a, b) => a.localeCompare(b, "ko"))
      .forEach(p => {
        const opt = document.createElement("option");
        opt.value = opt.textContent = p;
        region1.appendChild(opt);
      });

    // 시/도 선택
    region1.addEventListener("change", () => {
      region2.innerHTML = `<option value="">-- 시·군·구 --</option>`;
      region3.innerHTML = `<option value="">-- 읍·면·동 --</option>`;
      region2.disabled = true;
      region3.disabled = true;
      region3.required = false;
      regionHidden.value = "";

      if (region1.value) {
        Object.keys(regionMap[region1.value])
          .sort((a, b) => a.localeCompare(b, "ko"))
          .forEach(c => {
            const opt = document.createElement("option");
            opt.value = opt.textContent = c;
            region2.appendChild(opt);
          });
        region2.disabled = false;
      }
    });

    // 시·군·구 선택
    region2.addEventListener("change", () => {
      region3.innerHTML = `<option value="">-- 읍·면·동 --</option>`;
      region3.disabled = true;
      region3.required = false;
      regionHidden.value = "";

      if (region2.value) {
        const sub = regionMap[region1.value][region2.value];
        const subKeys = Object.keys(sub).filter(k => k !== "_id");

        if (subKeys.length === 0) {
          // 하위 읍·면·동 없는 경우 → 시·군·구 자체 regionId 사용
          regionHidden.value = sub._id || "";
        } else {
          subKeys
            .sort((a, b) => a.localeCompare(b, "ko"))
            .forEach(s => {
              const opt = document.createElement("option");
              opt.value = opt.textContent = s;
              region3.appendChild(opt);
            });
          region3.disabled = false;
          region3.required = true;
        }
      }
    });

    // 읍·면·동 선택
    region3.addEventListener("change", () => {
      regionHidden.value = "";
      if (region3.value) {
        const id = regionMap[region1.value][region2.value][region3.value];
        regionHidden.value = id;
      }
    });
  } catch (err) {
    console.error(err);
    // 필요 시 사용자에게 안내 메시지 표시
    // messageDiv가 있다면:
    if (typeof messageDiv !== "undefined" && messageDiv) {
      messageDiv.textContent = "지역 목록을 불러오지 못했습니다. 잠시 후 다시 시도하세요.";
      messageDiv.style.color = "red";
    }
  }
});


document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("salesForm");
    const fileInput = form.querySelector("input[name='productPhotos']");
    const messageBox = document.getElementById("message");

    const MAX_FILE_SIZE = 10 * 1024 * 1024; // 개별 파일 10MB
    const MAX_TOTAL_SIZE = 300 * 1024 * 1024; // 전체 파일 합계 30MB

    const checkFiles = (files) => {
        const oversizedFiles = [];
        let totalSize = 0;

        for (const file of files) {
            totalSize += file.size;
            if (file.size > MAX_FILE_SIZE) {
                oversizedFiles.push(`${file.name} (${(file.size / 1024 / 1024).toFixed(2)}MB)`);
            }
        }

        if (oversizedFiles.length > 0) {
            return `❌ 파일 용량이 너무 큽니다 (각 파일 최대 10MB, 전체 파일 최대 300MB):\n- ${oversizedFiles.join("\n- ")}`;
        }

        if (totalSize > MAX_TOTAL_SIZE) {
            return `❌ 전체 파일 용량이 너무 큽니다 (각 파일 최대 10MB, 전체 파일 최대 300MB). 현재 총 ${(totalSize / 1024 / 1024).toFixed(2)}MB`;
        }

        return ""; // 문제 없음
    };

    // 파일 선택 시 즉시 체크
    fileInput.addEventListener("change", (e) => {
        messageBox.textContent = "";
        const errorMsg = checkFiles(e.target.files);
        if (errorMsg) {
            messageBox.textContent = errorMsg;
            messageBox.style.color = "red";
            e.target.value = ""; // 선택 초기화
        }
    });

    // 제출 시에도 체크 (보안용)
    form.addEventListener("submit", (e) => {
        const errorMsg = checkFiles(fileInput.files);
        if (errorMsg) {
            e.preventDefault();
            messageBox.textContent = errorMsg;
            messageBox.style.color = "red";
        }
    });
});
