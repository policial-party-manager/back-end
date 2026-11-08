package sicau.policialPartyManager.api.dto;

import lombok.Data;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.ActivityLeave;
import sicau.policialPartyManager.model.entity.CultivationMaterial;
import sicau.policialPartyManager.model.entity.IdentityChange;
import sicau.policialPartyManager.model.entity.SignRecord;
import sicau.policialPartyManager.model.entity.StudyRecord;
import sicau.policialPartyManager.model.entity.ThoughtReport;

import java.util.List;

/**
 * 电子培养档案聚合（#80）：一次请求返回某成员完整培养信息。
 * <p>只出结构化数据，不做 PDF 导出——排版打印由前端负责。
 * <p>各区块在无数据时返回空集合，不省略字段、不报错。
 */
@Data
public class CultivationProfileVo {

    /** 基本信息（含当前政治身份、所属支部） */
    private UserVo basic;

    /** 当前培养联系人（#72）；换人后此处随之更新 */
    private List<sicau.policialPartyManager.service.Impl.MentorServiceImpl.MentorView> mentors;

    /** 身份变更历史（按时间倒序，含当前身份那一单） */
    private List<IdentityChange> identityChanges;

    /** 思想汇报记录 */
    private List<ThoughtReport> thoughtReports;

    /** 活动参与（签到记录） */
    private List<SignRecord> signRecords;

    /** 活动请假记录 */
    private List<ActivityLeave> leaves;

    /** 学习记录 */
    private List<StudyRecord> studyRecords;

    /** 培养材料（#73） */
    private List<CultivationMaterial> materials;

    /** 统计摘要：便于前端直接展示，不必二次计算 */
    private Summary summary;

    @Data
    public static class Summary {
        /** 思想汇报总数 */
        private int thoughtReportCount;
        /** 已通过的思想汇报数 */
        private int thoughtReportApproved;
        /** 活动签到次数 */
        private int signCount;
        /** 总学时（分钟） */
        private long studyMinutes;
        /** 培养材料数 */
        private int materialCount;
    }
}
