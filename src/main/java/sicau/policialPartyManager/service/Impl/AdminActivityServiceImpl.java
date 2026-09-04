package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.ActivitySaveRequest;
import sicau.policialPartyManager.api.dto.ActivityVo;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.ActivityDetail;
import sicau.policialPartyManager.model.entity.ActivityType;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.repository.ActivityDetailMapper;
import sicau.policialPartyManager.repository.ActivityMapper;
import sicau.policialPartyManager.repository.ActivityTypeMapper;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.service.AdminActivityService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 活动管理（后台）：活动（主体 + 详情）增删改查、状态调整，以及活动类型维护。
 */
@Service
@RequiredArgsConstructor
public class AdminActivityServiceImpl implements AdminActivityService {

    private final ActivityMapper activityMapper;
    private final ActivityDetailMapper activityDetailMapper;
    private final ActivityTypeMapper activityTypeMapper;
    private final BranchMapper branchMapper;

    @Override
    public PageResult<ActivityVo> pageActivities(long page, long size, String keyword, Long branchId,
                                                 Integer type, Integer status) {
        // 状态在 tb_activity_detail，先按状态过滤 activityId 集合
        Set<Long> statusIds = null;
        if (status != null) {
            statusIds = activityDetailMapper.selectList(new LambdaQueryWrapper<ActivityDetail>()
                            .eq(ActivityDetail::getStatus, status)).stream()
                    .map(ActivityDetail::getActivityId).collect(Collectors.toSet());
            if (statusIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0, page, size);
            }
        }

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (branchId != null) {
            wrapper.eq(Activity::getBranchId, branchId);
        }
        if (type != null) {
            wrapper.eq(Activity::getType, type);
        }
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(Activity::getTitle, k).or().like(Activity::getDescription, k));
        }
        if (statusIds != null) {
            wrapper.in(Activity::getId, statusIds);
        }
        wrapper.orderByDesc(Activity::getId);

        Page<Activity> result = activityMapper.selectPage(new Page<>(page, size), wrapper);
        List<ActivityVo> vos = buildVos(result.getRecords());
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    public ActivityVo getActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        return buildVos(List.of(activity)).get(0);
    }

    @Override
    public void createActivity(ActivitySaveRequest request, Long operatorId) {
        validateAndApply(null, request);
        Activity activity = toActivity(new Activity(), request);
        activityMapper.insert(activity);

        ActivityDetail detail = new ActivityDetail();
        detail.setActivityId(activity.getId());
        detail.setCreatorId(operatorId);
        applyDetail(detail, request);
        if (detail.getStatus() == null) {
            detail.setStatus(1);
        }
        activityDetailMapper.insert(detail);
    }

    @Override
    public void updateActivity(Long id, ActivitySaveRequest request, Long operatorId) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        validateAndApply(id, request);
        activityMapper.updateById(toActivity(activity, request));

        ActivityDetail detail = activityDetailMapper.selectById(id);
        boolean detailExisted = detail != null;
        if (detail == null) {
            detail = new ActivityDetail();
            detail.setActivityId(id);
            detail.setCreatorId(operatorId);
        }
        applyDetail(detail, request);
        if (!detailExisted && detail.getStatus() == null) {
            detail.setStatus(1);
        }
        activityDetailMapper.updateById(detail);
    }

    @Override
    public void deleteActivity(Long id) {
        if (activityMapper.selectById(id) == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        activityDetailMapper.deleteById(id);
        activityMapper.deleteById(id);
    }

    @Override
    public void updateActivityStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            throw new IllegalArgumentException("活动状态不合法（0/1/2）");
        }
        if (activityMapper.selectById(id) == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        ActivityDetail detail = activityDetailMapper.selectById(id);
        if (detail == null) {
            detail = new ActivityDetail();
            detail.setActivityId(id);
        }
        detail.setStatus(status);
        activityDetailMapper.updateById(detail);
    }

    @Override
    public List<ActivityType> listActivityTypes() {
        return activityTypeMapper.selectList(new LambdaQueryWrapper<ActivityType>().orderByAsc(ActivityType::getId));
    }

    @Override
    public void saveActivityType(ActivityType type) {
        if (!StringUtils.hasText(type.getName())) {
            throw new IllegalArgumentException("类型名称不能为空");
        }
        checkTypeNameFree(type.getName().trim(), null);
        type.setId(null);
        type.setName(type.getName().trim());
        activityTypeMapper.insert(type);
    }

    @Override
    public void updateActivityType(Long id, ActivityType type) {
        ActivityType existing = activityTypeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("活动类型不存在");
        }
        if (StringUtils.hasText(type.getName())) {
            checkTypeNameFree(type.getName().trim(), id);
            existing.setName(type.getName().trim());
        }
        if (type.getNeedSign() != null) {
            existing.setNeedSign(type.getNeedSign());
        }
        if (type.getCountToFile() != null) {
            existing.setCountToFile(type.getCountToFile());
        }
        activityTypeMapper.updateById(existing);
    }

    @Override
    public void deleteActivityType(Long id) {
        if (activityTypeMapper.selectById(id) == null) {
            return;
        }
        Long used = activityMapper.selectCount(new LambdaQueryWrapper<Activity>().eq(Activity::getType, id));
        if (used != null && used > 0) {
            throw new IllegalArgumentException("该类型已被活动使用，无法删除");
        }
        activityTypeMapper.deleteById(id);
    }

    // ======================= 私有辅助 =======================

    private List<ActivityVo> buildVos(List<Activity> activities) {
        if (activities.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = activities.stream().map(Activity::getId).collect(Collectors.toList());

        Map<Long, ActivityDetail> detailMap = activityDetailMapper.selectList(
                        new LambdaQueryWrapper<ActivityDetail>().in(ActivityDetail::getActivityId, ids)).stream()
                .collect(Collectors.toMap(ActivityDetail::getActivityId, Function.identity(), (a, b) -> a));

        Set<Long> branchIds = activities.stream().map(Activity::getBranchId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Branch> branchMap = branchIds.isEmpty() ? Collections.emptyMap()
                : branchMapper.selectBatchIds(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Function.identity(), (a, b) -> a));

        Set<Integer> typeIds = activities.stream().map(Activity::getType)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> typeLongIds = typeIds.stream().map(Integer::longValue).collect(Collectors.toSet());
        Map<Long, ActivityType> typeMap = typeLongIds.isEmpty() ? Collections.emptyMap()
                : activityTypeMapper.selectBatchIds(typeLongIds).stream()
                .collect(Collectors.toMap(ActivityType::getId, Function.identity(), (a, b) -> a));

        List<ActivityVo> vos = new ArrayList<>();
        for (Activity activity : activities) {
            ActivityVo vo = new ActivityVo();
            vo.setId(activity.getId());
            vo.setBranchId(activity.getBranchId());
            vo.setTitle(activity.getTitle());
            vo.setDescription(activity.getDescription());
            vo.setType(activity.getType());
            vo.setCover(activity.getCover());
            vo.setStartTime(activity.getStartTime());
            vo.setEndTime(activity.getEndTime());
            vo.setCreateTime(activity.getCreateTime());
            vo.setUpdateTime(activity.getUpdateTime());

            Branch branch = activity.getBranchId() == null ? null : branchMap.get(activity.getBranchId());
            vo.setBranchName(branch == null ? null : branch.getBranchName());
            ActivityType type = activity.getType() == null ? null : typeMap.get(activity.getType().longValue());
            vo.setTypeName(type == null ? null : type.getName());

            ActivityDetail detail = detailMap.get(activity.getId());
            if (detail != null) {
                vo.setLocation(detail.getLocation());
                vo.setLocationDescription(detail.getLocationDescription());
                vo.setSignStart(detail.getSignStart());
                vo.setSignEnd(detail.getSignEnd());
                vo.setMaxParticipants(detail.getMaxParticipants());
                vo.setStatus(detail.getStatus());
                vo.setCreatorId(detail.getCreatorId());
            }
            vos.add(vo);
        }
        return vos;
    }

    /** 校验引用与必填（编辑时排除自身无需，type 只是业务引用） */
    private void validateAndApply(Long id, ActivitySaveRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("活动标题不能为空");
        }
        if (request.getBranchId() != null) {
            Branch branch = branchMapper.selectById(request.getBranchId());
            if (branch == null || branch.getStatus() == null || branch.getStatus() != 1) {
                throw new IllegalArgumentException("所选支部不存在或已停用");
            }
        }
        if (request.getType() != null && activityTypeMapper.selectById(request.getType()) == null) {
            throw new IllegalArgumentException("所选活动类型不存在");
        }
    }

    private Activity toActivity(Activity target, ActivitySaveRequest request) {
        target.setBranchId(request.getBranchId());
        target.setTitle(request.getTitle());
        target.setDescription(request.getDescription());
        target.setType(request.getType());
        target.setCover(request.getCover());
        target.setStartTime(request.getStartTime());
        target.setEndTime(request.getEndTime());
        return target;
    }

    private void applyDetail(ActivityDetail detail, ActivitySaveRequest request) {
        detail.setLocation(request.getLocation());
        detail.setLocationDescription(request.getLocationDescription());
        detail.setSignStart(request.getSignStart());
        detail.setSignEnd(request.getSignEnd());
        detail.setMaxParticipants(request.getMaxParticipants());
        if (request.getStatus() != null) {
            detail.setStatus(request.getStatus());
        }
    }

    private void checkTypeNameFree(String name, Long excludeId) {
        LambdaQueryWrapper<ActivityType> wrapper = new LambdaQueryWrapper<ActivityType>()
                .eq(ActivityType::getName, name);
        if (excludeId != null) {
            wrapper.ne(ActivityType::getId, excludeId);
        }
        Long count = activityTypeMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("活动类型名称已存在");
        }
    }
}
