fetch('header.html')
    .then(response => response.text())
    .then(data => {
        document.getElementById('header').innerHTML = data;
    });

document.addEventListener('DOMContentLoaded', () => {
    loadNotices();
    // 로그인 상태 확인
    const btnBox = document.getElementById('btn-box');
    const adminBtns = document.querySelectorAll('.hidden');

    // 서버에서 로그인 정보를 확인하는 API 호출
    if (btnBox) {
        fetch('/api/user/status')
            .then(response => response.json())
            .then(data => {
                if (data.isLoggedIn) {
                    btnBox.innerHTML = `
                        `;
                    if(data.username == "관리자") {
                        btnBox.innerHTML = `
                            <a href="notice-w.html" class="write-button" id="write-btn">글쓰기</a>
                        `;
                        adminBtns.forEach(btn => btn.classList.remove('hidden'));
                    } else {
                        btnBox.innerHTML = `
                        `;
                    }
                } else {
                    btnBox.innerHTML = `
                        `;
                }

            })
            .catch(error => {
                console.error('Failed to fetch user status:', error);
                btnBox.innerHTML = `
                    `;
            });
    }
})

    function editNotice(index) {
        window.location.href = `notice-w.html?index=${index}`;
    }
    
    function loadNotices() {
        const list = document.getElementById('notice-list');
        const notices = JSON.parse(localStorage.getItem('notices')) || [];
        
        if(notices.length === 0) {
            list.innerHTML = "<li>등록된 공지사항이 없습니다.</li>";
            return;
        }
        
        list.innerHTML = '';
        notices.forEach((notice, index) => {
            const li = document.createElement('li');
            li.innerHTML = `
                <a href="notice-v.html?index=${index}">
                    ${notice.title}
                    </a>
                    <span class="date">${notice.date}</span>
                <button class="edit-btn hidden" onclick="editNotice(${index})">수정</button>
                <button class="delete-btn hidden" onclick="deleteNotice(${index})">삭제</button>
            `;
            list.appendChild(li);
        });
    }

    function deleteNotice(index) {
		const isConfirmed = confirm("정말 삭제하시겠습니까?");
		
		if(!isConfirmed) {
			window.location.href = "notice-l.html";
			return;
		}
		
        let notices = JSON.parse(localStorage.getItem('notices')) || [];
        notices.splice(index, 1);
        localStorage.setItem('notices', JSON.stringify(notices));
        loadNotices();
    }