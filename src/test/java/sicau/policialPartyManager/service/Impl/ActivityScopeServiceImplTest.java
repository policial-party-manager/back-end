package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sicau.policialPartyManager.model.entity.ActivityIdentity;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.ActivityIdentityMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 活动参与范围判定单测（#69 提取的共享口径）。
 * <p>这些断言原属报名侧，#69 把判定抽成共享组件后随实现一起迁到这里，
 * 保证报名、签到、统计三处用的是同一份规则。
 */
@ExtendWith(MockitoExtension.class)
class ActivityScopeServiceImplTest {

    @Mock
    private ActivityIdentityMapper activityIdentityMapper;
    @Mock
    private UserDetailMapper userDetailMapper;

    @InjectMocks
    private ActivityScopeServiceImpl service;

    private static final Long ACTIVITY = 1L;
    private static final Long USER = 42L;

    @Test
    void noRowsMeansOpenToAll() {
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of());

        service.requireEligible(ACTIVITY, USER);   // 不抛异常即通过
        assertTrue(service.isEligible(ACTIVITY, USER));
    }

    @Test
    void gradeMatchAllows() {
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        row.setCollege(null); // 不限学院
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUser("2023", "软件学院", null);

        assertTrue(service.isEligible(ACTIVITY, USER));
    }

    @Test
    void gradeMismatchRejected() {
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2024);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUser("2023", null, null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.requireEligible(ACTIVITY, USER));
        assertTrue(e.getMessage().contains("范围"), e.getMessage());
    }

    @Test
    void collegeMatchRequiredWhenRowHasCollege() {
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        row.setCollege("信息工程学院");
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUser("2023", "软件学院", null);

        assertFalse(service.isEligible(ACTIVITY, USER));
    }

    @Test
    void anyRowHitPasses() {
        ActivityIdentity row1 = new ActivityIdentity();
        row1.setGrade(2024);
        row1.setCollege("A");
        ActivityIdentity row2 = new ActivityIdentity();
        row2.setGrade(2023);
        row2.setCollege("软件学院");
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row1, row2));
        givenUser(" 2023 ", "软件学院", null); // trim 后比较

        assertTrue(service.isEligible(ACTIVITY, USER));
    }

    @Test
    void missingUserDetailRejectedWhenScopeConfigured() {
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        when(userDetailMapper.selectById(USER)).thenReturn(null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.requireEligible(ACTIVITY, USER));
        assertTrue(e.getMessage().contains("范围"), e.getMessage());
    }

    @Test
    void identityMismatchRejected() {
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        row.setTargetIdentity(5L); // 限预备党员
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUser("2023", null, 6L); // 正式党员

        assertFalse(service.isEligible(ACTIVITY, USER));
    }

    @Test
    void unregisteredIdentityGetsActionableMessage() {
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        row.setTargetIdentity(5L);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUser("2023", null, null); // 未登记

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.requireEligible(ACTIVITY, USER));
        assertTrue(e.getMessage().contains("未登记"), e.getMessage());
        assertTrue(e.getMessage().contains("支部管理员"), e.getMessage());
    }

    @Test
    void identityHitPasses() {
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        row.setTargetIdentity(6L);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUser("2023", null, 6L);

        assertTrue(service.isEligible(ACTIVITY, USER));
    }

    @Test
    void rowWithoutTargetIdentityKeepsLegacyBehavior() {
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));
        givenUser("2023", null, null); // 未登记也应放行

        assertTrue(service.isEligible(ACTIVITY, USER));
    }

    // ---------- 应到人数 ----------

    @Test
    void expectedCountNullWhenNoScopeRows() {
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of());

        assertNull(service.expectedCount(ACTIVITY));
    }

    @Test
    void expectedCountCountsMatchingMembers() {
        ActivityIdentity row = new ActivityIdentity();
        row.setGrade(2023);
        when(activityIdentityMapper.selectList(any())).thenReturn(List.of(row));

        UserDetail a = detailOf("2023", null, null);
        UserDetail b = detailOf("2024", null, null);
        UserDetail c = detailOf("2023", null, null);
        when(userDetailMapper.selectList(any())).thenReturn(List.of(a, b, c));

        assertEquals(2, service.expectedCount(ACTIVITY));
    }

    // ---------- helpers ----------

    private UserDetail detailOf(String grade, String college, Long identityId) {
        UserDetail ud = new UserDetail();
        ud.setGrade(grade);
        ud.setCollege(college);
        ud.setIdentityId(identityId);
        return ud;
    }

    private void givenUser(String grade, String college, Long identityId) {
        when(userDetailMapper.selectById(USER)).thenReturn(detailOf(grade, college, identityId));
    }
}
