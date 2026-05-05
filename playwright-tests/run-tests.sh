#!/bin/sh

cat << 'BANNER'

   ___ _           _            _       _       _                  _   _
  / __| |_  ___ __| |_____ _  _| |_    /_\ _  _| |_ ___ _ __  __ _| |_(_)___ _ _
 | (__| ' \/ -_) _| / / _ \ || |  _|  / _ \ || |  _/ _ \ '  \/ _` |  _| / _ \ ' \
  \___|_||_\___\__|_\_\___/\_,_|\__| /_/ \_\_,_|\__\___/_|_|_\__,_|\__|_\___/_||_|

  PrintHelm · Deployment Checkout
  ──────────────────────────────────────────────────────────────
BANNER

if [ -f /run/secrets/PLAYWRIGHT_USERNAME ]; then
  export PLAYWRIGHT_USERNAME=$(cat /run/secrets/PLAYWRIGHT_USERNAME)
fi

if [ -f /run/secrets/PLAYWRIGHT_PASSWORD ]; then
  export PLAYWRIGHT_PASSWORD=$(cat /run/secrets/PLAYWRIGHT_PASSWORD)
fi

TIMEOUT=300

wait_for() {
  NAME=$1
  URL=$2
  CHECK=$3
  ELAPSED=0
  echo "Waiting for ${NAME}..."
  until eval "$CHECK" > /dev/null 2>&1; do
    if [ $ELAPSED -ge $TIMEOUT ]; then
      echo "Timed out waiting for ${NAME}"
      exit 1
    fi
    sleep 5
    ELAPSED=$((ELAPSED + 5))
  done
  echo "${NAME} is ready"
}

echo "[runner] Phase 1 — Waiting for services"
wait_for "backend" \
  "http://printhelm-service-develop:8080/actuator/health" \
  "curl -sf http://printhelm-service-develop:8080/actuator/health | grep -q '\"UP\"'"

wait_for "frontend" \
  "http://printhelm-ui-develop:80" \
  "curl -sf http://printhelm-ui-develop:80/"

echo ""
echo "[runner] Phase 2 — Running tests"
npx playwright test "$@"
TEST_EXIT=$?
echo ""
echo "[runner] Test runner exited with code ${TEST_EXIT}"

echo ""
echo "[runner] Phase 3 — Uploading traces"
node /app/scripts/upload-traces.js

echo ""
echo "[runner] Phase 4 — Sending report"
node /app/scripts/send-report.js

echo ""
echo "[runner] Done"
exit $TEST_EXIT
