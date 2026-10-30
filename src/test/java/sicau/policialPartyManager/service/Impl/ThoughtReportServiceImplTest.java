package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 思想汇报提交与审阅流单测（#33 Agent Brief 验收对照）。
 */
@ExtendWith(MockitoExtension.class)
class ThoughtReportServiceImplTest {

    @Mock
    private ThoughtReportMapper reportMapper;
    @Mock
    private UserDetailMapper userDetailMapper;
    @Mock
    private BranchMapper branchMapper;

    @InjectMocks
    private ThoughtReportServiceImpl service;

    private User member;
    private User secretary;
    private ThoughtReportSubmitRequest submit;

    @BeforeAll
    static void initMybatisPlusCache() {
        // 裸单测无 Spring 上下文：手动注册实体 TableInfo，否则 LambdaQueryWrapper
        // 惰性解析列名时抛 "can not find lambda cache"
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ThoughtReport.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                UserDetail.class);
    }

    @BeforeEach
    void setUp() {
        member = new User("stu001", "STUDENT", 42L);
        secretary = new User("sec001", "BRANCH_ADMIN", 7L);
        submit = new ThoughtReportSubmitRequest();
        submit.setTitle("2026年第三季度思想汇报");
        submit.setContent("正文……");
    }

    // ---------- 提交（自动派审） ----------

    @Test
    void submitAssignsBranchSecretary() {
        givenBranchSecretary(7L);
        when(reportMapper.insert(any(ThoughtReport.class))).thenAnswer(inv -> {
            inv.getArgument(0, ThoughtReport.class).setId(9L);
            return 1;
        });
        when(userDetailMapper.selectById(7L)).thenReturn(detail(7L, "书记", 3L));

        ThoughtReportVo vo = service.submit(submit, member);

        ArgumentCaptor<ThoughtReport> saved = ArgumentCaptor.forClass(ThoughtReport.class);
        verify(reportMapper).insert(saved.capture());
        assertEquals(42L, saved.getValue().getUserId());
        assertEquals(7L, saved.getValue().getReviewerId());
        assertEquals(1, saved.getValue().getStatus());
        assertNotNull(saved.getValue().getSubmitTime());
        assertEquals(9L, vo.getId());
        assertEquals("张三", vo.getSubmitterName());
    }

    @Test
    void submitRejectedWhenNoBranch() {
        UserDetail noBranch = new UserDetail();
        noBranch.setUserId(42L);
        when(userDetailMapper.selectById(42L)).thenReturn(noBranch);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.submit(submit, member));
        assertTrue(e.getMessage().contains("支部"), e.getMessage());
        verify(reportMapper, never()).insert(any(ThoughtReport.class));
    }

    @Test
    void submitRejectedWhenUserDetailMissing() {
        when(userDetailMapper.selectById(42L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> service.submit(submit, member));
    }

    @Test
    void submitRejectedWhenBranchDeleted() {
        when(userDetailMapper.selectById(42L)).thenReturn(detail(42L, "张三", 3L));
        Branch branch = new Branch();
        branch.setId(3L);
        branch.setStatus(0);
        when(branchMapper.selectById(3L)).thenReturn(branch);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.submit(submit, member));
        assertTrue(e.getMessage().contains("支部"), e.getMessage());
    }

    @Test
    void submitRejectedWhenBranchHasNoSecretary() {
        when(userDetailMapper.selectById(42L)).thenReturn(detail(42L, "张三", 3L));
        Branch branch = new Branch();
        branch.setId(3L);
        branch.setStatus(1);
        branch.setSecretaryId(null);
        when(branchMapper.selectById(3L)).thenReturn(branch);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.submit(submit, member));
        assertTrue(e.getMessage().contains("秘书"), e.getMessage());
    }

    @Test
    void submitRejectedWhenSecretaryIsSelf() {
        givenBranchSecretary(42L);
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.submit(submit, member));
        assertTrue(e.getMessage().contains("自己"), e.getMessage());
    }

    // ---------- 审阅 ----------

    @Test
    void approveSetsStatus2WithAuditFields() {
        ThoughtReport report = pendingReport();
        when(reportMapper.selectById(5L)).thenReturn(report);
        ThoughtReviewRequest request = new ThoughtReviewRequest();
        request.setPass(true);
        request.setRemark("很好");

        service.review(5L, request, secretary);

        assertEquals(2, report.getStatus());
        assertEquals("很好", report.getReviewRemark());
        assertNotNull(report.getReviewTime());
        verify(reportMapper).updateById(report);
    }

    @Test
    void rejectSetsStatus3() {
        ThoughtReport report = pendingReport();
        when(reportMapper.selectById(5L)).thenReturn(report);
        ThoughtReviewRequest request = new ThoughtReviewRequest();
        request.setPass(false);

        service.review(5L, request, secretary);

        assertEquals(3, report.getStatus());
    }

    @Test
    void reviewingOthersAssignmentHidesExistence() {
        ThoughtReport report = pendingReport();
        report.setReviewerId(99L); // 已改派他人
        when(reportMapper.selectById(5L)).thenReturn(report);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.review(5L, passRequest(), secretary));
        // 不泄露存在性：与「不存在」同一文案
        assertEquals("思想汇报不存在", e.getMessage());
        verify(reportMapper, never()).updateById(any(ThoughtReport.class));
    }

    @Test
    void missingReportForReviewRejected() {
        when(reportMapper.selectById(5L)).thenReturn(null);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.review(5L, passRequest(), secretary));
        assertEquals("思想汇报不存在", e.getMessage());
    }

    @Test
    void doubleReviewRejected() {
        ThoughtReport report = pendingReport();
        report.setStatus(2);
        when(reportMapper.selectById(5L)).thenReturn(report);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.review(5L, passRequest(), secretary));
        assertTrue(e.getMessage().contains("已审阅"), e.getMessage());
    }

    // ---------- 撤回删除 ----------

    @Test
    void pendingReportDeletableByOwner() {
        ThoughtReport report = pendingReport();
        when(reportMapper.selectById(5L)).thenReturn(report);

        service.deleteMine(5L, member);

        verify(reportMapper).deleteById(5L);
    }

    @Test
    void reviewedReportNotDeletable() {
        ThoughtReport report = pendingReport();
        report.setStatus(3);
        when(reportMapper.selectById(5L)).thenReturn(report);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.deleteMine(5L, member));
        assertTrue(e.getMessage().contains("已审阅"), e.getMessage());
        verify(reportMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void deletingOthersReportHidesExistence() {
        ThoughtReport report = pendingReport();
        report.setUserId(111L);
        when(reportMapper.selectById(5L)).thenReturn(report);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.deleteMine(5L, member));
        assertEquals("思想汇报不存在", e.getMessage());
    }

    // ---------- 列表可见性 ----------

    @Test
    void reviewerPageFiltersByCurrentUser() {
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<ThoughtReport>();
        page.setRecords(List.of());
        page.setTotal(0L);
        when(reportMapper.selectPage(any(), any())).thenReturn(page);

        service.pageForReviewer(1, 10, null, secretary);

        ArgumentCaptor<AbstractWrapper<ThoughtReport, ?, ?>> captor =
                ArgumentCaptor.forClass(AbstractWrapper.class);
        verify(reportMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getSqlSegment(); // 强制物化 lambda 条件
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(7L),
                "审阅列表必须按 reviewerId=当前用户过滤, sql=" + sql);
    }

    @Test
    void minePageFiltersByUserId() {
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<ThoughtReport>();
        page.setRecords(List.of());
        page.setTotal(0L);
        when(reportMapper.selectPage(any(), any())).thenReturn(page);

        service.pageMine(1, 10, 1, member);

        ArgumentCaptor<AbstractWrapper<ThoughtReport, ?, ?>> captor =
                ArgumentCaptor.forClass(AbstractWrapper.class);
        verify(reportMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getSqlSegment(); // 强制物化 lambda 条件
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(42L),
                "我的列表必须按 userId=当前用户过滤, sql=" + sql);
    }

    @Test
    void resubmitAfterRejectCreatesNewRow() {
        // 驳回(3)后重提：旧的保留、新行 status=1，两条并存 -> 提交不检查历史行，
        // selectCount 不参与提交路径；新行 id 递增
        givenBranchSecretary(7L);
        when(reportMapper.insert(any(ThoughtReport.class))).thenAnswer(inv -> {
            inv.getArgument(0, ThoughtReport.class).setId(10L);
            return 1;
        });
        ThoughtReportVo vo = service.submit(submit, member);

        assertEquals(10L, vo.getId());
        verify(reportMapper).insert(any(ThoughtReport.class));
    }

    // ---------- helpers ----------

    private void givenBranchSecretary(Long secretaryId) {
        when(userDetailMapper.selectById(42L)).thenReturn(detail(42L, "张三", 3L));
        Branch branch = new Branch();
        branch.setId(3L);
        branch.setStatus(1);
        branch.setSecretaryId(secretaryId);
        when(branchMapper.selectById(3L)).thenReturn(branch);
    }

    private ThoughtReport pendingReport() {
        ThoughtReport report = new ThoughtReport();
        report.setId(5L);
        report.setUserId(42L);
        report.setReviewerId(7L);
        report.setTitle("2026年第三季度思想汇报");
        report.setStatus(1);
        return report;
    }

    private ThoughtReviewRequest passRequest() {
        ThoughtReviewRequest request = new ThoughtReviewRequest();
        request.setPass(true);
        return request;
    }

    private UserDetail detail(Long userId, String name, Long branchId) {
        UserDetail d = new UserDetail();
        d.setUserId(userId);
        d.setName(name);
        d.setBranchId(branchId);
        return d;
    }
}
