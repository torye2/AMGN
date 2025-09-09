// footer.html을 동적으로 주입하고, 하단 내비게이션 활성화 등을 초기화
(function () {
  async function ensureFooterLoaded() {
    const footerContainer = document.getElementById('footer');
    if (!footerContainer) return;

    // 이미 주입되어 있으면 재주입 생략
    const alreadyLoaded = !!footerContainer.querySelector('.bottom-nav') || !!footerContainer.querySelector('.site-footer');
    if (!alreadyLoaded) {
      try {
        const resp = await fetch('/footer.html', { cache: 'no-store' });
        if (!resp.ok) throw new Error('Network response was not ok');
        const html = await resp.text();
        footerContainer.innerHTML = html;
      } catch (err) {
        console.error('Failed to load footer:', err);
        return;
      }
    }

    // 주입 후 기능 초기화
    initFooterFeatures(footerContainer);
  }

  function initFooterFeatures(scopeEl) {
    // 현재 경로에 따라 하단 네비게이션 활성 탭 설정
    try {
      const path = location.pathname || '/';
      const items = scopeEl.querySelectorAll('.bottom-nav .nav-item');
      if (!items || items.length === 0) return;

      items.forEach(i => i.classList.remove('active'));
      const mapping = [
        { match: (p) => p.startsWith('/categories'), idx: 1 },
        { match: (p) => p.startsWith('/sell') || p.startsWith('/sale'), idx: 2 },
        { match: (p) => p.startsWith('/chat'), idx: 3 },
        { match: (p) => p.startsWith('/my') || p.startsWith('/mypage'), idx: 4 },
      ];

      const found = mapping.find(m => m.match(path));
      const activeIdx = found ? found.idx : 0;

      if (items[activeIdx]) items[activeIdx].classList.add('active');
    } catch (e) {
      // noop
    }
  }

  // 최초 로드 + BFCache(뒤/앞 이동) 복원 모두 처리
  document.addEventListener('DOMContentLoaded', ensureFooterLoaded);
  window.addEventListener('pageshow', ensureFooterLoaded);
})();
