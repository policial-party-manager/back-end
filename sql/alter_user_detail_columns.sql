-- =============================================================
-- 用户扩展字段迁移脚本（幂等，可重复执行）
-- tb_user_detail 新增：gender / college / grade / major / class_name /
--                      contact_person / remark（均可空）
-- 适用：MySQL 8.x（利用 information_schema 判断列是否存在）
-- =============================================================

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

CALL add_column_if_missing('tb_user_detail', 'gender',       "varchar(10) NULL COMMENT '性别'");
CALL add_column_if_missing('tb_user_detail', 'college',      "varchar(50) NULL COMMENT '学院'");
CALL add_column_if_missing('tb_user_detail', 'grade',        "varchar(10) NULL COMMENT '年级'");
CALL add_column_if_missing('tb_user_detail', 'major',        "varchar(50) NULL COMMENT '专业'");
CALL add_column_if_missing('tb_user_detail', 'class_name',   "varchar(50) NULL COMMENT '班级'");
CALL add_column_if_missing('tb_user_detail', 'contact_person', "varchar(50) NULL COMMENT '紧急联系人'");
CALL add_column_if_missing('tb_user_detail', 'remark',       "varchar(500) NULL COMMENT '备注'");

DROP PROCEDURE IF EXISTS add_column_if_missing;
