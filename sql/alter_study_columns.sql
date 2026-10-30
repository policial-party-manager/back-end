-- =============================================================
-- 学习域结构对齐（#32，幂等，可重复执行）
--
-- 1) tb_study.type varchar(6)->varchar(20)（原列只容 2 个汉字）；name 加唯一
-- 2) tb_study_record.user_id 改 NOT NULL（记录必须有主人）
--    注意：MySQL 8 不允许直接改被外键引用的列的可空性（错误 1832，且整条 ALTER
--    中止、同语句里的 score 一并落空）。必须先删 FK、改列、再重建 FK。
-- 3) score decimal(10,0)->decimal(5,2)（原整数精度会把 85.5 四舍五入）
-- 4) 加 unique(user_id, course_id)：一人一课一行，重复录入走更新
--
-- 本仓库开发库两表为 0 行（2026-10-30 实查）。其他环境若已有数据，先核查：
--   SELECT count(*) FROM tb_study_record WHERE user_id IS NULL;
--   SELECT name FROM tb_study GROUP BY name HAVING count(*)>1;
--   SELECT score FROM tb_study_record WHERE score NOT BETWEEN 0 AND 100;
-- 有冲突需先人工清重，再执行本脚本（否则加唯一约束/改精度会失败）。
-- =============================================================

ALTER TABLE tb_study
    MODIFY COLUMN type varchar(20) NOT NULL COMMENT '类型（自由文本，如 必修/选修/专题）';

-- user_id 被 FK 引用，先删后建（表为空时无引用完整性风险）
ALTER TABLE tb_study_record DROP FOREIGN KEY tb_study_record_tb_user_id_fk;

ALTER TABLE tb_study_record
    MODIFY COLUMN user_id int           NOT NULL COMMENT '用户id',
    MODIFY COLUMN score   decimal(5, 2) NOT NULL COMMENT '成绩（0~100，两位小数）';

ALTER TABLE tb_study_record
    ADD CONSTRAINT tb_study_record_tb_user_id_fk
        FOREIGN KEY (user_id) REFERENCES tb_user (id);

DROP PROCEDURE IF EXISTS add_unique_if_missing;
DELIMITER $$
CREATE PROCEDURE add_unique_if_missing(
    IN p_table  VARCHAR(64),
    IN p_index  VARCHAR(64),
    IN p_cols   VARCHAR(128)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND INDEX_NAME = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD CONSTRAINT `', p_index, '` UNIQUE (', p_cols, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_unique_if_missing('tb_study', 'tb_study_name_uindex', '`name`');
CALL add_unique_if_missing('tb_study_record', 'tb_study_record_user_id_course_id_uindex', '`user_id`, `course_id`');

DROP PROCEDURE IF EXISTS add_unique_if_missing;

-- 重建 FK 后补回子表普通索引（DROP FK 会带走 MySQL 自动建的支撑索引；判重保护）
DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER $$
CREATE PROCEDURE add_index_if_missing(
    IN p_table  VARCHAR(64),
    IN p_index  VARCHAR(64),
    IN p_cols   VARCHAR(128)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND INDEX_NAME = p_index
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` (', p_cols, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_index_if_missing('tb_study_record', 'tb_study_record_user_id_index', '`user_id`');
CALL add_index_if_missing('tb_study', 'tb_study_type_index', '`type`');

DROP PROCEDURE IF EXISTS add_index_if_missing;
