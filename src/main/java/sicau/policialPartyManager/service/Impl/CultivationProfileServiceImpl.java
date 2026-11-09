package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.api.dto.CultivationProfileVo;
import sicau.policialPartyManager.api.dto.UserVo;
import sicau.policialPartyManager.model.entity.ActivityLeave;
import sicau.policialPartyManager.model.entity.IdentityChange;
import sicau.policialPartyManager.model.entity.SignRecord;
import sicau.policialPartyManager.model.entity.StudyRecord;
import sicau.policialPartyManager.model.entity.ThoughtReport;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.ActivityLeaveMapper;
import sicau.policialPartyManager.repository.CultivationMaterialMapper;
import sicau.policialPartyManager.repository.IdentityChangeMapper;
import sicau.policialPartyManager.repository.SignRecordMapper;
import sicau.policialPartyManager.repository.StudyRecordMapper;
import sicau.policialPartyManager.repository.ThoughtReportMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.BranchScopeService;
import sicau.policialPartyManager.api.dto.CultivationProfileVo.Summary;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 电子培养档案聚合（#80）。
 * <p>支委限本支部、普通成员只能看自己、超管不限；越权统一「用户不存在」。
 * <p>各区块无数据时返回空集合；统计摘要在服务端算好，避免前端二次计算产生口径分歧。
 */
@Service
@RequiredArgsConstructor
public class CultivationProfileServiceImpl {

    private final UserDetailMapper userDetailMapper;
    private final IdentityChangeMapper identityChangeMapper;
    private final ThoughtReportMapper thoughtReportMapper;
    private final SignRecordMapper signRecordMapper;
    private final ActivityLeaveMapper activityLeaveMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final CultivationMaterialMapper materialMapper;
    private final MentorServiceImpl mentorService;
    private final ScopedMemberServiceImpl scopedMemberService;
    private final BranchScopeService branchScopeService;

    public CultivationProfileVo of(sicau.policialPartyManager.model.records.User operator, Long userId) {
        // 范围校验：超管不限；支委限本支部；普通成员只能看自己
        if (branchScopeService.isSuperAdmin(operator)) {
            // 放行
        } else if (branchScopeService.isBranchAdmin(operator)) {
            branchScopeService.requireManageableUser(operator, userId);
        } else if (!userId.equals(operator.userId())) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 范围已在上方判定完毕。这里不能再走 getUserScoped——
        // 它内部对非支委无条件抛「用户不存在」，会把「普通成员看自己」也挡掉（spec 故事 60）。
        UserVo basic = scopedMemberService.getUserForProfile(userId);

        CultivationProfileVo vo = new CultivationProfileVo();
        vo.setBasic(basic);
        vo.setMentors(mentorService.mentorViewsOf(userId));

        vo.setIdentityChanges(identityChangeMapper.selectList(new LambdaQueryWrapper<IdentityChange>()
                .eq(IdentityChange::getUserId, userId)
                // 软删除/作废过滤：本 spec 把 status=0 引入为「作废」，作废单不该作为历史展示给成员
                .ne(IdentityChange::getStatus, 0)
                .orderByDesc(IdentityChange::getId)));

        List<ThoughtReport> reports = thoughtReportMapper.selectList(new LambdaQueryWrapper<ThoughtReport>()
                .eq(ThoughtReport::getUserId, userId)
                .orderByDesc(ThoughtReport::getId));
        vo.setThoughtReports(reports);

        List<SignRecord> signs = signRecordMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .orderByDesc(SignRecord::getSignTime));
        vo.setSignRecords(signs);

        List<ActivityLeave> leaves = activityLeaveMapper.selectList(new LambdaQueryWrapper<ActivityLeave>()
                .eq(ActivityLeave::getUserId, userId)
                .eq(ActivityLeave::getStatus, 1)
                .orderByDesc(ActivityLeave::getId));
        vo.setLeaves(leaves);

        List<StudyRecord> studies = studyRecordMapper.selectList(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId)
                .orderByDesc(StudyRecord::getId));
        vo.setStudyRecords(studies);

        vo.setMaterials(materialMapper.selectList(new LambdaQueryWrapper<sicau.policialPartyManager.model.entity.CultivationMaterial>()
                .eq(sicau.policialPartyManager.model.entity.CultivationMaterial::getUserId, userId)
                .eq(sicau.policialPartyManager.model.entity.CultivationMaterial::getStatus, 1)
                .orderByDesc(sicau.policialPartyManager.model.entity.CultivationMaterial::getId)));

        Summary s = new Summary();
        s.setThoughtReportCount(reports.size());
        s.setThoughtReportApproved((int) reports.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 2).count());
        s.setSignCount(signs.size());
        long minutes = 0;
        for (StudyRecord r : studies) {
            LocalTime t = r.getStudyTime();
            if (t != null) {
                minutes += t.getHour() * 60L + t.getMinute();
            }
        }
        s.setStudyMinutes(minutes);
        s.setMaterialCount(vo.getMaterials().size());
        vo.setSummary(s);
        return vo;
    }

    /** 空档案骨架（成员存在但无任何培养数据时的稳定返回）。 */
    public List<ThoughtReport> emptyThoughtReports() {
        return new ArrayList<>();
    }
}
