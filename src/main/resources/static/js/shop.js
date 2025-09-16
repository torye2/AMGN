(function () {
  // sellerId 파라미터 파싱: seller_Id, sellerId, seller_id 모두 허용
  const params = new URLSearchParams(window.location.search);
  const sellerId =
    params.get('seller_Id') ||
    params.get('sellerId') ||
    params.get('seller_id');

  if (!sellerId) {
    console.warn('sellerId가 URL에 없습니다. 예: /shop.html?seller_Id=123');
    return;
  }

  // 후보 소유자 식별자들(문자/숫자 모두 문자열로 저장)
  const shopOwnerCandidates = new Set([String(sellerId)]);

  fetch(`/api/shop/${encodeURIComponent(sellerId)}`)
    .then((res) => {
      if (!res.ok) throw new Error('판매자 정보를 불러오지 못했습니다.');
      return res.json();
    })
    .then((data) => {
      // 닉네임/타이틀: userName 사용
      const nicknameEl = document.querySelector('.shop-nickname');
      const titleEl = document.querySelector('.shop-title');
      if (nicknameEl) nicknameEl.textContent = data.userName ?? '';
      if (titleEl) titleEl.textContent = data.userName ?? '';

      // 소유자 후보 수집
      const addCard = (v) => {
        if (v !== undefined && v !== null && String(v).trim() !== '') {
          shopOwnerCandidates.add(String(v));
        }
      };
      ['loginId', 'userId', 'ownerLoginId', 'username', 'userName'].forEach((k) => addCard(data[k]));
      ['id', 'sellerId', 'ownerId', 'userPk', 'userNo'].forEach((k) => addCard(data[k]));

      // 상품 개수
      const miniStatsEl = document.querySelector('.mini-stats');
      if (miniStatsEl) {
        const productCount = typeof data.productCount === 'number' ? data.productCount : 0;
        // 팔로워는 요구사항에 명시되지 않아 일단 표시만 유지(0으로 표기)
        miniStatsEl.textContent = `상품 ${productCount} | 팔로워 0`;
      }

      // 상점오픈 N일 전
      const openMeta = document.querySelector('.meta .meta-item:nth-child(1)');
      if (openMeta) {
        const days = typeof data.daysSinceOpen === 'number' ? data.daysSinceOpen : null;
        if (days !== null) {
          openMeta.innerHTML = `<span class="dot"></span> 상점오픈 ${days}일 전`;
        } else if (data.createdAt) {
          openMeta.innerHTML = `<span class="dot"></span> 상점오픈일 ${data.createdAt}`;
        }
      }

      // 상품판매 N회
      const salesMeta = document.querySelector('.meta .meta-item:nth-child(2)');
      if (salesMeta) {
        const sold = typeof data.soldCount === 'number'
          ? data.soldCount
          : (typeof data.sold_count === 'number' ? data.sold_count : 0);
        salesMeta.innerHTML = `<span class="dot"></span> 상품판매 ${sold}회`;
      }

      // 소개
      const descEl = document.querySelector('.shop-desc');
      if (descEl) {
        const intro = (data.intro ?? '').trim();
        descEl.textContent = intro.length ? intro : '소개가 없습니다.';
      }

      // 아바타 이미지 표시(있을 경우)
      try {
        const avatarBox = document.querySelector('.avatar');
        if (avatarBox) {
          const raw =
            data.profileImg ||
            data.profile_img ||
            (data.user_profile && data.user_profile.profile_img) ||
            data.photoUrl ||
            data.avatarUrl ||
            data.storeImageUrl ||
            data.profileImage ||
            data.profile_image;
          if (raw) {
            let img = avatarBox.querySelector('img.shop-avatar');
            if (!img) {
              img = document.createElement('img');
              img.className = 'shop-avatar';
              img.alt = '상점 이미지';
              avatarBox.appendChild(img);
            }
            // 선언 전 함수 호출을 피하기 위해 인라인 URL 계산
            const s = String(raw);
            const url = s.startsWith('http') ? s : (s.startsWith('/uploads') ? s : `/uploads/${s}`);
            img.src = url;

            const icon = avatarBox.querySelector('.store-icon');
            if (icon) icon.style.display = 'none';
          }
        }
      } catch (e) {
        console.warn('아바타 표시 중 오류:', e);
      }
    })
    .catch((err) => {
      console.error(err);
    });

    // --- 상점 주인에게만 보이는 '상점 수정' 버튼 및 편집 UI ---
    fetch('/api/user/status', { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error('status failed'))))
      .then((me) => {
        // URL의 sellerId와 로그인 사용자 user_id(또는 userId/id)가 정확히 일치해야만 노출
        const uid = (me && (me.user_id ?? me.userId ?? me.id)) ?? null;
        if (!(me && me.isLoggedIn && uid != null && String(uid) === String(sellerId))) return;

        const profileRight = document.querySelector('.profile-right');
        const topRow = document.querySelector('.top-row');
        const host = topRow || profileRight;
        if (!host || host.querySelector('.owner-actions')) return;

        // 액션 컨테이너
        const ownerActions = document.createElement('div');
        ownerActions.className = 'owner-actions';
        host.appendChild(ownerActions);

        // 수정 버튼
        const editBtn = document.createElement('button');
        editBtn.type = 'button';
        editBtn.className = 'edit-shop-btn';
        editBtn.textContent = '수정하기';
        ownerActions.appendChild(editBtn);

        // 저장/취소 버튼은 편집 모드에서만 노출
        const saveBtn = document.createElement('button');
        saveBtn.type = 'button';
        saveBtn.className = 'save-btn';
        saveBtn.textContent = '저장';
        saveBtn.style.display = 'none';

        const cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.className = 'cancel-btn';
        cancelBtn.textContent = '취소';
        cancelBtn.style.display = 'none';

        ownerActions.appendChild(saveBtn);
        ownerActions.appendChild(cancelBtn);

        // 편집 상태 관리
        let editing = false;
        let textarea = null;
        let fileInput = null;
        let originalIntro = '';
        let originalImgSrc = '';

        function getDescEl() {
          return document.querySelector('.shop-desc');
        }
        const avatarBox = document.querySelector('.avatar');

        function ensureAvatarImg() {
          if (!avatarBox) return null;
          let img = avatarBox.querySelector('img.shop-avatar');
          if (!img) {
            img = document.createElement('img');
            img.className = 'shop-avatar';
            img.alt = '상점 이미지';
            avatarBox.appendChild(img);
          }
          const icon = avatarBox.querySelector('.store-icon');
          if (icon) icon.style.display = 'none';
          return img;
        }

        function enterEdit() {
          if (editing) return;
          editing = true;

          // 소개 -> textarea로 전환 (동적으로 현재 DOM에서 조회)
          const descNode = getDescEl();
          const currentIntro = (descNode?.textContent || '').trim();
          originalIntro = currentIntro === '소개가 없습니다.' ? '' : currentIntro;

          textarea = document.createElement('textarea');
          textarea.className = 'intro-edit';
          textarea.value = originalIntro;
          if (descNode && descNode.parentNode) {
            descNode.replaceWith(textarea);
          } else {
            // 소개 노드가 없다면 프로필 오른쪽 영역에 추가
            profileRight.insertAdjacentElement('beforeend', textarea);
          }

          // 파일 입력 및 미리보기 준비
          if (avatarBox && !fileInput) {
            fileInput = document.createElement('input');
            fileInput.type = 'file';
            fileInput.accept = 'image/*';
            fileInput.className = 'avatar-input';
            avatarBox.appendChild(fileInput);

            const img = ensureAvatarImg();
            if (img) {
              originalImgSrc = img.src || '';
              fileInput.addEventListener('change', () => {
                if (fileInput.files && fileInput.files[0]) {
                  const url = URL.createObjectURL(fileInput.files[0]);
                  img.src = url;
                } else {
                  img.src = originalImgSrc || img.src;
                }
              });
            }
          }

          // 버튼 표시 전환
          editBtn.style.display = 'none';
          saveBtn.style.display = '';
          cancelBtn.style.display = '';
        }

        function exitEdit(updated = null) {
          if (!editing) return;
          editing = false;

          // textarea -> .shop-desc 복귀
          if (textarea) {
            const p = document.createElement('p');
            p.className = 'shop-desc';
            const text = updated?.intro != null ? String(updated.intro).trim() : originalIntro;
            p.textContent = text.length ? text : '소개가 없습니다.';
            textarea.replaceWith(p);
            textarea = null;
          }

          // 파일 입력 제거 및 이미지 복원/갱신
          if (avatarBox && fileInput) {
            fileInput.remove();
            fileInput = null;
          }
          const img = avatarBox ? avatarBox.querySelector('img.shop-avatar') : null;
          if (img) {
            const newRaw =
              updated?.profileImg ||
              updated?.profile_img ||
              updated?.user_profile?.profile_img ||
              updated?.photoUrl ||
              updated?.avatarUrl ||
              updated?.storeImageUrl ||
              updated?.profileImage ||
              updated?.profile_image;
            if (newRaw) {
              const url = String(newRaw).startsWith('http') ? newRaw : toImgUrl(newRaw);
              img.src = url;
            } else if (!updated && originalImgSrc) {
              img.src = originalImgSrc;
            }
          }

          // 버튼 표시 전환
          editBtn.style.display = '';
          saveBtn.style.display = 'none';
          cancelBtn.style.display = 'none';
        }

        editBtn.addEventListener('click', enterEdit);

        cancelBtn.addEventListener('click', () => {
          exitEdit(null);
        });

        saveBtn.addEventListener('click', async () => {
          if (!editing) return;
          try {
            saveBtn.disabled = true;
            cancelBtn.disabled = true;
            saveBtn.textContent = '저장 중...';

            const introVal = textarea ? textarea.value.trim() : '';

            // CSRF 헤더 추가 (존재 시)
            const csrfHeaderName = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const headers = {};
            if (csrfHeaderName && csrfToken) headers[csrfHeaderName] = csrfToken;

            // 멀티파트 FormData 구성 (서버 @RequestPart 이름: intro, profile_img)
            const fd = new FormData();
            fd.append('intro', introVal);
            if (fileInput && fileInput.files && fileInput.files[0]) {
              fd.append('profile_img', fileInput.files[0]);
            }

            // 상점 API: PUT + multipart/form-data
            const res = await fetch(`/api/shop/${encodeURIComponent(sellerId)}`, {
              method: 'PUT',
              body: fd,
              headers,
              credentials: 'include',
            });

            if (!res.ok) {
              const text = await res.text().catch(() => '');
              console.error('Shop update failed:', res.status, text);
              alert(`저장 실패 (${res.status}).\n${text?.slice(0,300) || ''}`);
              return;
            }

            const updated = await res.json().catch(() => ({}));
            exitEdit({
              intro: updated.intro ?? updated.profileIntro ?? introVal,
              photoUrl:
                updated.profileImg ||
                updated.profile_img ||
                updated.user_profile?.profile_img ||
                updated.photoUrl ||
                updated.avatarUrl ||
                updated.storeImageUrl ||
                updated.profileImage ||
                updated.profile_image,
            });
          } catch (e) {
            console.error('Save error:', e);
            alert(e?.message || '저장 중 오류가 발생했습니다.');
          } finally {
            saveBtn.disabled = false;
            cancelBtn.disabled = false;
            saveBtn.textContent = '저장';
          }
        });
      })
      .catch(() => { /* 상태 확인 실패 시 무시 */ });

  // 중복 편집 링크 주입 제거 (report-btn은 신고 전용)

  // --- 상품 목록 렌더링 ---
  const productsAll = document.getElementById('productsAll');
  if (!productsAll) return;

  // 초기 마크업 구성
  productsAll.innerHTML = `
    <div class="products-header">
      <span class="products-title">상품</span>
      <div class="products-sorts">
        <button type="button" data-sort="latest" class="sort-btn active">최신순</button>
        <button type="button" data-sort="low">저가순</button>
        <button type="button" data-sort="high">고가순</button>
      </div>
    </div>
    <div id="sellerProductsGrid" class="products-grid"></div>
  `;

  const grid = document.getElementById('sellerProductsGrid');

  const toImgUrl = (raw) => {
    if (!raw) return 'https://placehold.co/300x200?text=No+Image';
    const s = String(raw);
    return s.startsWith('/uploads') ? s : `/uploads/${s}`;
  };

  const formatPrice = (v) =>
    typeof v === 'number' ? `${Number(v).toLocaleString()} 원` : '';

  let originalList = [];

  const renderList = (list) => {
    if (!Array.isArray(list) || list.length === 0) {
      grid.innerHTML = '<p class="store-empty">등록된 상품이 없습니다.</p>';
      return;
    }
    grid.innerHTML = list
      .map((p) => {
        const img = Array.isArray(p.photoUrls) && p.photoUrls.length ? toImgUrl(p.photoUrls[0]) : toImgUrl(null);
        const price = formatPrice(p.price);
        const title = (p.title ?? '').trim();
        return `
          <a class="product-card" href="/productDetail.html?id=${encodeURIComponent(p.listingId)}" title="${title}">
            <div class="thumb-wrap">
              <img src="${img}" alt="${title}">
            </div>
            <div class="product-info">
              <div class="product-title">${title}</div>
              <div class="product-price">${price}</div>
            </div>
          </a>
        `;
      })
      .join('');
  };

  const applySort = (type) => {
    const list = [...originalList];
    switch (type) {
      case 'low':
        list.sort((a, b) => (a.price ?? 0) - (b.price ?? 0));
        break;
      case 'high':
        list.sort((a, b) => (b.price ?? 0) - (a.price ?? 0));
        break;
      case 'latest':
      default:
        // 최신순: createdAt 또는 listingId 기준 내림차순
        list.sort((a, b) => {
          const aKey = a.createdAt ? new Date(a.createdAt).getTime() : (a.listingId ?? 0);
          const bKey = b.createdAt ? new Date(b.createdAt).getTime() : (b.listingId ?? 0);
          return bKey - aKey;
        });
        break;
    }
    renderList(list);
  };

  // 정렬 버튼 이벤트
  productsAll.addEventListener('click', (e) => {
    const btn = e.target.closest('button[data-sort]');
    if (!btn) return;
    productsAll.querySelectorAll('.products-sorts .sort-btn, .products-sorts button')
      .forEach((b) => b.classList.remove('active'));
    btn.classList.add('active');
    applySort(btn.dataset.sort);
  });

  // 판매자 상품 가져오기
  fetch(`/product/seller/${encodeURIComponent(sellerId)}/products`)
    .then((res) => {
      if (!res.ok) throw new Error('판매자 상품 조회 실패');
      return res.json();
    })
    .then((list) => {
      originalList = Array.isArray(list) ? list : [];
      applySort('latest');
    })
    .catch((err) => {
      console.error('판매자 상품 불러오기 실패:', err);
      grid.innerHTML = '<p class="store-empty">상품을 불러오는 중 오류가 발생했습니다.</p>';
    });
})();
