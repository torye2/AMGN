(function () {
    const listEl = document.getElementById('faq-list');
    const writeBtn = document.getElementById('write-btn');
    const btnBox = document.getElementById('btn-box');
    let isAdmin = false;

    function renderEmpty(text) {
        const li = document.createElement('li');
        li.className = 'faq-item';
        li.textContent = text || '등록된 FAQ가 없습니다.';
        listEl.innerHTML = '';
        listEl.appendChild(li);
    }

    function createAdminActions(id, currentQ, currentA) {
        const box = document.createElement('div');
        box.className = 'admin-actions';
        box.style.marginTop = '8px';
        box.style.display = 'none'; // 기본은 숨김, 항목을 열었을 때만 표시
        box.style.gap = '8px';

        const editBtn = document.createElement('button');
        editBtn.type = 'button';
        editBtn.className = 'btn btn-secondary';
        editBtn.textContent = '수정';

        const delBtn = document.createElement('button');
        delBtn.type = 'button';
        delBtn.className = 'btn btn-danger';
        delBtn.textContent = '삭제';

        editBtn.addEventListener('click', async (e) => {
            e.stopPropagation();
            const newQ = prompt('질문을 수정하세요.', currentQ ?? '');
            if (newQ === null) return;
            const newA = prompt('답변을 수정하세요.', currentA ?? '');
            if (newA === null) return;

            const payload = {
                question: String(newQ).trim(),
                answer: String(newA).trim()
            };
            if (!payload.question || !payload.answer) {
                alert('질문과 답변을 모두 입력해 주세요.');
                return;
            }
            try {
                const res = await fetch(`/api/faqs/${id}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify(payload),
                });
                if (!res.ok) {
                    let msg = '수정에 실패했습니다.';
                    try {
                        const j = await res.json();
                        if (j && j.message) msg = j.message;
                    } catch (_) {}
                    alert(msg);
                    return;
                }
                alert('수정되었습니다.');
                loadFaqs();
            } catch (_) {
                alert('네트워크 오류로 수정에 실패했습니다. 잠시 후 다시 시도해 주세요.');
            }
        });

        delBtn.addEventListener('click', async (e) => {
            e.stopPropagation();
            if (!confirm('정말 삭제하시겠습니까?')) return;
            try {
                const res = await fetch(`/api/faqs/${id}`, {
                    method: 'DELETE',
                    credentials: 'same-origin',
                });
                if (!res.ok) {
                    let msg = '삭제에 실패했습니다.';
                    try {
                        const j = await res.json();
                        if (j && j.message) msg = j.message;
                    } catch (_) {}
                    alert(msg);
                    return;
                }
                alert('삭제되었습니다.');
                loadFaqs();
            } catch (_) {
                alert('네트워크 오류로 삭제에 실패했습니다. 잠시 후 다시 시도해 주세요.');
            }
        });

        box.appendChild(editBtn);
        box.appendChild(delBtn);
        return box;
    }

    function renderList(items) {
        listEl.innerHTML = '';
        if (!items || items.length === 0) {
            renderEmpty();
            return;
        }
        items.forEach(it => {
            const id = it.faqId ?? it.id;
            const q = (it.question || it.title || '(제목 없음)').toString();
            const a = (it.answer || it.content || '').toString();

            const li = document.createElement('li');
            li.className = 'faq-item';

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'faq-q';
            btn.setAttribute('aria-expanded', 'false');
            btn.textContent = q;

            const ans = document.createElement('div');
            ans.className = 'faq-a';
            ans.textContent = a;
            ans.hidden = true;

            // 관리자 버튼 박스 (기본은 숨김)
            let adminBox = null;
            if (isAdmin && id != null) {
                adminBox = createAdminActions(id, q, a);
            }

            btn.addEventListener('click', () => {
                const willOpen = ans.hidden;
                ans.hidden = !willOpen;
                btn.setAttribute('aria-expanded', String(willOpen));
                li.classList.toggle('open', willOpen);
                // 항목 열림 상태에 따라 관리자 버튼 박스 표시/숨김
                if (adminBox) {
                    adminBox.style.display = willOpen ? 'flex' : 'none';
                }
            });

            li.appendChild(btn);
            li.appendChild(ans);
            if (adminBox) {
                li.appendChild(adminBox);
            }

            listEl.appendChild(li);
        });
    }

    function loadFaqs() {
        fetch('/api/faqs', { credentials: 'same-origin' })
            .then(res => {
                if (!res.ok) {
                    if (res.status === 401) {
                        renderEmpty('로그인 후 FAQ를 확인할 수 있습니다.');
                        return null;
                    }
                    renderEmpty('FAQ를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
                    return null;
                }
                return res.json();
            })
            .then(data => {
                if (!data) return;
                renderList(data);
            })
            .catch(() => {
                renderEmpty('FAQ를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
            });
    }

    function setupWriteButtonAndAdminFlag() {
        if (!writeBtn || !btnBox) {
            // 버튼 영역이 없더라도 목록은 로드
            loadFaqs();
            return;
        }
        writeBtn.style.display = 'none';
        fetch('/api/user/status', { credentials: 'same-origin' })
            .then(res => (res.ok ? res.json() : null))
            .then(info => {
                isAdmin = !!(info && info.username === '관리자');
                writeBtn.style.display = isAdmin ? 'inline-block' : 'none';
            })
            .catch(() => {
                isAdmin = false;
                writeBtn.style.display = 'none';
            })
            .finally(() => {
                loadFaqs();
            });
    }

    // init
    if (!listEl) return;
    setupWriteButtonAndAdminFlag();
})();
