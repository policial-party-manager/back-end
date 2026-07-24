package sicau.policialPartyManager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("党建云平台 API")
                        .description("Policial Party Manager — 高校党建管理平台接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Enky")
                                .email("enky@sicau.edu.cn")))
                // JWT 认证方案：在 Swagger UI 中填入 token 即可测试需要登录的接口
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .name("JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录后获取 token，格式：Bearer <token>")));
    }
}
