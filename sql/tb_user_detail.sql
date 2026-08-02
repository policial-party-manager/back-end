create table tb_user_detail
(
    user_id              int         not null comment '用户id'
        primary key,
    student_id           varchar(10) not null comment '学生id',
    phone                varchar(11) null comment '手机号',
    name                 varchar(12) not null comment '学生姓名',
    email                varchar(25) null comment '邮箱',
    identity_card_number varchar(18) null comment '身份证号',
    constraint tb_user_detail_email_uindex
        unique (email),
    constraint tb_user_detail_phone_uindex
        unique (phone),
    constraint tb_user_detail_tb_user_id_fk
        foreign key (user_id) references tb_user (id)
            on delete cascade
);

