package sicau.policialPartyManager.log;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import lombok.Data;

/**
 * 统一操作日志文档（ES 索引 operation_log）。
 * <p>
 * 记录来源：登录/登出、全局错误、统一 HTTP 写操作拦截器；由事件监听器异步写入。
 */
@Data
@Document(indexName = "operation_log")
public class OperationLog {

    /** 主键（UUID） */
    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    /** 日志类型：LOGIN / LOGOUT / ERROR / OPERATION */
    @Field(type = FieldType.Keyword)
    private String logType;

    /** 业务模块（如 用户管理 / 认证 / 活动管理） */
    @Field(type = FieldType.Keyword)
    private String module;

    /** 动作描述（如 新增 / 编辑 / 批量导入 / 登录成功） */
    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String action;

    /** HTTP 方法（GET/POST/...，非请求类日志为空） */
    @Field(type = FieldType.Keyword)
    private String method;

    /** 请求路径 */
    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String path;

    /** 操作者 id（登录前为空） */
    @Field(type = FieldType.Long)
    private Long operatorId;

    /** 操作者（登录前用账号名/邮箱/手机号） */
    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String operatorName;

    /** 操作者角色编码 */
    @Field(type = FieldType.Keyword)
    private String role;

    /** 客户端 IP */
    @Field(type = FieldType.Keyword)
    private String ipAddress;

    /** User-Agent */
    @Field(type = FieldType.Text)
    private String userAgent;

    /** 是否成功 */
    @Field(type = FieldType.Boolean)
    private Boolean success;

    /** HTTP 状态码或业务码（错误日志必填） */
    @Field(type = FieldType.Integer)
    private Integer resultCode;

    /** 异常类型（ERROR 日志） */
    @Field(type = FieldType.Keyword)
    private String errorType;

    /** 详细说明/错误信息 */
    @Field(type = FieldType.Text)
    private String message;

    /** 耗时（毫秒） */
    @Field(type = FieldType.Long)
    private Long durationMs;

    /** 发生时间（epoch 毫秒，用于范围检索与排序） */
    @Field(type = FieldType.Long)
    private Long occurTime;

    /** 发生时间（ISO-8601，便于展示） */
    @Field(type = FieldType.Keyword)
    private String occurTimeText;
}
