#!/usr/bin/env bash
# E2E full-chain acceptance for the week's features (self-cleaning, re-runnable).
#
#   链: 登录 -> 角色/秘书/支部/成员 -> 发布活动 -> 报名(+重复拒) -> 签到(WKT POINT+重复拒)
#    -> 限身份活动(未登记拒) -> 支管发起身份变更 -> 支管自审拒 -> 超管审批生效
#    -> profile 现身份 -> 生效后报名通过 -> 思想汇报(自动派审/审批/越权不存在)
#    -> 学习(建课/85.5 精度/upsert/汇总) -> 越权面 -> branchId 逃逸字段被忽略
#
# 覆盖工单: #53 报名 #35 签到 #44 身份体系 #61 身份维度报名 #33 思想汇报
#           #32 学习 #36 DTO 化（越权收紧）
# 已知: @PreAuthorize 拒绝曾返回 500，#63 修复后为 403，本脚本断言 403。
# 用法: 应用需在 :8080 运行（mvn spring-boot:run）；superadmin 密码为默认 123456。
#       请求载荷保持 ASCII（Windows 代码页安全）；中文断言匹配响应字节。
set -uo pipefail
BASE=http://localhost:8080
PASS=0; FAIL=0
ok()  { PASS=$((PASS+1)); echo "  OK  $1"; }
bad() { FAIL=$((FAIL+1)); echo "  NG  $1 -> $2"; }

# ---- DB creds from project secret file ----
eval "$(python -c "
import io
for line in io.open('src/main/resources/application-secret.yaml', encoding='utf-8'):
    line=line.strip()
    if line.startswith('DB_'):
        k,_,v=line.partition(':'); v=v.strip().strip('\"').strip(\"'\")
        print(\"export %s='%s'\" % (k.strip(), v.replace(chr(39),chr(39)+chr(92)+chr(39)+chr(39))))
")"
[ -n "${DB_USERNAME:-}" ] || { echo "FATAL: no DB creds"; exit 1; }
export MYSQL_PWD="$DB_PASSWORD"

# ---- pre-clean leftovers of previous runs (idempotent start) ----
python - <<'PY'
import io, os, subprocess
sql = """set foreign_key_checks=0;
delete sr from tb_sign_record sr join tb_activity a on a.id=sr.activity_id where a.title like 'E2E%';
delete s from tb_activity_signup s join tb_activity a on a.id=s.activity_id where a.title like 'E2E%';
delete ai from tb_activity_identity ai join tb_activity a on a.id=ai.activity_id where a.title like 'E2E%';
delete d from tb_activity_detail d join tb_activity a on a.id=d.activity_id where a.title like 'E2E%';
delete from tb_activity where title like 'E2E%';
delete from tb_thought_report where title like 'E2E%';
delete r from tb_study_record r join tb_study c on c.id=r.course_id where c.name like 'E2E%';
delete from tb_study where name like 'E2E%';
delete from tb_identity_change where reason like 'E2E%' or review_remark like 'E2E%';
delete from tb_user_detail where user_id in (select id from tb_user where username like 'e2e_%');
delete from tb_user_role where user_id in (select id from tb_user where username like 'e2e_%');
delete from tb_user where username like 'e2e_%';
delete from tb_branch where branch_name like 'E2E%';
delete from tb_role where description like 'E2E%';
delete from tb_activity_type where name like 'E2E%';
set foreign_key_checks=1;"""
env = {k: v for k, v in os.environ.items() if k == "MYSQL_PWD"}
u = None
import re
creds = {}
for line in io.open("src/main/resources/application-secret.yaml", encoding="utf-8"):
    m = re.match(r"^(DB_USERNAME|DB_PASSWORD):\s*(.+)$", line.strip())
    if m:
        creds[m.group(1)] = m.group(2).strip().strip('"').strip("'")
r = subprocess.run(["mysql", "-h", "127.0.0.1", "-u", creds["DB_USERNAME"], "--protocol=TCP",
                    "--default-character-set=utf8mb4", "policial_party_manager", "-e", sql],
                   capture_output=True, text=True)
if r.returncode != 0:
    print("WARN pre-clean:", r.stderr.strip()[:160])
PY

g() { python -c '
import sys, json
d = json.load(sys.stdin); cur = d
for k in sys.argv[1].split("."):
    if cur is None: break
    if isinstance(cur, dict): cur = cur.get(k)
    elif isinstance(cur, list):
        try: cur = cur[int(k)]
        except Exception: cur = None
print("" if cur is None else cur)
' "$1" 2>/dev/null || echo ""; }

api() { local tok="${1:-}" m="$2" path="$3" body="${4:-}"
  local a=(-s -X "$m" "$BASE$path" -H 'Content-Type: application/json')
  [ -n "$tok" ] && a+=(-H "Authorization: Bearer $tok")
  [ -n "$body" ] && a+=(--data-binary "$body"); curl "${a[@]}"; }
stcode() { curl -s -o /dev/null -w '%{http_code}' -X "${2:-GET}" "$BASE$3" -H 'Content-Type: application/json' -H "Authorization: Bearer ${1:-}" ${4:+--data-binary "$4"}; }
q() { mysql -h 127.0.0.1 -u"$DB_USERNAME" --protocol=TCP --default-character-set=utf8mb4 policial_party_manager "$@"; }
PW='"password":"123456"'

echo "== 0) superadmin login =="
T_SUPER=$(api "" POST /api/v1/auth/login/userpass "{\"username\":\"superadmin\",$PW}" | g data.accessToken)
[ -n "$T_SUPER" ] && ok login || { echo FATAL: cannot login; exit 1; }

echo "== 1) roles / secretary / branch / student =="
api "$T_SUPER" POST /api/v4/admin/roles '{"roleCode":"branch_admin","description":"E2E branch role","permissionNames":[]}' >/dev/null
api "$T_SUPER" POST /api/v4/admin/roles '{"roleCode":"student","description":"E2E student role","permissionNames":[]}' >/dev/null
R=$(api "$T_SUPER" GET /api/v4/admin/roles)
BA=$(printf '%s' "$R" | python -c 'import sys,json;print(next(x["id"] for x in json.load(sys.stdin)["data"] if x["roleCode"].lower().replace("role_","")=="branch_admin"))' 2>/dev/null)
SR=$(printf '%s' "$R" | python -c 'import sys,json;print(next(x["id"] for x in json.load(sys.stdin)["data"] if x["roleCode"].lower().replace("role_","")=="student"))' 2>/dev/null)
[ -n "$BA" ] && [ -n "$SR" ] && ok "roles ba=$BA stu=$SR" || { bad roles "$R"; exit 1; }
api "$T_SUPER" POST /api/v4/admin/users "{\"username\":\"e2e_sec\",\"realName\":\"E2ESec\",\"studentId\":\"E2S001\",\"roleId\":$BA,$PW}" >/dev/null
SEC_ID=$(api "$T_SUPER" GET "/api/v4/admin/users/page?keyword=e2e_sec" | g data.records.0.id)
[ -n "$SEC_ID" ] && ok "secretary id=$SEC_ID" || { bad secretary "?"; exit 1; }
api "$T_SUPER" POST /api/v4/admin/branches "{\"branchName\":\"E2E-Test-Branch\",\"college\":\"TEST\",\"secretaryId\":$SEC_ID,\"description\":\"e2e\"}" >/dev/null
BR_ID=$(api "$T_SUPER" GET "/api/v4/admin/branches/page?keyword=E2E-Test-Branch" | g data.records.0.id)
api "$T_SUPER" POST /api/v4/admin/users "{\"username\":\"e2e_stu\",\"realName\":\"E2EStu\",\"studentId\":\"E2T001\",\"roleId\":$SR,\"branchId\":$BR_ID,\"college\":\"TEST\",\"grade\":\"2023\",$PW}" >/dev/null
STU_ID=$(api "$T_SUPER" GET "/api/v4/admin/users/page?keyword=e2e_stu" | g data.records.0.id)
[ -n "$BR_ID" ] && [ -n "$STU_ID" ] && ok "branch=$BR_ID student=$STU_ID" || { bad "branch/stu" "br=$BR_ID stu=$STU_ID"; exit 1; }
T_STU=$(api "" POST /api/v1/auth/login/userpass "{\"username\":\"e2e_stu\",$PW}" | g data.accessToken)
T_SEC=$(api "" POST /api/v1/auth/login/userpass "{\"username\":\"e2e_sec\",$PW}" | g data.accessToken)
[ -n "$T_STU" ] && [ -n "$T_SEC" ] && ok "student/secretary login" || { bad login "no token"; exit 1; }

echo "== 2) activity publish -> signup -> sign-in (#53/#35) =="
api "$T_SUPER" POST /api/v4/admin/activity-types '{"name":"E2EDay","needSign":1,"countToFile":1}' >/dev/null
AT_ID=$(api "$T_SUPER" GET /api/v4/admin/activity-types | python -c 'import sys,json;print(next(x["id"] for x in json.load(sys.stdin)["data"] if x["name"]=="E2EDay"))' 2>/dev/null)
NOWT=$(date +%Y-%m-%dT%H:%M:%S); START=$(date -d "+10 minutes" +%Y-%m-%dT%H:%M:%S); END=$(date -d "+3 hours" +%Y-%m-%dT%H:%M:%S)
api "$T_SUPER" POST /api/v4/admin/activities "{\"title\":\"E2E-Planting\",\"type\":$AT_ID,\"branchId\":$BR_ID,\"startTime\":\"$START\",\"endTime\":\"$END\",\"signStart\":\"$NOWT\",\"signEnd\":\"$END\",\"maxParticipants\":5,\"status\":1,\"description\":\"e2e\"}" >/dev/null
ACT_ID=$(api "$T_SUPER" GET "/api/v4/admin/activities/page?keyword=E2E-Planting" | g data.records.0.id)
[ -n "$ACT_ID" ] && ok "published id=$ACT_ID" || { bad publish "?"; exit 1; }
SU1=$(api "$T_STU" POST "/api/v4/activities/$ACT_ID/signup" '{}')
[ "$(printf '%s' "$SU1" | g code)" = "200" ] && ok "signup ok" || bad signup "$SU1"
printf '%s' "$(api "$T_STU" POST "/api/v4/activities/$ACT_ID/signup" '{}')" | grep -q "已报名" && ok "dup signup rejected" || bad "dup signup" "?"
SG1=$(api "$T_STU" POST "/api/v4/activities/$ACT_ID/sign" '{"device":"web","location":"POINT(103.96 30.57)","remark":"e2e"}')
[ "$(printf '%s' "$SG1" | g data.location)" = "POINT(103.96 30.57)" ] && ok "sign-in ok, POINT WKT round-trip (#35)" || bad "sign/POINT" "$SG1"
printf '%s' "$(api "$T_STU" POST "/api/v4/activities/$ACT_ID/sign" '{"device":"web"}')" | grep -q "已签到" && ok "dup sign rejected" || bad "dup sign" "?"
[ "$(api "$T_SUPER" GET "/api/v4/admin/activities/$ACT_ID/signs/page" | g data.total)" = "1" ] && ok "sign page total=1" || bad "sign page" "?"

echo "== 3) identity-gated signup (#44 -> #61) =="
api "$T_SUPER" POST /api/v4/admin/activities "{\"title\":\"E2E-PartyClass\",\"type\":$AT_ID,\"branchId\":$BR_ID,\"startTime\":\"$START\",\"endTime\":\"$END\",\"maxParticipants\":2,\"status\":1,\"description\":\"probationary only\"}" >/dev/null
ACT2_ID=$(api "$T_SUPER" GET "/api/v4/admin/activities/page?keyword=E2E-PartyClass" | g data.records.0.id)
q -e "insert into tb_activity_identity(activity_id,target_identity,grade,college) select $ACT2_ID, id, 2023, 'TEST' from tb_identity where identity_name='预备党员';" && ok "eligibility row seeded" || bad "seed row" "sql fail"
printf '%s' "$(api "$T_STU" POST "/api/v4/activities/$ACT2_ID/signup" '{}')" | grep -q "未登记" && ok "unregistered identity -> actionable msg (#61)" || bad "unregistered" "?"
CHG=$(api "$T_SEC" POST /api/v4/identity-changes "{\"userId\":$STU_ID,\"toIdentity\":5,\"reason\":\"E2E education done\"}")
CHG_ID=$(printf '%s' "$CHG" | g data.id)
[ "$(printf '%s' "$CHG" | g data.status)" = "1" ] && ok "sec applied, pending" || bad apply "$CHG"
SA=$(api "$T_SEC" POST "/api/v4/identity-changes/$CHG_ID/review" '{"pass":true}')
[ "$(stcode "$T_SEC" POST "/api/v4/identity-changes/$CHG_ID/review" '{"pass":true}')" = "403" ] && ok "sec self-approve denied 403 (#63 fixed)" || bad "self-approve" "$SA"
APV=$(api "$T_SUPER" POST "/api/v4/identity-changes/$CHG_ID/review" '{"pass":true,"remark":"E2E approved"}')
[ "$(printf '%s' "$APV" | g data.status)" = "2" ] && ok "super approved status=2" || bad approve "$APV"
printf '%s' "$(api "$T_STU" GET /api/v4/user/profile)" | grep -q "预备党员" && ok "profile identity written back (#36/#44)" || bad profile "?"
[ "$(api "$T_STU" POST "/api/v4/activities/$ACT2_ID/signup" '{}' | g code)" = "200" ] && ok "signup passes after identity (#61)" || bad "post-approve signup" "?"

echo "== 4) thought report (#33) =="
TR=$(api "$T_STU" POST /api/v4/thought-reports '{"title":"E2E Q3 Report","content":"party theory","file":""}')
TR_ID=$(printf '%s' "$TR" | g data.id)
[ "$(printf '%s' "$TR" | g data.reviewerId)" = "$SEC_ID" ] && ok "auto-assigned to secretary" || bad "auto-assign" "$TR"
[ "$(api "$T_SEC" GET "/api/v4/review/thought-reports/$TR_ID" | g data.statusName)" = "待审阅" ] && ok "sec sees detail" || bad detail "?"
[ "$(api "$T_SEC" POST "/api/v4/review/thought-reports/$TR_ID/review" '{"pass":true,"remark":"good"}' | g data.status)" = "2" ] && ok "sec approved" || bad approve "?"
printf '%s' "$(api "$T_SEC" POST /api/v4/review/thought-reports/999999/review '{"pass":true}')" | grep -q "不存在" && ok "foreign order not-found" || bad not-found "?"

echo "== 5) study (#32) =="
CRS=$(api "$T_SUPER" POST /api/v4/admin/study-courses '{"name":"E2E-Hist","type":"required"}')
CRS_ID=$(printf '%s' "$CRS" | g data.id)
[ -n "$CRS_ID" ] && ok "course id=$CRS_ID" || bad course "$CRS"
REC=$(api "$T_SUPER" POST /api/v4/admin/study-records "{\"userId\":$STU_ID,\"courseId\":$CRS_ID,\"studyTime\":\"02:30:00\",\"score\":85.5}")
printf '%s' "$REC" | grep -qE '"score": *85\.5' && ok "score 85.5 round-trip" || bad precision "$REC"
api "$T_SEC" POST /api/v4/admin/study-records "{\"userId\":$STU_ID,\"courseId\":$CRS_ID,\"studyTime\":\"03:00:00\",\"score\":92}" >/dev/null
SUM=$(api "$T_STU" GET /api/v4/user/study-records/mine/summary)
[ "$(printf '%s' "$SUM" | g data.recordCount)" = "1" ] && [ "$(printf '%s' "$SUM" | g data.totalMinutes)" = "180" ] && ok "upsert + summary 180min" || bad "upsert/summary" "$SUM"

echo "== 6) authz surfaces (403 since #63 fix) =="
[ "$(stcode "$T_STU" GET /api/v4/admin/study-records/page)" = "403" ] && ok "student -> admin endpoint 403" || bad "student deny" "?"
[ "$(stcode "$T_SEC" POST /api/v4/admin/study-courses '{"name":"x","type":"y"}')" = "403" ] && ok "sec -> course create 403" || bad "sec deny" "?"
[ "$(stcode "$T_SEC" GET /api/v4/admin/study-records/page)" = "200" ] && ok "sec managed page 200" || bad sec200 "?"
api "$T_STU" PUT /api/v4/user/profile '{"remark":"E2E-upd","branchId":999}' >/dev/null
[ "$(api "$T_STU" GET /api/v4/user/profile | g data.branchId)" = "$BR_ID" ] && ok "branchId escape ignored (#36)" || bad branchId "?"

echo; echo "== 7) cleanup =="
q <<SQL
set foreign_key_checks=0;
delete from tb_sign_record where activity_id in ($ACT_ID,$ACT2_ID);
delete from tb_activity_signup where activity_id in ($ACT_ID,$ACT2_ID);
delete from tb_activity_identity where activity_id=$ACT2_ID;
delete from tb_activity_detail where activity_id in ($ACT_ID,$ACT2_ID);
delete from tb_activity where id in ($ACT_ID,$ACT2_ID);
delete from tb_thought_report where title='E2E Q3 Report';
delete from tb_study_record where course_id=$CRS_ID;
delete from tb_study where id=$CRS_ID;
delete from tb_identity_change where reason like 'E2E%' or review_remark='E2E approved';
delete from tb_user_detail where user_id in ($SEC_ID,$STU_ID);
delete from tb_user_role where user_id in ($SEC_ID,$STU_ID);
delete from tb_user where id in ($SEC_ID,$STU_ID);
delete from tb_branch where id=$BR_ID;
delete from tb_role where description like 'E2E%';
delete from tb_activity_type where name='E2EDay';
set foreign_key_checks=1;
SQL
LEFT=$(q -N -e "select (select count(*) from tb_user where username like 'e2e_%')+(select count(*) from tb_branch where branch_name like 'E2E%')+(select count(*) from tb_activity where title like 'E2E%')+(select count(*) from tb_study where name like 'E2E%')+(select count(*) from tb_activity_type where name like 'E2E%');")
[ "$LEFT" = "0" ] && ok "residual=0" || bad cleanup "left=$LEFT"
echo; echo "=========== RESULT: PASS=$PASS FAIL=$FAIL ==========="; [ "$FAIL" -eq 0 ]
