package sicau.policialPartyManager.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import sicau.policialPartyManager.filter.JwtAuthFilter;
import sicau.policialPartyManager.log.OperationLogFilter;
import sicau.policialPartyManager.log.OperationLogPublisher;

import java.util.List;

/**
 * Spring Security 过滤链配置。
 * <p>
 * 规则约定：仅两类资源放行——接口文档与认证相关接口（/api/v1/auth/**），
 * 其余一律要求已登录（具体模块权限用方法级 {@code @PreAuthorize} 控制）。
 * 操作日志过滤器注册在 JwtAuthFilter 之后（非容器 Bean，避免被 Servlet 容器重复注册），
 * 认证通过/失败（401/403）与请求异常都能被记录。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** 放行：接口文档资源 */
    private static final String[] DOC_MATCHERS = {
            "/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-resources/**"
    };
    /** 放行：认证相关接口（登录/验证码/刷新/退出） */
    private static final String[] AUTH_MATCHERS = {"/api/v1/auth/**"};

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OperationLogPublisher publisher) throws Exception {
        OperationLogFilter operationLogFilter = new OperationLogFilter(publisher);
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(DOC_MATCHERS).permitAll()
                .requestMatchers(AUTH_MATCHERS).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(operationLogFilter, JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
