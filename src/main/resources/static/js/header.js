document.addEventListener('DOMContentLoaded', () => {
    // 1. fetch를 사용하여 header.html 파일을 비동기적으로 가져옵니다.
    fetch('/header.html')
      .then(response => {
        if (!response.ok) {
          throw new Error('Network response was not ok');
        }
        return response.text();
      })
      .then(html => {
        // 2. 가져온 HTML을 'header' div에 삽입합니다.
        document.getElementById('header').innerHTML = html;

        // 3. HTML이 삽입된 후, 헤더 내부에 있는 요소를 찾아서 이벤트 리스너를 추가합니다.
        const searchBtn = document.getElementById("searchBtn");
        const searchInput = document.getElementById("searchInput");

        if (searchBtn && searchInput) {
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
        }

        // 로그인 상태 확인
        const authLinksDiv = document.getElementById('auth-links');
        const reportMenu = document.getElementById('report-menu');

        // 서버에서 로그인 정보를 확인하는 API 호출
        if (authLinksDiv) {
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
                        if(data.username == "관리자") {
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
                    } else {
                        authLinksDiv.innerHTML = `
                            <a href="/login.html">로그인</a>
                            <a href="/signup.html">회원가입</a>
                        `;
                    }

                })
                .catch(error => {
                    console.error('Failed to fetch user status:', error);
                    authLinksDiv.innerHTML = `
                        <a href="/login.html">로그인</a>
                        <a href="/signup.html">회원가입</a>
                    `;
                });
        }
      })
      .catch(error => {
        console.error('There has been a problem with your fetch operation:', error);
      });
});

async function buildCategoryMenu() {
    const res = await fetch('/api/categories');
    const categories = await res.json();

    const categoryMap = {};
    categories.forEach(cat => categoryMap[cat.categoryId] = {...cat, children: []});
    const roots = [];

    categories.forEach(cat => {
        if (cat.parentId && categoryMap[cat.parentId]) {
            categoryMap[cat.parentId].children.push(categoryMap[cat.categoryId]);
        } else {
            roots.push(categoryMap[cat.categoryId]);
        }
    });

    const menuContainer = document.getElementById('category-menu');

    function createMenuItem(cat) {
        const itemDiv = document.createElement('div');
        itemDiv.classList.add('submenu-item');

        const link = document.createElement('a');
        link.textContent = cat.name;
        link.href = `/category/${encodeURIComponent(cat.name)}.html`;
        itemDiv.appendChild(link);

        if (cat.children.length > 0) {
            const subDiv = document.createElement('div');
            subDiv.classList.add('submenu');
            cat.children.forEach(child => subDiv.appendChild(createMenuItem(child)));
            itemDiv.appendChild(subDiv);
        }

        return itemDiv;
    }

    roots.forEach(root => menuContainer.appendChild(createMenuItem(root)));
}

document.addEventListener("DOMContentLoaded", buildCategoryMenu);