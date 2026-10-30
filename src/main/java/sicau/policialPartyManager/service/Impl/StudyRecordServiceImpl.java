package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.StudyRecordUpsertRequest;
import sicau.policialPartyManager.api.dto.StudyRecordVo;
import sicau.policialPartyManager.api.dto.StudySummaryVo;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.model.entity.Study;
import sicau.policialPartyManager.model.entity.StudyRecord;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.StudyMapper;
import sicau.policialPartyManager.repository.StudyRecordMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.StudyRecordService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 学习记录（#32）。
 * <p>
 * 权限模型：super_admin 全局；branch_admin 的「管辖范围」= 自己担任秘书的支部
 * （tb_branch.secretary_id = 当前用户，与 #33 派审同一关联、方向相反）。
 * 越出范围统一「用户不存在」语义，不泄露他支部人员存在性。
 * 一人一课一行：upsert 先查后写，DuplicateKeyException 兜底转更新。
 */
@Service
@RequiredArgsConstructor
public class StudyRecordServiceImpl implements StudyRecordService {

    private static final String ROLE_SUPER_ADMIN = "super_admin";

    private final StudyRecordMapper recordMapper;
    private final StudyMapper studyMapper;
    private final UserDetailMapper userDetailMapper;
    private final BranchMapper branchMapper;

    @Override
    @Transactional
    public StudyRecordVo upsert(StudyRecordUpsertRequest request, User operator) {
        Study course = studyMapper.selectById(request.getCourseId());
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        UserDetail target = requireManagedTarget(request.getUserId(), operator);

        StudyRecord existing = recordMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, target.getUserId())
                .eq(StudyRecord::getCourseId, course.getId()));
        if (existing != null) {
            applyInput(existing, request);
            recordMapper.updateById(existing);
            return toVo(existing, target.getName(), course);
        }

        StudyRecord record = new StudyRecord();
        record.setUserId(target.getUserId());
        record.setCourseId(course.getId());
        applyInput(record, request);
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发下同键已被插入：兜底为更新，语义与「重复即更新」一致
            StudyRecord raced = recordMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                    .eq(StudyRecord::getUserId, target.getUserId())
                    .eq(StudyRecord::getCourseId, course.getId()));
            if (raced == null) {
                throw new IllegalStateException("学习记录写入冲突且无法回读，请重试");
            }
            applyInput(raced, request);
            recordMapper.updateById(raced);
            return toVo(raced, target.getName(), course);
        }
        return toVo(record, target.getName(), course);
    }

    @Override
    public void delete(Long id, User operator) {
        StudyRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("学习记录不存在");
        }
        if (!isSuperAdmin(operator)) {
            // 支管只能删本支部成员的记录；越权统一「不存在」语义
            Set<Long> managed = managedBranchIds(operator.userId());
            UserDetail target = userDetailMapper.selectById(record.getUserId());
            if (managed.isEmpty() || target == null || target.getBranchId() == null
                    || !managed.contains(target.getBranchId())) {
                throw new IllegalArgumentException("学习记录不存在");
            }
        }
        recordMapper.deleteById(id);
    }

    @Override
    public PageResult<StudyRecordVo> pageMine(long page, long size, User user) {
        LambdaQueryWrapper<StudyRecord> wrapper = new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, user.userId())
                .orderByDesc(StudyRecord::getCreateTime);
        return page(page, size, wrapper);
    }

    @Override
    public StudySummaryVo summaryMine(User user) {
        List<StudyRecord> rows = recordMapper.selectList(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, user.userId()));
        StudySummaryVo summary = new StudySummaryVo();
        summary.setRecordCount(rows.size());
        long minutes = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (StudyRecord row : rows) {
            if (row.getStudyTime() != null) {
                minutes += row.getStudyTime().getHour() * 60L + row.getStudyTime().getMinute();
            }
            if (row.getScore() != null) {
                total = total.add(row.getScore());
            }
        }
        summary.setTotalMinutes(minutes);
        summary.setAvgScore(rows.isEmpty() ? null
                : total.divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP));
        return summary;
    }

    @Override
    public PageResult<StudyRecordVo> pageForBranch(long page, long size, Long userId, User operator) {
        if (isSuperAdmin(operator)) {
            return pageForAdmin(page, size, userId, null, null);
        }
        Set<Long> managed = managedBranchIds(operator.userId());
        if (managed.isEmpty()) {
            throw new IllegalArgumentException("您未管理任何支部（不是任何支部的秘书）");
        }
        List<UserDetail> members = userDetailMapper.selectList(new LambdaQueryWrapper<UserDetail>()
                .in(UserDetail::getBranchId, managed));
        Set<Long> memberIds = members.stream().map(UserDetail::getUserId).collect(Collectors.toSet());
        if (userId != null) {
            if (!memberIds.contains(userId)) {
                throw new IllegalArgumentException("用户不存在");
            }
            LambdaQueryWrapper<StudyRecord> wrapper = new LambdaQueryWrapper<StudyRecord>()
                    .eq(StudyRecord::getUserId, userId)
                    .orderByDesc(StudyRecord::getCreateTime);
            return page(page, size, wrapper);
        }
        if (memberIds.isEmpty()) {
            return PageResult.of(List.of(), 0L, page, size);
        }
        LambdaQueryWrapper<StudyRecord> wrapper = new LambdaQueryWrapper<StudyRecord>()
                .in(StudyRecord::getUserId, memberIds)
                .orderByDesc(StudyRecord::getCreateTime);
        return page(page, size, wrapper);
    }

    @Override
    public PageResult<StudyRecordVo> pageForAdmin(long page, long size, Long userId, Long courseId, String courseType) {
        LambdaQueryWrapper<StudyRecord> wrapper = new LambdaQueryWrapper<StudyRecord>()
                .orderByDesc(StudyRecord::getCreateTime);
        if (userId != null) {
            wrapper.eq(StudyRecord::getUserId, userId);
        }
        if (courseId != null) {
            wrapper.eq(StudyRecord::getCourseId, courseId);
        }
        if (StringUtils.hasText(courseType)) {
            // 按课程类型过滤：先取该类型课程 id 集合
            List<Study> courses = studyMapper.selectList(new LambdaQueryWrapper<Study>()
                    .eq(Study::getType, courseType.trim()));
            Set<Long> ids = courses.stream().map(Study::getId).collect(Collectors.toSet());
            if (ids.isEmpty()) {
                return PageResult.of(List.of(), 0L, page, size);
            }
            wrapper.in(StudyRecord::getCourseId, ids);
        }
        return page(page, size, wrapper);
    }

    // ---------- 私有 ----------

    /** 校验录入目标：用户须有档案；支管场景须属自己担任秘书的支部 */
    private UserDetail requireManagedTarget(Long targetUserId, User operator) {
        UserDetail target = userDetailMapper.selectById(targetUserId);
        if (isSuperAdmin(operator)) {
            if (target == null) {
                throw new IllegalArgumentException("用户不存在或无个人档案，无法记录学习情况");
            }
            return target;
        }
        Set<Long> managed = managedBranchIds(operator.userId());
        if (managed.isEmpty()) {
            throw new IllegalArgumentException("您未管理任何支部（不是任何支部的秘书）");
        }
        if (target == null || target.getBranchId() == null || !managed.contains(target.getBranchId())) {
            // 不泄露他支部人员存在性
            throw new IllegalArgumentException("用户不存在");
        }
        return target;
    }

    /** 该用户担任秘书的支部 id 集合 */
    private Set<Long> managedBranchIds(Long operatorUserId) {
        return branchMapper.selectList(new LambdaQueryWrapper<Branch>()
                        .eq(Branch::getSecretaryId, operatorUserId)
                        .eq(Branch::getStatus, 1))
                .stream().map(Branch::getId).collect(Collectors.toSet());
    }

    private boolean isSuperAdmin(User operator) {
        return ROLE_SUPER_ADMIN.equalsIgnoreCase(operator.role());
    }

    private void applyInput(StudyRecord record, StudyRecordUpsertRequest request) {
        record.setStudyTime(request.getStudyTime());
        record.setScore(request.getScore());
        record.setUrl(trimToNull(request.getUrl()));
        record.setRemark(trimToNull(request.getRemark()));
    }

    private PageResult<StudyRecordVo> page(long page, long size, LambdaQueryWrapper<StudyRecord> wrapper) {
        Page<StudyRecord> result = recordMapper.selectPage(new Page<>(page, size), wrapper);
        List<StudyRecord> rows = result.getRecords();
        Set<Long> userIds = rows.stream().map(StudyRecord::getUserId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> courseIds = rows.stream().map(StudyRecord::getCourseId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = userNames(userIds);
        Map<Long, Study> courses = courses(courseIds);
        List<StudyRecordVo> vos = rows.stream()
                .map(r -> toVo(r, names.get(r.getUserId()), courses.get(r.getCourseId())))
                .toList();
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    private StudyRecordVo toVo(StudyRecord record, String studentName, Study course) {
        StudyRecordVo vo = new StudyRecordVo();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setStudentName(studentName);
        vo.setCourseId(record.getCourseId());
        if (course != null) {
            vo.setCourseName(course.getName());
            vo.setCourseType(course.getType());
        }
        vo.setStudyTime(record.getStudyTime());
        vo.setScore(record.getScore());
        vo.setUrl(record.getUrl());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private Map<Long, String> userNames(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userDetailMapper.selectBatchIds(userIds).stream()
                .filter(d -> d.getName() != null)
                .collect(Collectors.toMap(UserDetail::getUserId, UserDetail::getName, (a, b) -> a));
    }

    private Map<Long, Study> courses(Set<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return studyMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Study::getId, s -> s, (a, b) -> a));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
