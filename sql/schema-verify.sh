#!/usr/bin/env bash
# 校验 sql/ 下的 DDL 能否干净地建出一套完整表结构。
#
# 作用：把「SQL 脚本坏了但没人发现」变成一条确定性命令（红/绿）。
# 背景：2026-10 曾有 17 个 tb_*.sql 尾部残留截断脏行 + 5 个文件混入非 UTF-8 字节，
#       导致 `mysql < ...` 全部失败；本脚本是那次的回归护栏。
#
# 用法（仓库根目录）：
#     DB_USERNAME=root DB_PASSWORD=*** ./sql/schema-verify.sh [库名]
#
# 注意：脚本会 DROP 并重建指定的临时库（默认 pp_schema_verify），
#       切勿把库名传成业务库。
set -uo pipefail

DB="${1:-pp_schema_verify}"
SQL_DIR="$(cd "$(dirname "$0")" && pwd)"
HOST="${DB_HOST:-127.0.0.1}"
MYSQL_USER="${DB_USERNAME:-root}"
MYSQL_PWD="${DB_PASSWORD:-}"
export MYSQL_PWD

# 安全护栏：绝不允许把业务库当临时库 DROP 掉
for forbidden in policial_party_manager party_manager; do
  if [ "$DB" = "$forbidden" ]; then
    echo "拒绝执行：'$DB' 是业务库，本脚本会 DROP 目标库" >&2
    exit 2
  fi
done

mysql_run() { mysql -h "$HOST" -u"$MYSQL_USER" --protocol=TCP --default-character-set=utf8mb4 "$@"; }

echo "== 重建临时库 $DB =="
mysql_run -e "DROP DATABASE IF EXISTS \`$DB\`; CREATE DATABASE \`$DB\` DEFAULT CHARACTER SET utf8mb4;" || exit 2

# 外键依赖顺序：被引用的表必须先建（单行，便于下面的 case 做成员判断）
ORDER="tb_user tb_role tb_activity_type tb_study tb_identity tb_role_permission tb_user_role tb_branch tb_user_detail tb_activity tb_activity_detail tb_activity_identity tb_sign_record tb_study_record tb_thought_report tb_news tb_notice tb_notice_top tb_operation tb_identity_change"

fail=0
for t in $ORDER; do
  f="$SQL_DIR/$t.sql"
  if [ ! -f "$f" ]; then
    echo "MISSING  $t"
    fail=$((fail + 1))
    continue
  fi
  if out=$(mysql_run "$DB" < "$f" 2>&1); then
    echo "OK       $t"
  else
    echo "FAILED   $t"
    printf '%s\n' "$out" | sed 's/^/           /'
    fail=$((fail + 1))
  fi
done

# 目录里有、但没被纳入顺序的文件（新增表忘了登记）
for f in "$SQL_DIR"/tb_*.sql; do
  t=$(basename "$f" .sql)
  case " $ORDER " in *" $t "*) ;; *) echo "UNLISTED $t"; fail=$((fail + 1)) ;; esac
done

created=$(mysql_run -N -e "select count(*) from information_schema.tables where table_schema='$DB';")
expected=$(printf '%s\n' $ORDER | wc -l | tr -d ' ')
echo "== 建表结果：$created 张（期望 $expected），失败 $fail 项 =="
[ "$fail" -eq 0 ] && [ "$created" -eq "$expected" ]
