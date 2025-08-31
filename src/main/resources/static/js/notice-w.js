
      fetch('header.html')
          .then(response => response.text())
          .then(data => {
              document.getElementById('header-p').innerHTML = data
          });
      
      window.addEventListener("DOMContentLoaded", () => {
          const queryParams = new URLSearchParams(window.location.search);
          const index = queryParams.get("index");

          if(index !== null) {
              const notices = JSON.parse(localStorage.getItem('notices')) || [];
              const notice = notices[index];
              
              if(notice) {
                  document.getElementById("title").value = notice.title;
                  document.getElementById("content").value = notice.content;

                  const formTitle = document.querySelector(".notice-writebox h3");
                  if(formTitle) {
                      formTitle.textContent = "공지사항 수정";
                  }
              } else {
                  alert("수정할 공지사항을 찾을 수 없습니다.");
                  window.location.href = "notice-l.html"
              }
          }
      })


      function saveNotice(event) {
          event.preventDefault();

          const title = document.getElementById('title').value.trim();
          const content = document.getElementById('content').value.trim();
          const date = new Date().toISOString().split('T')[0];

          if (!title || !content) {
              alert("제목과 내용을 모두 입력해주세요.");
              return;
          }

          const newNotice = { title, content, date };
          let notices = JSON.parse(localStorage.getItem('notices')) || [];
          
          const queryParams = new URLSearchParams(window.location.search);
          const index = queryParams.get("index");
          
          if(index !== null) {
              notices[index] = newNotice;
              localStorage.setItem('notices', JSON.stringify(notices));
              window.location.href = `notice-v.html?index=${index}`;
          } else {
              notices.unshift(newNotice);
              localStorage.setItem('notices', JSON.stringify(notices));
              window.location.href = "notice-l.html";
          }
      }
