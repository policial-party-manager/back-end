package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.LoginResponse;
import sicau.policialPartyManager.config.CasConfig;
import sicau.policialPartyManager.model.entity.User;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.repository.UserMapper;
import sicau.policialPartyManager.service.AuthService;
import sicau.policialPartyManager.service.SsoService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CAS 统一身份认证实现。
 * <p>
 * 登录链路：前端跳 {@code /api/v1/auth/sso/login} 取得 CAS 登录页地址；
 * CAS 验证通过后回跳 {@code /api/v1/auth/sso/callback?ticket=…}，本服务使用
 * proxyValidate 校验票据并解析属性，仅允许“已存在且启用”的本地账号，随后
 * 复用本站 JWT 签发逻辑返回登录态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoServiceImpl implements SsoService {

    private static final JEESessionStore SESSION_STORE = new JEESessionStore();

    private final CasConfig casConfig;
    private final Config pac4jConfig;
    private final AuthService authService;
    private final UserMapper userMapper;
    private final UserDetailMapper userDetailMapper;

    @Value("${cas.client-name:casClient}")
    private String clientName;

    @Override
    public String buildCasLoginUrl(HttpServletRequest request, HttpServletResponse response) {
        checkConfigured();
        WebContext context = new JEEContext(request, response);
        CallContext callContext = new CallContext(context, SESSION_STORE);
        try {
            Optional<RedirectionAction> redirectAction = pac4jConfig.getClients()
                    .findClient(clientName).get().getRedirectionAction(callContext);
            if (redirectAction.isEmpty()) {
                return null;
            }
            FoundAction foundAction = (FoundAction) redirectAction.get();
            String url = foundAction.getLocation();
            if (StringUtils.hasText(casConfig.getCasAppId())) {
                url += (url.contains("?") ? "&" : "?") + "appid=" + casConfig.getCasAppId();
            }
            return url;
        } catch (Exception e) {
            log.warn("生成 CAS 登录地址失败", e);
            return null;
        }
    }

    @Override
    public LoginResponse handleCallback(HttpServletRequest request) {
        checkConfigured();
        String ticket = request.getParameter("ticket");
        if (!StringUtils.hasText(ticket)) {
            throw new IllegalArgumentException("缺少 CAS 票据（ticket）");
        }

        Map<String, String> attributes = validateAndParse(ticket);

        String loginId = firstText(attributes, "loginid", "workcode", null);
        if (!StringUtils.hasText(loginId)) {
            loginId = attributes.get("user");
        }
        if (!StringUtils.hasText(loginId)) {
            throw new IllegalArgumentException("CAS 未返回可识别的账号（loginid/workcode）");
        }

        // 仅允许已存在账号（不开户），账号状态在签发处校验
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginId.trim()));
        if (user == null) {
            throw new IllegalArgumentException("账号未开通，请联系管理员");
        }

        // 仅空字段回填（email / certificatenum 不落库）
        backfillBlankFields(user.getId(), attributes);

        return authService.loginByUserId(user.getId());
    }

    // ======================= 私有辅助 =======================

    private void checkConfigured() {
        if (!StringUtils.hasText(casConfig.getCasServerUrl())
                || !StringUtils.hasText(casConfig.getCasCallbackUrl())) {
            throw new IllegalArgumentException("CAS 未配置，请联系管理员配置 cas.server-url / cas.callback-url");
        }
    }

    /** proxyValidate 校验并解析返回的属性（按无前缀 localName 归一） */
    private Map<String, String> validateAndParse(String ticket) {
        String server = casConfig.getCasServerUrl().endsWith("/")
                ? casConfig.getCasServerUrl() : casConfig.getCasServerUrl() + "/";
        String validateUrl = server + "proxyValidate";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(validateUrl);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("service", casConfig.getCasCallbackUrl()));
            params.add(new BasicNameValuePair("ticket", ticket));
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            String body;
            try (var httpResponse = httpClient.execute(httpPost)) {
                body = EntityUtils.toString(httpResponse.getEntity());
            }
            Element root = DocumentHelper.parseText(body).getRootElement();
            return parseCasXml(root);
        } catch (Exception e) {
            log.warn("CAS 票据校验失败", e);
            throw new IllegalArgumentException("CAS 票据校验失败，请重新登录");
        }
    }

    /** 解析 <cas:serviceResponse>：失败节点直接抛业务错；成功节点返回属性映射 */
    private Map<String, String> parseCasXml(Element root) {
        Element failure = child(root, "authenticationFailure");
        if (failure != null) {
            String code = failure.attributeValue("code");
            throw new IllegalArgumentException("CAS 认证失败：" + (code == null ? "未知原因" : code));
        }
        Element success = child(root, "authenticationSuccess");
        if (success == null) {
            throw new IllegalArgumentException("CAS 响应格式异常");
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("user", text(success, "user"));
        Element attrs = child(success, "attributes");
        if (attrs != null) {
            for (Element attr : attrs.elements()) {
                String name = attr.getQName().getName();
                String value = attr.getTextTrim();
                if (name != null && !name.isEmpty()) {
                    attributes.putIfAbsent(name, value);
                }
            }
        }
        return attributes;
    }

    /** 空字段回填：name/mobile/sex→gender/studentId（loginid）；email、身份证不落库 */
    private void backfillBlankFields(Long userId, Map<String, String> attributes) {
        UserDetail detail = userDetailMapper.selectById(userId);
        if (detail == null) {
            return;
        }
        boolean changed = false;
        if (!StringUtils.hasText(detail.getName()) && StringUtils.hasText(attributes.get("name"))) {
            detail.setName(attributes.get("name").trim());
            changed = true;
        }
        String mobile = firstText(attributes, "mobile", "telephone", null);
        if (!StringUtils.hasText(detail.getPhone()) && StringUtils.hasText(mobile)) {
            detail.setPhone(mobile.trim());
            changed = true;
        }
        String gender = mapSex(attributes.get("sex"));
        if (!StringUtils.hasText(detail.getGender()) && gender != null) {
            detail.setGender(gender);
            changed = true;
        }
        String loginId = attributes.get("loginid");
        if (!StringUtils.hasText(detail.getStudentId()) && StringUtils.hasText(loginId)) {
            detail.setStudentId(loginId.trim());
            changed = true;
        }
        if (changed) {
            userDetailMapper.updateById(detail);
        }
    }

    /** sex：1→男，2→女，其它忽略 */
    private String mapSex(String sex) {
        if (!StringUtils.hasText(sex)) {
            return null;
        }
        return switch (sex.trim()) {
            case "1" -> "男";
            case "2" -> "女";
            default -> null;
        };
    }

    private Element child(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        for (Element element : parent.elements()) {
            if (localName.equals(element.getQName().getName())) {
                return element;
            }
        }
        return null;
    }

    private String text(Element parent, String localName) {
        Element target = child(parent, localName);
        return target == null ? null : target.getTextTrim();
    }

    private String firstText(Map<String, String> map, String... keys) {
        for (String key : keys) {
            if (map != null && StringUtils.hasText(map.get(key))) {
                return map.get(key);
            }
        }
        return null;
    }
}
