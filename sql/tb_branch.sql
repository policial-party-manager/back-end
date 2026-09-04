create table tb_branch
(
    id           int auto_increment
        primary key,
    branch_name  varchar(20)                          not null comment '支部名称',
    college      varchar(10)                          not null comment '所属学院',
    secretary_id int                                  not null comment '支部书记用户id',
    description  text                                 null comment '支部简介',
    create_time  timestamp  default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  timestamp  default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间',
    status       tinyint(1) default 0                 not null comment '是否删除',
    constraint tb_branch_tb_user_id_fk
        foreign key (secretary_id) references tb_user (id)
)
    comment '党支部表';

create index tb_branch_college_index
    on tb_branch (college);

