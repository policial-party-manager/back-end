package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.IdentitySaveRequest;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.Identity;
import sicau.policialPartyManager.model.entity.IdentityChange;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.IdentityChangeMapper;
import sicau.policialPartyManager.repository.IdentityMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.IdentityService;

/**
 * 政治身份字典（#44）。重名与被引用保护在应用层给可读 400。
 * 注意：不注入任何角色相关 Mapper —— 字典维护与登录角色完全解耦（#44 红线）。
 */
@Service
@RequiredArgsConstructor
public class IdentityServiceImpl implements IdentityService {

    private final IdentityMapper identityMapper;
    private final IdentityChangeMapper changeMapper;
    private final UserDetailMapper userDetailMapper;

    @Override
    public Identity create(IdentitySaveRequest request) {
        String name = requireText(request.getIdentityName(), "身份名");
        checkNameFree(name, null);
        Identity identity = new Identity();
        identity.setIdentityName(name);
        identity.setLevel(request.getLevel());
        identity.setRoleId(request.getRoleId());
        identity.setDescription(trimToNull(request.getDescription()));
        identityMapper.insert(identity);
        return identity;
    }

    @Override
    public Identity update(Long id, IdentitySaveRequest request) {
        Identity existing = identityMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("身份不存在");
        }
        String name = requireText(request.getIdentityName(), "身份名");
        checkNameFree(name, id);
        existing.setIdentityName(name);
        existing.setLevel(request.getLevel());
        existing.setRoleId(request.getRoleId());
        existing.setDescription(trimToNull(request.getDescription()));
        identityMapper.updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        Identity existing = identityMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("身份不存在");
        }
        Long users = userDetailMapper.selectCount(new LambdaQueryWrapper<UserDetail>()
                .eq(UserDetail::getIdentityId, id));
        Long asFrom = changeMapper.selectCount(new LambdaQueryWrapper<IdentityChange>()
                .eq(IdentityChange::getFromIdentity, id));
        Long asTo = changeMapper.selectCount(new LambdaQueryWrapper<IdentityChange>()
                .eq(IdentityChange::getToIdentity, id));
        long referenced = nullToZero(users) + nullToZero(asFrom) + nullToZero(asTo);
        if (referenced > 0) {
            throw new IllegalArgumentException("该身份已被 " + referenced + " 处引用（用户当前身份或变更单），不能删除");
        }
        identityMapper.deleteById(id);
    }

    @Override
    public PageResult<Identity> page(long page, long size) {
        Page<Identity> result = identityMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Identity>().orderByAsc(Identity::getLevel));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    /** 同名（排除自身）视为冲突，风格对齐课程字典/活动类型 */
    private void checkNameFree(String name, Long excludeId) {
        LambdaQueryWrapper<Identity> wrapper = new LambdaQueryWrapper<Identity>()
                .eq(Identity::getIdentityName, name);
        if (excludeId != null) {
            wrapper.ne(Identity::getId, excludeId);
        }
        Long count = identityMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("身份名已存在");
        }
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private String requireText(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
