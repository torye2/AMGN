import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 5, duration: '1m' };
const base = 'http://localhost:8080';

function itemsFrom(res) {
    const j = res.json();
    if (!j) return [];
    if (Array.isArray(j)) return j;            // List<ListingDto>
    if (Array.isArray(j.content)) return j.content; // Page<ListingDto>
    return [];
}
function numericId(item) {
    const cand = item?.listingId ?? item?.id;  // 현 DTO면 listingId가 정답
    const n = Number(cand);
    return Number.isInteger(n) ? n : null;
}

export default function () {
    const list = http.get(`${base}/product/list?page=0&size=5&sort=listingId,DESC`,
        { tags: { name: 'GET /product/list' }});
    check(list, { 'list 200': r => r.status === 200 });

    const items = itemsFrom(list);
    const ids = items.map(numericId).filter(v => Number.isInteger(v) && v > 0);

    if (!ids.length) { sleep(1); return; } // 유효 id가 없으면 상세 스킵(404 방지)

    const id = ids[Math.floor(Math.random() * ids.length)];
    const show = http.get(`${base}/product/${id}`, { tags: { name: 'GET /product/{id}' } });
    check(show, { 'detail 200': r => r.status === 200 });

    sleep(1);
}
