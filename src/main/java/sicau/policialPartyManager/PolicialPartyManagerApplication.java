package sicau.policialPartyManager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("sicau.policialPartyManager.repository")
public class PolicialPartyManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolicialPartyManagerApplication.class, args);
    }

}
