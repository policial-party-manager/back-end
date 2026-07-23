package sicau.policialPartyManager.filter;

import cn.hutool.core.text.AntPathMatcher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sicau.policialPartyManager.config.AuthenticationConfig;
import sicau.policialPartyManager.config.SecurityConfig;
import sicau.policialPartyManager.dao.enums.HttpResultCode;
import sicau.policialPartyManager.service.impl.AuthServiceImpl;
import sicau.policialPartyManager.utils.JwtUtils;

import java.io.IOException;
import java.util.List;

@Component
@AllArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    private final RedisTemplate<String, Object> redisTemplate;

    private final AuthenticationConfig authenticationConfig;

    private final SecurityConfig securityConfig;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 从请求头中获取token
        String token = request.getHeader(jwtUtils.getTokenHeader());
        if (token == null || !token.startsWith(jwtUtils.getTokenPrefix())) {
            sendError(response, HttpResultCode.UNAUTHORIZED_LOGIN);
            return;
        }

        // 检查token是否过期
        if (!jwtUtils.isTokenExpired(token)) {
            sendError(response, HttpResultCode.TOKEN_EXPIRED);
            return;
        }

        // 检查token是否有效
        if (!jwtUtils.isTokenValid(token)) {
            sendError(response, HttpResultCode.TOKEN_INVALID);
            return;
        }

        // 从token中获取userId
        token = token.substring(jwtUtils.getTokenPrefix().length());
        String userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            sendError(response, HttpResultCode.UNAUTHORIZED_LOGIN);
            return;
        }

        // 检查token中unique code是否有效
        String uniqueCode = jwtUtils.getUniqueCodeFromToken(token);
        if (uniqueCode == null || !authenticationConfig.checkUnique(Long.parseLong(userId), uniqueCode)) {
            sendError(response, HttpResultCode.UNIQUE_CODE_ERROR);
            return;
        }

        // 从redis中获取用户角色
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) redisTemplate.opsForValue().get(AuthServiceImpl.getUserRolesKey() + ":" + userId);
        if (roles == null || roles.isEmpty()) {
            sendError(response, HttpResultCode.AUTHORIZATION_ROLE_ERROR);
            return;
        }
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        // 设置用户角色
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, authorities));
        filterChain.doFilter(request, response);
    }

    /**
     * 检查是否需要过滤请求
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return securityConfig.getWhiteList().stream().anyMatch(path -> pathMatcher.match(path, uri));
    }

    /**
     * 发送错误响应
     */
    private void sendError(HttpServletResponse response, HttpResultCode httpResultCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(httpResultCode.getHttpStatusCode());
        response.getWriter().write(httpResultCode.getMessage());
    }
}