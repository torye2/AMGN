document.addEventListener('DOMContentLoaded', async () => {
    await requireAdmin();
    let page = 0, size = 20;

    const el = {
        status: qs('#status'),
        reporter: qs('#reporter'),
        reported: qs('#reported'),
        listingId: qs('#listingId'),
        btnSearch: qs('#btnSearch'),
        tbody: qs('#tbody'),
        prev: qs('#prev'),
        next: qs('#next'),
        pageInfo: qs('#pageInfo'),
    };

    async function load(){
        const params = new URLSearchParams({ page, size });
        if (el.status.value) params.set('status', el.status.value);
        // (필요시) 서버에서 필터 확장 시 함께 전달
        if (el.reporter.value) params.set('reporterId', el.reporter.value.trim());
        if (el.reported.value) params.set('reportedId', el.reported.value.trim());
        if (el.listingId.value) params.set('listingId', el.listingId.value.trim());

        const data = await fetchJson(`/api/admin/reports?${params.toString()}`);
        const rows = data.content || [];
        el.tbody.innerHTML = rows.map(r => `
      <tr>
        <td>${r.reportId}</td>
        <td><span class="badge ${r.status.toLowerCase()}">${r.status}</span></td>
        <td>${r.reasonCode}</td>
        <td>${r.reporterId}</td>
        <td>${r.reportedUserId}</td>
        <td>${r.listingId ?? ''}</td>
        <td>${new Date(r.createdAt).toLocaleString()}</td>
        <td><a class="btn" href="/admin/report-detail.html?id=${r.reportId}">열기</a></td>
      </tr>
    `).join('');

        el.pageInfo.textContent = `${(data.pageable?.pageNumber ?? page) + 1} / ${data.totalPages || 1}`;
        el.prev.disabled = data.first === true || (data.pageable?.pageNumber ?? page) <= 0;
        el.next.disabled = data.last === true || ((data.pageable?.pageNumber ?? page) >= (data.totalPages - 1));
    }

    el.btnSearch.addEventListener('click', () => { page = 0; load(); });
    el.prev.addEventListener('click', () => { if(page>0){ page--; load(); }});
    el.next.addEventListener('click', () => { page++; load(); });

    await load();
});
