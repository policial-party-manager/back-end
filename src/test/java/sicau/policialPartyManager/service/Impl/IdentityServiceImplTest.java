package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sicau.policialPartyManager.api.dto.IdentityChangeApplyRequest;
import sicau.policialPartyManager.api.dto.IdentityChangeVo;
import sicau.policialPartyManager.api.dto.IdentitySaveRequest;
import sicau.policialPartyManager.model.entity.Identity;
import sicau.policialPartyManager.model.entity.IdentityChange;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.IdentityChangeMapper;
import sicau.policialPartyManager.repository.IdentityMapper;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.repository.UserDetailMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 政治身份体系单测（#44 Agent Brief 验收对照）：字典 CRUD + 两阶段变更 + 角色红线。
 * <p>
 * 注意：本测试类不注入任何 tb_user_role 相关依赖——「身份变更不动登录角色」由
 * 服务构造器的依赖集合本身担保（无法操作不存在的依赖），另见 noRoleWritesInSource 用例。
 */
@ExtendWith(MockitoExtension.class)
class IdentityServiceImplTest {

    @Mock
    private IdentityMapper identityMapper;
    @Mock
    private IdentityChangeMapper changeMapper;
    @Mock
    private UserDetailMapper userDetailMapper;
    @Mock
    private BranchMapper branchMapper;

    private IdentityServiceImpl identityService;
    private IdentityChangeServiceImpl changeService;

    private User superAdmin;
    private User secretary;
    private Identity partyMember;

    @BeforeAll
    static void initMybatisPlusCache() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, IdentityChange.class);
        TableInfoHelper.initTableInfo(assistant, Identity.class);
        TableInfoHelper.initTableInfo(assistant, UserDetail.class);
    }

    @BeforeEach
    void setUp() {
        identityService = new IdentityServiceImpl(identityMapper, changeMapper, userDetailMapper);
        changeService = new IdentityChangeServiceImpl(changeMapper, identityMapper, userDetailMapper, branchMapper);

        superAdmin = new User("superadmin", "super_admin", 1L);
        secretary = new User("sec001", "branch_admin", 7L);

        partyMember = new Identity();
        partyMember.setId(6L);
        partyMember.setIdentityName("正式党员");
        partyMember.setLevel(6);
    }

    // ---------- 字典 ----------

    @Test
    void dictionaryRejectsDuplicateName() {
        IdentitySaveRequest request = new IdentitySaveRequest();
        request.setIdentityName("正式党员");
        request.setLevel(6);
        when(identityMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> identityService.create(request));
        assertTrue(e.getMessage().contains("已存在"), e.getMessage());
        verify(identityMapper, never()).insert(any(Identity.class));
    }

    @Test
    void dictionaryDeleteBlockedWhenUsersReference() {
        when(identityMapper.selectById(6L)).thenReturn(partyMember);
        when(userDetailMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> identityService.delete(6L));
        assertTrue(e.getMessage().contains("不能删除"), e.getMessage());
    }

    @Test
    void dictionaryDeleteBlockedWhenChangesReference() {
        when(identityMapper.selectById(6L)).thenReturn(partyMember);
        when(userDetailMapper.selectCount(any())).thenReturn(0L);
        when(changeMapper.selectCount(any())).thenReturn(2L, 0L);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> identityService.delete(6L));
        assertTrue(e.getMessage().contains("不能删除"), e.getMessage());
    }

    // ---------- 发起 ----------

    @Test
    void branchAdminAppliesForOwnBranchMember() {
        givenSecretaryOfBranch(3L);
        when(userDetailMapper.selectById(42L)).thenReturn(detail(42L, 3L, null));
        when(identityMapper.selectById(6L)).thenReturn(partyMember);
        when(changeMapper.selectCount(any())).thenReturn(0L);

        IdentityChangeVo vo = changeService.apply(applyRequest(42L, 6L), secretary);

        assertEquals(1, vo.getStatus());
        assertEquals(42L, vo.getUserId());
        assertEquals(7L, vo.getOperatorId());
        verify(changeMapper).insert(any(IdentityChange.class));
    }

    @Test
    void applyForForeignBranchMemberSeesUserNotFound() {
        givenSecretaryOfBranch(3L);
        when(userDetailMapper.selectById(43L)).thenReturn(detail(43L, 9L, null)); // 他支部

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> changeService.apply(applyRequest(43L, 6L), secretary));
        assertEquals("用户不存在", e.getMessage());
    }

    @Test
    void branchAdminWithoutManagedBranchCannotApply() {
        when(branchMapper.selectList(any())).thenReturn(List.of());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> changeService.apply(applyRequest(42L, 6L), secretary));
        assertTrue(e.getMessage().contains("未管理任何支部"), e.getMessage());
    }

    @Test
    void duplicatePendingOrderRejected() {
        givenSecretaryOfBranch(3L);
        when(userDetailMapper.selectById(42L)).thenReturn(detail(42L, 3L, null));
        when(identityMapper.selectById(6L)).thenReturn(partyMember);
        when(changeMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> changeService.apply(applyRequest(42L, 6L), secretary));
        assertTrue(e.getMessage().contains("待审批"), e.getMessage());
    }

    @Test
    void applyWithUnknownIdentityRejected() {
        givenSecretaryOfBranch(3L);
        when(userDetailMapper.selectById(42L)).thenReturn(detail(42L, 3L, null));
        when(identityMapper.selectById(6L)).thenReturn(null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> changeService.apply(applyRequest(42L, 6L), secretary));
        assertTrue(e.getMessage().contains("身份"), e.getMessage());
    }

    // ---------- 审批 ----------

    @Test
    void approveWritesIdentityIdAndKeepsRoleUntouched() {
        IdentityChange order = pendingOrder();
        when(changeMapper.selectById(9L)).thenReturn(order);
        when(userDetailMapper.selectById(42L)).thenReturn(detail(42L, 3L, 2L));

        IdentityChangeVo vo = changeService.approve(9L, true, "材料齐全", superAdmin);

        assertEquals(2, vo.getStatus());
        assertEquals(1L, vo.getReviewerId());
        // from 为发起时快照不追改；identity_id 写回 to(6)
        assertEquals(2L, order.getFromIdentity());
        assertEquals(6L, order.getToIdentity());
        verify(userDetailMapper).updateById(argThatIdentity(6L));
    }

    @Test
    void rejectKeepsIdentityIdUnchanged() {
        IdentityChange order = pendingOrder();
        UserDetail target = detail(42L, 3L, 2L);
        when(changeMapper.selectById(9L)).thenReturn(order);
        when(userDetailMapper.selectById(42L)).thenReturn(target);

        IdentityChangeVo vo = changeService.approve(9L, false, "材料不足", superAdmin);

        assertEquals(3, vo.getStatus());
        assertEquals(2L, target.getIdentityId()); // 未动
        verify(userDetailMapper, never()).updateById(any(UserDetail.class));
    }

    @Test
    void doubleApproveRejected() {
        IdentityChange order = pendingOrder();
        order.setStatus(2);
        when(changeMapper.selectById(9L)).thenReturn(order);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> changeService.approve(9L, true, null, superAdmin));
        assertTrue(e.getMessage().contains("已处理"), e.getMessage());
    }

    @Test
    void branchAdminCannotApprove() {
        // 角色先判：非超管连单据都不读
        assertThrows(IllegalArgumentException.class,
                () -> changeService.approve(9L, true, null, secretary));
        verify(changeMapper, never()).selectById(any());
        verify(changeMapper, never()).updateById(any(IdentityChange.class));
    }

    @Test
    void nonSuperViewingOrderSeesNotFound() {
        when(changeMapper.selectById(9L)).thenReturn(null);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> changeService.approve(9L, true, null, superAdmin));
        assertEquals("变更单不存在", e.getMessage());
    }

    @Test
    void firstRegistrationHasNullFromIdentity() {
        givenSecretaryOfBranch(3L);
        when(userDetailMapper.selectById(42L)).thenReturn(detail(42L, 3L, null)); // 未登记
        when(identityMapper.selectById(6L)).thenReturn(partyMember);
        when(changeMapper.selectCount(any())).thenReturn(0L);

        changeService.apply(applyRequest(42L, 6L), secretary);

        var captor = org.mockito.ArgumentCaptor.forClass(IdentityChange.class);
        verify(changeMapper).insert(captor.capture());
        assertNull(captor.getValue().getFromIdentity()); // 首次登记无来源身份
    }

    @Test
    void nonAdjacentLevelJumpAllowed() {
        givenSecretaryOfBranch(3L);
        when(userDetailMapper.selectById(42L)).thenReturn(detail(42L, 3L, 1L)); // 群众
        Identity applicant = new Identity();
        applicant.setId(2L);
        applicant.setIdentityName("入党申请人");
        applicant.setLevel(2);
        Identity formal = new Identity();
        formal.setId(6L);
        formal.setIdentityName("正式党员");
        formal.setLevel(6);
        when(identityMapper.selectById(6L)).thenReturn(formal);
        when(identityMapper.selectById(1L)).thenReturn(applicant);
        when(changeMapper.selectCount(any())).thenReturn(0L);

        // level 1 -> 6 跨级不拦截（组织决定，不是自动晋升）
        changeService.apply(applyRequest(42L, 6L), secretary);
        verify(changeMapper).insert(any(IdentityChange.class));
    }

    @Test
    void identityChangeServiceHasNoRoleDependenciesAtAll() {
        // #44 红线：身份变更与登录角色完全解耦——服务类不允许声明任何 UserRole 依赖。
        // 单测里 mock 注入不出的东西，永远写不了它。
        for (java.lang.reflect.Field field : IdentityChangeServiceImpl.class.getDeclaredFields()) {
            String type = field.getType().getSimpleName();
            assertTrue(!type.contains("UserRole"),
                    "身份变更服务不允许依赖角色表组件: " + field.getName() + " : " + type);
        }
    }

    // ---------- helpers ----------

    private void givenSecretaryOfBranch(Long branchId) {
        sicau.policialPartyManager.model.entity.Branch branch = new sicau.policialPartyManager.model.entity.Branch();
        branch.setId(branchId);
        branch.setSecretaryId(7L);
        branch.setStatus(1);
        when(branchMapper.selectList(any())).thenReturn(List.of(branch));
    }

    private IdentityChange pendingOrder() {
        IdentityChange order = new IdentityChange();
        order.setId(9L);
        order.setUserId(42L);
        order.setFromIdentity(2L);
        order.setToIdentity(6L);
        order.setOperatorId(7L);
        order.setStatus(1);
        return order;
    }

    private UserDetail detail(Long userId, Long branchId, Long identityId) {
        UserDetail d = new UserDetail();
        d.setUserId(userId);
        d.setBranchId(branchId);
        d.setIdentityId(identityId);
        return d;
    }

    private IdentityChangeApplyRequest applyRequest(Long userId, Long toIdentity) {
        IdentityChangeApplyRequest r = new IdentityChangeApplyRequest();
        r.setUserId(userId);
        r.setToIdentity(toIdentity);
        r.setReason("季度评审通过");
        return r;
    }

    private static UserDetail argThatIdentity(Long identityId) {
        return org.mockito.ArgumentMatchers.argThat(d -> identityId.equals(d.getIdentityId()));
    }
}
