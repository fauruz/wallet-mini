# Bugs found

## 1. Lost update on concurrent transfers (fixed)
- **Symptom:** 50 parallel transfers of 10,000 from a wallet holding 100,000 all returned 200; total balance across wallets changed between runs (money created and destroyed).
- **Reproduce:** `node tests/race-test.js <token> b@example.com` (N=50) on the unlocked version.
- **Root cause:** read-check-write on wallet balance without locking; concurrent requests read the same stale balance.
- **Fix:** `SELECT ... FOR UPDATE` (pessimistic lock) on both wallets, locked in ascending id order to avoid deadlock; top-up uses the same lock.
- **Verification:** 3 runs after fix, each exactly 10x200 and 40x400, total balance constant.