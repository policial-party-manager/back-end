-- =============================================================
-- 活动相关列对齐脚本（幂等，可重复执行）
-- 目标：cover / description 在 tb_activity，location / max_participants
--       在 tb_activity_detail，缺列补列、缺长度放宽（适用 MySQL 8）
-- =============================================================

-- 1) tb_activity：description / cover 补列与放宽
DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN p_table_name  VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition  TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('tb_activity', 'cover',       "varchar(500) NULL COMMENT '封面图（file:/url:开头）'");
CALL add_column_if_missing('tb_activity', 'description', "varchar(2000) NULL COMMENT '活动简介'");

-- 放宽长度 / 允许为空（若已存在则 MODIFY）
ALTER TABLE tb_activity MODIFY COLUMN cover       varchar(500)  NULL COMMENT '封面图（file:/url:开头）';
ALTER TABLE tb_activity MODIFY COLUMN description varchar(2000) NULL COMMENT '活动简介';
-- 标题与支部（允许校级活动=不选支部）
ALTER TABLE tb_activity MODIFY COLUMN title       varchar(200) NOT NULL DEFAULT '无标题' COMMENT '标题';
ALTER TABLE tb_activity MODIFY COLUMN branch_id   int          NULL COMMENT '支部id（空=校级活动）';
ALTER TABLE tb_activity MODIFY COLUMN start_time  datetime     NULL COMMENT '开始时间';
ALTER TABLE tb_activity MODIFY COLUMN end_time    datetime     NULL COMMENT '结束时间';

-- 2) tb_activity_detail：location / max_participants 补列与放宽
CALL add_column_if_missing('tb_activity_detail', 'location',           "polygon NULL COMMENT '地点（WKT，空=线上活动）'");
CALL add_column_if_missing('tb_activity_detail', 'max_participants',   "int NULL COMMENT '人数上限（空=不限）'");

ALTER TABLE tb_activity_detail MODIFY COLUMN location             polygon NULL COMMENT '地点（WKT，空=线上活动）';
ALTER TABLE tb_activity_detail MODIFY COLUMN location_description varchar(500) NULL DEFAULT NULL COMMENT '地址（文字说明）';
ALTER TABLE tb_activity_detail MODIFY COLUMN sign_start           datetime NULL COMMENT '签到开始时间';
ALTER TABLE tb_activity_detail MODIFY COLUMN sign_end             datetime NULL COMMENT '签到结束时间';
ALTER TABLE tb_activity_detail MODIFY COLUMN status               tinyint NOT NULL DEFAULT 1 COMMENT '活动状态(0/1/2)';

DROP PROCEDURE IF EXISTS add_column_if_missing;
