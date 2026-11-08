create table tb_cultivation_material
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
        foreign key (uploader_id) references tb_user (id)
)
    comment '培养材料（电子培养档案的素材）';

create index tb_cultivation_material_user_id_index
    on tb_cultivation_material (user_id);
