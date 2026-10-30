package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.ActivityDetail;
import sicau.policialPartyManager.model.entity.ActivityIdentity;
import sicau.policialPartyManager.model.entity.ActivitySignup;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.ActivityDetailMapper;
import sicau.policialPartyManager.repository.ActivityIdentityMapper;
import sicau.policialPartyManager.repository.ActivityMapper;
import sicau.policialPartyManager.repository.ActivitySignupMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 活动报名业务规则单测（#53 Agent Brief 的验收条件逐条对应）。
 */
@ExtendWith(MockitoExtension.class)
class ActivitySignupServiceImplTest {

    @Mock
    private ActivitySignupMapper signupMapper;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivityDetailMapper activityDetailMapper;
    @Mock
    private ActivityIdentityMapper activityIdentityMapper;
    @Mock
    private UserDetailMapper userDetailMapper;

    @InjectMocks
    private ActivitySignupServiceImpl service;

    private Activity activity;
    private ActivityDetail detail;

    @BeforeEach
    void setUp() {
        activity = new Activity();
        activity.setId(1L);
        activity.setTitle("理论学习");
        // start_time 为空 -> 视为尚未开始

        detail = new ActivityDetail();
        detail.setActivityId(1L);
        detail.setStatus(1);
    }

    // ---------- 报名 ----------

    @Test
    void signupSucceedsAndReturnsRecord() {
        givenOpenActivity();
        when(signupMapper.selectCount(any())).thenReturn(0L);
        when(signupMapper.insert(any(ActivitySignup.class))).thenAnswer(inv -> {
            inv.getArgument(0, ActivitySignup.class).setId(9L);
            return 1;
        });

        ActivitySignup record = service.signup(1L, 42L);

        assertEquals(9L, record.getId());
        assertEquals(1L, record.getActivityId());
        assertEquals(42L, record.getUserId());
        assertNotNull(record.getSignupTime());
    }

    @Test
    void missingActivityRejected() {
        when(activityMapper.selectById(1L)).thenReturn(null);
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
        assertTrue(e.getMessage().contains("活动不存在"), e.getMessage());
    }

    @Test
    void unpublishedActivityRejected() {
        detail.setStatus(0);
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(activityDetailMapper.selectById(1L)).thenReturn(detail);
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
        assertTrue(e.getMessage().contains("未发布"), e.getMessage());
    }

    @Test
    void startedActivityRejected() {
        activity.setStartTime(LocalDateTime.now().minusHours(1));
        givenActivityAndDetail();
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
        assertTrue(e.getMessage().contains("已开始"), e.getMessage());
    }

    @Test
    void nullStartTimeMeansNotStarted() {
        // activity.startTime 为空 -> 可以报名
        givenOpenActivity();
        when(signupMapper.selectCount(any())).thenReturn(0L);

        assertNotNull(service.signup(1L, 42L));
    }

    @Test
    void duplicateSignupRejected() {
        givenOpenActivity();
        when(signupMapper.selectCount(any())).thenReturn(1L);
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
        assertTrue(e.getMessage().contains("已报名"), e.getMessage());
        verify(signupMapper, never()).insert(any(ActivitySignup.class));
    }

    @Test
    void duplicateKeyFromRaceMappedToBusinessError() {
        givenOpenActivity();
        when(signupMapper.selectCount(any())).thenReturn(0L);
        when(signupMapper.insert(any(ActivitySignup.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
    }

    @Test
    void capacityLimitEnforcedWhenMaxSet() {
        detail.setMaxParticipants(3);
        givenActivityAndDetail();
        when(signupMapper.selectCount(any())).thenReturn(0L, 3L); // 1st 重复检查, 2nd 已报名数

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
        assertTrue(e.getMessage().contains("名额已满"), e.getMessage());
    }

    @Test
    void nullMaxParticipantsMeansUnlimited() {
        detail.setMaxParticipants(null);
        givenOpenActivity();
        when(signupMapper.selectCount(any())).thenReturn(0L, 9999L);

        assertNotNull(service.signup(1L, 42L));
    }

    // ---------- 范围（年级/学院；身份维度本期跳过） ----------

    @Test
    void noEligibilityRowsMeansOpenToAll() {
        givenOpenActivity();
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of());
        when(signupMapper.selectCount(any())).thenReturn(0L);

        assertNotNull(service.signup(1L, 42L));
    }

    @Test
    void gradeMatchAllowsSignup() {
        givenActivityAndDetail();
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        row.setCollege(null); // 不限学院
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUserDetail("2023", "软件学院");
        when(signupMapper.selectCount(any())).thenReturn(0L);

        assertNotNull(service.signup(1L, 42L));
    }

    @Test
    void gradeMismatchRejected() {
        givenActivityAndDetail();
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2024);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUserDetail("2023", null);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
        assertTrue(e.getMessage().contains("范围"), e.getMessage());
    }

    @Test
    void collegeMatchRequiredWhenRowHasCollege() {
        givenActivityAndDetail();
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        row.setCollege("信息工程学院");
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUserDetail("2023", "软件学院");

        assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
    }

    @Test
    void anyRowHitPasses() {
        givenActivityAndDetail();
        ActivityIdentity row1 = new ActivityIdentity();
        row1.setGrade(2024);
        row1.setCollege("A");
        ActivityIdentity row2 = new ActivityIdentity();
        row2.setGrade(2023);
        row2.setCollege("软件学院");
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row1, row2));
        givenUserDetail(" 2023 ", "软件学院"); // trim 后比较
        when(signupMapper.selectCount(any())).thenReturn(0L);

        assertNotNull(service.signup(1L, 42L));
    }

    @Test
    void missingUserDetailRejectedWhenEligibilityConfigured() {
        givenActivityAndDetail();
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        when(userDetailMapper.selectById(42L)).thenReturn(null);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
        assertTrue(e.getMessage().contains("范围"), e.getMessage());
    }

    // ---------- 身份维度（#61 接通） ----------

    @Test
    void identityMismatchRejected() {
        givenActivityAndDetail();
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        row.setTargetIdentity(5L); // 限预备党员
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUserDetailWithIdentity("2023", null, 6L); // 正式党员

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
        assertTrue(e.getMessage().contains("范围"), e.getMessage());
    }

    @Test
    void unregisteredIdentityGetsActionableMessage() {
        givenActivityAndDetail();
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        row.setTargetIdentity(5L);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUserDetailWithIdentity("2023", null, null); // 未登记

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
        assertTrue(e.getMessage().contains("未登记"), e.getMessage());
        assertTrue(e.getMessage().contains("支部管理员"), e.getMessage());
    }

    @Test
    void identityHitPasses() {
        givenActivityAndDetail();
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        row.setTargetIdentity(6L);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUserDetailWithIdentity("2023", null, 6L); // 三维度全中
        when(signupMapper.selectCount(any())).thenReturn(0L);

        assertNotNull(service.signup(1L, 42L));
    }

    @Test
    void rowWithoutTargetIdentityKeepsLegacyBehavior() {
        // 行未设 target_identity（DDL NOT NULL，代码对脏数据宽松）：维持年级/学院语义
        givenActivityAndDetail();
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUserDetailWithIdentity("2023", null, null); // 未登记也应放行
        when(signupMapper.selectCount(any())).thenReturn(0L);

        assertNotNull(service.signup(1L, 42L));
    }

    // ---------- 取消 ----------

    @Test
    void cancelRemovesOwnSignup() {
        activity.setStartTime(LocalDateTime.now().plusDays(1)); // 未开始
        givenActivityAndDetail();
        ActivitySignup mine = new ActivitySignup();
        mine.setId(9L);
        mine.setActivityId(1L);
        mine.setUserId(42L);
        when(signupMapper.selectOne(any())).thenReturn(mine);

        service.cancel(1L, 42L);

        verify(signupMapper).deleteById(9L);
    }

    @Test
    void cancelWithoutSignupRejected() {
        givenActivityAndDetail();
        when(signupMapper.selectOne(any())).thenReturn(null);
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.cancel(1L, 42L));
        assertTrue(e.getMessage().contains("未报名"), e.getMessage());
        verify(signupMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void cancelAfterStartRejected() {
        activity.setStartTime(LocalDateTime.now().minusHours(1));
        givenActivityAndDetail();

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.cancel(1L, 42L));
        assertTrue(e.getMessage().contains("已开始"), e.getMessage());
    }

    // ---------- helpers ----------

    private void givenActivityAndDetail() {
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(activityDetailMapper.selectById(1L)).thenReturn(detail);
    }

    /** 无范围配置路径下的最小 stub（lenient 关闭时只 stub 用到的） */
    private void givenOpenActivity() {
        givenActivityAndDetail();
        // 未配置范围 -> 不查用户档案
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of());
    }

    private void givenUserDetailWithIdentity(String grade, String college, Long identityId) {
        UserDetail ud = new UserDetail();
        ud.setUserId(42L);
        ud.setGrade(grade);
        ud.setCollege(college);
        ud.setIdentityId(identityId);
        when(userDetailMapper.selectById(42L)).thenReturn(ud);
    }

    private void givenUserDetail(String grade, String college) {
        UserDetail ud = new UserDetail();
        ud.setUserId(42L);
        ud.setGrade(grade);
        ud.setCollege(college);
        when(userDetailMapper.selectById(42L)).thenReturn(ud);
    }
}
