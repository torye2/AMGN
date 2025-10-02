// login_test_form_fixed.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 5, duration: '1m' };

export default function () {
    // 1) XSRF
    const t = http.get('http://localhost:8080/api/csrf');
    const token = (t.json() || {}).token || '';
    const body = 'id=rkdgywh&password=@a123456';

    // (바로 아래처럼, 로그인 요청 직후에 추가)
    const res = http.post('http://localhost:8080/login', body, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded', // 폼 로그인일 때
            'X-XSRF-TOKEN': token || ''
        },
        redirects: 0                // ★ 리다이렉트 따라가지 않기
    });

// ★ 디버그 로그 3줄
    console.log('login status =', res.status);
    console.log('Location =', res.headers['Location']);
    console.log('Set-Cookie =', res.headers['Set-Cookie']); // ← JSESSIONID=... 보이나?


    // 디버그: 최초 응답의 Set-Cookie 확인
    // console.log('Set-Cookie=', login.headers['Set-Cookie']);

    const okStatus = res.status >= 200 && res.status < 400;
    const gotSession = (res.headers['Set-Cookie'] || '').includes('JSESSIONID=');

    check(null, {
        'login ok status': () => okStatus,
        'JSESSIONID issued': () => gotSession
    }, { step:'login' });

    // 3) 이후 요청은 k6가 쿠키 자르에 저장해서 자동 전송 (단, Secure면 http에서 안 감)
    const me = http.get('http://localhost:8080/api/user/me');
    check(me, { 'me.loggedIn == true': r => (r.status===200 && (r.json()?.loggedIn===true)) }, { step:'login' });

    http.get('http://localhost:8080/product/all');
    sleep(1);
}
