fetch('header.html')
    .then(response => response.text())
    .then(data => {
        document.getElementById('header-p').innerHTML = data;
    });

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
                <button class="edit-btn" onclick="editNotice(${index})">수정</button>
                <button class="delete-btn" onclick="deleteNotice(${index})">삭제</button>
            `;
            list.appendChild(li);
        });
    }

    window.onload = loadNotices;

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