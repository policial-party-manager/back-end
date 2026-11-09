package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sicau.policialPartyManager.api.dto.BatchResult;
import sicau.policialPartyManager.model.entity.User;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.model.entity.UserMentor;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.repository.UserMapper;
import sicau.policialPartyManager.repository.UserMentorMapper;
import sicau.policialPartyManager.service.BranchScopeService;

import java.util.ArrayList;
import java.util.List;

/**
 * 培养联系人（#72）。
 * <p>培养联系人是「当前关系」而不是历史快照：换人之后，旧思想汇报展示的联系人随之更新。
 * <p>约束：同一被培养人最多 2 名在职联系人。唯一约束只能防重复，防不住第 3 个人，
 * 因此上限由本服务强制。
 */
@Service
@RequiredArgsConstructor
public class MentorServiceImpl {

    /** 每人最多在职培养联系人数 */
    public static final int MAX_MENTORS = 2;

    /** 党员身份门槛：tb_identity.level >= 5（预备党员 / 正式党员） */
    private static final int PARTY_MEMBER_LEVEL = 5;

    private final UserMentorMapper userMentorMapper;
    private final UserMapper userMapper;
    private final UserDetailMapper userDetailMapper;
    private final sicau.policialPartyManager.repository.IdentityMapper identityMapper;
    private final BranchScopeService branchScopeService;

    /** 指派培养联系人；已存在则幂等返回。 */
    @Transactional
    public void assign(sicau.policialPartyManager.model.records.User operator, Long userId, Long mentorId) {
        branchScopeService.requireManageableUser(operator, userId);
        if (mentorId == null) {
            throw new IllegalArgumentException("培养联系人不能为空");
        }
        if (mentorId.equals(userId)) {
            throw new IllegalArgumentException("不能指定本人为培养联系人");
        }
        if (userMapper.selectById(mentorId) == null) {
            throw new IllegalArgumentException("培养联系人不存在");
        }
        // DDL 注释声明「培养联系人须为党员」：正式党员(level=6) 或 预备党员(level=5)。
        // 未登记政治身份的关系人不能担任，否则「谁带的」在档案里无从追溯。
        sicau.policialPartyManager.model.entity.UserDetail mentorDetail = userDetailMapper.selectById(mentorId);
        Long identityId = mentorDetail == null ? null : mentorDetail.getIdentityId();
        if (identityId == null) {
            throw new IllegalArgumentException("培养联系人须为党员（预备党员或正式党员），该用户尚未登记政治身份");
        }
        sicau.policialPartyManager.model.entity.Identity identity = identityMapper.selectById(identityId);
        Integer level = identity == null ? null : identity.getLevel();
        if (level == null || level < PARTY_MEMBER_LEVEL) {
            throw new IllegalArgumentException("培养联系人须为党员（预备党员或正式党员）");
        }
        UserMentor existed = userMentorMapper.selectOne(new LambdaQueryWrapper<UserMentor>()
                .eq(UserMentor::getUserId, userId)
                .eq(UserMentor::getMentorId, mentorId));
        if (existed != null) {
            if (existed.getStatus() != null && existed.getStatus() == 1) {
                return; // 幂等
            }
            existed.setStatus(1);
            userMentorMapper.updateById(existed);
            return;
        }
        long active = countActive(userId);
        if (active >= MAX_MENTORS) {
            throw new IllegalArgumentException("每位成员最多设置 " + MAX_MENTORS + " 名培养联系人");
        }
        UserMentor m = new UserMentor();
        m.setUserId(userId);
        m.setMentorId(mentorId);
        m.setStatus(1);
        userMentorMapper.insert(m);
    }

    /** 移除培养联系人（软删除）。 */
    @Transactional
    public void remove(sicau.policialPartyManager.model.records.User operator, Long userId, Long mentorId) {
        branchScopeService.requireManageableUser(operator, userId);
        UserMentor existed = userMentorMapper.selectOne(new LambdaQueryWrapper<UserMentor>()
                .eq(UserMentor::getUserId, userId)
                .eq(UserMentor::getMentorId, mentorId)
                .eq(UserMentor::getStatus, 1));
        if (existed == null) {
            throw new IllegalArgumentException("培养联系人不存在");
        }
        existed.setStatus(0);
        userMentorMapper.updateById(existed);
    }

    /** 某成员当前在职的培养联系人 id 列表。 */
    public List<Long> mentorsOf(Long userId) {
        List<UserMentor> rows = userMentorMapper.selectList(new LambdaQueryWrapper<UserMentor>()
                .eq(UserMentor::getUserId, userId)
                .eq(UserMentor::getStatus, 1)
                .orderByAsc(UserMentor::getId));
        List<Long> ids = new ArrayList<>();
        for (UserMentor r : rows) {
            ids.add(r.getMentorId());
        }
        return ids;
    }

    /** 某成员当前培养联系人明细（id + 姓名），供档案与思想汇报展示。 */
    public List<MentorView> mentorViewsOf(Long userId) {
        List<UserMentor> rows = userMentorMapper.selectList(new LambdaQueryWrapper<UserMentor>()
                .eq(UserMentor::getUserId, userId)
                .eq(UserMentor::getStatus, 1)
                .orderByAsc(UserMentor::getId));
        List<MentorView> views = new ArrayList<>();
        for (UserMentor r : rows) {
            MentorView v = new MentorView();
            v.setMentorId(r.getMentorId());
            User u = userMapper.selectById(r.getMentorId());
            UserDetail d = userDetailMapper.selectById(r.getMentorId());
            v.setRealName(d == null ? null : d.getName());
            v.setUsername(u == null ? null : u.getUsername());
            views.add(v);
        }
        return views;
    }

    /** 按培养联系人反查当前带教的成员 id。 */
    public List<Long> menteesOf(sicau.policialPartyManager.model.records.User operator, Long mentorId) {
        // 支委只能看本支部成员；超管不限
        List<UserMentor> rows = userMentorMapper.selectList(new LambdaQueryWrapper<UserMentor>()
                .eq(UserMentor::getMentorId, mentorId)
                .eq(UserMentor::getStatus, 1));
        List<Long> ids = new ArrayList<>();
        for (UserMentor r : rows) {
            if (branchScopeService.isSuperAdmin(operator)) {
                ids.add(r.getUserId());
                continue;
            }
            Long mine = branchScopeService.branchIdOf(operator);
            UserDetail d = userDetailMapper.selectById(r.getUserId());
            if (mine != null && d != null && mine.equals(d.getBranchId())) {
                ids.add(r.getUserId());
            }
        }
        return ids;
    }

    /** 批量指派同一培养联系人给多个成员；逐条成功，200 上限。 */
    @Transactional
    public BatchResult batchAssign(sicau.policialPartyManager.model.records.User operator,
                                   List<Long> userIds, Long mentorId) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一条记录");
        }
        if (userIds.size() > ScopedMemberServiceImpl.BATCH_LIMIT) {
            throw new IllegalArgumentException("单次批量最多 " + ScopedMemberServiceImpl.BATCH_LIMIT + " 条，请分批操作");
        }
        BatchResult result = BatchResult.of(userIds.size());
        for (Long uid : userIds) {
            try {
                assign(operator, uid, mentorId);
                result.success();
            } catch (IllegalArgumentException e) {
                result.fail(uid, e.getMessage());
            }
        }
        return result;
    }

    private long countActive(Long userId) {
        Long n = userMentorMapper.selectCount(new LambdaQueryWrapper<UserMentor>()
                .eq(UserMentor::getUserId, userId)
                .eq(UserMentor::getStatus, 1));
        return n == null ? 0 : n;
    }

    /** 培养联系人展示对象 */
    @lombok.Data
    public static class MentorView {
        private Long mentorId;
        private String realName;
        private String username;
    }
}
