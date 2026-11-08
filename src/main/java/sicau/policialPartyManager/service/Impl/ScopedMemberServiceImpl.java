package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.BatchResult;
import sicau.policialPartyManager.api.dto.MemberProfileUpdateRequest;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.UserVo;
import sicau.policialPartyManager.model.entity.User;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.repository.UserMapper;
import sicau.policialPartyManager.service.BranchScopeService;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 带数据范围的成员操作（#70 / #82）。
 * <p>超管全院、支委限本支部；越权与真不存在同文案。
 */
@Service
@RequiredArgsConstructor
public class ScopedMemberServiceImpl {

    /** 批量操作单次上限（#82） */
    public static final int BATCH_LIMIT = 200;

    private final UserMapper userMapper;
    private final UserDetailMapper userDetailMapper;
    private final BranchScopeService branchScopeService;
    private final sicau.policialPartyManager.log.OperationLogPublisher logPublisher;

    private static final String[] EXPORT_HEADERS = {
            "学号", "姓名", "手机号", "邮箱", "学院", "年级", "专业", "班级", "备注"};

    /** 按操作者范围解析生效的支部过滤：超管=入参；支委=强制本支部。 */
    private Long resolveBranchFilter(sicau.policialPartyManager.model.records.User operator, Long requested) {
        if (branchScopeService.isSuperAdmin(operator)) {
            return requested;
        }
        Long mine = branchScopeService.branchIdOf(operator);
        if (mine == null) {
            throw new IllegalArgumentException("您尚未归属任何党支部，请联系管理员");
        }
        return mine;
    }

    public PageResult<UserVo> pageUsersScoped(sicau.policialPartyManager.model.records.User operator, long page, long size, String keyword,
                                              String grade, String major, String college,
                                              Long identityId, Integer status) {
        Long branchFilter = resolveBranchFilter(operator, null);
        Set<Long> candidateIds = filterDetailIds(branchFilter, keyword, grade, major, college, identityId);
        if (candidateIds != null && candidateIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (candidateIds != null) {
            wrapper.in(User::getId, candidateIds);
        }
        wrapper.orderByDesc(User::getId);

        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(buildVos(userPage.getRecords()), userPage.getTotal(), page, size);
    }

    public UserVo getUserScoped(sicau.policialPartyManager.model.records.User operator, Long id) {
        branchScopeService.requireManageableUser(operator, id);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return buildVos(List.of(user)).get(0);
    }

    @Transactional
    public void updateContactScoped(sicau.policialPartyManager.model.records.User operator, Long id, MemberProfileUpdateRequest request) {
        branchScopeService.requireManageableUser(operator, id);
        UserDetail detail = userDetailMapper.selectById(id);
        if (detail == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        // #74：改动前后快照（只在实际变化时记录）
        String before = snapshotContact(detail);
        if (request.getPhone() != null) {
            String v = trimToNull(request.getPhone());
            ensureUnique(id, v, null, "手机号已被使用");
            detail.setPhone(v);
        }
        if (request.getEmail() != null) {
            String v = trimToNull(request.getEmail());
            ensureUnique(id, null, v, "邮箱已被使用");
            detail.setEmail(v);
        }
        if (request.getRemark() != null) {
            detail.setRemark(trimToNull(request.getRemark()));
        }
        userDetailMapper.updateById(detail);
        String after = snapshotContact(detail);
        if (!after.equals(before)) {
            logPublisher.dataChange("成员管理", "修改联系方式", id, before, after);
        }
    }

    /** 快照只含本次可改的字段，避免把无关字段的差异误报成变更。 */
    private String snapshotContact(UserDetail d) {
        return "{\"phone\":" + json(d.getPhone())
                + ",\"email\":" + json(d.getEmail())
                + ",\"remark\":" + json(d.getRemark()) + "}";
    }

    private String json(String v) {
        if (v == null) {
            return "null";
        }
        return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public byte[] exportScoped(sicau.policialPartyManager.model.records.User operator, String keyword, String grade, String major,
                               String college, Long identityId, Integer status) {
        Long branchFilter = resolveBranchFilter(operator, null);
        Set<Long> candidates = filterDetailIds(branchFilter, keyword, grade, major, college, identityId);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (candidates != null) {
            if (candidates.isEmpty()) {
                return toWorkbook(List.of());
            }
            wrapper.in(User::getId, candidates);
        }
        wrapper.orderByDesc(User::getId);
        return toWorkbook(buildVos(userMapper.selectList(wrapper)));
    }

    // ---------- 批量（#82 的成员部分） ----------

    @Transactional
    public BatchResult batchUpdateStatus(sicau.policialPartyManager.model.records.User operator, List<Long> ids, Integer status) {
        if (!branchScopeService.isSuperAdmin(operator)) {
            throw new IllegalArgumentException("仅超级管理员可停用/恢复成员");
        }
        requireWithinLimit(ids);
        BatchResult result = BatchResult.of(ids.size());
        for (Long id : ids) {
            try {
                User user = userMapper.selectById(id);
                if (user == null) {
                    result.fail(id, "用户不存在");
                    continue;
                }
                user.setStatus(status);
                userMapper.updateById(user);
                result.success();
            } catch (IllegalArgumentException e) {
                result.fail(id, e.getMessage());
            }
        }
        return result;
    }

    @Transactional
    public BatchResult batchUpdateBranch(sicau.policialPartyManager.model.records.User operator, List<Long> ids, Long branchId) {
        if (!branchScopeService.isSuperAdmin(operator)) {
            throw new IllegalArgumentException("仅超级管理员可调整所属支部");
        }
        requireWithinLimit(ids);
        BatchResult result = BatchResult.of(ids.size());
        for (Long id : ids) {
            try {
                UserDetail detail = userDetailMapper.selectById(id);
                if (detail == null) {
                    result.fail(id, "用户不存在");
                    continue;
                }
                detail.setBranchId(branchId);
                userDetailMapper.updateById(detail);
                result.success();
            } catch (IllegalArgumentException e) {
                result.fail(id, e.getMessage());
            }
        }
        return result;
    }

    public void requireWithinLimit(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一条记录");
        }
        if (ids.size() > BATCH_LIMIT) {
            throw new IllegalArgumentException("单次批量最多 " + BATCH_LIMIT + " 条，请分批操作");
        }
    }

    // ---------- internals ----------

    private Set<Long> filterDetailIds(Long branchId, String keyword, String grade,
                                      String major, String college, Long identityId) {
        boolean any = branchId != null || StringUtils.hasText(keyword) || StringUtils.hasText(grade)
                || StringUtils.hasText(major) || StringUtils.hasText(college) || identityId != null;
        if (!any) {
            return null;
        }
        LambdaQueryWrapper<UserDetail> wrapper = new LambdaQueryWrapper<>();
        if (branchId != null) {
            wrapper.eq(UserDetail::getBranchId, branchId);
        }
        if (StringUtils.hasText(grade)) {
            wrapper.eq(UserDetail::getGrade, grade.trim());
        }
        if (StringUtils.hasText(major)) {
            wrapper.eq(UserDetail::getMajor, major.trim());
        }
        if (StringUtils.hasText(college)) {
            wrapper.eq(UserDetail::getCollege, college.trim());
        }
        if (identityId != null) {
            wrapper.eq(UserDetail::getIdentityId, identityId);
        }
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(UserDetail::getName, k)
                    .or().like(UserDetail::getStudentId, k)
                    .or().like(UserDetail::getPhone, k)
                    .or().like(UserDetail::getEmail, k));
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (UserDetail d : userDetailMapper.selectList(wrapper)) {
            ids.add(d.getUserId());
        }
        if (StringUtils.hasText(keyword)) {
            for (User u : userMapper.selectList(new LambdaQueryWrapper<User>()
                    .like(User::getUsername, keyword.trim()))) {
                if (branchId == null) {
                    ids.add(u.getId());
                } else {
                    UserDetail d = userDetailMapper.selectById(u.getId());
                    if (d != null && branchId.equals(d.getBranchId())) {
                        ids.add(u.getId());
                    }
                }
            }
        }
        return ids;
    }

    private void ensureUnique(Long selfId, String phone, String email, String message) {
        if (phone != null) {
            Long n = userDetailMapper.selectCount(new LambdaQueryWrapper<UserDetail>()
                    .eq(UserDetail::getPhone, phone).ne(UserDetail::getUserId, selfId));
            if (n != null && n > 0) {
                throw new IllegalArgumentException(message);
            }
        }
        if (email != null) {
            Long n = userDetailMapper.selectCount(new LambdaQueryWrapper<UserDetail>()
                    .eq(UserDetail::getEmail, email).ne(UserDetail::getUserId, selfId));
            if (n != null && n > 0) {
                throw new IllegalArgumentException(message);
            }
        }
    }

    private List<UserVo> buildVos(List<User> users) {
        List<UserVo> list = new ArrayList<>();
        for (User u : users) {
            UserVo vo = new UserVo();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setStatus(u.getStatus());
            vo.setCreateTime(u.getCreateTime());
            UserDetail d = userDetailMapper.selectById(u.getId());
            if (d != null) {
                vo.setRealName(d.getName());
                vo.setStudentId(d.getStudentId());
                vo.setPhone(d.getPhone());
                vo.setEmail(d.getEmail());
                vo.setGender(d.getGender());
                vo.setCollege(d.getCollege());
                vo.setGrade(d.getGrade());
                vo.setMajor(d.getMajor());
                vo.setClassName(d.getClassName());
                vo.setRemark(d.getRemark());
                vo.setBranchId(d.getBranchId());
                vo.setIdentityId(d.getIdentityId());
            }
            list.add(vo);
        }
        return list;
    }

    private byte[] toWorkbook(List<UserVo> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("成员名单");
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
            }
            int r = 1;
            for (UserVo vo : rows) {
                Row row = sheet.createRow(r++);
                set(row, 0, vo.getStudentId());
                set(row, 1, vo.getRealName());
                set(row, 2, vo.getPhone());
                set(row, 3, vo.getEmail());
                set(row, 4, vo.getCollege());
                set(row, 5, vo.getGrade());
                set(row, 6, vo.getMajor());
                set(row, 7, vo.getClassName());
                set(row, 8, vo.getRemark());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("导出失败", e);
        }
    }

    private void set(Row row, int idx, String v) {
        Cell c = row.createCell(idx);
        c.setCellValue(v == null ? "" : v);
    }

    private String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}