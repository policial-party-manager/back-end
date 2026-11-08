package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.api.dto.DashboardVo;
import sicau.policialPartyManager.api.dto.DistributionVo;
import sicau.policialPartyManager.model.entity.Identity;
import sicau.policialPartyManager.model.entity.IdentityChange;
import sicau.policialPartyManager.model.entity.ThoughtReport;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.IdentityChangeMapper;
import sicau.policialPartyManager.repository.IdentityMapper;
import sicau.policialPartyManager.repository.ThoughtReportMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.BranchScopeService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页统计（#81）。
 * <p>只做能驱动动作的数：待审核思想汇报、待审批身份变更、阶段分布。
 * <p>支委版限本支部（按成员所属支部），超管版为全院。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl {

    private final ThoughtReportMapper thoughtReportMapper;
    private final IdentityChangeMapper identityChangeMapper;
    private final UserDetailMapper userDetailMapper;
    private final IdentityMapper identityMapper;
    private final BranchScopeService branchScopeService;

    public DashboardVo dashboard(sicau.policialPartyManager.model.records.User operator) {
        Long scopeBranchId = resolveScope(operator);

        // 待审核思想汇报：status=1
        long pendingReports;
        long pendingChanges;
        if (scopeBranchId == null) {
            Long n = thoughtReportMapper.selectCount(new LambdaQueryWrapper<ThoughtReport>()
                    .eq(ThoughtReport::getStatus, 1));
            pendingReports = n == null ? 0 : n;
            Long m = identityChangeMapper.selectCount(new LambdaQueryWrapper<IdentityChange>()
                    .eq(IdentityChange::getStatus, 1));
            pendingChanges = m == null ? 0 : m;
        } else {
            List<Long> userIds = userIdsOfBranch(scopeBranchId);
            if (userIds.isEmpty()) {
                pendingReports = 0;
                pendingChanges = 0;
            } else {
                Long n = thoughtReportMapper.selectCount(new LambdaQueryWrapper<ThoughtReport>()
                        .eq(ThoughtReport::getStatus, 1)
                        .in(ThoughtReport::getUserId, userIds));
                pendingReports = n == null ? 0 : n;
                Long m = identityChangeMapper.selectCount(new LambdaQueryWrapper<IdentityChange>()
                        .eq(IdentityChange::getStatus, 1)
                        .in(IdentityChange::getUserId, userIds));
                pendingChanges = m == null ? 0 : m;
            }
        }

        DashboardVo vo = new DashboardVo();
        vo.setPendingThoughtReports(pendingReports);
        vo.setPendingIdentityChanges(pendingChanges);
        vo.setIdentityDistribution(identityDistribution(scopeBranchId));
        return vo;
    }

    /** 阶段分布：各政治身份人数 */
    public List<DashboardVo.IdentityCount> identityDistribution(Long scopeBranchId) {
        List<Identity> identities = identityMapper.selectList(new LambdaQueryWrapper<Identity>()
                .orderByAsc(Identity::getLevel));
        LambdaQueryWrapper<UserDetail> wrapper = new LambdaQueryWrapper<>();
        if (scopeBranchId != null) {
            wrapper.eq(UserDetail::getBranchId, scopeBranchId);
        }
        List<UserDetail> members = userDetailMapper.selectList(wrapper);

        List<DashboardVo.IdentityCount> result = new ArrayList<>();
        for (Identity id : identities) {
            DashboardVo.IdentityCount c = new DashboardVo.IdentityCount();
            c.setIdentityId(id.getId());
            c.setIdentityName(id.getIdentityName());
            c.setLevel(id.getLevel());
            c.setCount(members.stream()
                    .filter(m -> id.getId() != null && id.getId().equals(m.getIdentityId()))
                    .count());
            result.add(c);
        }
        // 未登记身份的单独一行，避免「总数对不上」
        long unregistered = members.stream().filter(m -> m.getIdentityId() == null).count();
        if (unregistered > 0) {
            DashboardVo.IdentityCount c = new DashboardVo.IdentityCount();
            c.setIdentityId(null);
            c.setIdentityName("未登记");
            c.setLevel(0);
            c.setCount(unregistered);
            result.add(c);
        }
        return result;
    }

    /** 按维度切分的阶段分布 */
    public DistributionVo distribution(sicau.policialPartyManager.model.records.User operator,
                                       String dimension) {
        Long scopeBranchId = resolveScope(operator);
        LambdaQueryWrapper<UserDetail> wrapper = new LambdaQueryWrapper<>();
        if (scopeBranchId != null) {
            wrapper.eq(UserDetail::getBranchId, scopeBranchId);
        }
        List<UserDetail> members = userDetailMapper.selectList(wrapper);
        List<Identity> identities = identityMapper.selectList(new LambdaQueryWrapper<Identity>()
                .orderByAsc(Identity::getLevel));

        String dim = normalizeDimension(dimension);
        Map<String, List<UserDetail>> grouped = new LinkedHashMap<>();
        for (UserDetail m : members) {
            String key = switch (dim) {
                case "college" -> m.getCollege();
                case "major" -> m.getMajor();
                case "className" -> m.getClassName();
                case "grade" -> m.getGrade();
                default -> null;
            };
            grouped.computeIfAbsent(key == null ? "未填写" : key, k -> new ArrayList<>()).add(m);
        }

        DistributionVo vo = new DistributionVo();
        vo.setDimension(dim);
        List<DistributionVo.Group> groups = new ArrayList<>();
        for (Map.Entry<String, List<UserDetail>> e : grouped.entrySet()) {
            DistributionVo.Group g = new DistributionVo.Group();
            g.setKey(e.getKey());
            g.setTotal(e.getValue().size());
            List<DashboardVo.IdentityCount> counts = new ArrayList<>();
            for (Identity id : identities) {
                DashboardVo.IdentityCount c = new DashboardVo.IdentityCount();
                c.setIdentityId(id.getId());
                c.setIdentityName(id.getIdentityName());
                c.setLevel(id.getLevel());
                c.setCount(e.getValue().stream()
                        .filter(m -> id.getId() != null && id.getId().equals(m.getIdentityId()))
                        .count());
                counts.add(c);
            }
            g.setIdentities(counts);
            groups.add(g);
        }
        vo.setGroups(groups);
        return vo;
    }

    // ---------- internals ----------

    /** null = 全院（超管）；非 null = 限该支部（支委） */
    private Long resolveScope(sicau.policialPartyManager.model.records.User operator) {
        if (branchScopeService.isSuperAdmin(operator)) {
            return null;
        }
        Long mine = branchScopeService.branchIdOf(operator);
        if (mine == null) {
            throw new IllegalArgumentException("您尚未归属任何党支部，请联系管理员");
        }
        return mine;
    }

    private List<Long> userIdsOfBranch(Long branchId) {
        List<Long> ids = new ArrayList<>();
        for (UserDetail d : userDetailMapper.selectList(new LambdaQueryWrapper<UserDetail>()
                .eq(UserDetail::getBranchId, branchId))) {
            ids.add(d.getUserId());
        }
        return ids;
    }

    private String normalizeDimension(String dimension) {
        if (dimension == null) {
            return "college";
        }
        return switch (dimension.trim()) {
            case "major", "className", "grade", "college" -> dimension.trim();
            default -> "college";
        };
    }
}
