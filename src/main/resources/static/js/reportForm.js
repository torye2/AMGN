document.addEventListener("DOMContentLoaded", () => {
    const p = new URLSearchParams(location.search);
    const listingId = p.get('listingId');
    const nick = p.get('reportedNickname');
    const uid = p.get('reportedUserId');

    // 폼 요소 바인딩
    const f = document.getElementById('reportForm');
    const nickInput = document.getElementById('reportedNickname');
    const uidHidden = document.getElementById('reportedUserId');   // hidden
    const listingHidden = document.getElementById('listingId');     // hidden

    if (nick && nickInput) { nickInput.value = nick; nickInput.readOnly = true; }
    if (uid && uidHidden) uidHidden.value = uid;
    if (listingId && listingHidden) listingHidden.value = listingId;
})