document.addEventListener('DOMContentLoaded', () => {
    // 기존 fetch 로직
    const urlParams = new URLSearchParams(window.location.search);
    const listingId = urlParams.get('id');

    if (listingId) {
        fetch(`/product/${listingId}`)
    }

    // 채팅하기 버튼에 클릭 이벤트 리스너 추가
    const chatButton = document.getElementById('chat-button');
    if (chatButton) {
        chatButton.addEventListener('click', () => {
            window.location.href = 'chat.html';
        });
    }
});