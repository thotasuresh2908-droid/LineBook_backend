#!/usr/bin/env bash
# End-to-end check of the running backend:
#   create a customer -> their bill appears -> collect (partial) -> replay -> receipt -> agents overview.
# Usage: ./smoke-test.sh                       (backend at http://localhost:9000)
#        BASE_URL=http://host:9000 ./smoke-test.sh
set -euo pipefail

BASE="${BASE_URL:-http://localhost:9000}"
JSON=(-H 'Content-Type: application/json')
say() { printf '\n== %s\n' "$*"; }
field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p" | head -n1; }
status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

say "Health";            curl -fsS "$BASE/actuator/health"; echo
say "Packages";          curl -fsS "$BASE/api/packages"; echo
say "Agents";            curl -fsS "$BASE/api/agents"; echo

say "Create a customer (active, recurring -> this month's bill is created in the same transaction)"
CUSTOMER=$(curl -fsS -X POST "$BASE/api/customers" "${JSON[@]}" \
  -d '{"name":"Smoke Test","phone":"9000000001","area":"Ameerpet","agentId":"ag-01","packageId":"pkg-family","billingDay":28}')
echo "$CUSTOMER"
CID=$(echo "$CUSTOMER" | field id)
BILL="bl-$(date +%Y-%m)-$CID"

say "Their bill";                       curl -fsS "$BASE/api/bills/$BILL"; echo
say "Their bills across cycles";        curl -fsS "$BASE/api/bills/by-customer/$CID"; echo
say "Bill picker finds it";             curl -fsS "$BASE/api/bills/collectable?q=Smoke"; echo

say "Collect 100 (partial)"
COLLECTION=$(curl -fsS -X POST "$BASE/api/collections" "${JSON[@]}" \
  -d "{\"billId\":\"$BILL\",\"amount\":100,\"method\":\"upi\",\"collectionId\":\"smoke-$CID\"}")
echo "$COLLECTION"
COLLECTION_ID=$(echo "$COLLECTION" | field id)

say "Same request again (replay) must not collect twice"
curl -fsS -X POST "$BASE/api/collections" "${JSON[@]}" \
  -d "{\"billId\":\"$BILL\",\"amount\":100,\"method\":\"upi\",\"collectionId\":\"smoke-$CID\"}"; echo

say "Bill is now partial (paid 100, pending 199)"
curl -fsS "$BASE/api/bills/$BILL"; echo

say "Receipt (agent name is filled in from the agent)"
curl -fsS "$BASE/api/collections/$COLLECTION_ID/receipt"; echo

say "Today"
curl -fsS "$BASE/api/collections/summary"; echo
curl -fsS "$BASE/api/bills/summary"; echo
curl -fsS "$BASE/api/collections/daily?days=7"; echo

say "Agents page"
curl -fsS "$BASE/api/agents/overview"; echo

say "Collecting more than is pending must be refused (expect 422)"
CODE=$(status -X POST "$BASE/api/collections" "${JSON[@]}" -d "{\"billId\":\"$BILL\",\"amount\":500}")
echo "POST /api/collections (500 > 199 pending) -> $CODE"
[ "$CODE" = "422" ] || { echo "expected 422"; exit 1; }

say "A customer with an unknown agent must be refused (expect 400)"
CODE=$(status -X POST "$BASE/api/customers" "${JSON[@]}" \
  -d '{"name":"Nobody","phone":"9000000002","area":"Ameerpet","agentId":"ag-99"}')
echo "POST /api/customers (agent ag-99) -> $CODE"
[ "$CODE" = "400" ] || { echo "expected 400"; exit 1; }

echo; echo "Smoke test passed."
