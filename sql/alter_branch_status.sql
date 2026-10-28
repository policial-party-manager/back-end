-- =============================================================
-- tb_branch.status 语义对齐（幂等，可重复执行）
--
-- 背景：DDL 原本是 `default 0` + 注释「是否删除」，而代码与 README 的约定是
--        1 = 正常，0 = 已删除。默认值 0 会让「未显式写 status」的新建支部
--        （如 BranchServiceImpl.save() 旧写法）立刻变成已删除，列表查不到。
--
-- 本脚本把列默认值与注释对齐到约定；不改动已有数据。
-- 注意：升级前若有人直接 INSERT 且未写 status，那些行的 status 会是 0，
--        在本约定下等同于「已删除」。执行前请先人工确认：
--            SELECT id, branch_name, status FROM tb_branch WHERE status = 0;
--        确认属于误伤的行，可按下式恢复，再执行本脚本：
--            UPDATE tb_branch SET status = 1 WHERE id IN (...);
-- =============================================================

ALTER TABLE tb_branch
    MODIFY COLUMN status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态（1：正常，0：已删除）';
