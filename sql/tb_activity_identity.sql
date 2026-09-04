create table tb_activity_identity
(
    id              int auto_increment
        primary key,
    activity_id     int         not null comment '活动id',
    target_identity int         not null comment '目标身份',
    grade           int         not null comment '年级',
    college         varchar(10) null comment '学院',
    constraint tb_activity_identity_tb_activity_id_fk
        foreign key (activity_id) references tb_activity (id),
    constraint tb_activity_identity_tb_identity_id_fk
        foreign key (target_identity) references tb_identity (id)
)
    comment '活动目标身份';

标身份';

