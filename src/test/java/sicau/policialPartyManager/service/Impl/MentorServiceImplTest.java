package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sicau.policialPartyManager.model.entity.Identity;
import sicau.policialPartyManager.model.entity.User;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.model.entity.UserMentor;
import sicau.policialPartyManager.repository.IdentityMapper;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 培养联系人规则单测（#72）。
 * <p>覆盖：1~2 名上限、幂等指派、不能指派本人、党员门槛、跨支部拒绝、批量逐条结果。
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
    private IdentityMapper identityMapper;
    @Mock
    private BranchScopeService branchScopeService;

    @InjectMocks
    private MentorServiceImpl service;

    private static final sicau.policialPartyManager.model.records.User OPERATOR =
            new sicau.policialPartyManager.model.records.User("zhang", "branch_admin", 7L);
    private static final Long USER = 42L;
    private static final Long MENTOR = 100L;

    // ---------- helpers ----------

    private UserDetail detailOf(Long userId, Long identityId) {
        UserDetail d = new UserDetail();
        d.setUserId(userId);
        d.setIdentityId(identityId);
        return d;
    }

    private Identity identityOf(Long id, int level) {
        Identity i = new Identity();
        i.setId(id);
        i.setLevel(level);
        return i;
    }

    /** 100 号是可担任培养联系人的正式党员（level=6） */
    private void givenPartyMemberMentor() {
        when(userMapper.selectById(MENTOR)).thenReturn(new User());
        when(userDetailMapper.selectById(MENTOR)).thenReturn(detailOf(MENTOR, 6L));
        when(identityMapper.selectById(6L)).thenReturn(identityOf(6L, 6));
    }

    // ---------- 指派 ----------

    @Test
    void assignFirstMentor() {
        givenPartyMemberMentor();
        when(userMentorMapper.selectOne(any())).thenReturn(null);
        when(userMentorMapper.selectCount(any())).thenReturn(0L);

        service.assign(OPERATOR, USER, MENTOR);

        verify(userMentorMapper).insert(any(UserMentor.class));
    }

    @Test
    void thirdMentorRejected() {
        givenPartyMemberMentor();
        when(userMentorMapper.selectOne(any())).thenReturn(null);
        when(userMentorMapper.selectCount(any())).thenReturn(2L);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.assign(OPERATOR, USER, MENTOR));
        assertTrue(e.getMessage().contains("最多"), e.getMessage());
        verify(userMentorMapper, never()).insert(any(UserMentor.class));
    }

    @Test
    void duplicateAssignIsIdempotent() {
        givenPartyMemberMentor();
        UserMentor existed = new UserMentor();
        existed.setId(1L);
        existed.setStatus(1);
        when(userMentorMapper.selectOne(any())).thenReturn(existed);

        service.assign(OPERATOR, USER, MENTOR);

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
        when(userMapper.selectById(MENTOR)).thenReturn(null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.assign(OPERATOR, USER, MENTOR));
        assertTrue(e.getMessage().contains("不存在"), e.getMessage());
    }

    @Test
    void nonPartyMemberCannotBeMentor() {
        // DDL 注释声明「培养联系人须为党员」，代码必须真的拦住非党员
        when(userMapper.selectById(MENTOR)).thenReturn(new User());
        when(userDetailMapper.selectById(MENTOR)).thenReturn(detailOf(MENTOR, 3L)); // 积极分子
        when(identityMapper.selectById(3L)).thenReturn(identityOf(3L, 3));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.assign(OPERATOR, USER, MENTOR));
        assertTrue(e.getMessage().contains("党员"), e.getMessage());
    }

    @Test
    void unregisteredIdentityCannotBeMentor() {
        when(userMapper.selectById(MENTOR)).thenReturn(new User());
        when(userDetailMapper.selectById(MENTOR)).thenReturn(detailOf(MENTOR, null));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.assign(OPERATOR, USER, MENTOR));
        assertTrue(e.getMessage().contains("未登记"), e.getMessage());
    }

    @Test
    void probationaryMemberCanBeMentor() {
        // level=5 预备党员也合格（门槛 >= 5）
        when(userMapper.selectById(MENTOR)).thenReturn(new User());
        when(userDetailMapper.selectById(MENTOR)).thenReturn(detailOf(MENTOR, 5L));
        when(identityMapper.selectById(5L)).thenReturn(identityOf(5L, 5));
        when(userMentorMapper.selectOne(any())).thenReturn(null);
        when(userMentorMapper.selectCount(any())).thenReturn(0L);

        service.assign(OPERATOR, USER, MENTOR);

        verify(userMentorMapper).insert(any(UserMentor.class));
    }

    @Test
    void crossBranchAssignRejected() {
        doThrow(new IllegalArgumentException("用户不存在"))
                .when(branchScopeService).requireManageableUser(OPERATOR, USER);

        assertThrows(IllegalArgumentException.class,
                () -> service.assign(OPERATOR, USER, MENTOR));
    }

    // ---------- 查询 ----------

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
                () -> service.remove(OPERATOR, USER, MENTOR));
        assertTrue(e.getMessage().contains("不存在"), e.getMessage());
    }

    // ---------- 批量 ----------

    @Test
    void batchOverLimitRejectedBeforeAnyWrite() {
        List<Long> ids = new ArrayList<>();
        for (long i = 0; i < ScopedMemberServiceImpl.BATCH_LIMIT + 1; i++) {
            ids.add(i);
        }
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.batchAssign(OPERATOR, ids, MENTOR));
        assertTrue(e.getMessage().contains("分批"), e.getMessage());
        verify(userMentorMapper, never()).insert(any(UserMentor.class));
    }

    @Test
    void batchEmptyRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.batchAssign(OPERATOR, List.of(), MENTOR));
    }

    @Test
    void batchReportsPerItemFailure() {
        lenient().when(userMapper.selectById(MENTOR)).thenReturn(new User());
        lenient().when(userDetailMapper.selectById(MENTOR)).thenReturn(detailOf(MENTOR, 6L));
        lenient().when(identityMapper.selectById(6L)).thenReturn(identityOf(6L, 6));
        lenient().when(userMentorMapper.selectOne(any())).thenReturn(null);
        lenient().when(userMentorMapper.selectCount(any())).thenReturn(0L);
        lenient().doThrow(new IllegalArgumentException("用户不存在"))
                .when(branchScopeService).requireManageableUser(any(), any());
        doNothing().when(branchScopeService).requireManageableUser(any(), org.mockito.ArgumentMatchers.eq(42L));

        var result = service.batchAssign(OPERATOR, List.of(42L, 43L), MENTOR);

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(43L, result.getErrors().get(0).getId());
    }
}
