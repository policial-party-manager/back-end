package sicau.policialPartyManager.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sicau.policialPartyManager.api.dto.LoginResponse;

/**
 * 学校 CAS 统一身份认证登录（新增登录方式）。
 */
public interface SsoService {

    /**
     * 生成跳转 CAS 登录页的地址（追加 appid）。
     *
     * @return CAS 登录页 URL；生成失败返回 null
     * @throws IllegalArgumentException CAS 未配置
     */
    String buildCasLoginUrl(HttpServletRequest request, HttpServletResponse response);

    /**
     * 处理 CAS 回跳：校验 ticket → 解析用户属性 → 仅允许已存在且启用账号 → 签发本站 token。
     *
     * @throws IllegalArgumentException 票据校验失败 / 账号未开通 / 账号停用 等（供上层回跳携带 error）
     */
    LoginResponse handleCallback(HttpServletRequest request);
}
