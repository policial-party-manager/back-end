-- =============================================================
-- spec #68 各票的存量库迁移（幂等：可重复执行）
--
-- 覆盖：#72 培养联系人、#73 培养材料、#78 活动请假、
--       #76 参与范围「不限年级」、#75 身份变更作废域、#74 删除死表
--
-- 注意：索引一律写在 CREATE TABLE 内部，不用 CREATE INDEX IF NOT EXISTS
--      （MySQL 8.0 不支持该语法）；也不用存储过程，避免
--      `mysql < file` 与跨语句连接导致的 PROCEDURE does not exist。
-- =============================================================

-- ---------- 一、培养域（#72 / #73） ----------

CREATE TABLE IF NOT EXISTS tb_user_mentor
(
    id          int auto_increment
        primary key,
    user_id     int                                  not null comment '被培养人id（tb_user.id）',
    mentor_id   int                                  not null comment '培养联系人id（tb_user.id）',
    status      tinyint    default 1                 not null comment '状态（1：在职，0：已移除）',
    create_time timestamp  default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp  default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint tb_user_mentor_user_mentor_uindex
        unique (user_id, mentor_id),
    constraint tb_user_mentor_tb_user_id_fk
        foreign key (user_id) references tb_user (id),
    constraint tb_user_mentor_tb_mentor_id_fk
        foreign key (mentor_id) references tb_user (id),
    index tb_user_mentor_user_id_index (user_id),
    index tb_user_mentor_mentor_id_index (mentor_id)
)
    comment '培养联系人（与 tb_user_detail.contact_person 紧急联系人无关）';

CREATE TABLE IF NOT EXISTS tb_cultivation_material
(
    id            int auto_increment
        primary key,
    user_id       int                                  not null comment '成员id（tb_user.id）',
    material_type varchar(20)                          not null comment '材料类型（入党申请书/思想汇报/政审材料/培训证书/其他）',
    file          varchar(255)                         not null comment '文件地址（file: 前缀相对路径）',
    file_name     varchar(100)                         null comment '原始文件名',
    uploader_id   int                                  not null comment '上传人id（tb_user.id）',
    remark        varchar(200)                         null comment '备注',
    status        tinyint    default 1                 not null comment '状态（1：正常，0：已删除）',
    create_time   timestamp  default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint tb_cultivation_material_tb_user_id_fk
        foreign key (user_id) references tb_user (id),
    constraint tb_cultivation_material_tb_uploader_id_fk
        foreign key (uploader_id) references tb_user (id),
    index tb_cultivation_material_user_id_index (user_id)
)
    comment '培养材料（电子培养档案的素材）';

-- ---------- 二、活动请假（#78） ----------

CREATE TABLE IF NOT EXISTS tb_activity_leave
(
    id          int auto_increment
        primary key,
    activity_id int                                  not null comment '活动id',
    user_id     int                                  not null comment '请假人id',
    reason      varchar(200)                         null comment '请假理由',
    operator_id int                                  not null comment '录入人id',
    status      tinyint    default 1                 not null comment '状态（1：生效，0：已撤销）',
    create_time timestamp  default CURRENT_TIMESTAMP not null comment '录入时间',
    update_time timestamp  default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint tb_activity_leave_activity_user_uindex
        unique (activity_id, user_id),
    constraint tb_activity_leave_tb_activity_id_fk
        foreign key (activity_id) references tb_activity (id),
    constraint tb_activity_leave_tb_user_id_fk
        foreign key (user_id) references tb_user (id),
    constraint tb_activity_leave_tb_operator_id_fk
        foreign key (operator_id) references tb_user (id),
    index tb_activity_leave_activity_id_index (activity_id),
    index tb_activity_leave_user_id_index (user_id)
)
    comment '活动请假（与签到记录是两个生命周期的实体）';

-- ---------- 三、参与范围「不限年级」（#76） ----------

ALTER TABLE tb_activity_identity
    MODIFY COLUMN grade int NULL COMMENT '年级（NULL=不限年级）';

-- ---------- 四、身份变更作废域（#75） ----------

ALTER TABLE tb_identity_change
    DROP CHECK chk_identity_change_status;

ALTER TABLE tb_identity_change
    MODIFY COLUMN status tinyint DEFAULT 1 NOT NULL
        COMMENT '单据状态（0：作废，1：待审批，2：已生效，3：驳回）';

ALTER TABLE tb_identity_change
    ADD CONSTRAINT chk_identity_change_status
        CHECK (`status` IN (0, 1, 2, 3));

-- ---------- 五、删除从未被写入的死表（#74） ----------

DROP TABLE IF EXISTS tb_operation;
