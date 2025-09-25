function getCookie(name){
    return document.cookie.split('; ').find(c => c.startsWith(name+'='))?.split('=')[1];
}

async function fetchJson(url, options={}){
    const res = await fetch(url, { credentials:'include', ...options });
    if(res.ok){
        const ct = res.headers.get('content-type')||'';
        return ct.includes('application/json') ? res.json() : res.text();
    }
    let msg = `오류 (HTTP ${res.status})`;
    try{
        const ct = res.headers.get('content-type')||'';
        if(ct.includes('application/json')){
            const j = await res.json();
            if(j?.message) msg = j.message;
        }else{
            const t = await res.text();
            if(t) msg = t;
        }
    }catch{}
    const e = new Error(msg); e.status = res.status; throw e;
}

// 로그인/권한 확인 (ROLE_ADMIN)
async function requireAdmin(){
    const me = await fetchJson('/api/user/me');
    const roles = me.roles || me.authorities || [];
    const isAdmin = Array.isArray(roles) && roles.some(r => (''+r).includes('ADMIN'));
    if(!isAdmin){
        alert('관리자만 접근 가능합니다.');
        location.href = '/';
        throw new Error('ADMIN_REQUIRED');
    }
    return me;
}

function qs(sel, root=document){ return root.querySelector(sel); }
function qsa(sel, root=document){ return Array.from(root.querySelectorAll(sel)); }
