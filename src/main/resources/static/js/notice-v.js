fetch('header.html')
.then(response => response.text())
.then(data => {
    document.getElementById('header-p').innerHTML = data;
});

const queryParams = new URLSearchParams(window.location.search);
const index = queryParams.get('index');

const notices = JSON.parse(localStorage.getItem('notices')) || [];
const notice = notices[index];

if (notice) {
    document.getElementById('notice-title').textContent = notice.title;
    document.getElementById('notice-date').textContent = notice.date;
    document.getElementById('notice-content').textContent = notice.content;
} else {
    alert('공지사항을 찾을 수 없습니다.');
    window.location.href = "notice-l.html"
}

function deleteNotice() {
    if(confirm("정말 삭제하시겠습니까")) {
        notices.splice(index, 1);
        localStorage.setItem('notices', JSON.stringify(notices));
        alert("삭제되었습니다.");
        window.location.href = "notice-l.html"
    }
}

function editNotice() {
    window.location.href = `notice-w.html?index=${index}`;
}

document.addEventListener('DOMContentLoaded', () => {
    const btnBox = document.getElementById('btn-box');

    // 서버에서 로그인 정보를 확인하는 API 호출
    if (btnBox) {
        fetch('/api/user/status')
            .then(response => response.json())
            .then(data => {
                if (data.isLoggedIn) {
                    btnBox.innerHTML = `
                    <a class="back-btn" href="notice-l.html">목록</a>
                    `;
                    if(data.username == "관리자") {
                        btnBox.innerHTML = `
                        <button class="edit-btn" onclick="editNotice()">수정</button>
                        <button class="delete-btn" onclick="deleteNotice()">삭제</button>
                        <a class="back-btn" href="notice-l.html">목록</a>
                        `;
                    } else {
                        btnBox.innerHTML = `
                        <a class="back-btn" href="notice-l.html">목록</a>
                        `;
                    }
                } else {
                    btnBox.innerHTML = `
                    <a class="back-btn" href="notice-l.html">목록</a>
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