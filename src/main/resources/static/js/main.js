// /js/main.js
(function () {
  'use strict';

  // =========================
  // DOM 캐시
  // =========================
  const wishBox        = document.getElementById('wishSidebar');
  const wishEmpty      = document.getElementById('wishSidebarEmpty');
  const wishGuest      = document.getElementById('wishSidebarGuest');

  const topCard        = document.getElementById('topSellersCard');
  const topList        = document.getElementById('topSellers');
  const topLoading     = document.getElementById('topSellersLoading');
  const topEmpty       = document.getElementById('topSellersEmpty');

  // 지역 모달
  const regionModalEl  = document.getElementById('regionModal');
  const regionSearch   = document.getElementById('regionSearchInput');
  const regionList     = document.getElementById('regionList');
  const regionApplyBtn = document.getElementById('applyRegionBtn');

  // Bootstrap Modal 인스턴스 (있을 때만)
  const BSModal = (window.bootstrap && window.bootstrap.Modal) ? window.bootstrap.Modal : null;

  // =========================
  // 초기 실행
  // =========================
  document.addEventListener('DOMContentLoaded', () => {
    // 최근 찜 사이드바
    if (wishBox) loadRecentWishes(6);

    // 이달의 판매왕
    if (topList && topLoading && topEmpty) loadTopSellers();

    // 지역 모달
    if (regionModalEl && regionSearch && regionList && regionApplyBtn) {
      setupRegionModal();
    }

    // 전역에서 열 수 있게 helper 노출 (원하면 헤더에서 호출)
    window.openRegionModal = () => {
      if (!BSModal || !regionModalEl) return;
      new BSModal(regionModalEl).show();
    };
  });

  // =========================
  // 최근 찜 사이드바
  // =========================
  async function loadRecentWishes(limit = 6) {
    if (!wishBox) return;

    wishBox.innerHTML = '<div class="text-muted small">불러오는 중...</div>';
    if (wishEmpty) wishEmpty.style.display = 'none';
    if (wishGuest) wishGuest.style.display = 'none';

    try {
      const res = await fetch('/product/wish/my', { credentials: 'include', cache: 'no-store' });
      if (res.status === 401) {
        // 비로그인
        wishBox.innerHTML = '';
        if (wishGuest) wishGuest.style.display = 'block';
        return;
      }
      if (!res.ok) throw new Error('찜 목록을 불러오지 못했습니다.');
      const data = await res.json();
      const ids = Array.isArray(data.listingIds) ? data.listingIds.slice(0, limit) : [];

      if (ids.length === 0) {
        wishBox.innerHTML = '';
        if (wishEmpty) wishEmpty.style.display = 'block';
        return;
      }

      // 상세 병렬 로드
      const itemPromises = ids.map(id =>
        fetch(`/product/${encodeURIComponent(id)}`, { credentials: 'include', cache: 'no-store' })
          .then(r => r.ok ? r.json() : null)
          .catch(() => null)
      );
      const items = (await Promise.all(itemPromises)).filter(Boolean);

      // 렌더
      wishBox.innerHTML = '';
      items.forEach(p => {
        const raw = (Array.isArray(p.photoUrls) && p.photoUrls.length > 0) ? p.photoUrls[0] : p.photoUrl;
        const imgUrl = normalizeImg(raw) || 'https://placehold.co/64x64?text=No+Image';

        const a = document.createElement('a');
        a.href = `/productDetail.html?id=${p.listingId}`;
        a.className = 'wish-item';
        a.innerHTML = `
          <img src="${imgUrl}" alt="${escapeHtml(p.title ?? '')}" />
          <div class="title">${escapeHtml(p.title ?? '')}</div>
        `;
        wishBox.appendChild(a);
      });
    } catch (e) {
      console.error('최근 찜 불러오기 실패:', e);
      wishBox.innerHTML = '<div class="text-muted small">최근 찜을 불러오지 못했습니다.</div>';
    }
  }

  // =========================
  // TOP Sellers
  // =========================
  function loadTopSellers() {
    fetch('/awards/top-sellers?metric=units&limit=3', {
      headers: { 'Accept': 'application/json' },
      credentials: 'same-origin',
      cache: 'no-store'
    })
    .then(async (res) => {
      if (res.status === 204) return [];
      const ct = res.headers.get('content-type') || '';
      const text = await res.text();
      if (!res.ok) throw new Error(text || '요청 실패');
      if (!text) return [];
      if (!ct.includes('application/json')) throw new Error('JSON이 아닌 응답입니다.');
      return JSON.parse(text);
    })
    .then(rows => {
      if (topLoading) topLoading.style.display = 'none';
      if (!rows || rows.length === 0) {
        if (topEmpty) topEmpty.classList.remove('d-none');
        return;
      }

      const medalClass = ['gold','silver','bronze'];
      const medalText  = ['1','2','3'];

      topList.innerHTML = '';
      rows.slice(0,3).forEach((r, idx) => {
        const col = document.createElement('div');
        col.className = 'col-12 col-md-4';

        const name = r.sellerName ?? `판매자 #${r.sellerId}`;
        const units = Number(r.value ?? 0);
        const avatarUrl = r.avatarUrl || 'https://placehold.co/88x88?text=%F0%9F%8C%9F';

        const url = new URL('/shop.html', window.location.origin);
        url.searchParams.set('sellerId', String(r.sellerId));

        col.innerHTML = `
          <a href="${url.toString()}" class="text-decoration-none text-reset">
            <div class="leader-item">
              <div class="leader-medal ${medalClass[idx] || 'gold'}">${medalText[idx] || (idx+1)}</div>
              <img class="leader-avatar" src="${avatarUrl}" alt="${escapeHtml(name)}">
              <div class="flex-grow-1">
                <div class="leader-name">${escapeHtml(name)}</div>
                <div class="leader-stats">이번 달 판매수량 ${units.toLocaleString()}개</div>
              </div>
            </div>
          </a>
        `;
        topList.appendChild(col);
      });

      if (topCard) topCard.style.display = 'block';
    })
    .catch(err => {
      console.error(err);
      if (topLoading) topLoading.style.display = 'none';
      if (topEmpty) {
        topEmpty.classList.remove('d-none');
        topEmpty.classList.replace('alert-light', 'alert-warning');
        topEmpty.textContent = err.message || '리더보드를 불러오지 못했습니다.';
      }
    });
  }

  // =========================
  // 지역 선택 모달
  // =========================
  function setupRegionModal() {
    let selected = null;  // { regionId, name, path }

    // 기존 선택값 UI 반영
    const savedId    = localStorage.getItem('selectedRegionId');
    const savedLabel = localStorage.getItem('selectedRegionLabel') || localStorage.getItem('selectedRegion');

    // 모달 열릴 때 기본 목록/포커스
    regionModalEl.addEventListener('shown.bs.modal', async () => {
      regionSearch.value = savedLabel || '';
      regionSearch.focus();
      await searchAndRender(regionSearch.value.trim());
    });

    // 검색 입력 디바운스
    regionSearch.addEventListener('input', debounce(async (e) => {
      const q = e.target.value.trim();
      await searchAndRender(q);
    }, 250));

    // 항목 클릭 선택
    regionList.addEventListener('click', (e) => {
      const item = e.target.closest('.list-group-item');
      if (!item) return;
      [...regionList.children].forEach(li => li.classList.remove('active'));
      item.classList.add('active');

      selected = {
        regionId: item.dataset.id ? Number(item.dataset.id) : null,
        name: item.dataset.name || '',
        path: item.dataset.path || ''
      };
    });

    // 적용 버튼
    regionApplyBtn.addEventListener('click', async () => {
      // 선택 없으면 첫 항목/검색어로 자동 선택 시도
      if (!selected) {
        const first = regionList.querySelector('.list-group-item');
        if (first) {
          selected = {
            regionId: first.dataset.id ? Number(first.dataset.id) : null,
            name: first.dataset.name || '',
            path: first.dataset.path || ''
          };
        }
      }
      if (selected?.regionId) {
        localStorage.setItem('selectedRegionId', String(selected.regionId));
        localStorage.setItem('selectedRegionLabel', selected.path || selected.name || '');
      } else {
        // id가 없으면 라벨만 저장 (향후 역매핑 가능)
        const label = regionSearch.value.trim();
        localStorage.setItem('selectedRegionId', '');
        localStorage.setItem('selectedRegionLabel', label);
      }

      // 다른 스크립트에서 감지할 수 있게 커스텀 이벤트 발행
      window.dispatchEvent(new CustomEvent('region:changed', {
        detail: {
          regionId: localStorage.getItem('selectedRegionId'),
          label: localStorage.getItem('selectedRegionLabel')
        }
      }));
    });

    // 초기 선택 표시
    if (savedId || savedLabel) {
      // 모달 열리면 반영되므로 여기선 패스
    }

    // 내부 함수: 검색 및 렌더
    async function searchAndRender(q) {
      regionList.innerHTML = '<div class="list-group-item text-muted">불러오는 중...</div>';
      selected = null;
      try {
        let rows = [];
        if (q) {
          // 백엔드: /api/suggest?q=키워드&limit=20  (Region suggest)
          const url = `/api/suggest?q=${encodeURIComponent(q)}&limit=20`;
          const res = await fetch(url, { cache: 'no-store' });
          if (res.ok) {
            rows = await res.json();
          }
        } else {
          // 키워드 없으면 인기/최하위 지역 추천이 있으면 좋으나, 없으면 빈 목록
          rows = [];
        }

        renderRegionList(rows);
      } catch (e) {
        console.error('region suggest error:', e);
        regionList.innerHTML = '<div class="list-group-item text-danger">검색 중 오류가 발생했습니다.</div>';
      }
    }

    function renderRegionList(rows) {
      regionList.innerHTML = '';
      const arr = Array.isArray(rows) ? rows : [];
      if (arr.length === 0) {
        regionList.innerHTML = '<div class="list-group-item text-muted">검색 결과가 없습니다.</div>';
        return;
      }
      arr.forEach(r => {
        const id   = r.regionId ?? r.region_id ?? r.id;
        const name = r.name ?? '';
        const path = r.path ?? name;

        const a = document.createElement('a');
        a.href = 'javascript:void(0)';
        a.className = 'list-group-item list-group-item-action';
        a.dataset.id = id != null ? String(id) : '';
        a.dataset.name = name || '';
        a.dataset.path = path || '';

        a.innerHTML = `
          <div class="d-flex w-100 justify-content-between">
            <h6 class="mb-1">${escapeHtml(path || name || '')}</h6>
          </div>
          <small class="text-muted">${id ? ('ID: ' + id) : ''}</small>
        `;
        regionList.appendChild(a);
      });

      // 저장된 id/라벨이 있으면 선택 표시
      const wantId = (localStorage.getItem('selectedRegionId') || '').trim();
      const wantLabel = (localStorage.getItem('selectedRegionLabel') || '').trim();
      let matched = null;

      if (wantId && /^\d+$/.test(wantId)) {
        matched = [...regionList.children].find(li => li.dataset.id === wantId);
      }
      if (!matched && wantLabel) {
        matched = [...regionList.children].find(li => (li.dataset.path || li.dataset.name || '') === wantLabel);
      }
      if (matched) matched.classList.add('active');
    }
  }

  // =========================
  // 유틸
  // =========================
  function debounce(fn, ms) {
    let t = null;
    return function (...args) {
      clearTimeout(t);
      t = setTimeout(() => fn.apply(this, args), ms);
    };
  }

  function normalizeImg(u) {
    if (!u) return '/images/placeholder.png';
    if (u.startsWith('http://') || u.startsWith('https://')) return u;
    if (u.startsWith('/uploads')) return u;
    return `/uploads/${u}`.replace('/uploads//uploads', '/uploads');
  }

  function escapeHtml(s) {
    return String(s ?? '')
      .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
      .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
  }
})();
