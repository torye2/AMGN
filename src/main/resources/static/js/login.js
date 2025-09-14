document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(location.search);
    const msg = params.get("message");
    if (msg) {
        alert(decodeURIComponent(msg));
        // 또는 document.getElementById("login-msg").innerText = msg;
    }

    const errorMsg   = params.get('error');       // 에러 메시지

    // 폼 상단 공통 에러 박스에 표시
    if (errorMsg) {
        const formError = document.getElementById('formError');
        if (formError) {
            formError.textContent = decodeURIComponent(errorMsg);
            formError.style.display = 'block';
        }
    }
})