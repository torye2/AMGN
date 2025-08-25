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
        // 2. 가져온 HTML을 'header-placeholder' div에 삽입합니다.
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
      })
      .catch(error => {
        console.error('There has been a problem with your fetch operation:', error);
      });
});