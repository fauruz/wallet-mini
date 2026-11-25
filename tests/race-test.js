const BASE = 'http://localhost:8080';
const [token, toEmail] = process.argv.slice(2);
const N = 50;
const AMOUNT = 10000;

const results = await Promise.all(
    Array.from({ length: N }, () =>
        fetch(`${BASE}/api/transfers`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({ toEmail, amount: AMOUNT }),
        }).then((r) => r.status)
    )
);

const count = {};
for (const s of results) count[s] = (count[s] || 0) + 1;
console.log('Status counts:', count);