create table tb_thought_report
(
    id            int auto_increment
        primary key,
    user_id       int                                   not null comment '用户id',
    reviewer_id   int                                   not null comment '审阅人（提交时自动派定为本人的支部书记）',
    title         varchar(100) default '无标题'         not null comment '标题',
    content       text                                  null comment '内容',
    file          varchar(50)                           null comment '附件地址（file: 前缀相对路径，可空）',
    status        tinyint                               not null comment '审阅状态（1：待审阅，2：通过，3：驳回）',
    review_remark varchar(50)                           null comment '审阅建议',
    submit_time   timestamp   default CURRENT_TIMESTAMP not null comment '提交时间',
    review_time   timestamp                             null comment '审阅时间',
    constraint chk_thought_report_status
        check (`status` in (1, 2, 3)),
    constraint tb_thought_report_tb_user_id_fk
        foreign key (user_id) references tb_user (id),
    constraint tb_thought_report_tb_user_id_fk_2
        foreign key (reviewer_id) references tb_user (id)
)
    comment '思想汇报表';

create index tb_thought_report_reviewer_id_index
    on tb_thought_report (reviewer_id);

create index tb_thought_report_user_id_index
    on tb_thought_report (user_id);
