package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sicau.policialPartyManager.model.entity.UserMentor;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.repository.UserMapper;
import sicau.policialPartyManager.repository.UserMentorMapper;
import sicau.policialPartyManager.service.BranchScopeService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 培养联系人规则单测（#72）。
 */
@ExtendWith(MockitoExtension.class)
class MentorServiceImplTest {

    @Mock
    private UserMentorMapper userMentorMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserDetailMapper userDetailMapper;
    @Mock
    private BranchScopeService branchScopeService;

    @InjectMocks
    private MentorServiceImpl service;

    private static final User OPERATOR = new User("zhang", "branch_admin", 7L);
    private static final Long USER = 42L;

    @Test
    void assignFirstMentor() {
        when(userMapper.selectById(100L)).thenReturn(new sicau.policialPartyManager.model.entity.User());
        when(userMentorMapper.selectOne(any())).thenReturn(null);
        when(userMentorMapper.selectCount(any())).thenReturn(0L);

        service.assign(OPERATOR, USER, 100L);

        verify(userMentorMapper).insert(any(UserMentor.class));
    }

    @Test
    void thirdMentorRejected() {
        when(userMapper.selectById(100L)).thenReturn(new sicau.policialPartyManager.model.entity.User());
        when(userMentorMapper.selectOne(any())).thenReturn(null);
        when(userMentorMapper.selectCount(any())).thenReturn(2L); // 已有 2 名

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.assign(OPERATOR, USER, 100L));
        assertTrue(e.getMessage().contains("最多"), e.getMessage());
        verify(userMentorMapper, never()).insert(any(UserMentor.class));
    }

    @Test
    void duplicateAssignIsIdempotent() {
        UserMentor existed = new UserMentor();
        existed.setId(1L);
        existed.setStatus(1);
        when(userMapper.selectById(100L)).thenReturn(new sicau.policialPartyManager.model.entity.User());
        when(userMentorMapper.selectOne(any())).thenReturn(existed);

        service.assign(OPERATOR, USER, 100L);

        verify(userMentorMapper, never()).insert(any(UserMentor.class));
    }

    @Test
    void cannotAssignSelfAsMentor() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.assign(OPERATOR, USER, USER));
        assertTrue(e.getMessage().contains("本人"), e.getMessage());
    }

    @Test
    void missingMentorRejected() {
        when(userMapper.selectById(100L)).thenReturn(null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.assign(OPERATOR, USER, 100L));
        assertTrue(e.getMessage().contains("不存在"), e.getMessage());
    }

    @Test
    void crossBranchAssignRejected() {
        doThrow(new IllegalArgumentException("用户不存在"))
                .when(branchScopeService).requireManageableUser(OPERATOR, USER);

        assertThrows(IllegalArgumentException.class,
                () -> service.assign(OPERATOR, USER, 100L));
    }

    @Test
    void mentorsOfReturnsActiveOnly() {
        UserMentor a = new UserMentor();
        a.setMentorId(100L);
        a.setStatus(1);
        UserMentor b = new UserMentor();
        b.setMentorId(101L);
        b.setStatus(1);
        when(userMentorMapper.selectList(any())).thenReturn(List.of(a, b));

        assertEquals(List.of(100L, 101L), service.mentorsOf(USER));
    }

    @Test
    void removeMissingLinkRejected() {
        when(userMentorMapper.selectOne(any())).thenReturn(null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.remove(OPERATOR, USER, 100L));
        assertTrue(e.getMessage().contains("不存在"), e.getMessage());
    }

    @Test
    void batchOverLimitRejectedBeforeAnyWrite() {
        List<Long> ids = new ArrayList<>();
        for (long i = 0; i < ScopedMemberServiceImpl.BATCH_LIMIT + 1; i++) {
            ids.add(i);
        }
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.batchAssign(OPERATOR, ids, 100L));
        assertTrue(e.getMessage().contains("分批"), e.getMessage());
        verify(userMentorMapper, never()).insert(any(UserMentor.class));
    }

    @Test
    void batchEmptyRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.batchAssign(OPERATOR, List.of(), 100L));
    }

    @Test
    void batchReportsPerItemFailure() {
        // 43 号成员越权 → 该条失败，其余成功；上限与所有权检查都用 lenient，
        // 让测试专注于「逐条结果」这一个外部行为。
        lenient().when(userMapper.selectById(100L))
                .thenReturn(new sicau.policialPartyManager.model.entity.User());
        lenient().when(userMentorMapper.selectOne(any())).thenReturn(null);
        lenient().when(userMentorMapper.selectCount(any())).thenReturn(0L);
        lenient().doThrow(new IllegalArgumentException("用户不存在"))
                .when(branchScopeService).requireManageableUser(any(), any());
        // 42 号放行（不抛），43 号抛
        org.mockito.Mockito.doNothing()
                .when(branchScopeService).requireManageableUser(any(), org.mockito.ArgumentMatchers.eq(42L));

        var result = service.batchAssign(OPERATOR, List.of(42L, 43L), 100L);

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(43L, result.getErrors().get(0).getId());
    }
}
