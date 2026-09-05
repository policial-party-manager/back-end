package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.LoginRequest;
import sicau.policialPartyManager.api.dto.LoginResponse;
import sicau.policialPartyManager.model.entity.*;
import sicau.policialPartyManager.repository.*;
import sicau.policialPartyManager.utils.CodeUtil;
import sicau.policialPartyManager.utils.JwtUtil;
import sicau.policialPartyManager.utils.MailUtil;
import sicau.policialPartyManager.service.AuthService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * 认证服务实现：用户名密码 / 邮箱验证码 / 手机号验证码登录，
 * 验证码发送（邮件；短信通道留白）、刷新 token、退出登录（吊销 refresh token）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 角色编码兜底 */
    private static final String ROLE_STUDENT = "student";
    /** refresh token 白名单 Redis key 前缀 */
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final BranchMapper branchMapper;
    private final UserDetailMapper userDetailMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CodeUtil codeUtil;
    private final MailUtil mailUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginResponse loginUsernamePassword(LoginRequest request) {
        String username = trim(request.getUsername());
        String password = trim(request.getPassword());

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new IllegalArgumentException("用户名不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        ensureEnabled(user);
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse loginEmail(LoginRequest request) {
        String email = trim(request.getEmail());
        String code = trim(request.getVerifyCode());

        UserDetail detail = userDetailMapper.selectOne(
                new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getEmail, email));
        if (detail == null) {
            throw new IllegalArgumentException("该邮箱尚未注册");
        }
        if (!codeUtil.verify(email, code)) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        User user = userMapper.selectById(detail.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        ensureEnabled(user);
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse loginPhone(LoginRequest request) {
        String phone = trim(request.getPhone());
        String code = trim(request.getVerifyCode());

        UserDetail detail = userDetailMapper.selectOne(
                new LambdaQueryWrapper<UserDetail>().eq(UserDetail::getPhone, phone));
        if (detail == null) {
            throw new IllegalArgumentException("该手机号尚未注册");
        }
        if (!codeUtil.verify(phone, code)) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        User user = userMapper.selectById(detail.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        ensureEnabled(user);
        return buildLoginResponse(user);
    }

    @Override
    public String verifyCode(LoginRequest request) {
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        boolean hasEmail = StringUtils.hasText(email);
        boolean hasPhone = StringUtils.hasText(phone);
        if (!hasEmail && !hasPhone) {
            throw new IllegalArgumentException("请提供邮箱或手机号");
        }

        // 二者同时提供时以邮箱为准；至少提供其一由 Bean Validation（类级校验）保证
        String target = hasEmail ? email : phone;

        String code = codeUtil.createAndStore(target);
        if (hasEmail) {
            mailUtil.sendVerifyCode(email, code);
        } else {
            // 短信通道尚未接入（留白）：验证码已存 Redis，后续接入短信服务时在此处真实发送
            log.warn("[短信通道未接入] 目标手机号: {}, 验证码: {}（5 分钟内有效，仅本地联调可见）", phone, code);
        }
        return "验证码已发送，请查收";
    }

    @Override
    public LoginResponse refresh(LoginRequest request) {
        String refreshToken = trim(request.getRefreshToken());

        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("refresh token 无效或已过期，请重新登录");
        }
        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            throw new IllegalArgumentException("refresh token 无效");
        }

        // 白名单校验：只有登录时下发的 refresh token 才能刷新
        String stored = redisTemplate.opsForValue().get(refreshKey(refreshToken));
        if (stored == null || !stored.equals(String.valueOf(userId))) {
            throw new IllegalArgumentException("refresh token 已失效，请重新登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        ensureEnabled(user);

        // 轮换：作废旧 token，签发新 token 对
        redisTemplate.delete(refreshKey(refreshToken));
        return buildLoginResponse(user);
    }

    @Override
    public void logout(LoginRequest request) {
        String refreshToken = trim(request.getRefreshToken());
        if (StringUtils.hasText(refreshToken)) {
            // 吊销 refresh token：从白名单删除后 refresh 接口将拒绝
            redisTemplate.delete(refreshKey(refreshToken));
        }
        // access token 为无状态 JWT，不做服务端黑名单，由前端丢弃即可，到期自然失效
    }

    /**
     * 通过 tb_user_role + tb_role 获取用户角色编码并规范化：
     * 兼容库中可能存储的 "ROLE_SUPER_ADMIN" / "ROLE_super_admin" / "super_admin" 等写法，
     * 统一返回小写角色编码（super_admin / branch_admin / student）。
     */
    @Override
    public String getUserRole(Long userId) {
        UserRole userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRole == null || userRole.getRoleId() == null) {
            return ROLE_STUDENT;
        }
        Role role = roleMapper.selectById(userRole.getRoleId());
        return role == null ? ROLE_STUDENT : normalizeRole(role.getRoleName());
    }

    // ======================= 私有辅助方法 =======================

    /** 登录成功统一装配：角色、token 签发与白名单、支部/姓名等用户信息 */
    private LoginResponse buildLoginResponse(User user) {
        String role = getUserRole(user.getId());
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), role);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), role);
        saveRefreshToken(refreshToken, user.getId());

        Long branchId = null;
        String branchName = null;
        String realName = null;
        UserDetail userDetail = userDetailMapper.selectById(user.getId());
        if (userDetail != null) {
            realName = userDetail.getName();
            if (userDetail.getBranchId() != null) {
                branchId = userDetail.getBranchId();
                Branch branch = branchMapper.selectById(userDetail.getBranchId());
                if (branch == null) {
                    throw new RuntimeException("该用户党支部无法查询");
                }
                branchName = branch.getBranchName();
            }
        }

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .realName(realName)
                .role(role)
                .branchId(branchId)
                .branchName(branchName)
                .build();
    }

    /** 校验账号可用（1=启用，2=停用） */
    private void ensureEnabled(User user) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已被停用");
        }
    }

    /** refresh token 写入 Redis 白名单，过期时间与 refresh token 一致 */
    private void saveRefreshToken(String refreshToken, Long userId) {
        redisTemplate.opsForValue().set(refreshKey(refreshToken), String.valueOf(userId),
                Duration.ofMillis(jwtUtil.getRefreshTokenExpirationMillis()));
    }

    /** 以 token 的 SHA-256 作为白名单 key（避免超长 key 与明文敏感信息落库） */
    private String refreshKey(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return REFRESH_KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /** 去掉可选的 ROLE_ 前缀并转小写，得到统一角色编码 */
    private String normalizeRole(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            return ROLE_STUDENT;
        }
        String role = roleName.trim();
        if (role.regionMatches(true, 0, "ROLE_", 0, 5)) {
            role = role.substring(5);
        }
        return role.toLowerCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
