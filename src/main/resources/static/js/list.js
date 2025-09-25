// /js/list.js — 카테고리/지역 상위 선택 시 하위까지 포함 + 예쁜 카드 UI (SOLD 제외)
(function () {
  'use strict';

  const API_BASE = '/product';

  // ---- DOM ----
  const gridEl       = document.getElementById('productGrid');
  const emptyEl      = document.getElementById('empty');
  const countEl      = document.getElementById('count');
  const breadcrumbEl = document.getElementById('breadcrumb');
  const sortSelect   = document.getElementById('sortSelect');

  if (!gridEl || !emptyEl || !countEl || !breadcrumbEl || !sortSelect) {
    console.error('필요한 DOM 요소가 없습니다. (productGrid/empty/count/breadcrumb/sortSelect)');
    return;
  }

  // ---- URL 파라미터 ----
  const params     = new URLSearchParams(location.search);
  const categoryId = params.get('categoryId') || params.get('id') || null; // 둘 다 지원
  let   regionId   = params.get('regionId') || null;
  const page       = toInt(params.get('page'), 0);
  const size       = toInt(params.get('size'), 20);

  // ---- 초기 실행 ----
  sortSelect.addEventListener('change', loadProducts);
  loadProducts();

  // =========================
  // Core
  // =========================
  async function loadProducts() {
    try {
      await resolveRegionIdIfNeeded();   // URL에 없으면 localStorage/검색어로 역매핑
      await updateBreadcrumb();          // 상단 경로 표시

      gridEl.innerHTML = '';
      renderSkeleton(8);                 // 스켈레톤 임시 표시
      emptyEl.style.display = 'none';

      let items = [];

      // 1) 통합 엔드포인트 (서버에서 상/하위 확장 처리)
      const qs = new URLSearchParams();
      if (categoryId) qs.set('categoryId', categoryId);
      if (regionId)   qs.set('regionId',   regionId);
      qs.set('page', String(page));
      qs.set('size', String(size));

      let usedCombined = false;
      if (categoryId || regionId) {
        try {
          const url = `${API_BASE}/list?${qs.toString()}`;
          console.debug('[list.js] try combined:', url);
          const res = await fetch(url, { credentials: 'include' });
          if (res.ok) {
            const body = await res.json();
            items = Array.isArray(body?.content) ? body.content
                  : (Array.isArray(body) ? body : []);
            usedCombined = true;

            // 서버가 200인데 결과가 비면 폴백으로 전환 (하위 카테고리 직접 병합)
            if ((items?.length ?? 0) === 0 && categoryId) {
              usedCombined = false;
              console.debug('[list.js] combined returned empty; switching to fallback');
            }
          } else {
            console.warn('[list.js] combined endpoint not OK:', res.status);
          }
        } catch (e) {
          console.warn('[list.js] combined endpoint error -> fallback:', e);
        }
      }

      // 2) 폴백: 클라이언트가 직접 "자식 카테고리 전부" 모아서 병합
      if (!usedCombined) {
        console.debug('[list.js] fallback mode start');
        if (categoryId) {
          // (a) 자신 + 모든 하위 카테고리 id 수집
          const catIds = await getDescendantCategoryIds(String(categoryId)); // 자신 포함
          console.debug('[list.js] fallback categoryIds:', catIds);

          // (b) 각 카테고리 호출 → 병합 + 중복 제거
          const map = new Map(); // listingId 기준 dedupe
          for (const cid of catIds) {
            try {
              const res = await fetch(`${API_BASE}/category/${encodeURIComponent(cid)}`, { credentials: 'include' });
              if (!res.ok) {
                console.warn('[list.js] fallback /category/{id} not OK:', cid, res.status);
                continue;
              }
              const data = await res.json();
              const arr = Array.isArray(data) ? data
                        : (Array.isArray(data?.content) ? data.content : []);
              for (const it of arr) {
                const id = String(it.listingId ?? it.id ?? '');
                if (!id) continue;
                if (!map.has(id)) map.set(id, it);
              }
            } catch (e) {
              console.warn('[list.js] fallback /category/{id} error:', cid, e);
            }
          }
          let merged = Array.from(map.values());

          // (c) 지역 필터 필요 시 클라이언트에서 적용
          items = regionId ? merged.filter(it => matchRegion(it, regionId)) : merged;
          console.debug('[list.js] fallback merged items:', items.length);

        } else if (regionId) {
          // 카테고리 없이 지역만
          try {
            const res = await fetch(`${API_BASE}/region/${encodeURIComponent(regionId)}`, { credentials: 'include' });
            if (res.ok) {
              const data = await res.json();
              items = Array.isArray(data) ? data : (Array.isArray(data?.content) ? data.content : []);
            } else {
              const res2 = await fetch(`${API_BASE}/list?regionId=${encodeURIComponent(regionId)}&page=${page}&size=${size}`, { credentials: 'include' });
              const body2 = await res2.json();
              items = Array.isArray(body2?.content) ? body2.content : (Array.isArray(body2) ? body2 : []);
            }
          } catch {
            const res3 = await fetch(`${API_BASE}/list?regionId=${encodeURIComponent(regionId)}&page=${page}&size=${size}`, { credentials: 'include' });
            const body3 = await res3.json();
            items = Array.isArray(body3?.content) ? body3.content : (Array.isArray(body3) ? body3 : []);
          }

        } else {
          // 둘 다 없음 → 전체
          const res = await fetch(`${API_BASE}/list?page=${page}&size=${size}`, { credentials: 'include' });
          if (res.ok) {
            const body = await res.json();
            items = Array.isArray(body?.content) ? body.content : (Array.isArray(body) ? body : []);
          } else {
            items = [];
          }
        }
      }

      // ✅ 2.5) SOLD 제외 필터 (공통)
      items = (Array.isArray(items) ? items : []).filter(
        it => String(it.status || 'ACTIVE').trim().toUpperCase() !== 'SOLD'
      );

      // 3) 정렬 + 렌더
      const sorted = sortProducts(items, sortSelect.value);
      gridEl.innerHTML = '';
      if (!sorted || sorted.length === 0) {
        emptyEl.style.display = 'block';
        emptyEl.textContent   = '등록된 상품이 없습니다.';
        countEl.textContent   = '0개 상품';
      } else {
        emptyEl.style.display = 'none';
        countEl.textContent   = `${sorted.length}개 상품`;
        sorted.forEach(item => gridEl.appendChild(productCard(item)));
      }
    } catch (e) {
      console.error(e);
      gridEl.innerHTML       = '';
      emptyEl.style.display  = 'block';
      emptyEl.textContent    = '상품을 불러오지 못했습니다.';
      countEl.textContent    = '0개 상품';
    }
  }

  // =========================
  // Helpers (카테고리 하위 확장)
  // =========================
  async function getDescendantCategoryIds(rootId) {
    try {
      const res = await fetch('/api/categories', { cache: 'no-store' });
      if (!res.ok) return [rootId];

      const cats = await res.json();
      if (!Array.isArray(cats) || cats.length === 0) return [rootId];

      const byId = new Map(cats.map(c => [String(c.categoryId), c]));
      const root = byId.get(String(rootId));
      if (!root) return [rootId];

      // 1) path 기반 (권장)
      if (root.path) {
        const rootPath = root.path;
        const result = cats
          .filter(c => {
            const p = c.path || '';
            return p === rootPath || p.startsWith(rootPath + ' > ');
          })
          .map(c => String(c.categoryId));
        if (!result.includes(String(rootId))) result.push(String(rootId));
        return Array.from(new Set(result));
      }

      // 2) parentId 기반 (path가 없다면)
      const childrenMap = new Map(); // parentId -> [child...]
      for (const c of cats) {
        const pid = c.parentId == null ? null : String(c.parentId);
        if (!childrenMap.has(pid)) childrenMap.set(pid, []);
        childrenMap.get(pid).push(c);
      }

      const result = new Set([String(rootId)]);
      const stack  = [String(rootId)];
      while (stack.length) {
        const cur = stack.pop();
        const kids = childrenMap.get(cur) || [];
        for (const k of kids) {
          const idStr = String(k.categoryId);
          if (!result.has(idStr)) {
            result.add(idStr);
            stack.push(idStr);
          }
        }
      }
      return Array.from(result);
    } catch (e) {
      console.warn('[list.js] getDescendantCategoryIds error:', e);
      return [rootId];
    }
  }

  // =========================
  // Utils
  // =========================
  function toInt(v, d = 0) {
    const n = Number(v);
    return Number.isFinite(n) ? n : d;
  }

  async function resolveRegionIdIfNeeded() {
    if (regionId) return regionId;

    // 1) localStorage에 숫자 id 저장된 경우
    const savedId = localStorage.getItem('selectedRegionId');
    if (savedId && /^\d+$/.test(savedId)) {
      regionId = savedId;
      return regionId;
    }

    // 2) 라벨만 저장된 경우 → /api/suggest 로 역매핑
    const label = (localStorage.getItem('selectedRegionLabel') || localStorage.getItem('selectedRegion') || '').trim();
    if (label && label !== '내 근처') {
      try {
        const res = await fetch(`/api/suggest?q=${encodeURIComponent(label)}&limit=1`, { cache: 'no-store' });
        if (res.ok) {
          const rows = await res.json();
          const id = rows?.[0]?.regionId ?? rows?.[0]?.region_id;
          if (id) {
            regionId = String(id);
            localStorage.setItem('selectedRegionId', regionId);
            return regionId;
          }
        }
      } catch {}
    }
    return null;
  }

  async function updateBreadcrumb() {
    const parts = [];

    if (categoryId) {
      try {
        const res = await fetch('/api/categories', { cache: 'no-store' });
        if (res.ok) {
          const cats = await res.json();
          const cat  = Array.isArray(cats) ? cats.find(c => String(c.categoryId) === String(categoryId)) : null;
          // path가 있으면 path, 없으면 name
          const catLabel = (cat?.path) ? cat.path : (cat?.name ? cat.name : '#' + categoryId);
          parts.push(`카테고리: ${catLabel}`);
        } else {
          parts.push(`카테고리: #${categoryId}`);
        }
      } catch {
        parts.push(`카테고리: #${categoryId}`);
      }
    }

    if (regionId) {
      try {
        const res = await fetch('/api/regions', { cache: 'no-store' });
        if (res.ok) {
          const rows  = await res.json();
          const found = Array.isArray(rows) ? rows.find(r => String(r.regionId ?? r.region_id) === String(regionId)) : null;
          const regLabel = found?.path || found?.name || ('#' + regionId);
          parts.push(`지역: ${regLabel}`);
        } else {
          parts.push(`지역: #${regionId}`);
        }
      } catch {
        const label = localStorage.getItem('selectedRegionLabel') || localStorage.getItem('selectedRegion');
        parts.push(`지역: ${label || ('#' + regionId)}`);
      }
    }

    breadcrumbEl.textContent = parts.length ? parts.join(' · ') : '전체 상품';
  }

  function sortProducts(list, key) {
    const cloned = list.slice();
    switch (key) {
      case 'priceAsc':  cloned.sort((a, b) => (toNum(a.price) - toNum(b.price))); break;
      case 'priceDesc': cloned.sort((a, b) => (toNum(b.price) - toNum(a.price))); break;
      case 'recent':
      default:          cloned.sort((a, b) => (toNum(b.listingId ?? b.id) - toNum(a.listingId ?? a.id))); break;
    }
    return cloned;
  }

  function toNum(v) {
    if (v == null) return 0;
    const n = (typeof v === 'string') ? Number(v) : v;
    return Number.isFinite(n) ? n : 0;
  }

  function formatPrice(won) {
    if (won == null) return '-';
    const num = (typeof won === 'string') ? Number(won) : won;
    return Number.isFinite(num) ? num.toLocaleString('ko-KR') + '원' : String(won) + '원';
  }

  function normalizeImg(u) {
    if (!u) return '/images/placeholder.png';
    if (u.startsWith('http://') || u.startsWith('https://')) return u;
    if (u.startsWith('/uploads')) return u;
    return `/uploads/${u}`;
  }

  function escapeHtml(s) {
    return String(s ?? '')
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  // 서버 DTO가 가진 다양한 모양 지원: regionId / region?.id / region_id / regionName 등
  function matchRegion(item, wantId) {
    const want = String(wantId);
    const candIds = [
      item.regionId, item.region_id,
      item.region?.id, item.region?.regionId, item.region?.region_id,
    ].filter(v => v != null).map(String);

    if (candIds.some(id => id === want)) return true;

    // 라벨 기반 비교 (정확도↓)
    const label = (item.regionName || item.addressText || item.region?.name || item.region?.path || '').trim();
    if (!label) return false;
    const savedLabel = (localStorage.getItem('selectedRegionLabel') || localStorage.getItem('selectedRegion') || '').trim();
    if (!savedLabel) return false;

    return label.includes(savedLabel);
  }

  // =========================
  // 카드 UI
  // =========================
  function productCard(item) {
    const CONDITION_LABEL = { NEW:'새상품', LIKE_NEW:'사용감 거의 없음', GOOD:'상태 좋음', FAIR:'보통', POOR:'사용감 많음' };
    const TRADE_LABEL     = { MEETUP:'직거래', DELIVERY:'택배', BOTH:'직거래/택배' };
    const label = (v, map) => (v == null ? '' : (map[String(v).trim().toUpperCase()] ?? v));

    const status = String(item.status || '').trim().toUpperCase(); // ACTIVE | RESERVED | SOLD
    const negotiable = String(item.negotiable || '').trim().toUpperCase() === 'Y';

    // 이미지
    const photo  = (item.photoUrls && item.photoUrls.length > 0) ? item.photoUrls[0] : null;
    const imgSrc = normalizeImg(photo);

    const listingId = item.listingId ?? item.id;
    const href      = `/productDetail.html?id=${encodeURIComponent(listingId)}`;

    // 지역/부가 메타 (있으면 노출)
    const regionText = item.regionName || item.addressText || '';

    const a = document.createElement('a');
    a.href = href;
    a.className = 'card';

    const ribbonHtml = (status === 'RESERVED')
      ? `<div class="ribbon">예약중</div>`
      : (status === 'SOLD')
        ? `<div class="ribbon sold">판매완료</div>`
        : '';

    const condCode = String(item.itemCondition || '').trim().toUpperCase();
    const condChipClass = condCode === 'NEW' ? 'chip cond-new' : 'chip';
    const condChip = item.itemCondition
        ? `<span class="${condChipClass}">${label(item.itemCondition, CONDITION_LABEL)}</span>` : '';

    const tradeChip = item.tradeType
        ? `<span class="chip trade">${label(item.tradeType, TRADE_LABEL)}</span>` : '';

    const negoChip = negotiable
        ? `<span class="chip neg-y">네고 가능</span>` : '';

    const regionChip = regionText
        ? `<span class="chip">${escapeHtml(regionText)}</span>` : '';

    a.innerHTML = `
      <div class="thumb-wrap">
        ${ribbonHtml}
        <img class="thumb" src="${imgSrc}" alt="${escapeHtml(item.title ?? '상품 이미지')}"
             onerror="this.classList.add('skeleton'); this.src='/images/placeholder.png'"/>
        <div class="price-badge">${formatPrice(item.price)}</div>
      </div>
      <div class="body">
        <h3 class="title">${escapeHtml(item.title ?? '')}</h3>
        <div class="meta-row">
          ${condChip}
          ${tradeChip}
          ${negoChip}
          ${regionChip}
        </div>
      </div>
    `;
    return a;
  }

  // =========================
  // 스켈레톤
  // =========================
  function renderSkeleton(n=8){
    gridEl.innerHTML = '';
    for(let i=0;i<n;i++){
      const s = document.createElement('div');
      s.className = 'card';
      s.innerHTML = `
        <div class="thumb-wrap skeleton"></div>
        <div class="body">
          <div class="skeleton" style="height:16px; width:80%; border-radius:8px;"></div>
          <div class="meta-row" style="margin-top:8px;">
            <div class="skeleton" style="height:22px; width:70px; border-radius:999px;"></div>
            <div class="skeleton" style="height:22px; width:60px; border-radius:999px;"></div>
          </div>
        </div>`;
      gridEl.appendChild(s);
    }
  }
})();
