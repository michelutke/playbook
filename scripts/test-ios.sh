#!/usr/bin/env bash
# Run iOS XCTest smoke tests with a live Ktor backend.
# Requires: docker compose, python3, xcpretty (gem install xcpretty)
# Note: stop any local dev server on port 8080 before running.
set -euo pipefail

BACKEND_URL="http://localhost:8080"
PG="docker compose exec -T db psql -U playbook -d playbook_ios_test"

cleanup() {
  echo "Stopping backend (pid $KTOR_PID)..."
  kill "$KTOR_PID" 2>/dev/null || true
}
trap cleanup EXIT

echo "==> Starting database..."
docker compose up -d db
for i in $(seq 1 15); do
  docker compose exec db pg_isready -U playbook && break || sleep 2
done

docker compose exec db psql -U playbook -d playbook_e2e -c "DROP DATABASE IF EXISTS playbook_ios_test;"
docker compose exec db psql -U playbook -d playbook_e2e -c "CREATE DATABASE playbook_ios_test OWNER playbook;"

echo "==> Building and starting Ktor backend..."
./gradlew :server:installDist -q

DATABASE_URL="jdbc:postgresql://localhost:5433/playbook_ios_test" \
DATABASE_USER="playbook" \
DATABASE_PASSWORD="playbook" \
JWT_SECRET="ios-local-test-secret" \
PORT="8080" \
server/build/install/server/bin/server &
KTOR_PID=$!

echo "==> Waiting for backend..."
for i in $(seq 1 30); do
  curl -sf "$BACKEND_URL/health" && echo " ready" && break
  printf "."; sleep 2
done

echo "==> Seeding test data..."

# Register user — response: {"token":"...","userId":"..."}
RESPONSE=$(curl -sf -X POST "$BACKEND_URL/auth/register" \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@playbook.test","password":"testpassword","displayName":"Test Coach"}')
USER_ID=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['userId'])")

CLUB_ID=$(python3 -c "import uuid; print(uuid.uuid4())")
TEAM_ID=$(python3 -c "import uuid; print(uuid.uuid4())")
EVENT_ID=$(python3 -c "import uuid; print(uuid.uuid4())")

$PG -c "INSERT INTO clubs (id, name, sport_type, status, created_at, updated_at) VALUES ('$CLUB_ID', 'Test Club', 'Football', 'active', now(), now());"
$PG -c "INSERT INTO club_managers (id, club_id, user_id, invited_email, status, added_by, added_at) VALUES (gen_random_uuid(), '$CLUB_ID', '$USER_ID', 'test@playbook.test', 'active', '$USER_ID', now());"
$PG -c "INSERT INTO teams (id, club_id, name, status, created_at, updated_at) VALUES ('$TEAM_ID', '$CLUB_ID', 'Test Team', 'active', now(), now());"
$PG -c "INSERT INTO team_memberships (id, team_id, user_id, role, added_by, joined_at) VALUES (gen_random_uuid(), '$TEAM_ID', '$USER_ID', 'coach', '$USER_ID', now());"
$PG -c "INSERT INTO events (id, title, type, start_at, end_at, created_by, status, created_at, updated_at) VALUES ('$EVENT_ID', 'Test Training', 'training', now() + interval '1 day', now() + interval '1 day' + interval '2 hours', '$USER_ID', 'active', now(), now());"
$PG -c "INSERT INTO event_teams (event_id, team_id) VALUES ('$EVENT_ID', '$TEAM_ID');"

echo "==> Seeded: user + club manager + team + coach + event"

echo "==> Running iOS XCTest..."
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=latest' \
  -resultBundlePath TestResults-ios.xcresult \
  CODE_SIGNING_ALLOWED=NO \
  | xcpretty
