-- =============================================================
-- 党建云平台 · 一键建库脚本
--
-- 用法（必须在仓库根目录执行；SOURCE 的相对路径以当前目录为准）：
--     mysql -u root -p < sql/databases.sql
--
-- 说明：
--   * 建库（不存在时）+ 按外键依赖顺序依次建表，共 21 张
--   * 适用于全新环境；库中已有同名表时重跑会报 "Table already exists"
--   * 单表结构在 sql/tb_*.sql；历史库的增量变更在 sql/alter_*.sql
--   * 改完任何 DDL 后执行 sql/schema-verify.sh 可验证脚本仍能干净执行
-- =============================================================

CREATE DATABASE IF NOT EXISTS `policial_party_manager`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `policial_party_manager`;

-- 一、基础表（无外键依赖）
SOURCE sql/tb_user.sql;
SOURCE sql/tb_role.sql;
SOURCE sql/tb_activity_type.sql;
SOURCE sql/tb_study.sql;

-- 二、只依赖 tb_user / tb_role
SOURCE sql/tb_identity.sql;
SOURCE sql/tb_role_permission.sql;
SOURCE sql/tb_user_role.sql;
SOURCE sql/tb_branch.sql;

-- 三、依赖 tb_branch
SOURCE sql/tb_user_detail.sql;

-- 四、活动域（依赖 tb_activity / tb_identity）
SOURCE sql/tb_activity.sql;
SOURCE sql/tb_activity_detail.sql;
SOURCE sql/tb_activity_identity.sql;
SOURCE sql/tb_sign_record.sql;
SOURCE sql/tb_activity_signup.sql;

-- 五、学习与汇报
SOURCE sql/tb_study_record.sql;
SOURCE sql/tb_thought_report.sql;

-- 六、内容与日志
SOURCE sql/tb_news.sql;
SOURCE sql/tb_notice.sql;
SOURCE sql/tb_notice_top.sql;
SOURCE sql/tb_operation.sql;
SOURCE sql/tb_identity_change.sql;

-- 字典播种（幂等，与 sql/alter_identity_flow.sql 同源；README「政治身份级别」为权威来源）
INSERT INTO tb_identity (identity_name, level, role_id, description)
VALUES
    ('普通学生',   1, NULL, '非党员'),
    ('入党申请人', 2, NULL, '已提交申请'),
    ('积极分子',   3, NULL, '已确定为积极分子'),
    ('发展对象',   4, NULL, '已确定为发展对象'),
    ('预备党员',   5, NULL, '已批准预备'),
    ('正式党员',   6, NULL, '已转正')
AS new
ON DUPLICATE KEY UPDATE tb_identity.identity_name = new.identity_name;

SELECT '数据库 policial_party_manager 初始化完成（21 张表）' AS result;
