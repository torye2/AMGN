/* header.js - 헤더/카테고리/지역선택(모달)/검색 연동 (regionId 유지 포함) */

/* ==============================
 * BFCache 복원까지 고려한 헤더 초기화
 * ============================== */
(function () {
  async function ensureHeaderLoaded() {
    const headerContainer = document.getElementById('header');
    if (!headerContainer) return;

    // 헤더가 이미 페이지에 있으면 재주입 생략
    const alreadyLoaded = !!headerContainer.querySelector('.header-icon');
    if (!alreadyLoaded) {
      try {
        // 프로젝트가 공용 header.html 을 제공한다면 주입
        const resp = await fetch('/header.html', { cache: 'no-store' });
        if (resp.ok) {
          headerContainer.innerHTML = await resp.text();
        }
      } catch {
        // 정적 헤더만 쓰는 페이지라면 조용히 패스
      }
    }

    // 헤더 내부 기능 초기화
    initHeaderFeatures();

    // Bootstrap 동적 로드 + 지역 모달 주입 + UI 초기화
    await ensureBootstrapLoaded();
    ensureRegionModal();
    initRegionUI();
  }

  document.addEventListener('DOMContentLoaded', ensureHeaderLoaded);
  window.addEventListener('pageshow', ensureHeaderLoaded);
})();

/* ==============================
 * 헤더 내부 기능 초기화
 * ============================== */
function initHeaderFeatures() {
  // --- 검색(현재 지역 유지) ---
  const searchBtn = document.getElementById('searchBtn');
  const searchInput = document.getElementById('searchInput');

  if (searchBtn && searchInput && searchBtn.dataset.bound !== 'true') {
    const goSearch = () => {
      const q = searchInput.value || '';
      const rid = localStorage.getItem('selectedRegionId');
      const regionParam = rid ? `&regionId=${encodeURIComponent(rid)}` : '';
      location.href = `/search?query=${encodeURIComponent(q)}${regionParam}`;
    };

    searchBtn.addEventListener('click', goSearch);
    searchInput.addEventListener('keypress', (e) => {
      if (e.key === 'Enter') goSearch();
    });
    searchBtn.dataset.bound = 'true';
  }

  // --- 로그인 상태 표시 ---
  const authLinksDiv = document.getElementById('auth-links');
  const reportMenu = document.getElementById('report-menu');

  if (authLinksDiv && authLinksDiv.dataset.loaded !== 'true') {
    fetch('/api/user/status', { credentials: 'include' })
      .then((r) => r.json())
      .then((data) => {
        if (data.isLoggedIn) {
          authLinksDiv.innerHTML = `
            <p class="welcome-message">${data.nickname}님 환영합니다!</p>
            <form action="/logout" method="post" style="display:inline;">
              <button type="submit" style="background:none; border:none; padding:0; cursor:pointer;">로그아웃</button>
            </form>
          `;
          if (reportMenu) {
            reportMenu.innerHTML =
              (data.username === '관리자')
                ? `<div><a href="/reportList">신고목록</a></div><div><a href="/reportForm">신고하기</a></div><div>문의하기</div>`
                : `<div><a href="/reportForm">신고하기</a></div><div>문의하기</div>`;
          }
        } else {
          authLinksDiv.innerHTML = `<a href="/login.html">로그인</a><a href="/signup.html">회원가입</a>`;
        }
        authLinksDiv.dataset.loaded = 'true';
      })
      .catch(() => {
        authLinksDiv.innerHTML = `<a href="/login.html">로그인</a><a href="/signup.html">회원가입</a>`;
        authLinksDiv.dataset.loaded = 'true';
      });
  }

  // --- 카테고리 메뉴(현재 지역 유지) ---
  buildCategoryMenu();

  // --- 지역 라벨 초기 반영 ---
  const labelEl = document.getElementById('regionBtnLabel');
  if (labelEl) {
    labelEl.textContent = localStorage.getItem('selectedRegionLabel') || '지역';
  }
}

/* ==============================
 * 카테고리 메뉴 구성 (/api/categories)
 * - 카테고리 링크 생성 시 현재 지역(regionId) 유지
 * ============================== */
async function buildCategoryMenu() {
  const menuContainer = document.getElementById('category-menu');
  if (!menuContainer) return;
  if (menuContainer.childElementCount > 0) return; // 이미 채워짐

  let res;
  try {
    res = await fetch('/api/categories', { cache: 'no-store' });
  } catch (e) {
    console.error('Failed to fetch categories:', e);
    return;
  }
  if (!res.ok) {
    console.error('Failed to fetch categories:', res.status);
    return;
  }

  const categories = await res.json();

  // tree 구성
  const byId = {};
  categories.forEach((cat) => (byId[cat.categoryId] = { ...cat, children: [] }));
  const roots = [];
  categories.forEach((cat) => {
    if (cat.parentId && byId[cat.parentId]) byId[cat.parentId].children.push(byId[cat.categoryId]);
    else roots.push(byId[cat.categoryId]);
  });

  // 현재 저장된 지역ID (있으면 유지)
  function getSavedRegionIdSync() {
    const id = localStorage.getItem('selectedRegionId');
    return (id && /^\d+$/.test(id)) ? id : null;
    }

  function createItem(cat) {
    const itemDiv = document.createElement('div');
    itemDiv.classList.add('submenu-item');

    const link = document.createElement('a');
    link.textContent = cat.name;

    const rid = getSavedRegionIdSync();
    const qs = new URLSearchParams();
    qs.set('categoryId', String(cat.categoryId)); // id 대신 categoryId 사용
    if (rid) qs.set('regionId', rid);             // ★ 지역 유지

    link.href = `/list.html?${qs.toString()}`;
    itemDiv.appendChild(link);

    if (cat.children?.length) {
      const subDiv = document.createElement('div');
      subDiv.classList.add('submenu2');
      cat.children.forEach((child) => subDiv.appendChild(createItem(child)));
      itemDiv.appendChild(subDiv);
    }
    return itemDiv;
  }

  roots.forEach((root) => menuContainer.appendChild(createItem(root)));
}

/* ==============================
 * Bootstrap 로더 (필요 시 동적 로드)
 * ============================== */
async function ensureBootstrapLoaded() {
  const needJs = !(window.bootstrap && window.bootstrap.Modal);
  const needCss = !document.querySelector('link[rel="stylesheet"][href*="bootstrap"]');
  const tasks = [];

  if (needCss) {
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css';
    link.crossOrigin = 'anonymous';
    link.referrerPolicy = 'no-referrer';
    document.head.appendChild(link);
  }

  if (needJs) {
    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js';
    script.crossOrigin = 'anonymous';
    script.defer = true;
    tasks.push(
      new Promise((resolve, reject) => {
        script.onload = resolve;
        script.onerror = reject;
      })
    );
    document.body.appendChild(script);
  }

  if (tasks.length) {
    try {
      await Promise.all(tasks);
    } catch (e) {
      console.warn('Bootstrap 로드 실패:', e);
    }
  }
}

/* ==============================
 * 지역 모달 공통 주입
 * ============================== */
function ensureRegionModal() {
  if (document.getElementById('regionModal')) return;

  const modalHtml = `
    <div class="modal fade" id="regionModal" tabindex="-1" aria-labelledby="regionModalLabel" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title" id="regionModalLabel">지역 선택</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>
          </div>
          <div class="modal-body">
            <input type="text" class="form-control mb-3" id="regionSearchInput" placeholder="지역 검색 (예: 정자동)">
            <div id="regionList" class="list-group" style="max-height: 320px; overflow:auto;"></div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">취소</button>
            <button type="button" class="btn btn-link text-danger" id="resetRegionBtn">초기화</button>
            <button type="button" class="btn btn-primary" id="applyRegionBtn" data-bs-dismiss="modal">적용</button>
          </div>
        </div>
      </div>
    </div>
  `;
  document.body.insertAdjacentHTML('beforeend', modalHtml);
}

/* ==============================
 * 지역 모달 UI (DB 자동완성 /api/suggest 연동)
 * ============================== */
function initRegionUI() {
  const LIST_ID  = 'regionList';
  const INPUT_ID = 'regionSearchInput';
  const APPLY_ID = 'applyRegionBtn';

  const list = document.getElementById(LIST_ID);
  const input = document.getElementById(INPUT_ID);
  const applyBtn = document.getElementById(APPLY_ID);

  if (!list || !input || !applyBtn) {
    // 모달 주입 타이밍 이슈 대비
    setTimeout(initRegionUI, 100);
    return;
  }

  const $all = (s) => Array.from(document.querySelectorAll(s));
  const debounce = (fn, wait = 250) => {
    let t;
    return (...a) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...a), wait);
    };
  };

  // 선택 상태
  let selected = {
    id: localStorage.getItem('selectedRegionId') || '',
    label: localStorage.getItem('selectedRegionLabel') || '지역',
  };

  // 지역 라벨 반영
  const labelEl = document.getElementById('regionBtnLabel');
  if (labelEl) labelEl.textContent = selected.label || '지역';

  // 자동완성 API 호출
  async function fetchRegionSuggest(keyword = '') {
    const url = `/api/suggest?q=${encodeURIComponent(keyword)}&limit=50&onlyLeaf=true`;
    const res = await fetch(url, { cache: 'no-store' });
    // const res = await fetch(`/api/suggest?q=${encodeURIComponent(keyword)}&limit=50`, { cache: 'no-store' });
    if (!res.ok) throw new Error('지역 목록 요청 실패');

    // 기대 응답: [{regionId, name, path}, ...]
    const rows = await res.json();
    return rows.map((r) => ({
      id: r.regionId ?? r.id ?? r.region_id,
      label: r.path || r.name || r.label,
    }));
  }

  function renderList(results) {
    list.innerHTML = '';

    if (!Array.isArray(results) || results.length === 0) {
      const empty = document.createElement('div');
      empty.className = 'text-muted px-2';
      empty.textContent = '검색 결과가 없습니다.';
      list.appendChild(empty);
      return;
    }

    results.forEach((r) => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'list-group-item list-group-item-action';
      btn.textContent = r.label;
      btn.dataset.regionId = r.id;
      btn.dataset.regionLabel = r.label;

      if (String(r.id) === String(selected.id)) btn.classList.add('active');

      btn.addEventListener('click', () => {
        selected.id = String(r.id);
        selected.label = r.label;
        $all(`#${LIST_ID} .list-group-item`).forEach((x) => x.classList.remove('active'));
        btn.classList.add('active');
      });

      list.appendChild(btn);
    });
  }

  // 최초 로딩
  fetchRegionSuggest('')
    .then(renderList)
    .catch(() => {
      list.innerHTML = '<div class="text-danger px-2">지역 목록 로딩 실패</div>';
    });

  // 입력 디바운스 검색
  input.addEventListener(
    'input',
    debounce(async () => {
      try {
        renderList(await fetchRegionSuggest(input.value));
      } catch (e) {
        console.error(e);
      }
    }, 250)
  );

  // 적용: 저장 + 라벨 갱신 + 지역 목록 페이지 이동
  applyBtn.addEventListener('click', () => {
    if (!selected.id) {
      localStorage.removeItem('selectedRegionId');
      localStorage.removeItem('selectedRegionLabel');
      if (labelEl) labelEl.textContent = '지역';
      location.href = '/list.html';
      return;
    }

    localStorage.setItem('selectedRegionId', selected.id);
    localStorage.setItem('selectedRegionLabel', selected.label);
    if (labelEl) labelEl.textContent = selected.label;

    // 지역 유지해서 목록으로
    location.href = `/list.html?regionId=${encodeURIComponent(selected.id)}`;
  });

  // 모달 열릴 때마다 초기화
  const regionOpenBtn = document.getElementById('regionOpenBtn');
  if (regionOpenBtn && !regionOpenBtn.dataset.bound) {
    regionOpenBtn.addEventListener('click', () => {
      const current = localStorage.getItem('selectedRegionLabel') || '지역';
      if (labelEl) labelEl.textContent = current;
      input.value = '';
      fetchRegionSuggest('')
        .then(renderList)
        .catch(() => {
          list.innerHTML = '<div class="text-danger px-2">지역 목록 로딩 실패</div>';
        });
    });
    regionOpenBtn.dataset.bound = 'true';
  }

  // region 모달 UI 초기화 함수 내부
  const resetBtn = document.getElementById('resetRegionBtn');
  if (resetBtn && !resetBtn.dataset.bound) {
    resetBtn.addEventListener('click', () => {
      // 1) 저장 제거
      localStorage.removeItem('selectedRegionId');
      localStorage.removeItem('selectedRegionLabel');

      // 2) 헤더 라벨 복구
      const labelEl = document.getElementById('regionBtnLabel');
      if (labelEl) labelEl.textContent = '지역';

      // 3) 현재 URL에서 regionId 파라미터 제거 후 이동
      const url = new URL(location.href);
      url.searchParams.delete('regionId');
      // (선택) page 등 다른 파라미터는 보존됨
      window.location.href = url.toString();
    });
    resetBtn.dataset.bound = 'true';
  }
}