const BASE = 'http://localhost:8080';
const [token, toEmail] = process.argv.slice(2);
const key = crypto.randomUUID();

const send = (amount, k = key) =>
    fetch(`${BASE}/api/transfers`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
            'Idempotency-Key': k,
        },
        body: JSON.stringify({ toEmail, amount }),
    }).then(async (r) => ({ status: r.status, body: await r.json() }));

// 20 request cùng một key, cùng lúc
const results = await Promise.all(Array.from({ length: 20 }, () => send(10000)));

const statusCount = {};
let fresh = 0;
let replayed = 0;
for (const r of results) {
    statusCount[r.status] = (statusCount[r.status] || 0) + 1;
    if (r.status === 200) r.body.replayed ? replayed++ : fresh++;
}
console.log('Status counts:', statusCount);
console.log('Fresh:', fresh, 'Replayed:', replayed);

// Cùng key nhưng số tiền khác
const conflict = await send(20000);
console.log('Same key, different amount ->', conflict.status);