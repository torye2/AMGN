// BFCache(뒤로가기/앞으로가기) 복원 시에도 헤더/카테고리 초기화가 보장되도록 개선
(function () {
  async function ensureHeaderLoaded() {
    const headerContainer = document.getElementById('header');
    if (!headerContainer) return;

    // 헤더가 이미 주입되어 있으면 재주입 생략
    const alreadyLoaded = !!headerContainer.querySelector('.header-icon');
    if (!alreadyLoaded) {
      try {
        const resp = await fetch('/header.html', { cache: 'no-store' });
        if (!resp.ok) throw new Error('Network response was not ok');
        const html = await resp.text();
        headerContainer.innerHTML = html;
      } catch (err) {
        console.error('Failed to load header:', err);
        return;
      }
    }

    // 헤더 주입 후 내부 기능 초기화
    initHeaderFeatures();

    // Bootstrap이 없으면 동적 로드 후 지역 모달 보장
    await ensureBootstrapLoaded();
    ensureRegionModal();
  }

  function initHeaderFeatures() {
    // 검색 이벤트 초기화(중복 바인딩 방지)
    const searchBtn = document.getElementById("searchBtn");
    const searchInput = document.getElementById("searchInput");
    if (searchBtn && searchInput && searchBtn.dataset.bound !== 'true') {
      searchBtn.addEventListener("click", function () {
        const query = searchInput.value;
        window.location.href = `/search?query=${encodeURIComponent(query)}`;
      });

      searchInput.addEventListener("keypress", function (e) {
        if (e.key === "Enter") {
          const query = searchInput.value;
          window.location.href = `/search?query=${encodeURIComponent(query)}`;
        }
      });
      searchBtn.dataset.bound = 'true';
    }

    // 로그인 상태 확인(요소 존재 시에만)
    const authLinksDiv = document.getElementById('auth-links');
    const reportMenu = document.getElementById('report-menu');

    if (authLinksDiv && authLinksDiv.dataset.loaded !== 'true') {
      fetch('/api/user/status')
        .then(response => response.json())
        .then(data => {
          if (data.isLoggedIn) {
            authLinksDiv.innerHTML = `
                            <p class="welcome-message">${data.nickname}님 환영합니다!</p>
                            <form action="/logout" method="post" style="display:inline;">
                                <button type="submit" style="background:none; border:none; padding:0; cursor:pointer;">로그아웃</button>
                            </form>
                        `;
            if (reportMenu) {
              if (data.username == "관리자") {
                reportMenu.innerHTML = `
                            <div><a href="/reportList">신고목록</a></div>
                            <div><a href="/reportForm">신고하기</a></div>
                            <div>문의하기</div>
                        `;
              } else {
                reportMenu.innerHTML = `
                            <div><a href="/reportForm">신고하기</a></div>
                            <div>문의하기</div>
                        `;
              }
            }
          } else {
            authLinksDiv.innerHTML = `
                            <a href="/login.html">로그인</a>
                            <a href="/signup.html">회원가입</a>
                        `;
          }
          authLinksDiv.dataset.loaded = 'true';
        })
        .catch(error => {
          console.error('Failed to fetch user status:', error);
          authLinksDiv.innerHTML = `
                        <a href="/login.html">로그인</a>
                        <a href="/signup.html">회원가입</a>
                    `;
          authLinksDiv.dataset.loaded = 'true';
        });
    }

    // 카테고리 메뉴 보장 로딩
    buildCategoryMenu();
  }

  // 최초 로드 + BFCache 복원 모두 처리
  document.addEventListener('DOMContentLoaded', ensureHeaderLoaded);
  window.addEventListener('pageshow', () => {
    ensureHeaderLoaded();
  });
})();

async function buildCategoryMenu() {
    const menuContainer = document.getElementById('category-menu');
    if (!menuContainer) return;
    // 이미 채워져 있으면 재로딩 생략
    if (menuContainer.childElementCount > 0) return;

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

    const categoryMap = {};
    categories.forEach(cat => categoryMap[cat.categoryId] = { ...cat, children: [] });
    const roots = [];

    categories.forEach(cat => {
        if (cat.parentId && categoryMap[cat.parentId]) {
            categoryMap[cat.parentId].children.push(categoryMap[cat.categoryId]);
        } else {
            roots.push(categoryMap[cat.categoryId]);
        }
    });

    function createMenuItem(cat) {
        const itemDiv = document.createElement('div');
        itemDiv.classList.add('submenu-item');

        const link = document.createElement('a');
        link.textContent = cat.name;
        link.href = `/list.html?id=${encodeURIComponent(cat.categoryId)}`;
        itemDiv.appendChild(link);

        if (cat.children.length > 0) {
            const subDiv = document.createElement('div');
            subDiv.classList.add('submenu2');
            cat.children.forEach(child => subDiv.appendChild(createMenuItem(child)));
            itemDiv.appendChild(subDiv);
        }

        return itemDiv;
    }

    roots.forEach(root => menuContainer.appendChild(createMenuItem(root)));
}

document.addEventListener("DOMContentLoaded", buildCategoryMenu);

// Bootstrap 로더 (필요 시 동적 로드)
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
    tasks.push(new Promise((resolve, reject) => {
      script.onload = resolve;
      script.onerror = reject;
    }));
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

// 지역 모달이 모든 페이지에서 동작하도록 공통 주입
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
        <input type="text" class="form-control mb-3" id="regionSearchInput" placeholder="지역 검색 (예: 위례동)">
        <div id="regionList" class="list-group" style="max-height: 320px; overflow:auto;"></div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">취소</button>
        <button type="button" class="btn btn-primary" id="applyRegionBtn" data-bs-dismiss="modal">적용</button>
      </div>
    </div>
  </div>
</div>`;
  document.body.insertAdjacentHTML('beforeend', modalHtml);
}

(function(){
    // 기본 제공 지역 목록 (예시). 백엔드 연동 시 이 배열을 API 응답으로 대체 가능.
    const regions = [
        '내 근처','위례동','잠실동','가락동','문정동','판교동','정자동','서현동','야탑동',
        '강남구','서초구','송파구','분당구','용인시','수원시','광교','일산','부평구','해운대구'
    ];

    function $(s){ return document.querySelector(s); }
    function $all(s){ return Array.from(document.querySelectorAll(s)); }

    function initRegionUI(attempts=0){
        const list = $('#regionList');
        const search = $('#regionSearchInput');
        const applyBtn = $('#applyRegionBtn');

        // 모달 DOM이 아직 없으면 재시도 (헤더/모달 비동기 주입 대비)
        if (!list || !search || !applyBtn) {
            if (attempts < 20) setTimeout(() => initRegionUI(attempts+1), 100);
            return;
        }

        let selected = localStorage.getItem('selectedRegion') || '내 근처';

        function render(filter=''){
            const q = (filter || '').trim().toLowerCase();
            list.innerHTML = '';
            regions
                .filter(r => !q || r.toLowerCase().includes(q))
                .forEach(r => {
                    const item = document.createElement('button');
                    item.type = 'button';
                    item.className = 'list-group-item list-group-item-action';
                    item.textContent = r;
                    item.setAttribute('data-region', r);
                    if (r === selected) item.classList.add('active');
                    item.addEventListener('click', () => {
                        selected = r;
                        $all('#regionList .list-group-item').forEach(x => x.classList.remove('active'));
                        item.classList.add('active');
                    });
                    list.appendChild(item);
                });
        }

        render();

        search.addEventListener('input', () => render(search.value));

        // 적용 버튼: 라벨 반영 + 저장
        applyBtn.addEventListener('click', () => {
            localStorage.setItem('selectedRegion', selected);
            const labelEl = document.getElementById('regionBtnLabel');
            if (labelEl) labelEl.textContent = selected;
        });

        // 초기 라벨 세팅 (헤더가 비동기로 주입되었을 수 있어 재시도)
        function applyInitialLabel(attempts=0){
            const labelEl = document.getElementById('regionBtnLabel');
            if (labelEl) {
                const saved = localStorage.getItem('selectedRegion') || '내 근처';
                labelEl.textContent = saved;
                return;
            }
            if (attempts < 10) setTimeout(() => applyInitialLabel(attempts+1), 100);
        }
        applyInitialLabel();
    }

    // 헤더/모달 주입 타이밍과 무관하게 재시도하며 초기화
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => setTimeout(initRegionUI, 0));
    } else {
        setTimeout(initRegionUI, 0);
    }
})();