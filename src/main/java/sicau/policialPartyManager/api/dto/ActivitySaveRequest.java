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
}
