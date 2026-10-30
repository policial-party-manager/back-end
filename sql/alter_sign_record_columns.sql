-- =============================================================
-- 签到记录表结构对齐（幂等，可重复执行）
--
-- 背景：原 DDL 把 device / location / remark 三列都设成 NOT NULL 且无默认值，
--       而 Web / 小程序端通常拿不到设备号与定位，导致签到插不进去；
--       同时缺 (activity_id, user_id) 唯一约束，重复签到只能靠应用层挡。
--
-- 本脚本：三列改可空 + 补唯一约束，不改动已有数据。
-- =============================================================

ALTER TABLE tb_sign_record
    MODIFY COLUMN device   varchar(10) NULL COMMENT '签到设备（web/app/mini 等，可空）',
    MODIFY COLUMN location point       NULL COMMENT '地点（POINT，可空；读写按 WKT 字符串处理）',
    MODIFY COLUMN remark   varchar(20) NULL COMMENT '备注';

-- 唯一约束：MySQL 不支持 ADD CONSTRAINT IF NOT EXISTS，用 information_schema 判重
DROP PROCEDURE IF EXISTS add_sign_record_unique;
DELIMITER $$
CREATE PROCEDURE add_sign_record_unique()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tb_sign_record'
          AND INDEX_NAME = 'tb_sign_record_activity_user_uindex'
    ) THEN
        ALTER TABLE tb_sign_record
            ADD CONSTRAINT tb_sign_record_activity_user_uindex UNIQUE (activity_id, user_id);
    END IF;
END$$
DELIMITER ;

CALL add_sign_record_unique();
DROP PROCEDURE IF EXISTS add_sign_record_unique;
