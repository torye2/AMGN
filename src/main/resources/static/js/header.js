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
        link.href = `list.html?id=${encodeURIComponent(cat.categoryId)}`;
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