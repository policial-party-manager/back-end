create table tb_activity_type
(
    id            int auto_increment
        primary key,
    name          varchar(10)          not null comment '类型名称',
    need_sign     tinyint(1) default 1 not null comment '是否需要签到',
    count_to_file tinyint(1) default 1 not null comment '是否计入档案'
)
    comment '活动类型';

