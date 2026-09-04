package sicau.policialPartyManager.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 邮件工具：发送验证码邮件。
 * <p>
 * 未配置 {@code spring.mail.host}（或 {@code spring.mail.username}）时 Spring 不会创建
 * JavaMailSender，本工具自动降级为把验证码打印到日志，方便本地开发与前后端联调；
 * 配置真实 SMTP 后即可真正发送。
 */
@Slf4j
@Component
public class MailUtil {

    private static final String SUBJECT = "【党建管理系统】验证码";

    private final JavaMailSender mailSender;
    private final String mailUsername;
    private final boolean enabled;

    public MailUtil(ObjectProvider<JavaMailSender> mailSenderProvider,
                    @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailUsername = mailUsername;
        this.enabled = mailSender != null && StringUtils.hasText(mailUsername);
        if (!enabled) {
            log.warn("邮件未配置（缺 spring.mail.host 或 spring.mail.username），发送将降级为日志打印，仅供本地联调");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 发送验证码邮件。未启用邮件时把验证码打印到日志，返回 false。
     *
     * @return 是否真正通过 SMTP 发送成功
     */
    public boolean sendVerifyCode(String to, String code) {
        if (!enabled || mailSender == null) {
            log.warn("[验证码-降级输出] 收件方: {}, 验证码: {}（5 分钟内有效）", to, code);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(to);
            message.setSubject(SUBJECT);
            message.setText("您的验证码为：" + code + "，5 分钟内有效。若非本人操作，请忽略本邮件。");
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("验证码邮件发送失败，收件方: {}", to, e);
            return false;
        }
    }
}
