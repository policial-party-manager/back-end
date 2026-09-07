package sicau.policialPartyManager.config;

import lombok.Data;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.cas.config.CasProtocol;
import org.pac4j.core.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CAS 统一身份认证配置（pac4j）。
 * <p>
 * 未配置 cas.server-url / cas.callback-url 时，仅不产生可用的登录跳转，
 * 相关接口会给出“CAS 未配置”提示，不影响其余登录方式与启动。
 */
@Configuration
@Data
public class CasConfig {

    @Value("${cas.server-url:}")
    private String casServerUrl;

    @Value("${cas.client-name:casClient}")
    private String casClientName;

    @Value("${cas.callback-url:}")
    private String casCallbackUrl;

    @Value("${cas.app-id:}")
    private String casAppId;

    @Bean
    public CasConfiguration casConfiguration() {
        final CasConfiguration casConfiguration = new CasConfiguration();
        casConfiguration.setLoginUrl(casServerUrl + "login");
        casConfiguration.setPrefixUrl(casServerUrl);
        casConfiguration.setProtocol(CasProtocol.CAS30);
        return casConfiguration;
    }

    @Bean
    public CasClient casClient(final CasConfiguration casConfiguration) {
        final CasClient casClient = new CasClient();
        casClient.setConfiguration(casConfiguration);
        casClient.setName(casClientName);
        casClient.setCallbackUrl(casCallbackUrl);
        return casClient;
    }

    @Bean
    public Config config(final CasClient casClient) {
        return new Config(casClient);
    }
}
