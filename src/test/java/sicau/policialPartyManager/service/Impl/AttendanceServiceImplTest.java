package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sicau.policialPartyManager.api.dto.AttendanceVo;
import sicau.policialPartyManager.model.entity.ActivityDetail;
import sicau.policialPartyManager.model.entity.ActivityLeave;
import sicau.policialPartyManager.model.entity.SignRecord;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.ActivityDetailMapper;
import sicau.policialPartyManager.repository.ActivityLeaveMapper;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.SignRecordMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.ActivityScopeService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 考勤四态与统计口径单测（#78）。
 * <p>正常签到 / 迟到 / 请假 / 缺席 的判定，以及参与率分母（应到）的处理。
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private SignRecordMapper signRecordMapper;
    @Mock
    private ActivityLeaveMapper activityLeaveMapper;
    @Mock
    private ActivityDetailMapper activityDetailMapper;
    @Mock
    private UserDetailMapper userDetailMapper;
    @Mock
    private BranchMapper branchMapper;
    @Mock
    private ActivityScopeService activityScopeService;

    @InjectMocks
    private AttendanceServiceImpl service;

    private static final Long ACT = 1L;
    private final LocalDateTime signStart = LocalDateTime.of(2026, 11, 8, 14, 0);

    @Test
    void onTimeSignIsNormalAndAfterStartIsLate() {
        givenSignStart();
        givenSigns(
                sign(10L, signStart.minusMinutes(10)),  // 正常
                sign(11L, signStart.plusMinutes(5))     // 迟到
        );
        givenLeaves();
        when(activityScopeService.expectedCount(ACT)).thenReturn(null);

        AttendanceVo vo = service.of(ACT);

        assertEquals(2, vo.getActualCount());
        assertEquals(1, vo.getLateCount());
        assertEquals(AttendanceServiceImpl.SIGNED_NORMAL, vo.getSigned().get(0).getAttendanceStatus());
        assertEquals(AttendanceServiceImpl.SIGNED_LATE, vo.getSigned().get(1).getAttendanceStatus());
    }

    @Test
    void leaveWithoutSignCountsAsLeave() {
        givenSignStart();
        givenSigns();
        givenLeaves(leave(20L, "生病"));
        when(activityScopeService.expectedCount(ACT)).thenReturn(null);

        AttendanceVo vo = service.of(ACT);

        assertEquals(1, vo.getLeaveCount());
        assertEquals(AttendanceServiceImpl.LEAVE, vo.getLeave().get(0).getAttendanceStatus());
        assertEquals("生病", vo.getLeave().get(0).getLeaveReason());
    }

    @Test
    void leaveButActuallySignedCountsAsSigned() {
        // 已请假的人到场签到：以签到为准，不计入请假名单
        givenSignStart();
        givenSigns(sign(20L, signStart.plusMinutes(1)));
        givenLeaves(leave(20L, "临时能来"));
        when(activityScopeService.expectedCount(ACT)).thenReturn(null);

        AttendanceVo vo = service.of(ACT);

        assertEquals(0, vo.getLeaveCount());
        assertEquals(1, vo.getActualCount());
    }

    @Test
    void absentIsExpectedButNeitherSignedNorLeft() {
        givenSignStart();
        givenSigns(sign(10L, signStart.minusMinutes(5)));
        givenLeaves(leave(20L, "生病"));
        when(activityScopeService.expectedCount(ACT)).thenReturn(3);
        when(activityScopeService.expectedUserIds(ACT)).thenReturn(List.of(10L, 20L, 30L));
        givenUserDetail(30L);

        AttendanceVo vo = service.of(ACT);

        assertEquals(1, vo.getAbsentCount());
        assertEquals(30L, vo.getAbsent().get(0).getUserId());
        assertEquals(AttendanceServiceImpl.ABSENT, vo.getAbsent().get(0).getAttendanceStatus());
    }

    @Test
    void leaveDoesNotAppearInAbsentList() {
        givenSignStart();
        givenSigns();
        givenLeaves(leave(20L, "生病"));
        when(activityScopeService.expectedCount(ACT)).thenReturn(1);
        when(activityScopeService.expectedUserIds(ACT)).thenReturn(List.of(20L));

        AttendanceVo vo = service.of(ACT);

        assertEquals(0, vo.getAbsentCount(), "请假的人不应出现在缺席名单");
        assertTrue(vo.getAbsent().isEmpty());
    }

    @Test
    void participationRateIsActualOverExpected() {
        givenSignStart();
        givenSigns(sign(10L, signStart.minusMinutes(1)));
        givenLeaves();
        when(activityScopeService.expectedCount(ACT)).thenReturn(4);
        when(activityScopeService.expectedUserIds(ACT)).thenReturn(List.of(10L, 11L, 12L, 13L));

        AttendanceVo vo = service.of(ACT);

        assertEquals(0.25, vo.getParticipationRate());
    }

    @Test
    void unboundedScopeYieldsNullAbsentAndRate() {
        // 活动无参与范围：应到不可界定，不猜
        givenSignStart();
        givenSigns(sign(10L, signStart.minusMinutes(1)));
        givenLeaves();
        when(activityScopeService.expectedCount(ACT)).thenReturn(null);

        AttendanceVo vo = service.of(ACT);

        assertNull(vo.getExpectedCount());
        assertNull(vo.getAbsentCount());
        assertNull(vo.getParticipationRate());
        assertTrue(vo.getAbsent().isEmpty());
    }

    @Test
    void zeroExpectedDoesNotDivideByZero() {
        givenSignStart();
        givenSigns();
        givenLeaves();
        when(activityScopeService.expectedCount(ACT)).thenReturn(0);
        when(activityScopeService.expectedUserIds(ACT)).thenReturn(List.of());

        AttendanceVo vo = service.of(ACT);

        assertNull(vo.getParticipationRate());
    }

    // ---------- helpers ----------

    private void givenSignStart() {
        ActivityDetail d = new ActivityDetail();
        d.setActivityId(ACT);
        d.setSignStart(signStart);
        lenient().when(activityDetailMapper.selectById(ACT)).thenReturn(d);
    }

    private void givenSigns(SignRecord... records) {
        lenient().when(signRecordMapper.selectList(any())).thenReturn(List.of(records));
    }

    private void givenLeaves(ActivityLeave... leaves) {
        lenient().when(activityLeaveMapper.selectList(any())).thenReturn(List.of(leaves));
    }

    private void givenUserDetail(Long userId) {
        UserDetail d = new UserDetail();
        d.setUserId(userId);
        d.setName("成员" + userId);
        lenient().when(userDetailMapper.selectById(userId)).thenReturn(d);
    }

    private SignRecord sign(Long userId, LocalDateTime at) {
        SignRecord s = new SignRecord();
        s.setActivityId(ACT);
        s.setUserId(userId);
        s.setSignTime(at);
        return s;
    }

    private ActivityLeave leave(Long userId, String reason) {
        ActivityLeave l = new ActivityLeave();
        l.setActivityId(ACT);
        l.setUserId(userId);
        l.setReason(reason);
        l.setStatus(1);
        return l;
    }
}
