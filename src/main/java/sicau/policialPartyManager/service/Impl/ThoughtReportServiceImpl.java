package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.ThoughtReportSubmitRequest;
import sicau.policialPartyManager.api.dto.ThoughtReportVo;
import sicau.policialPartyManager.api.dto.ThoughtReviewRequest;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.model.entity.ThoughtReport;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.ThoughtReportMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.ThoughtReportService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThoughtReportServiceImpl implements ThoughtReportService {

    /** 审阅状态：1 待审阅 / 2 通过 / 3 驳回（与 tb_thought_report CHECK 一致） */
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;

    /** 越权与不存在统一此语义，不泄露记录存在性（#33 防串审决策） */
    private static final String NOT_FOUND = "思想汇报不存在";

    private final ThoughtReportMapper reportMapper;
    private final UserDetailMapper userDetailMapper;
    private final BranchMapper branchMapper;

    @Override
    public ThoughtReportVo submit(ThoughtReportSubmitRequest request, User user) {
        Long userId = user.userId();
        UserDetail detail = userDetailMapper.selectById(userId);
        if (detail == null || detail.getBranchId() == null) {
            throw new IllegalArgumentException("提交人没有所属支部，无法确定审阅人");
        }
        Branch branch = branchMapper.selectById(detail.getBranchId());
        if (branch == null || branch.getStatus() == null || branch.getStatus() != 1) {
            throw new IllegalArgumentException("所属支部不存在或已停用，无法确定审阅人");
        }
        if (branch.getSecretaryId() == null) {
            throw new IllegalArgumentException("所属支部未设置秘书，无法派定审阅人，请联系管理员");
        }
        if (branch.getSecretaryId().equals(userId)) {
            throw new IllegalArgumentException("支部秘书不能审阅自己的汇报，请由其他党员提交");
        }

        ThoughtReport report = new ThoughtReport();
        report.setUserId(userId);
        report.setReviewerId(branch.getSecretaryId());
        report.setTitle(request.getTitle().trim());
        report.setContent(request.getContent());
        report.setFile(trimToNull(request.getFile()));
        report.setStatus(STATUS_PENDING);
        report.setSubmitTime(LocalDateTime.now());
        reportMapper.insert(report);
        return toVo(report, nameOf(userId), nameOf(report.getReviewerId()), true);
    }

    @Override
    public PageResult<ThoughtReportVo> pageMine(long page, long size, Integer status, User user) {
        LambdaQueryWrapper<ThoughtReport> wrapper = new LambdaQueryWrapper<ThoughtReport>()
                .eq(ThoughtReport::getUserId, user.userId())
                .orderByDesc(ThoughtReport::getSubmitTime);
        applyStatus(wrapper, status);
        return page(page, size, wrapper);
    }

    @Override
    public void deleteMine(Long id, User user) {
        ThoughtReport report = requireOwned(id, user.userId());
        if (report.getStatus() != null && report.getStatus() != STATUS_PENDING) {
            throw new IllegalArgumentException("该汇报已审阅，不能撤回");
        }
        reportMapper.deleteById(id);
    }

    @Override
    public PageResult<ThoughtReportVo> pageForReviewer(long page, long size, Integer status, User reviewer) {
        LambdaQueryWrapper<ThoughtReport> wrapper = new LambdaQueryWrapper<ThoughtReport>()
                .eq(ThoughtReport::getReviewerId, reviewer.userId())
                .orderByDesc(ThoughtReport::getSubmitTime);
        applyStatus(wrapper, status);
        return page(page, size, wrapper);
    }

    @Override
    public ThoughtReportVo getForReviewer(Long id, User reviewer) {
        ThoughtReport report = reportMapper.selectById(id);
        if (report == null || !report.getReviewerId().equals(reviewer.userId())) {
            throw new IllegalArgumentException(NOT_FOUND);
        }
        return toVo(report, nameOf(report.getUserId()), nameOf(report.getReviewerId()), true);
    }

    @Override
    public ThoughtReportVo review(Long id, ThoughtReviewRequest request, User reviewer) {
        ThoughtReport report = reportMapper.selectById(id);
        if (report == null || !report.getReviewerId().equals(reviewer.userId())) {
            throw new IllegalArgumentException(NOT_FOUND);
        }
        if (report.getStatus() == null || report.getStatus() != STATUS_PENDING) {
            throw new IllegalArgumentException("该汇报已审阅，不能重复审阅");
        }
        report.setStatus(Boolean.TRUE.equals(request.getPass()) ? STATUS_APPROVED : STATUS_REJECTED);
        report.setReviewRemark(trimToNull(request.getRemark()));
        report.setReviewTime(LocalDateTime.now());
        reportMapper.updateById(report);
        return toVo(report, nameOf(report.getUserId()), nameOf(report.getReviewerId()), false);
    }

    @Override
    public PageResult<ThoughtReportVo> pageAll(long page, long size, Integer status, Long userId) {
        LambdaQueryWrapper<ThoughtReport> wrapper = new LambdaQueryWrapper<ThoughtReport>()
                .orderByDesc(ThoughtReport::getSubmitTime);
        if (userId != null) {
            wrapper.eq(ThoughtReport::getUserId, userId);
        }
        applyStatus(wrapper, status);
        return page(page, size, wrapper);
    }

    // ---------- 私有 ----------

    private void applyStatus(LambdaQueryWrapper<ThoughtReport> wrapper, Integer status) {
        if (status != null) {
            if (status != STATUS_PENDING && status != STATUS_APPROVED && status != STATUS_REJECTED) {
                throw new IllegalArgumentException("状态不合法（1 待审阅 / 2 通过 / 3 驳回）");
            }
            wrapper.eq(ThoughtReport::getStatus, status);
        }
    }

    private PageResult<ThoughtReportVo> page(long page, long size, LambdaQueryWrapper<ThoughtReport> wrapper) {
        Page<ThoughtReport> result = reportMapper.selectPage(new Page<>(page, size), wrapper);
        List<ThoughtReport> rows = result.getRecords();
        Set<Long> userIds = rows.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getUserId(), r.getReviewerId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = namesOf(userIds);
        List<ThoughtReportVo> vos = rows.stream()
                .map(r -> toVo(r, names.get(r.getUserId()), names.get(r.getReviewerId()), false))
                .toList();
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /** withContent=true 仅用于单条详情（本人或指派审阅人），列表不带正文 */
    private ThoughtReportVo toVo(ThoughtReport report, String submitterName, String reviewerName, boolean withContent) {
        ThoughtReportVo vo = new ThoughtReportVo();
        vo.setId(report.getId());
        vo.setUserId(report.getUserId());
        vo.setSubmitterName(submitterName);
        vo.setReviewerId(report.getReviewerId());
        vo.setReviewerName(reviewerName);
        vo.setTitle(report.getTitle());
        if (withContent) {
            vo.setContent(report.getContent());
        }
        vo.setFile(report.getFile());
        vo.setStatus(report.getStatus());
        vo.setStatusName(statusName(report.getStatus()));
        vo.setReviewRemark(report.getReviewRemark());
        vo.setSubmitTime(report.getSubmitTime());
        vo.setReviewTime(report.getReviewTime());
        return vo;
    }

    private String statusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case STATUS_PENDING -> "待审阅";
            case STATUS_APPROVED -> "通过";
            case STATUS_REJECTED -> "驳回";
            default -> "未知";
        };
    }

    private ThoughtReport requireOwned(Long id, Long userId) {
        ThoughtReport report = reportMapper.selectById(id);
        if (report == null || !report.getUserId().equals(userId)) {
            throw new IllegalArgumentException(NOT_FOUND);
        }
        return report;
    }

    private String nameOf(Long userId) {
        UserDetail detail = userDetailMapper.selectById(userId);
        return detail == null ? null : detail.getName();
    }

    private Map<Long, String> namesOf(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userDetailMapper.selectBatchIds(userIds).stream()
                .filter(d -> d.getName() != null)
                .collect(Collectors.toMap(UserDetail::getUserId, UserDetail::getName, (a, b) -> a));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
