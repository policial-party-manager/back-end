package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动管理-列表/详情 VO（tb_activity + tb_activity_detail + 支部/类型名称）
 */
@Data
public class ActivityVo {

    private Long id;

    /** 支部 id（可空=校级活动） */
    private Long branchId;

    private String branchName;

    private String title;

    private String description;

    /** 活动类型 id（关联 tb_activity_type.id，可空） */
    private Integer type;

    private String typeName;

    private String cover;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 地点 WKT（polygon，null 表示线上活动） */
    private String location;

    private String locationDescription;

    private LocalDateTime signStart;

    private LocalDateTime signEnd;

    private Integer maxParticipants;

    /** 活动状态（tb_activity_detail.status：0/1/2） */
    private Integer status;

    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
