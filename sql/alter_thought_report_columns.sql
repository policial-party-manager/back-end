-- =============================================================
-- 思想汇报表结构对齐（#33，幂等，可重复执行）
--
-- 1) title varchar(10) -> varchar(100)：季度+年份标题装不下原列宽
-- 2) file 改可空：附件不是必交项（一期本地上传，file: 前缀相对路径）
-- 3) 补 status CHECK（1 待审阅 / 2 通过 / 3 驳回），风格对齐 tb_notice
-- 不改任何数据；本表若已有历史 status=0/4+ 脏值会因 CHECK 失败，
-- 执行前核查：SELECT status, count(*) FROM tb_thought_report GROUP BY status;
-- =============================================================

ALTER TABLE tb_thought_report
    MODIFY COLUMN title varchar(100) NOT NULL DEFAULT '无标题' COMMENT '标题',
    MODIFY COLUMN file  varchar(50)  NULL COMMENT '附件地址（file: 前缀相对路径，可空）';

DROP PROCEDURE IF EXISTS add_thought_report_status_check;
DELIMITER $$
CREATE PROCEDURE add_thought_report_status_check()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tb_thought_report'
          AND CONSTRAINT_NAME = 'chk_thought_report_status'
    ) THEN
        ALTER TABLE tb_thought_report
            ADD CONSTRAINT chk_thought_report_status CHECK (`status` in (1, 2, 3));
    END IF;
END$$
DELIMITER ;

CALL add_thought_report_status_check();
DROP PROCEDURE IF EXISTS add_thought_report_status_check;
