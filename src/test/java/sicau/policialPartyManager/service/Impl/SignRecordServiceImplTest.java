package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import sicau.policialPartyManager.api.dto.SignInRequest;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.ActivityDetail;
import sicau.policialPartyManager.model.entity.ActivityType;
import sicau.policialPartyManager.model.entity.SignRecord;
import sicau.policialPartyManager.repository.ActivityDetailMapper;
import sicau.policialPartyManager.repository.ActivityMapper;
import sicau.policialPartyManager.repository.ActivityTypeMapper;
import sicau.policialPartyManager.repository.SignRecordMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 签到业务规则单测（#35 Agent Brief 的验收条件逐条对应）。
 */
@ExtendWith(MockitoExtension.class)
class SignRecordServiceImplTest {

    @Mock
    private SignRecordMapper signRecordMapper;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivityDetailMapper activityDetailMapper;
    @Mock
    private ActivityTypeMapper activityTypeMapper;

    @InjectMocks
    private SignRecordServiceImpl service;

    private Activity activity;
    private ActivityDetail detail;
    private SignInRequest request;

    @BeforeEach
    void setUp() {
        activity = new Activity();
        activity.setId(1L);
        activity.setTitle("主题党日活动");
        activity.setType(7);

        detail = new ActivityDetail();
        detail.setActivityId(1L);
        detail.setStatus(1);

        request = new SignInRequest();
        request.setActivityId(1L);
    }

    @Test
    void signInSucceedsWhenPublishedAndInsideWindow() {
        LocalDateTime now = LocalDateTime.now();
        detail.setSignStart(now.minusHours(1));
        detail.setSignEnd(now.plusHours(1));
        stubHappyPath(0L);
        when(signRecordMapper.insert(any(SignRecord.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SignRecord.class).setId(99L);
            return 1;
        });

        SignRecord record = service.sign(request, 42L);

        assertEquals(99L, record.getId());
        assertEquals(1L, record.getActivityId());
        assertEquals(42L, record.getUserId());
        assertEquals("sign", record.getSignType());
        assertNotNull(record.getSignTime());
    }

    @Test
    void emptyWindowBoundsMeanAlwaysOpen() {
        // signStart / signEnd 都为空 -> 任何时间可签
        stubHappyPath(0L);

        SignRecord record = service.sign(request, 42L);

        assertNotNull(record.getSignTime());
        verify(signRecordMapper).insert(any(SignRecord.class));
    }

    @Test
    void onlyStartBoundRespected() {
        detail.setSignStart(LocalDateTime.now().plusHours(1));
        givenActivityAndDetail();

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.sign(request, 42L));
        assertTrue(e.getMessage().contains("尚未开始"), e.getMessage());
        verify(signRecordMapper, never()).insert(any(SignRecord.class));
    }

    @Test
    void onlyEndBoundRespected() {
        detail.setSignEnd(LocalDateTime.now().minusHours(1));
        givenActivityAndDetail();

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.sign(request, 42L));
        assertTrue(e.getMessage().contains("已结束"), e.getMessage());
    }

    @Test
    void unpublishedActivityRejected() {
        detail.setStatus(0);
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(activityDetailMapper.selectById(1L)).thenReturn(detail);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.sign(request, 42L));
        assertTrue(e.getMessage().contains("未发布"), e.getMessage());
    }

    @Test
    void missingActivityRejected() {
        when(activityMapper.selectById(1L)).thenReturn(null);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.sign(request, 42L));
        assertTrue(e.getMessage().contains("活动不存在"), e.getMessage());
    }

    @Test
    void activityTypeWithoutSignRejected() {
        ActivityType type = new ActivityType();
        type.setNeedSign(0);
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(activityDetailMapper.selectById(1L)).thenReturn(detail);
        when(activityTypeMapper.selectById(7L)).thenReturn(type);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.sign(request, 42L));
        assertTrue(e.getMessage().contains("无需签到"), e.getMessage());
    }

    @Test
    void duplicateSignRejectedByQuery() {
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(activityDetailMapper.selectById(1L)).thenReturn(detail);
        when(activityTypeMapper.selectById(7L)).thenReturn(null);
        when(signRecordMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> service.sign(request, 42L));
        assertTrue(e.getMessage().contains("已签到"), e.getMessage());
        verify(signRecordMapper, never()).insert(any(SignRecord.class));
    }

    @Test
    void duplicateKeyFromRaceMappedToBusinessError() {
        stubHappyPath(0L);
        when(signRecordMapper.insert(any(SignRecord.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        assertThrows(IllegalArgumentException.class, () -> service.sign(request, 42L));
    }

    @Test
    void optionalFieldsTrimmedToNull() {
        stubHappyPath(0L);
        request.setDevice("  ");
        request.setLocation("");
        request.setRemark(null);

        SignRecord record = service.sign(request, 42L);

        assertNull(record.getDevice());
        assertNull(record.getLocation());
        assertNull(record.getRemark());
    }

    // ---- helpers ----

    private void givenActivityAndDetail() {
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(activityDetailMapper.selectById(1L)).thenReturn(detail);
    }

    private void stubHappyPath(long existingCount) {
        givenActivityAndDetail();
        when(activityTypeMapper.selectById(7L)).thenReturn(null); // 类型未配置 -> 不拦
        when(signRecordMapper.selectCount(any())).thenReturn(existingCount);
    }
}
