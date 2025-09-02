document.addEventListener('DOMContentLoaded', () => {
    fetch('/review/header.html')
      .then(res => {
          if (!res.ok) throw new Error('Network response was not ok');
          return res.text();
      })
      .then(html => {
          document.getElementById('header').innerHTML = html;

          // 검색 버튼 이벤트
          const searchBtn = document.getElementById("searchBtn");
          const searchInput = document.getElementById("searchInput");
          if (searchBtn && searchInput) {
              searchBtn.addEventListener("click", () => {
                  const query = searchInput.value;
                  window.location.href = `/search?query=${encodeURIComponent(query)}`;
              });
              searchInput.addEventListener("keypress", (e) => {
                  if (e.key === "Enter") {
                      const query = searchInput.value;
                      window.location.href = `/search?query=${encodeURIComponent(query)}`;
                  }
              });
          }

          // 로그인 상태 표시
          const authLinksDiv = document.getElementById('auth-links');
          if (authLinksDiv) {
              fetch('/api/user/status')
                  .then(res => res.json())
                  .then(data => {
                      if (data.isLoggedIn) {
                          authLinksDiv.innerHTML = `
                              <p class="welcome-message">${data.nickname}님 환영합니다!</p>
                              <form action="/logout" method="post" style="display:inline;">
                                  <button type="submit" style="background:none; border:none; cursor:pointer;">로그아웃</button>
                              </form>
                          `;
                      } else {
                          authLinksDiv.innerHTML = `
                              <a href="/login.html">로그인</a>
                              <a href="/signup.html">회원가입</a>
                          `;
                      }
                  })
                  .catch(() => {
                      authLinksDiv.innerHTML = `
                          <a href="/login.html">로그인</a>
                          <a href="/signup.html">회원가입</a>
                      `;
                  });
          }
      })
      .catch(err => console.error('Header fetch failed:', err));
});
