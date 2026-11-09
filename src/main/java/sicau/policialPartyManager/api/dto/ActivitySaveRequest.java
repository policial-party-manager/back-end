package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动管理-新增/编辑请求（活动主体 + 活动详情字段；标题必填）
 */
@Data
public class ActivitySaveRequest {

    /** 支部 id（可空=校级活动） */
    private Long branchId;

    @NotBlank(message = "活动标题不能为空")
    private String title;

    private String description;

    /** 活动类型 id（关联 tb_activity_type.id，可空） */
    private Integer type;

    private String cover;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 地点 WKT（polygon，null 表示线上活动） */
    private String location;

    private String locationDescription;

    private LocalDateTime signStart;

    private LocalDateTime signEnd;

    private Integer maxParticipants;

    /** 活动状态（新增不填默认 1；编辑时可调整 0/1/2） */
    private Integer status;

    /**
     * 参与范围（#76）：可多行，命中任意一行即通过。
     * <p>为 null 表示不修改既有范围；空列表表示清空（全场开放）。
     * 行内三个维度留空即该维度不限。
     */
    private java.util.List<ScopeRow> scopes;

    /** 参与范围单行：政治身份 / 年级 / 学院，任意留空=该维度不限 */
    @Data
    public static class ScopeRow {
        /** 目标政治身份 id（tb_identity.id）；null=不限身份 */
        private Long targetIdentity;
        /** 年级（如 2023）；null=不限年级 */
        private Integer grade;
        /** 学院；空=不限学院 */
        private String college;
    }
}
