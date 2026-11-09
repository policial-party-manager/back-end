package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.repository.UserDetailMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 支部数据范围判定单测（#70）。
 * <p>判据是成员所属支部 {@code tb_user_detail.branch_id}，不是 {@code tb_branch.secretary_id}。
 */
@ExtendWith(MockitoExtension.class)
class BranchScopeServiceImplTest {

    @Mock
    private UserDetailMapper userDetailMapper;

    @InjectMocks
    private BranchScopeServiceImpl service;

    private static final User SUPER = new User("admin", "super_admin", 1L);
    private static final User BRANCH = new User("zhang", "branch_admin", 7L);
    private static final User STUDENT = new User("li", "student", 9L);

    @Test
    void superAdminIsRecognized() {
        assertTrue(service.isSuperAdmin(SUPER));
        assertFalse(service.isSuperAdmin(BRANCH));
        assertTrue(service.isBranchAdmin(BRANCH));
    }

    @Test
    void branchIdComesFromUserDetailNotSecretary() {
        UserDetail d = new UserDetail();
        d.setUserId(7L);
        d.setBranchId(3L);
        when(userDetailMapper.selectById(7L)).thenReturn(d);

        assertEquals(3L, service.branchIdOf(BRANCH));
        assertNull(service.branchIdOf(STUDENT)); // 非支委没有支部范围
    }

    @Test
    void superAdminPassesAnyBranch() {
        service.requireManageableBranch(SUPER, 999L);   // 不抛异常
    }

    @Test
    void branchAdminPassesOwnBranchOnly() {
        UserDetail d = new UserDetail();
        d.setUserId(7L);
        d.setBranchId(3L);
        when(userDetailMapper.selectById(7L)).thenReturn(d);

        service.requireManageableBranch(BRANCH, 3L);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.requireManageableBranch(BRANCH, 4L));
        assertTrue(e.getMessage().contains("不存在"), e.getMessage());
    }

    @Test
    void branchAdminCrossBranchUserReadsAsNotExist() {
        // 操作者所属支部=3
        UserDetail me = new UserDetail();
        me.setUserId(7L);
        me.setBranchId(3L);
        when(userDetailMapper.selectById(7L)).thenReturn(me);

        // 目标成员属于支部 4
        UserDetail other = new UserDetail();
        other.setUserId(50L);
        other.setBranchId(4L);
        when(userDetailMapper.selectById(50L)).thenReturn(other);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.requireManageableUser(BRANCH, 50L));
        assertEquals("用户不存在", e.getMessage()); // 越权与真不存在同文案
    }

    @Test
    void branchAdminCanManageOwnBranchMember() {
        UserDetail me = new UserDetail();
        me.setUserId(7L);
        me.setBranchId(3L);
        when(userDetailMapper.selectById(7L)).thenReturn(me);

        UserDetail mine = new UserDetail();
        mine.setUserId(51L);
        mine.setBranchId(3L);
        when(userDetailMapper.selectById(51L)).thenReturn(mine);

        service.requireManageableUser(BRANCH, 51L); // 不抛异常
    }

    @Test
    void studentIsRejectedOnBothEntryPoints() {
        assertThrows(IllegalArgumentException.class,
                () -> service.requireManageableBranch(STUDENT, 3L));
        assertThrows(IllegalArgumentException.class,
                () -> service.requireManageableUser(STUDENT, 51L));
    }

    @Test
    void branchAdminWithoutBranchSeesNothing() {
        when(userDetailMapper.selectById(7L)).thenReturn(null);

        assertNull(service.branchIdOf(BRANCH));
        assertThrows(IllegalArgumentException.class,
                () -> service.requireManageableBranch(BRANCH, 3L));
    }
}
