package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import sicau.policialPartyManager.api.dto.ImportResult;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.UserSaveRequest;
import sicau.policialPartyManager.api.dto.UserVo;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.model.entity.Role;
import sicau.policialPartyManager.model.entity.User;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.model.entity.UserRole;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.RoleMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.repository.UserMapper;
import sicau.policialPartyManager.repository.UserRoleMapper;
import sicau.policialPartyManager.service.AdminUserService;
import sicau.policialPartyManager.utils.ExcelUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户管理（后台）：分页搜索、增改、启停用、重置密码、Excel 批量导入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    /** 默认密码 */
    public static final String DEFAULT_PASSWORD = "123456";

    /** 导入列：前 6 列必填区 + 后 7 列选填（新扩展字段） */
    private static final String[] IMPORT_HEADERS = {"学号", "姓名", "手机号", "邮箱", "所属支部", "角色",
            "性别", "学院", "年级", "专业", "班级", "紧急联系人", "备注"};
    private static final String[][] IMPORT_EXAMPLE = {
            {"20230001", "张三", "13800138000", "zhangsan@stu.sicau.edu.cn", "第一党支部", "普通成员",
                    "男", "信息工程学院", "2023", "软件工程", "软工2301", "张三家长 13800000000", ""},
            {"20230002", "李四", "13900139000", "lisi@stu.sicau.edu.cn", "第一党支部", "",
                    "", "", "", "", "", "", ""}
    };
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    private final UserMapper userMapper;
    private final UserDetailMapper userDetailMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final BranchMapper branchMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVo> pageUsers(long page, long size, String keyword, Long branchId, Long roleId, Integer status) {
        // 1) 先根据 关键字/支部/角色 缩小 userId 候选集
        Set<Long> candidateIds = null;

        boolean hasKeyword = StringUtils.hasText(keyword);
        if (hasKeyword || branchId != null) {
            LambdaQueryWrapper<UserDetail> wrapper = new LambdaQueryWrapper<>();
            if (branchId != null) {
                wrapper.eq(UserDetail::getBranchId, branchId);
            }
            if (hasKeyword) {
                String k = keyword.trim();
                wrapper.and(w -> w.like(UserDetail::getName, k)
                        .or().like(UserDetail::getStudentId, k)
                        .or().like(UserDetail::getPhone, k)
                        .or().like(UserDetail::getEmail, k));
            }
            candidateIds = userDetailMapper.selectList(wrapper).stream()
                    .map(UserDetail::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));
            // 关键字也可能命中 tb_user.username
            if (hasKeyword) {
                candidateIds.addAll(userMapper.selectList(new LambdaQueryWrapper<User>()
                                .like(User::getUsername, keyword.trim())).stream()
                        .map(User::getId).collect(Collectors.toSet()));
            }
        }

        if (roleId != null) {
            Set<Long> roleUserIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getRoleId, roleId)).stream()
                    .map(UserRole::getUserId).collect(Collectors.toSet());
            if (candidateIds == null) {
                candidateIds = roleUserIds;
            } else {
                candidateIds.retainAll(roleUserIds);
            }
        }

        if (candidateIds != null && candidateIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, page, size);
        }

        // 2) 主表分页
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            userWrapper.eq(User::getStatus, status);
        }
        if (candidateIds != null) {
            userWrapper.in(User::getId, candidateIds);
        }
        userWrapper.orderByDesc(User::getId);

        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), userWrapper);
        List<UserVo> records = buildVos(userPage.getRecords());
        return PageResult.of(records, userPage.getTotal(), page, size);
    }

    @Override
    public UserVo getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return buildVos(List.of(user)).get(0);
    }

    @Override
    public void createUser(UserSaveRequest request) {
        String username = trim(request.getUsername());
        // 必填校验由 Bean Validation（Create 组）在控制器层完成
        checkUsernameFree(username);
        if (request.getBranchId() != null) {
            checkBranch(request.getBranchId());
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(StringUtils.hasText(request.getPassword())
                ? request.getPassword() : DEFAULT_PASSWORD));
        user.setStatus(1);
        userMapper.insert(user);
        Long userId = user.getId();

        saveDetail(userId, request);
        assignRole(userId, request.getRoleId());
    }

    @Override
    public void updateUser(Long id, UserSaveRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (request.getBranchId() != null) {
            checkBranch(request.getBranchId());
        }

        // 用户名不允许修改；编辑支持部分更新（request 中为 null 的字段保持原值，空串表示清空）
        UserDetail detail = userDetailMapper.selectById(id);
        boolean detailExisted = detail != null;
        if (!detailExisted
                && !StringUtils.hasText(request.getRealName())
                && !StringUtils.hasText(request.getStudentId())) {
            throw new IllegalArgumentException("该用户缺少详情资料，请补充姓名与学号");
        }
        if (!detailExisted) {
            detail = new UserDetail();
            detail.setUserId(id);
        }
        fillDetail(detail, request);
        checkDetailUnique(id, detail.getStudentId(), detail.getPhone(), detail.getEmail());
        if (detailExisted) {
            userDetailMapper.updateById(detail);
        } else {
            userDetailMapper.insert(detail);
        }

        if (request.getRoleId() != null) {
            assignRole(id, request.getRoleId());
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 1 && status != 2)) {
            throw new IllegalArgumentException("状态不合法（1 启用 / 2 停用）");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public ImportResult importUsers(MultipartFile file) {
        List<String[]> rows;
        try {
            rows = ExcelUtil.readDataRows(file, IMPORT_HEADERS);
        } catch (IOException e) {
            throw new IllegalArgumentException("Excel 解析失败：" + e.getMessage());
        }

        int success = 0;
        List<ImportResult.RowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNo = i + 2; // Excel 实际行号（表头为第 1 行）
            try {
                importOneUser(row);
                success++;
            } catch (RuntimeException e) {
                errors.add(new ImportResult.RowError(rowNo, e.getMessage()));
            }
        }
        return ImportResult.of(rows.size(), success, errors);
    }

    @Override
    public byte[] template() {
        try {
            return ExcelUtil.createTemplate("用户导入", IMPORT_HEADERS, IMPORT_EXAMPLE);
        } catch (IOException e) {
            throw new IllegalStateException("生成导入模板失败", e);
        }
    }

    // ======================= 私有辅助 =======================

    /** 批量组装 VO（一次查详情/角色/支部，避免逐条 N+1） */
    private List<UserVo> buildVos(List<User> users) {
        if (users.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        Map<Long, UserDetail> detailMap = userDetailMapper.selectList(
                        new LambdaQueryWrapper<UserDetail>().in(UserDetail::getUserId, userIds)).stream()
                .collect(Collectors.toMap(UserDetail::getUserId, Function.identity(), (a, b) -> a));

        Map<Long, Long> userRoleMap = userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds)).stream()
                .collect(Collectors.toMap(UserRole::getUserId, UserRole::getRoleId, (a, b) -> a));
        Set<Long> roleIds = new LinkedHashSet<>(userRoleMap.values());
        Map<Long, Role> roleMap = roleIds.isEmpty() ? Collections.emptyMap()
                : roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity(), (a, b) -> a));

        Set<Long> branchIds = detailMap.values().stream()
                .map(UserDetail::getBranchId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Branch> branchMap = branchIds.isEmpty() ? Collections.emptyMap()
                : branchMapper.selectBatchIds(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Function.identity(), (a, b) -> a));

        List<UserVo> vos = new ArrayList<>();
        for (User user : users) {
            UserVo vo = new UserVo();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setStatus(user.getStatus());
            vo.setCreateTime(user.getCreateTime());
            vo.setLastLoginTime(user.getLastLoginTime());

            UserDetail detail = detailMap.get(user.getId());
            if (detail != null) {
                vo.setRealName(detail.getName());
                vo.setStudentId(detail.getStudentId());
                vo.setPhone(detail.getPhone());
                vo.setEmail(detail.getEmail());
                vo.setIdentityCardNumber(detail.getIdentityCardNumber());
                vo.setGender(detail.getGender());
                vo.setCollege(detail.getCollege());
                vo.setGrade(detail.getGrade());
                vo.setMajor(detail.getMajor());
                vo.setClassName(detail.getClassName());
                vo.setContactPerson(detail.getContactPerson());
                vo.setRemark(detail.getRemark());
                vo.setBranchId(detail.getBranchId());
                Branch branch = detail.getBranchId() == null ? null : branchMap.get(detail.getBranchId());
                vo.setBranchName(branch == null ? null : branch.getBranchName());
            }
            Long roleId = userRoleMap.get(user.getId());
            vo.setRoleId(roleId);
            Role role = roleId == null ? null : roleMap.get(roleId);
            vo.setRole(role == null ? null : normalizeRole(role.getRoleName()));
            vos.add(vo);
        }
        return vos;
    }

    /** 新增用户详情：先唯一性检查再插入（仅用于 createUser 前置校验后） */
    private void saveDetail(Long userId, UserSaveRequest request) {
        UserDetail detail = new UserDetail();
        detail.setUserId(userId);
        fillDetail(detail, request);
        if (detail.getName() == null || detail.getStudentId() == null) {
            throw new IllegalArgumentException("姓名与学号不能为空");
        }
        checkDetailUnique(userId, detail.getStudentId(), detail.getPhone(), detail.getEmail());
        userDetailMapper.insert(detail);
    }

    /** 覆盖式填充：request 中为 null 的字段保持不变（新增即默认空）；空串视为清空 */
    private void fillDetail(UserDetail detail, UserSaveRequest request) {
        setIfPresent(detail::setName, request.getRealName());
        setIfPresent(detail::setStudentId, request.getStudentId());
        setIfPresent(detail::setPhone, request.getPhone());
        setIfPresent(detail::setEmail, request.getEmail());
        setIfPresent(detail::setIdentityCardNumber, request.getIdentityCardNumber());
        setIfPresent(detail::setGender, request.getGender());
        setIfPresent(detail::setCollege, request.getCollege());
        setIfPresent(detail::setGrade, request.getGrade());
        setIfPresent(detail::setMajor, request.getMajor());
        setIfPresent(detail::setClassName, request.getClassName());
        setIfPresent(detail::setContactPerson, request.getContactPerson());
        setIfPresent(detail::setRemark, request.getRemark());
        if (request.getBranchId() != null) {
            detail.setBranchId(request.getBranchId());
        }
    }

    /** value 为 null 则不动；否则去空白，空串存 null */
    private void setIfPresent(java.util.function.Consumer<String> setter, String value) {
        if (value == null) {
            return;
        }
        setter.accept(trimToNull(value));
    }

    /** 详情唯一约束检查（学号/手机/邮箱）；dupSkipId 不为空时排除该用户 */
    private void checkDetailUnique(Long dupSkipId, String studentId, String phone, String email) {
        if (!StringUtils.hasText(studentId) && !StringUtils.hasText(phone) && !StringUtils.hasText(email)) {
            return;
        }
        LambdaQueryWrapper<UserDetail> wrapper = new LambdaQueryWrapper<>();
        if (dupSkipId != null) {
            wrapper.ne(UserDetail::getUserId, dupSkipId);
        }
        wrapper.and(w -> {
            boolean first = true;
            if (StringUtils.hasText(studentId)) {
                w.eq(UserDetail::getStudentId, studentId);
                first = false;
            }
            if (StringUtils.hasText(phone)) {
                if (first) {
                    w.eq(UserDetail::getPhone, phone);
                    first = false;
                } else {
                    w.or().eq(UserDetail::getPhone, phone);
                }
            }
            if (StringUtils.hasText(email)) {
                if (first) {
                    w.eq(UserDetail::getEmail, email);
                } else {
                    w.or().eq(UserDetail::getEmail, email);
                }
            }
        });
        if (!userDetailMapper.selectList(wrapper).isEmpty()) {
            throw new IllegalArgumentException("学号/手机号/邮箱与其他用户冲突");
        }
    }

    private void checkUsernameFree(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }

    private void checkBranch(Long branchId) {
        if (branchId == null) {
            return;
        }
        Branch branch = branchMapper.selectById(branchId);
        if (branch == null || branch.getStatus() == null || branch.getStatus() != 1) {
            throw new IllegalArgumentException("所选党支部不存在或已停用");
        }
    }

    /** 单角色覆盖：先删该用户旧角色，再按需插入新角色 */
    private void assignRole(Long userId, Long roleId) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (roleId == null) {
            Role student = findRoleByCode("student");
            roleId = student == null ? null : student.getId();
        }
        if (roleId == null) {
            return;
        }
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("所选角色不存在");
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }

    /** 按规范化编码查角色（兼容库中 ROLE_ 前缀/大小写差异） */
    private Role findRoleByCode(String code) {
        return roleMapper.selectList(null).stream()
                .filter(r -> code.equals(normalizeRole(r.getRoleName())))
                .findFirst().orElse(null);
    }

    /** 角色名规范化：去 ROLE_ 前缀转小写 */
    private String normalizeRole(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            return "";
        }
        String role = roleName.trim();
        if (role.regionMatches(true, 0, "ROLE_", 0, 5)) {
            role = role.substring(5);
        }
        return role.toLowerCase();
    }

    /** 导入单行用户（列：0-5 基础，6-12 新扩展字段，均可空） */
    private void importOneUser(String[] row) {
        String studentId = cell(row, 0);
        String realName = cell(row, 1);
        String phone = cell(row, 2);
        String email = cell(row, 3);
        String branchName = cell(row, 4);
        String roleText = cell(row, 5);

        if (!StringUtils.hasText(studentId)) {
            throw new IllegalArgumentException("学号不能为空");
        }
        if (!StringUtils.hasText(realName)) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (StringUtils.hasText(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (StringUtils.hasText(email) && !email.contains("@")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }

        Long existing = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, studentId));
        if (existing != null && existing > 0) {
            throw new IllegalArgumentException("用户名（学号）已存在");
        }
        if (StringUtils.hasText(phone)) {
            Long p = userDetailMapper.selectCount(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getPhone, phone));
            if (p != null && p > 0) {
                throw new IllegalArgumentException("手机号已存在");
            }
        }
        if (StringUtils.hasText(email)) {
            Long e = userDetailMapper.selectCount(new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email));
            if (e != null && e > 0) {
                throw new IllegalArgumentException("邮箱已存在");
            }
        }
        Long s = userDetailMapper.selectCount(new LambdaQueryWrapper<UserDetail>()
                .eq(UserDetail::getStudentId, studentId));
        if (s != null && s > 0) {
            throw new IllegalArgumentException("学号已存在");
        }

        // 支部按名称匹配（启用中）
        Long branchId = null;
        if (StringUtils.hasText(branchName)) {
            Branch branch = branchMapper.selectList(new LambdaQueryWrapper<Branch>()
                            .eq(Branch::getBranchName, branchName.trim()).eq(Branch::getStatus, 1)).stream()
                    .findFirst().orElse(null);
            if (branch == null) {
                throw new IllegalArgumentException("所属支部不存在（请先维护支部）");
            }
            branchId = branch.getId();
        }

        // 角色列解析：空 → 普通成员
        String roleCode = "student";
        if (StringUtils.hasText(roleText)) {
            String candidate = normalizeRole(roleText);
            if (candidate.isEmpty()) {
                candidate = "student";
            }
            String map = switch (candidate) {
                case "普通成员" -> "student";
                case "支部管理员" -> "branch_admin";
                case "超级管理员" -> "super_admin";
                default -> candidate;
            };
            if (!List.of("student", "branch_admin", "super_admin").contains(map)) {
                throw new IllegalArgumentException("角色列取值无效：" + roleText);
            }
            roleCode = map;
        }
        Role role = findRoleByCode(roleCode);
        if (role == null) {
            throw new IllegalArgumentException("角色「" + roleCode + "」不存在，请先在角色管理中创建");
        }

        User user = new User();
        user.setUsername(studentId);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setStatus(1);
        userMapper.insert(user);
        Long userId = user.getId();

        UserDetail detail = new UserDetail();
        detail.setUserId(userId);
        detail.setName(realName);
        detail.setStudentId(studentId);
        detail.setPhone(cellOrNull(row, 2));
        detail.setEmail(cellOrNull(row, 3));
        detail.setBranchId(branchId);
        // 新扩展字段（选填）
        detail.setGender(cellOrNull(row, 6));
        detail.setCollege(cellOrNull(row, 7));
        detail.setGrade(cellOrNull(row, 8));
        detail.setMajor(cellOrNull(row, 9));
        detail.setClassName(cellOrNull(row, 10));
        detail.setContactPerson(cellOrNull(row, 11));
        detail.setRemark(cellOrNull(row, 12));
        userDetailMapper.insert(detail);

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private String cell(String[] row, int index) {
        return index < row.length && row[index] != null ? row[index].trim() : "";
    }

    private String cellOrNull(String[] row, int index) {
        String value = cell(row, index);
        return value.isEmpty() ? null : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String v = trim(value);
        return v == null || v.isEmpty() ? null : v;
    }
}
