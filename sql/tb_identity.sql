create table tb_identity
(
    id            int auto_increment
        primary key,
    identity_name varchar(10) not null comment '身份名',
    level         int         null comment '发展阶段1-6',
    role_id       int         not null comment '对应角色id',
    description   text        null comment '说明',
    constraint tb_identity_tb_role_id_fk
        foreign key (role_id) references tb_role (id)
)
    comment '政治身份';

�身份';

