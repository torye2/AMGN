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

function isMainPage() {
  return location.pathname === '/main.html' || location.pathname.endsWith('/main.html');
}

/* ==============================!
 * 헤더 내부 기능 초기화
 * ============================== */
async function initHeaderFeatures() {
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
      .then(async (data) => {
        if (data.isLoggedIn) {
          authLinksDiv.innerHTML = `
            <p class="welcome-message">${data.nickname}님 환영합니다!</p>
            <form id="logoutForm" action="/logout" method="POST" style="display:inline;">
                <input type="hidden" name="_csrf" id="csrfLogoutField" value="">
                <button type="submit" style="background:none; border:none; padding:0; cursor:pointer;">로그아웃</button>
            </form>
          `;
          let token = getCookie('XSRF-TOKEN');
          if (!token) {
            try {
              const info = await ensureCsrf();
              token = info.token;
            } catch (e) {
              console.warn('CSRF 토큰 로딩 실패', e);
            }
          }
          const hidden = document.getElementById('csrfLogoutField');
          if (hidden && token) hidden.value = token;
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

  async function ensureCsrf() {
    const r = await fetch('/api/csrf', { credentials: 'same-origin' });
    const j = await r.json(); // { headerName, token }
    return j; // 필요 시 헤더명도 동적으로 사용
  }

  function getCookie(name) {
    return document.cookie.split('; ').find(v => v.startsWith(name + '='))?.split('=')[1];
  }

  // --- 카테고리 메뉴(현재 지역 유지) ---
  buildCategoryMenu();

  // --- 지역 라벨 초기 반영 ---
  const labelEl = document.getElementById('regionBtnLabel');
  if (labelEl) {
    // 메인에서는 항상 "지역" 표시, 그 외 페이지는 저장값 사용
    labelEl.textContent = isMainPage()
      ? '지역'
      : (localStorage.getItem('selectedRegionLabel') || '지역');
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
            <button type="button" class="btn btn-outline-primary" id="useGpsBtn">내 위치로 찾기</button>
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

  // 지역 라벨 반영 (메인은 항상 '지역' 유지)
  const labelEl = document.getElementById('regionBtnLabel');
  if (labelEl) labelEl.textContent = isMainPage() ? '지역' : (selected.label || '지역');

  // 자동완성 API 호출
  async function fetchRegionSuggest(keyword = '') {
    const url = `/api/suggest?q=${encodeURIComponent(keyword)}&limit=50&onlyLeaf=true`;
    const res = await fetch(url, { cache: 'no-store' });
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
      const labelEl2 = document.getElementById('regionBtnLabel');
      if (labelEl2) labelEl2.textContent = '지역';
      location.href = '/list.html';
      return;
    }

    localStorage.setItem('selectedRegionId', selected.id);
    localStorage.setItem('selectedRegionLabel', selected.label);
    const labelEl2 = document.getElementById('regionBtnLabel');
    if (labelEl2 && !isMainPage()) labelEl2.textContent = selected.label; // 메인은 '지역' 유지

    // 지역 유지해서 목록으로
    location.href = `/list.html?regionId=${encodeURIComponent(selected.id)}`;
  });

  // 모달 열릴 때마다 초기화
  const regionOpenBtn = document.getElementById('regionOpenBtn');
  if (regionOpenBtn && !regionOpenBtn.dataset.bound) {
    regionOpenBtn.addEventListener('click', () => {
      const current = localStorage.getItem('selectedRegionLabel') || '지역';
      const labelEl3 = document.getElementById('regionBtnLabel');
      if (labelEl3) labelEl3.textContent = isMainPage() ? '지역' : current;
      input.value = '';
      fetchRegionSuggest('')
        .then(renderList)
        .catch(() => {
          list.innerHTML = '<div class="text-danger px-2">지역 목록 로딩 실패</div>';
        });
    });
    regionOpenBtn.dataset.bound = 'true';
  }

  // 초기화 버튼: 저장 초기화 후 메인으로 이동
  const resetBtn = document.getElementById('resetRegionBtn');
  if (resetBtn && !resetBtn.dataset.bound) {
    resetBtn.addEventListener('click', () => {
      // 1) 저장 제거
      localStorage.removeItem('selectedRegionId');
      localStorage.removeItem('selectedRegionLabel');

      // 2) 헤더 라벨 복구
      const labelEl2 = document.getElementById('regionBtnLabel');
      if (labelEl2) labelEl2.textContent = '지역';

      // 3) 메인 페이지로 이동
      window.location.href = '/main.html';
    });
    resetBtn.dataset.bound = 'true';
  }

  // GPS 버튼 바인딩
  const useGpsBtn = document.getElementById('useGpsBtn');
  if (useGpsBtn && !useGpsBtn.dataset.bound) {
    useGpsBtn.addEventListener('click', useGpsAndSelectDong);
    useGpsBtn.dataset.bound = 'true';
  }
}

/* ==============================
 * GPS 사용 → (카카오 프록시)역지오코딩 → "시/구 분리 후보" 지역 매칭 → 저장/이동
 *  - 서버 프록시: GET /api/geo/coord2regioncode?lat=..&lng=..  (응답: { label, lat, lng })
 * ============================== */

// 위치 한 번 얻기
async function getCurrentPositionOnce(opts) {
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, opts);
  });
}

// 실패 보강용
async function getPositionWithFallback() {
  try {
    return await getCurrentPositionOnce({
      enableHighAccuracy: true,
      timeout: 8000,
      maximumAge: 300000,
    });
  } catch (e) {
    if (e.code === 2) { // POSITION_UNAVAILABLE
      try {
        return await getCurrentPositionOnce({
          enableHighAccuracy: false,
          timeout: 8000,
          maximumAge: 300000,
        });
      } catch (_) {}
    }
    // watchPosition 1회 대기
    try {
      return await new Promise((resolve, reject) => {
        const id = navigator.geolocation.watchPosition(
          (pos) => { navigator.geolocation.clearWatch(id); resolve(pos); },
          (err) => { navigator.geolocation.clearWatch(id); reject(err); },
          { enableHighAccuracy: false }
        );
        setTimeout(() => { navigator.geolocation.clearWatch(id); reject(new Error('watch timeout')); }, 10000);
      });
    } catch (_) {
      throw e;
    }
  }
}

async function useGpsAndSelectDong() {
  if (!('geolocation' in navigator)) {
    alert('이 브라우저는 위치 정보를 지원하지 않습니다.');
    return;
  }
  const isSecure = window.isSecureContext || location.protocol === 'https:' || location.hostname === 'localhost';
  if (!isSecure) {
    alert('현재 접속 주소가 HTTPS가 아닙니다. HTTPS에서 다시 시도하세요.');
    return;
  }

  // 1) 현재 위치
  let coords;
  try {
    const pos = await getPositionWithFallback();
    coords = pos.coords;
  } catch (e) {
    if (e.code === 1) {
      alert('위치 권한이 거부되었습니다. 브라우저/OS 설정에서 위치 접근을 허용해주세요.');
    } else if (e.code === 2) {
      alert('현재 위치를 결정할 수 없습니다.\nWi-Fi를 켜고 실외/창가에서 다시 시도하거나, 지역 검색으로 선택해주세요.');
    } else if (e.code === 3) {
      alert('위치 요청이 시간 초과되었습니다. 다시 시도해주세요.');
    } else {
      alert('위치 정보를 가져오지 못했습니다.');
    }
    return;
  }

  const { latitude: lat, longitude: lng } = coords;

  // 2) 서버 프록시로 카카오 역지오코딩 (응답: { label, lat, lng })
  let label;
  try {
    const r = await fetch(`/api/geo/coord2regioncode?lat=${lat}&lng=${lng}`, { credentials: 'include' });
    if (!r.ok) {
      const txt = await r.text();
      console.error('geo proxy error:', r.status, txt);
      throw new Error('역지오코딩 실패');
    }
    const data = await r.json();
    label = data.label; // 예: "경기도 > 성남시 수정구 > 태평2동"
  } catch (e) {
    console.error(e);
    alert('동/읍/면 정보를 찾지 못했습니다.');
    return;
  }

  // 3) "시/구 분리" 후보 라벨 생성 후 순차 매칭 + 스코어링
  const parts = String(label).split('>').map(s => s.trim());
  const r1 = parts[0] || ''; // 예: 경기도
  const r2 = parts[1] || ''; // 예: 성남시 수정구
  const r3 = parts[2] || ''; // 예: 태평2동

  // 동명에서 숫자(예: 2) 제거하여 "태평동" 형태도 함께 시도 (리/가까지 고려)
  const r3Base = r3.replace(/\d+(?=(동|리|가))/g, '');

  function splitCityGu(s) {
    // "성남시 수정구", "수원시 영통구", "부산광역시 금정구", "용인시 수지구"
    const m = s.match(/^(.+?(시|도|군|특별시|광역시))\s+(.+?구)$/);
    return m ? [m[1], m[3]] : [s];
  }

  const [cityMaybe, guMaybe] = splitCityGu(r2);

  // 후보 검색어들 (공백/ > / / 구분자 조합 + 숫자 제거 동명 포함)
  const candidatesRaw = [
    // '>' 구분
    [r1, cityMaybe, guMaybe, r3].filter(Boolean).join(' > '),
    [r1, r2, r3].filter(Boolean).join(' > '),
    [r1, cityMaybe, guMaybe, r3Base].filter(Boolean).join(' > '),
    [r1, r2, r3Base].filter(Boolean).join(' > '),

    // 공백 구분
    [r1, cityMaybe, guMaybe, r3].filter(Boolean).join(' '),
    [r1, r2, r3].filter(Boolean).join(' '),
    [r1, cityMaybe, guMaybe, r3Base].filter(Boolean).join(' '),
    [r1, r2, r3Base].filter(Boolean).join(' '),
    [cityMaybe, guMaybe, r3].filter(Boolean).join(' '),
    [cityMaybe, guMaybe, r3Base].filter(Boolean).join(' '),
    [guMaybe, r3].filter(Boolean).join(' '),
    [guMaybe, r3Base].filter(Boolean).join(' '),

    // '/' 구분 (DB 경로가 "경기도/성남시 수정구/태평동" 형태인 경우를 대비)
    [r1, cityMaybe, guMaybe, r3].filter(Boolean).join('/'),
    [r1, r2, r3].filter(Boolean).join('/'),
    [r1, cityMaybe, guMaybe, r3Base].filter(Boolean).join('/'),
    [r1, r2, r3Base].filter(Boolean).join('/'),
    [cityMaybe, guMaybe, r3].filter(Boolean).join('/'),
    [cityMaybe, guMaybe, r3Base].filter(Boolean).join('/'),

    // 동만
    r3,
    r3Base
  ].filter(Boolean);

  // 중복 제거
  const candidates = [...new Set(candidatesRaw)];

  async function trySuggest(q) {
    const res = await fetch(`/api/suggest?q=${encodeURIComponent(q)}&limit=10&onlyLeaf=true`, { cache: 'no-store' });
    if (!res.ok) return null;
    const rows = await res.json();
    if (!Array.isArray(rows) || rows.length === 0) return null;

    const toText = (r) => String(r.path || r.name || r.label || '');
    const scoreRow = (r) => {
      const t = toText(r);
      let s = 0;
      if (r3 && t.includes(r3)) s += 3;          // 동(원형) 일치 가중
      if (r3Base && t.includes(r3Base)) s += 3;  // 동(숫자삭제) 일치 가중
      if (guMaybe && t.includes(guMaybe)) s += 2; // 구 일치
      if (cityMaybe && t.includes(cityMaybe)) s += 1; // 시 일치
      if (r1 && t.includes(r1)) s += 1;          // 광역/도 일치
      return s;
    };
    rows.sort((a, b) => scoreRow(b) - scoreRow(a));
    return rows[0];
  }

  let best = null;
  for (const q of candidates) {
    try {
      const hit = await trySuggest(q);
      if (hit && (hit.regionId || hit.region_id || hit.id)) {
        best = hit;
        break;
      }
    } catch {}
  }

  if (!best) {
    alert(`지역 매칭 실패: ${label}`);
    return;
  }

  const regionId = String(best.regionId ?? best.region_id ?? best.id);
  const labelUsed = best.path || best.name || label;

  // 4) 저장 & 라벨 변경(메인은 '지역' 유지)
  localStorage.setItem('selectedRegionId', regionId);
  localStorage.setItem('selectedRegionLabel', labelUsed);

  const labelEl2 = document.getElementById('regionBtnLabel');
  if (labelEl2 && !isMainPage()) labelEl2.textContent = labelUsed;

  // 5) 목록으로 이동
  location.href = `/list.html?regionId=${encodeURIComponent(regionId)}`;
}