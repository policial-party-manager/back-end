-- =============================================================
-- 参与范围「不限年级」可表达（#76，幂等，可重复执行）
--
-- tb_activity_identity.grade 原为 int NOT NULL，导致「不限年级」无法表达，
-- 只能为每个年级各建一行。改为可空：NULL = 不限年级。
--
-- 判定侧语义（ActivityScopeServiceImpl）：
--   行上 grade 为 NULL → 该维度不参与校验
--   行上 college 为空   → 不限学院
--   行上 target_identity 为 NULL → 不限身份
-- =============================================================

ALTER TABLE tb_activity_identity
    MODIFY COLUMN grade int NULL COMMENT '年级（NULL=不限年级）';
