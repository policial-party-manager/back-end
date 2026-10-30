-- =============================================================
-- 政治身份体系落库（#44，幂等，可重复执行）
--
-- 1) tb_identity：identity_name varchar(10)->20、加唯一、role_id 改可空
--    （决策：role_id 仅展示映射，不参与鉴权；播种时不猜角色，留 NULL）
-- 2) 播种 6 个政治身份（README「政治身份级别」表为权威来源）
-- 3) tb_user_detail：加 identity_id（当前政治身份，NULL=未登记）+ FK
-- 4) tb_identity_change：from_identity 改可空（首次登记无来源身份）、
--    加 status/reviewer_id/review_time/review_remark + 状态 CHECK + 索引
--
-- 现有数据：不回填任何人的 identity_id（NULL=未登记是预期状态，不做猜测性 UPDATE）。
-- 播种按 identity_name 判等（ON DUPLICATE KEY 空转），若既有环境同名不同 level，
-- 本脚本不覆盖，请人工核对：SELECT * FROM tb_identity;
-- 依赖 tb_role 存在（role_id 可空后不再强制要求 ROLE_ 种子行）。
-- =============================================================

-- ---------- 1) tb_identity ----------

ALTER TABLE tb_identity
    MODIFY COLUMN identity_name varchar(20) NOT NULL COMMENT '身份名',
    MODIFY COLUMN role_id int NULL COMMENT '对应角色id（仅展示映射，不参与鉴权；身份变更永不联动登录角色）';

DROP PROCEDURE IF EXISTS add_identity_unique_name;
DELIMITER $$
CREATE PROCEDURE add_identity_unique_name()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity'
          AND INDEX_NAME = 'tb_identity_identity_name_uindex'
    ) THEN
        ALTER TABLE tb_identity
            ADD CONSTRAINT tb_identity_identity_name_uindex UNIQUE (identity_name);
    END IF;
END$$
DELIMITER ;
CALL add_identity_unique_name();
DROP PROCEDURE IF EXISTS add_identity_unique_name;

-- ---------- 2) 播种（幂等：名称已存在则原样保留） ----------

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

-- ---------- 3) tb_user_detail.identity_id ----------

DROP PROCEDURE IF EXISTS add_user_detail_identity;
DELIMITER $$
CREATE PROCEDURE add_user_detail_identity()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user_detail'
          AND COLUMN_NAME = 'identity_id'
    ) THEN
        ALTER TABLE tb_user_detail
            ADD COLUMN identity_id int NULL COMMENT '当前政治身份（FK tb_identity；NULL=未登记；身份变更审批生效时写回）';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user_detail'
          AND CONSTRAINT_NAME = 'tb_user_detail_tb_identity_id_fk'
    ) THEN
        ALTER TABLE tb_user_detail
            ADD CONSTRAINT tb_user_detail_tb_identity_id_fk
                FOREIGN KEY (identity_id) REFERENCES tb_identity (id);
    END IF;
END$$
DELIMITER ;
CALL add_user_detail_identity();
DROP PROCEDURE IF EXISTS add_user_detail_identity;

-- ---------- 4) tb_identity_change 审批流列 ----------

DROP PROCEDURE IF EXISTS add_identity_change_columns;
DELIMITER $$
CREATE PROCEDURE add_identity_change_columns()
BEGIN
    DECLARE from_nullable VARCHAR(8) DEFAULT 'YES';

    -- from_identity 被 FK 引用：MySQL 8 不允许直接修改 FK 使用的列的可空性
    -- （#32 的 1832 同款限制），因此 DROP FK -> MODIFY -> ADD FK，逐步判重幂等。
    SELECT is_nullable INTO from_nullable FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change'
       AND COLUMN_NAME = 'from_identity';
    IF from_nullable = 'NO' THEN
        IF EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change'
              AND CONSTRAINT_NAME = 'tb_identity_change_tb_identity_id_fk') THEN
            ALTER TABLE tb_identity_change
                DROP FOREIGN KEY tb_identity_change_tb_identity_id_fk;
        END IF;
        ALTER TABLE tb_identity_change
            MODIFY COLUMN from_identity int NULL COMMENT '从什么身份（首次登记可为 NULL）';
        IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change'
              AND CONSTRAINT_NAME = 'tb_identity_change_tb_identity_id_fk') THEN
            ALTER TABLE tb_identity_change
                ADD CONSTRAINT tb_identity_change_tb_identity_id_fk
                    FOREIGN KEY (from_identity) REFERENCES tb_identity (id);
        END IF;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change' AND COLUMN_NAME = 'status') THEN
        ALTER TABLE tb_identity_change
            ADD COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '单据状态（1：待审批，2：已生效，3：驳回）' AFTER reason;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change' AND COLUMN_NAME = 'reviewer_id') THEN
        ALTER TABLE tb_identity_change
            ADD COLUMN reviewer_id int NULL COMMENT '审批人（仅 super_admin 审批）' AFTER status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change' AND COLUMN_NAME = 'review_time') THEN
        ALTER TABLE tb_identity_change
            ADD COLUMN review_time timestamp NULL COMMENT '审批时间' AFTER reviewer_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change' AND COLUMN_NAME = 'review_remark') THEN
        ALTER TABLE tb_identity_change
            ADD COLUMN review_remark varchar(50) NULL COMMENT '审批意见' AFTER review_time;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change'
          AND CONSTRAINT_NAME = 'chk_identity_change_status') THEN
        ALTER TABLE tb_identity_change
            ADD CONSTRAINT chk_identity_change_status CHECK (`status` in (1, 2, 3));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change'
          AND CONSTRAINT_NAME = 'tb_identity_change_tb_user_id_fk_2') THEN
        ALTER TABLE tb_identity_change
            ADD CONSTRAINT tb_identity_change_tb_user_id_fk_2
                FOREIGN KEY (reviewer_id) REFERENCES tb_user (id);
    END IF;
END$$
DELIMITER ;
CALL add_identity_change_columns();
DROP PROCEDURE IF EXISTS add_identity_change_columns;

-- 审批流索引（判重）
DROP PROCEDURE IF EXISTS add_identity_change_indexes;
DELIMITER $$
CREATE PROCEDURE add_identity_change_indexes()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change' AND INDEX_NAME = 'tb_identity_change_user_id_index') THEN
        CREATE INDEX tb_identity_change_user_id_index ON tb_identity_change (user_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_identity_change' AND INDEX_NAME = 'tb_identity_change_status_index') THEN
        CREATE INDEX tb_identity_change_status_index ON tb_identity_change (status);
    END IF;
END$$
DELIMITER ;
CALL add_identity_change_indexes();
DROP PROCEDURE IF EXISTS add_identity_change_indexes;
