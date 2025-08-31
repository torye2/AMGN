fetch('header1.html')
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