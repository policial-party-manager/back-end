create table tb_identity_change
(
    id            int auto_increment
        primary key,
    user_id       int                                  not null comment '用户id',
    from_identity int                                  null comment '从什么身份（首次登记可为 NULL）',
    to_identity   int                                  not null comment '到什么身份',
    operator_id   int                                  not null comment '发起操作者id',
    reason        text                                 null comment '变更原因',
    status        tinyint    default 1                 not null comment '单据状态（1：待审批，2：已生效，3：驳回）',
    reviewer_id   int                                  null comment '审批人（仅 super_admin 审批）',
    review_time   timestamp                            null comment '审批时间',
    review_remark varchar(50)                          null comment '审批意见',
    create_time   timestamp  default CURRENT_TIMESTAMP not null comment '发起时间',
    constraint chk_identity_change_status
        check (`status` in (1, 2, 3)),
    constraint tb_identity_change_tb_identity_id_fk
        foreign key (from_identity) references tb_identity (id),
    constraint tb_identity_change_tb_identity_id_fk_2
        foreign key (to_identity) references tb_identity (id),
    constraint tb_identity_change_tb_user_id_fk
        foreign key (user_id) references tb_user (id),
    constraint tb_identity_change_tb_user_id_fk_2
        foreign key (reviewer_id) references tb_user (id)
)
    comment '政治身份变更单（两阶段：支管/超管发起 -> 超管审批；生效才写回 tb_user_detail.identity_id）';

create index tb_identity_change_operator_id_index
    on tb_identity_change (operator_id);

create index tb_identity_change_user_id_index
    on tb_identity_change (user_id);

create index tb_identity_change_status_index
    on tb_identity_change (status);
