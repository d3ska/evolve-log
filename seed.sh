#!/usr/bin/env bash
# Usage: COOKIE=<JSESSIONID value> bash seed.sh
set -euo pipefail

BASE="http://localhost:8080"
HDR_COOKIE="JSESSIONID=${COOKIE}"

call() {
  local method=$1 path=$2 body=${3:-}
  if [[ -n "$body" ]]; then
    curl -s -X "$method" "$BASE$path" \
      -H "Cookie: $HDR_COOKIE" \
      -H "Content-Type: application/json" \
      -d "$body"
  else
    curl -s -X "$method" "$BASE$path" \
      -H "Cookie: $HDR_COOKIE"
  fi
}

node_run() { node -e "$1" "${2:-}"; }

# ── Verify auth ───────────────────────────────────────────────────────────────
echo "Checking auth..."
ME=$(call GET /api/auth/me)
EMAIL=$(node_run "const d=JSON.parse(process.argv[1]); console.log(d.success ? d.data.email : 'NOT_AUTH')" "$ME")
if [[ "$EMAIL" == "NOT_AUTH" ]]; then
  echo "ERROR: Not authenticated. Check your COOKIE value."
  exit 1
fi
echo "Authenticated as: $EMAIL"

# ── Fetch exercise definitions ────────────────────────────────────────────────
echo "Fetching exercise definitions..."
DEFS=$(call GET "/api/exercises/definitions")

def_id() {
  local name=$1
  node_run "
const defs = JSON.parse(process.argv[1]).data;
const m = defs.find(d => d.name.toLowerCase() === '$name'.toLowerCase());
console.log(m ? m.id : '');
" "$DEFS"
}

ID_BENCH=$(def_id "Barbell Bench Press")
ID_INCLINE=$(def_id "Incline Dumbbell Press")
ID_CABLE_FLY=$(def_id "Cable Fly")
ID_OHP=$(def_id "Barbell Overhead Press")
ID_LATERAL=$(def_id "Lateral Raise")
ID_DEADLIFT=$(def_id "Barbell Deadlift")
ID_PULLUP=$(def_id "Pull-Up")
ID_ROW=$(def_id "Barbell Row")
ID_LAT=$(def_id "Lat Pulldown")
ID_CURL=$(def_id "Barbell Curl")
ID_SQUAT=$(def_id "Barbell Back Squat")
ID_LEG_PRESS=$(def_id "Leg Press")
ID_LEG_CURL=$(def_id "Leg Curl")
ID_RDL=$(def_id "Romanian Deadlift")

echo "Bench=$ID_BENCH  OHP=$ID_OHP  Deadlift=$ID_DEADLIFT  Squat=$ID_SQUAT"

if [[ -z "$ID_BENCH" || -z "$ID_SQUAT" || -z "$ID_DEADLIFT" ]]; then
  echo "ERROR: Could not resolve exercise definitions. Is the server running?"
  exit 1
fi

# ── Session creators ──────────────────────────────────────────────────────────
post_session() {
  local payload=$1
  RESP=$(call POST /api/workouts "$payload")
  node_run "const d=JSON.parse(process.argv[1]); console.log(d.success ? d.data.id : 'FAILED:'+JSON.stringify(d))" "$RESP"
}

push_body() {
  local date=$1 bench=$2 ohp=$3 notes=$4
  local incline
  incline=$(node_run "console.log(($bench * 0.65).toFixed(1))")
  node_run "
console.log(JSON.stringify({
  date: '${date}T10:00:00',
  durationMinutes: 65,
  notes: '$notes',
  exercises: [
    {name:'Barbell Bench Press',   sets:4,reps:6, weightKg:$bench,    position:0, exerciseDefinitionId:'$ID_BENCH',    rpe:8.0},
    {name:'Incline Dumbbell Press',sets:3,reps:10,weightKg:$incline,  position:1, exerciseDefinitionId:'$ID_INCLINE',  rpe:7.5},
    {name:'Cable Fly',             sets:3,reps:12,weightKg:15.0,      position:2, exerciseDefinitionId:'$ID_CABLE_FLY',rpe:7.0},
    {name:'Barbell Overhead Press',sets:4,reps:6, weightKg:$ohp,      position:3, exerciseDefinitionId:'$ID_OHP',      rpe:8.0},
    {name:'Lateral Raise',         sets:3,reps:15,weightKg:10.0,      position:4, exerciseDefinitionId:'$ID_LATERAL',  rpe:7.0}
  ]
}))
"
}

pull_body() {
  local date=$1 dl=$2 row=$3 notes=$4
  node_run "
console.log(JSON.stringify({
  date: '${date}T11:00:00',
  durationMinutes: 70,
  notes: '$notes',
  exercises: [
    {name:'Barbell Deadlift',sets:4,reps:5, weightKg:$dl,   position:0, exerciseDefinitionId:'$ID_DEADLIFT',rpe:8.5},
    {name:'Pull-Up',         sets:4,reps:8, weightKg:0,     position:1, exerciseDefinitionId:'$ID_PULLUP',  rpe:8.0},
    {name:'Barbell Row',     sets:4,reps:8, weightKg:$row,  position:2, exerciseDefinitionId:'$ID_ROW',     rpe:7.5},
    {name:'Lat Pulldown',    sets:3,reps:12,weightKg:60.0,  position:3, exerciseDefinitionId:'$ID_LAT',     rpe:7.0},
    {name:'Barbell Curl',    sets:3,reps:10,weightKg:35.0,  position:4, exerciseDefinitionId:'$ID_CURL',    rpe:7.0}
  ]
}))
"
}

leg_body() {
  local date=$1 sq=$2 rdl=$3 notes=$4
  node_run "
console.log(JSON.stringify({
  date: '${date}T09:00:00',
  durationMinutes: 75,
  notes: '$notes',
  exercises: [
    {name:'Barbell Back Squat',  sets:4,reps:6, weightKg:$sq,   position:0, exerciseDefinitionId:'$ID_SQUAT',    rpe:8.5},
    {name:'Romanian Deadlift',   sets:3,reps:10,weightKg:$rdl,  position:1, exerciseDefinitionId:'$ID_RDL',      rpe:7.5},
    {name:'Leg Press',           sets:3,reps:12,weightKg:120.0, position:2, exerciseDefinitionId:'$ID_LEG_PRESS',rpe:7.0},
    {name:'Leg Curl',            sets:3,reps:12,weightKg:40.0,  position:3, exerciseDefinitionId:'$ID_LEG_CURL', rpe:7.0}
  ]
}))
"
}

# ── Date helper (works on Windows Git Bash / Linux) ───────────────────────────
days_ago() {
  node_run "
const d = new Date();
d.setDate(d.getDate() - $1);
console.log(d.toISOString().slice(0,10));
"
}

echo ""
echo "Creating 24 sessions across 8 weeks (Push / Pull / Legs)..."

create() {
  local label=$1 body=$2
  printf "  %-30s " "$label"
  ID=$(post_session "$body")
  echo "$ID"
}

# Week -8
create "Week 8 ago – Push" "$(push_body "$(days_ago 56)" 90    55   "Felt strong")"
create "Week 8 ago – Pull" "$(pull_body "$(days_ago 54)" 140   70   "")"
create "Week 8 ago – Legs" "$(leg_body  "$(days_ago 52)" 100   75   "")"

# Week -7
create "Week 7 ago – Push" "$(push_body "$(days_ago 49)" 92.5  57.5 "")"
create "Week 7 ago – Pull" "$(pull_body "$(days_ago 47)" 142.5 72.5 "")"
create "Week 7 ago – Legs" "$(leg_body  "$(days_ago 45)" 102.5 77.5 "")"

# Week -6
create "Week 6 ago – Push" "$(push_body "$(days_ago 42)" 92.5  57.5 "Bench felt easier")"
create "Week 6 ago – Pull" "$(pull_body "$(days_ago 40)" 145   75   "")"
create "Week 6 ago – Legs" "$(leg_body  "$(days_ago 38)" 105   80   "")"

# Week -5
create "Week 5 ago – Push" "$(push_body "$(days_ago 35)" 95    60   "New bench PR")"
create "Week 5 ago – Pull" "$(pull_body "$(days_ago 33)" 147.5 75   "")"
create "Week 5 ago – Legs" "$(leg_body  "$(days_ago 31)" 107.5 82.5 "")"

# Week -4
create "Week 4 ago – Push" "$(push_body "$(days_ago 28)" 95    60   "")"
create "Week 4 ago – Pull" "$(pull_body "$(days_ago 26)" 150   77.5 "Deadlift PR")"
create "Week 4 ago – Legs" "$(leg_body  "$(days_ago 24)" 110   85   "")"

# Week -3
create "Week 3 ago – Push" "$(push_body "$(days_ago 21)" 97.5  62.5 "")"
create "Week 3 ago – Pull" "$(pull_body "$(days_ago 19)" 150   80   "")"
create "Week 3 ago – Legs" "$(leg_body  "$(days_ago 17)" 112.5 85   "")"

# Week -2
create "Week 2 ago – Push" "$(push_body "$(days_ago 14)" 97.5  62.5 "")"
create "Week 2 ago – Pull" "$(pull_body "$(days_ago 12)" 152.5 80   "")"
create "Week 2 ago – Legs" "$(leg_body  "$(days_ago 10)" 115   87.5 "Squat PR")"

# Week -1
create "This week  – Push" "$(push_body "$(days_ago 5)"  100   65   "100kg bench milestone!")"
create "This week  – Pull" "$(pull_body "$(days_ago 3)"  155   82.5 "")"
create "This week  – Legs" "$(leg_body  "$(days_ago 1)"  117.5 90   "")"

echo ""
echo "Done! 24 sessions created."
echo "Open the Progress tab → 'Last 8 weeks' to see volume and overload data."
