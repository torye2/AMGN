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
    .then(async (data) => {
      // 닉네임/타이틀: nickName → nickname → userName 순으로 사용, 없으면 닉네임 API 폴백
      const nicknameEl = document.querySelector('.shop-nickname');
      const titleEl = document.querySelector('.shop-title');

      let displayName =
        data?.nickName ??
        data?.nickname ??
        data?.userName ??
        data?.username ??
        data?.name ??
        '';

      if (!displayName || String(displayName).trim() === '') {
        try {
          const r = await fetch(`/api/user/nickname/${encodeURIComponent(sellerId)}`);
          if (r.ok) {
            const j = await r.json();
            displayName = j?.nickname ?? displayName;
          }
        } catch (_) { /* 폴백 실패시 무시 */ }
      }

      if (nicknameEl) nicknameEl.textContent = displayName || '';
      if (titleEl) titleEl.textContent = displayName || '';

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
        // 기존 팔로워 표시값을 보존하여, 이후 DB 기준 갱신(fetchFollowerCount)과 충돌하지 않도록 함
        const existingText = miniStatsEl.textContent || '';
        const followerMatch = existingText.match(/팔로워\s*(\d+)/);
        const followerCount = followerMatch ? followerMatch[1] : '0';
        miniStatsEl.textContent = `상품 ${productCount} | 팔로워 ${followerCount}`;
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
            if (!getCookie('XSRF-TOKEN')) await ensureCsrf();
            const xsrf = getCookie('XSRF-TOKEN');

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
                headers: { 'Accept': 'application/json', 'X-XSRF-TOKEN': xsrf },
              credentials: 'include'
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

  // 초기 마크업 구성 (탭 아래에 정렬 바 배치)
  productsAll.innerHTML = `
    <div class="products-header" style="display:block;">
      <nav id="shopTopTabs" class="shop-tabs" role="tablist" aria-label="상점 섹션">
        <button class="tab-item active" role="tab" aria-selected="true" data-tab="products">
          상품 <span class="count" id="tabCountProducts">0</span>
        </button>
        <button class="tab-item" role="tab" aria-selected="false" data-tab="reviews">
          상점후기 <span class="count" id="tabCountReviews">0</span>
        </button>
        <button class="tab-item" role="tab" aria-selected="false" data-tab="following">
          팔로잉 <span class="count" id="tabCountFollowing">0</span>
        </button>
        <button class="tab-item" role="tab" aria-selected="false" data-tab="followers">
          팔로워 <span class="count" id="tabCountFollowers">0</span>
        </button>
      </nav>
      <div id="productsSortBar" class="products-sorts" style="margin-top:10px;">
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
      // 실제 목록 개수로 탭/미니 통계 갱신
      const cnt = originalList.length;
      const pc = document.getElementById('tabCountProducts');
      if (pc) pc.textContent = String(cnt);
      const mini = document.querySelector('.mini-stats');
      if (mini) {
        const txt = mini.textContent || '';
        const fm = txt.match(/팔로워\s*(\d+)/);
        const followerCount = fm ? fm[1] : '0';
        mini.textContent = `상품 ${cnt} | 팔로워 ${followerCount}`;
      }
      applySort('latest');
    })
    .catch((err) => {
      console.error('판매자 상품 불러오기 실패:', err);
      grid.innerHTML = '<p class="store-empty">상품을 불러오는 중 오류가 발생했습니다.</p>';
    });
})();
(function () {
    const qs = new URLSearchParams(location.search);
    const sellerId =
        qs.get('seller_Id') ||
        qs.get('sellerId') ||
        qs.get('seller_id');
    if (!sellerId) return;

    const btn = document.querySelector('.follow-btn');

    // 미니 통계의 팔로워 수 갱신 (상품 개수는 보존) + 탭 카운트 동기화
    function updateFollowerCountUI(count) {
        const mini = document.querySelector('.mini-stats');
        if (mini) {
            const text = mini.textContent || '';
            const m = text.match(/상품\s*(\d+)/);
            const productCount = m ? m[1] : '0';
            mini.textContent = `상품 ${productCount} | 팔로워 ${count}`;
        }
        const fc = document.getElementById('tabCountFollowers');
        if (fc) fc.textContent = String(count ?? 0);
    }

    // DB에서 팔로워 수 재조회 (following_id = sellerId)
    async function fetchFollowerCount() {
        try {
            const r = await fetch(`/api/follows/${encodeURIComponent(sellerId)}/count`, { credentials: 'include' });
            if (!r.ok) return;
            const j = await r.json();
            const cnt = (j && (j.count ?? j.data?.count)) ?? 0;
            updateFollowerCountUI(cnt);
        } catch {
            /* 실패 시 표시 유지 */
        }
    }

    // DB에서 팔로잉 수 재조회 (follower_id = sellerId)
    async function fetchFollowingCount() {
        try {
            const r = await fetch(`/api/follows/${encodeURIComponent(sellerId)}/following/count`, { credentials: 'include' });
            if (!r.ok) return;
            const j = await r.json();
            const cnt = (j && (j.count ?? j.data?.count)) ?? 0;
            const el = document.getElementById('tabCountFollowing');
            if (el) el.textContent = String(cnt);
        } catch {
            /* 실패 시 표시 유지 */
        }
    }

    // 초기 카운트 로드 (DB 기준)
    fetchFollowerCount();
    fetchFollowingCount();
    // 리뷰 카운트 선반영 (탭 클릭 전에도 표시)
    fetch(`/api/reviews/seller/${encodeURIComponent(sellerId)}`, { credentials: 'include' })
      .then(r => r.ok ? r.json() : Promise.resolve([]))
      .then(list => {
        const n = Array.isArray(list) ? list.length
                : (Array.isArray(list?.data) ? list.data.length : 0);
        const rc = document.getElementById('tabCountReviews');
        if (rc) rc.textContent = String(n);
        const rtc = document.getElementById('reviewsTitleCount');
        if (rtc) rtc.textContent = String(n);
      })
      .catch(() => {});

    // 탭 전환 로직
    // 팔로워 탭 로딩
    let _followersLoaded = false;
    async function loadFollowers() {
        const grid = document.getElementById('followersGrid');
        const empty = document.getElementById('followersEmpty');
        const titleCnt = document.getElementById('followersTitleCount');
        if (!grid) return;

        grid.innerHTML = '';
        empty.style.display = 'none';

        try {
            const r = await fetch(`/api/follows/${encodeURIComponent(sellerId)}/followers`, { credentials: 'include' });
            if (!r.ok) throw new Error('팔로워 조회 실패');
            const j = await r.json();
            const ids = Array.isArray(j.ids) ? j.ids : (Array.isArray(j.data?.ids) ? j.data.ids : []);
            titleCnt && (titleCnt.textContent = String(ids.length ?? 0));

            if (!ids.length) {
                empty.style.display = 'block';
                return;
            }

            // 각 팔로워의 프로필/카운트 조회
            const cards = await Promise.all(ids.map(async (uid) => {
                // 사용자 기본 정보 (닉네임/상점명/상품수/아바타)
                let userName = String(uid);
                let productCount = 0;
                let avatarUrl = null;

                try {
                    const s = await fetch(`/api/shop/${encodeURIComponent(uid)}`, { credentials: 'include' });
                    if (s.ok) {
                        const info = await s.json();
                        userName = info.userName || info.username || userName;
                        productCount = typeof info.productCount === 'number' ? info.productCount : 0;

                        const raw =
                          info.profileImg || info.profile_img ||
                          info.user_profile?.profile_img || info.photoUrl ||
                          info.avatarUrl || info.storeImageUrl ||
                          info.profileImage || info.profile_image;
                        if (raw) {
                            const rr = String(raw);
                            avatarUrl = rr.startsWith('http') ? rr : (rr.startsWith('/uploads') ? rr : `/uploads/${rr}`);
                        }
                    }
                } catch {}

                // 해당 유저의 팔로워 수
                let followers = 0;
                try {
                    const f = await fetch(`/api/follows/${encodeURIComponent(uid)}/count`, { credentials: 'include' });
                    if (f.ok) {
                        const jj = await f.json();
                        followers = (jj && (jj.count ?? jj.data?.count)) ?? 0;
                    }
                } catch {}

                // 카드 DOM (클릭 시 상점 페이지로 이동)
                const a = document.createElement('a');
                a.className = 'follower-card';
                a.href = `/shop.html?sellerId=${encodeURIComponent(uid)}`;
                a.innerHTML = `
                    <div class="follower-avatar">
                        <img alt="avatar" src="${avatarUrl || 'https://placehold.co/64x64?text=%20'}">
                    </div>
                    <div class="follower-body">
                        <div class="follower-name">${userName ?? '-'}</div>
                        <div class="follower-stars">★★★★★</div>
                        <div class="follower-meta">상품${productCount}  |  팔로워${followers}</div>
                    </div>
                `;
                return a;
            }));

            // 렌더
            grid.innerHTML = '';
            cards.forEach(c => grid.appendChild(c));
        } catch (e) {
            grid.innerHTML = `<div class="store-empty">조회 실패: ${e.message}</div>`;
        }
    }

    // 팔로잉 탭 로딩
    let _followingLoaded = false;
    async function loadFollowing() {
        const grid = document.getElementById('followingGrid');
        const empty = document.getElementById('followingEmpty');
        const titleCnt = document.getElementById('followingTitleCount');
        if (!grid) return;

        grid.innerHTML = '';
        empty.style.display = 'none';

        try {
            // sellerId가 팔로우하는 대상 목록
            const r = await fetch(`/api/follows/${encodeURIComponent(sellerId)}/following`, { credentials: 'include' });
            if (!r.ok) throw new Error('팔로잉 조회 실패');
            const j = await r.json();
            const ids = Array.isArray(j.ids) ? j.ids : (Array.isArray(j.data?.ids) ? j.data.ids : []);
            titleCnt && (titleCnt.textContent = String(ids.length ?? 0));

            if (!ids.length) {
                empty.style.display = 'block';
                return;
            }

            const cards = await Promise.all(ids.map(async (uid) => {
                let userName = String(uid);
                let productCount = 0;
                let followers = 0;
                let avatarUrl = null;

                try {
                    const s = await fetch(`/api/shop/${encodeURIComponent(uid)}`, { credentials: 'include' });
                    if (s.ok) {
                        const info = await s.json();
                        userName = info.userName || info.username || userName;
                        productCount = typeof info.productCount === 'number' ? info.productCount : 0;

                        const raw =
                          info.profileImg || info.profile_img ||
                          info.user_profile?.profile_img || info.photoUrl ||
                          info.avatarUrl || info.storeImageUrl ||
                          info.profileImage || info.profile_image;
                        if (raw) {
                            const rr = String(raw);
                            avatarUrl = rr.startsWith('http') ? rr : (rr.startsWith('/uploads') ? rr : `/uploads/${rr}`);
                        }
                    }
                } catch {}

                try {
                    const f = await fetch(`/api/follows/${encodeURIComponent(uid)}/count`, { credentials: 'include' });
                    if (f.ok) {
                        const jj = await f.json();
                        followers = (jj && (jj.count ?? jj.data?.count)) ?? 0;
                    }
                } catch {}

                const a = document.createElement('a');
                a.className = 'follower-card';
                a.href = `/shop.html?sellerId=${encodeURIComponent(uid)}`;
                a.innerHTML = `
                    <div class="follower-avatar">
                        <img alt="avatar" src="${avatarUrl || 'https://placehold.co/64x64?text=%20'}">
                    </div>
                    <div class="follower-body">
                        <div class="follower-name">${userName ?? '-'}</div>
                        <div class="follower-stars">★★★★★</div>
                        <div class="follower-meta">상품${productCount}  |  팔로워${followers}</div>
                    </div>
                `;
                return a;
            }));

            grid.innerHTML = '';
            cards.forEach(c => grid.appendChild(c));
        } catch (e) {
            grid.innerHTML = `<div class="store-empty">조회 실패: ${e.message}</div>`;
        }
    }

    // 상점후기 탭 로딩
    let _reviewsLoaded = false;
    function timeAgo(isoOrDate) {
        const d = typeof isoOrDate === 'string' ? new Date(isoOrDate) : isoOrDate;
        const diff = Math.floor((Date.now() - d.getTime()) / 1000);
        if (diff < 60) return `${diff}초 전`;
        const m = Math.floor(diff / 60);
        if (m < 60) return `${m}분 전`;
        const h = Math.floor(m / 60);
        if (h < 24) return `${h}시간 전`;
        const days = Math.floor(h / 24);
        return `${days}일 전`;
    }
    function renderStars(n) {
        const s = Math.max(0, Math.min(5, Number(n||0)));
        return '★'.repeat(s) + '☆'.repeat(5 - s);
    }
    async function loadShopReviews() {
        const panel = document.getElementById('reviewsPanel');
        if (!panel) return;
        const list = panel.querySelector('.reviews-list');
        if (!list) return;

        list.innerHTML = '';
        list.classList.remove('empty');

        try {
            const r = await fetch(`/api/reviews/seller/${encodeURIComponent(sellerId)}`, { credentials: 'include' });
            if (!r.ok) throw new Error('후기 조회 실패');
            const reviews = await r.json();

            if (!Array.isArray(reviews) || reviews.length === 0) {
                list.classList.add('empty');
                list.innerHTML = '<li>상점 후기가 없습니다.</li>';
                return;
            }

            const items = await Promise.all(reviews.map(async rev => {
                // 후기 작성자(팔로워/구매자) 프로필/닉네임/아바타
                let raterName = String(rev.raterId);
                let avatarUrl = null;
                try {
                    const s = await fetch(`/api/shop/${encodeURIComponent(rev.raterId)}`);
                    if (s.ok) {
                        const info = await s.json();
                        raterName = info.userName || info.username || raterName;
                        const raw =
                          info.profileImg || info.profile_img ||
                          info.user_profile?.profile_img || info.photoUrl ||
                          info.avatarUrl || info.storeImageUrl ||
                          info.profileImage || info.profile_image;
                        if (raw) {
                            const rr = String(raw);
                            avatarUrl = rr.startsWith('http') ? rr : (rr.startsWith('/uploads') ? rr : `/uploads/${rr}`);
                        }
                    }
                } catch {}

                const li = document.createElement('li');
                li.className = 'review-item';
                li.innerHTML = `
                  <div class="review-left">
                    <img alt="avatar" src="${avatarUrl || 'https://placehold.co/48x48?text=%20'}">
                  </div>
                  <div class="review-body">
                    <div class="review-head">
                      <div>
                        <div class="review-user">${raterName}</div>
                        <div class="review-stars">${renderStars(rev.score)}</div>
                      </div>
                      <div class="review-time">${rev.createdAt ? timeAgo(rev.createdAt) : ''}</div>
                    </div>
                    ${rev.listingTitle ? `<div class="review-product">${rev.listingTitle}</div>` : ''}
                    <div class="review-text">${(rev.rvComment || '').replace(/\n/g,'<br>')}</div>
                  </div>
                `;
                return li;
            }));

            items.forEach(el => list.appendChild(el));

            // 카운트 업데이트 (탭/패널 타이틀 모두)
            const n = items.length;
            const rc = document.getElementById('tabCountReviews');
            if (rc) rc.textContent = String(n);
            const rtc = document.getElementById('reviewsTitleCount');
            if (rtc) rtc.textContent = String(n);
        } catch (e) {
            list.classList.add('empty');
            list.innerHTML = `<li style="color:#ef4444;">조회 실패: ${e.message}</li>`;
        }
    }

    function switchShopTab(name) {
        document.querySelectorAll('.shop-tabs .tab-item').forEach(b => {
            const on = b.dataset.tab === name;
            b.classList.toggle('active', on);
            b.setAttribute('aria-selected', on ? 'true' : 'false');
        });

        // 탭 바는 항상 보이게 하고, 콘텐츠만 토글
        const sortBar = document.getElementById('productsSortBar');
        const prodGrid = document.getElementById('sellerProductsGrid');
        const rv = document.getElementById('reviewsPanel');
        const fg = document.getElementById('followingPanel');
        const fr = document.getElementById('followersPanel');

        if (sortBar) sortBar.hidden = (name !== 'products');
        if (prodGrid) prodGrid.hidden = (name !== 'products');
        if (rv) rv.hidden = (name !== 'reviews');
        if (fg) fg.hidden = (name !== 'following');
        if (fr) fr.hidden = (name !== 'followers');

        // 필요 시 데이터 로드
        if (name === 'followers' && !_followersLoaded) {
            _followersLoaded = true;
            loadFollowers();
        }
        if (name === 'following' && !_followingLoaded) {
            _followingLoaded = true;
            loadFollowing();
        }
        if (name === 'reviews' && !_reviewsLoaded) {
            _reviewsLoaded = true;
            loadShopReviews();
        }
    }
    document.addEventListener('click', (e) => {
        const tabBtn = e.target.closest('.shop-tabs .tab-item[data-tab]');
        if (!tabBtn) return;
        switchShopTab(tabBtn.dataset.tab);
    });
    // 초기 탭: 상품
    switchShopTab('products');

    // 내 상점이면 버튼 숨김, 아니면 토글 바인딩
    fetch('/api/user/status', { credentials: 'include' })
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(async (me) => {
            const myId = me && (me.userId ?? me.user_id ?? me.id);
            if (myId != null && String(myId) === String(sellerId)) {
                if (btn) btn.style.display = 'none';
                return;
            }
            if (!btn) return;

            // 나의 팔로우 상태 조회
            let isFollowing = false;
            try {
                const r = await fetch(`/api/follows/${encodeURIComponent(sellerId)}/me`, { credentials: 'include' });
                if (r.ok) {
                    const s = await r.json();
                    isFollowing = !!(s && (s.following ?? s.data?.following));
                }
            } catch { /* 무시 */ }

            function renderButton() {
                btn.textContent = isFollowing ? '언팔로우' : '팔로우';
                btn.setAttribute('aria-label', isFollowing ? '언팔로우' : '팔로우');
                btn.classList.toggle('is-following', isFollowing);
            }
            renderButton();

            btn.addEventListener('click', async () => {
                if (!getCookie('XSRF-TOKEN')) await ensureCsrf();
                const xsrf = getCookie('XSRF-TOKEN');

                try {
                    btn.disabled = true;
                    let res;
                    if (isFollowing) {
                        // 언팔로우
                        res = await fetch(`/api/follows/${encodeURIComponent(sellerId)}`, {
                            method: 'DELETE',
                            headers: { 'Accept': 'application/json', 'X-XSRF-TOKEN': xsrf },
                            credentials: 'include'
                        });
                    } else {
                        // 팔로우
                        res = await fetch(`/api/follows/${encodeURIComponent(sellerId)}`, {
                            method: 'POST',
                            headers: { 'Accept': 'application/json', 'X-XSRF-TOKEN': xsrf },
                            credentials: 'include'
                        });
                    }
                    if (res.status === 401) {
                        alert('로그인이 필요합니다.');
                        location.href = '/login';
                        return;
                    }
                    if (!res.ok) throw new Error('요청 실패');

                    // 항상 DB 기준 최신값 재조회
                    await fetchFollowerCount();

                    isFollowing = !isFollowing; // 토글
                    renderButton();
                } catch (e) {
                    alert(e?.message || '요청 실패');
                } finally {
                    btn.disabled = false;
                }
            });
        })
        .catch(() => { /* 상태 조회 실패 시 무시 */ });
})();

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