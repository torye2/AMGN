document.addEventListener('DOMContentLoaded', async () => {
    await requireAdmin();

    const id = Number(new URLSearchParams(location.search).get('id'));
    if(!id){ alert('잘못된 접근입니다.'); location.href='/admin/reports.html'; return; }

    const xsrf = getCookie('XSRF-TOKEN');

    async function load(){
        const d = await fetchJson(`/api/admin/reports/${id}`);

        qs('#meta').innerHTML = `
      <div class="kv" style="margin-top:4px">
        <div class="k">Report ID</div><div class="v"><b>#${d.reportId}</b></div>
        <div class="k">Status</div><div class="v"><span class="badge ${d.status.toLowerCase()}">${d.status}</span></div>
        <div class="k">Reason</div><div class="v">${d.reasonCode}${d.reasonText ? ` (${d.reasonText})` : ''}</div>
        <div class="k">Reporter</div><div class="v">${d.reporterId}</div>
        <div class="k">Reported</div><div class="v">${d.reportedUserId}</div>
        <div class="k">Listing</div><div class="v">${d.listingId ?? ''}</div>
        <div class="k">Created</div><div class="v">${new Date(d.createdAt).toLocaleString()}</div>
        <div class="k">Updated</div><div class="v">${new Date(d.updatedAt).toLocaleString()}</div>
      </div>
    `;

        qs('#desc').textContent = d.description || '';

        const ev = d.evidence || [];
        qs('#evidence').innerHTML = ev.length ? ev.map(e =>
            `<a href="${e.filePath}" target="_blank" rel="noreferrer noopener"><img src="${e.filePath}" alt=""></a>`
        ).join('') : '<div class="muted">첨부된 증거가 없습니다.</div>';

        const acts = d.actions || [];
        qs('#actions').innerHTML = acts.length ? acts.map(a => `
      <li>
        <b>${a.actionType}</b> by ${a.actorUserId} · ${new Date(a.createdAt).toLocaleString()}<br/>
        ${a.comment ? `<span>${a.comment}</span>` : ''}
      </li>
    `).join('') : '<li class="muted">처리 로그가 없습니다.</li>';
    }

    // 상태 전환 버튼
    qsa('[data-act]').forEach(btn => {
        btn.addEventListener('click', async () => {
            const actionType = btn.dataset.act;
            try{
                await fetchJson(`/api/admin/reports/${id}/actions`, {
                    method:'POST',
                    headers:{ 'Content-Type':'application/json', ...(xsrf ? {'X-XSRF-TOKEN': xsrf} : {}) },
                    body: JSON.stringify({ actionType, comment: actionType==='ASSIGN'?'검토 시작':'' })
                });
                await load();
                alert('상태가 업데이트되었습니다.');
            }catch(e){ alert(e.message||'상태 변경 실패'); }
        });
    });

    // 정지 실행
    qs('#btnSuspend').addEventListener('click', async () => {
        const days = Number(qs('#suspendDays').value || 0);
        const reasonText = qs('#suspendReason').value || null;
        if(!confirm(days===0 ? '영구 정지를 실행할까요?' : `${days}일 정지를 실행할까요?`)) return;
        try{
            await fetchJson(`/api/admin/reports/${id}/suspend`, {
                method:'POST',
                headers:{ 'Content-Type':'application/json', ...(xsrf ? {'X-XSRF-TOKEN': xsrf} : {}) },
                body: JSON.stringify({ days, reasonText })
            });
            await load();
            alert('정지 처분이 완료되었습니다.');
        }catch(e){ alert(e.message||'정지 실패'); }
    });

    // 메모 추가
    qs('#btnNote').addEventListener('click', async () => {
        const note = qs('#note').value.trim();
        if(!note) return;
        try{
            await fetchJson(`/api/admin/reports/${id}/actions`, {
                method:'POST',
                headers:{ 'Content-Type':'application/json', ...(xsrf ? {'X-XSRF-TOKEN': xsrf} : {}) },
                body: JSON.stringify({ actionType:'NOTE', comment: note })
            });
            qs('#note').value = '';
            await load();
        }catch(e){ alert(e.message||'메모 추가 실패'); }
    });

    await load();
});
