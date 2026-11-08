package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.ActivityDetail;
import sicau.policialPartyManager.model.entity.ActivitySignup;
import sicau.policialPartyManager.repository.ActivityDetailMapper;
import sicau.policialPartyManager.repository.ActivityMapper;
import sicau.policialPartyManager.repository.ActivitySignupMapper;
import sicau.policialPartyManager.service.ActivityScopeService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 活动报名业务规则单测（#53 Agent Brief 的验收条件逐条对应）。
 * <p>#69 起，参与范围判定抽到 {@link ActivityScopeService}，
 * 判定规则本身的断言迁至 {@code ActivityScopeServiceImplTest}；
 * 本类只验证「报名流程是否正确调用范围判定」以及报名自身的规则。
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
    private ActivityScopeService activityScopeService;

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
        when(signupMapper.selectCount(any())).thenReturn(0L, 3L);

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

    // ---------- 与范围判定的协作（判定规则本身见 ActivityScopeServiceImplTest） ----------

    @Test
    void scopeViolationBlocksSignupBeforeInsert() {
        givenActivityAndDetail();
        doThrow(new IllegalArgumentException("您不符合该活动的报名范围（年级/学院/政治身份）"))
                .when(activityScopeService).requireEligible(1L, 42L);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.signup(1L, 42L));
        assertTrue(e.getMessage().contains("范围"), e.getMessage());
        verify(signupMapper, never()).insert(any(ActivitySignup.class));
    }

    @Test
    void scopeIsCheckedWithActivityAndUserIds() {
        givenOpenActivity();
        when(signupMapper.selectCount(any())).thenReturn(0L);

        service.signup(1L, 42L);

        verify(activityScopeService).requireEligible(1L, 42L);
    }

    // ---------- 取消 ----------

    @Test
    void cancelRemovesOwnSignup() {
        activity.setStartTime(LocalDateTime.now().plusDays(1));
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

    private void givenOpenActivity() {
        givenActivityAndDetail();
    }
}
