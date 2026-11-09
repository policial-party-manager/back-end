-- =============================================================
-- 身份变更历史不可删（#75，幂等，可重复执行）
--
-- 1) tb_identity_change.status 增加 0 = 作废（沿用软删除约定「默认 1，1=正常」）
--    仅超管运维手段可置 0，系统不提供任何置 0 的 API。
-- 2) 重建 CHECK 约束：1 待审批 / 2 已生效 / 3 驳回 / 0 作废
--
-- 历史记录仍不可物理删除：本脚本只放宽状态域，不提供删除路径。
-- =============================================================

-- ---------- 1) 放宽 status 取值域到 0..3 ----------

ALTER TABLE tb_identity_change
    DROP CHECK chk_identity_change_status;

ALTER TABLE tb_identity_change
    MODIFY COLUMN status tinyint DEFAULT 1 NOT NULL
        COMMENT '单据状态（0：作废，1：待审批，2：已生效，3：驳回）';

ALTER TABLE tb_identity_change
    ADD CONSTRAINT chk_identity_change_status
        CHECK (`status` IN (0, 1, 2, 3));
