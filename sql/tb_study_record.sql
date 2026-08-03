create table tb_study_record
(
    id          int auto_increment
        primary key,
    user_id     int                                 null comment '用户id',
    course_id   int                                 not null comment '课程id',
    study_time  time                                not null comment '学时',
    score       decimal                             not null comment '成绩',
    url         varchar(30)                         null comment '证书链接',
    remark      text                                null comment '备注',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint tb_study_record_tb_study_id_fk
        foreign key (course_id) references tb_study (id),
    constraint tb_study_record_tb_user_id_fk
        foreign key (user_id) references tb_user (id)
)
    comment '学习记录';

create index tb_study_record_user_id_index
    on tb_study_record (user_id);

