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
                <a href="notice-v.html?index=${index}">${notice.title}
                <span class="date">${notice.date}</span>
                </a>
                    
            `;
        list.appendChild(li);
    });
}

window.onload = loadNotices;