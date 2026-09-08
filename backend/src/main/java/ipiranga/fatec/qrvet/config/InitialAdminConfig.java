package ipiranga.fatec.qrvet.config;

import ipiranga.fatec.qrvet.dtos.request.BootstrapAdminRequest;
import ipiranga.fatec.qrvet.services.BootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitialAdminConfig {
    private static final Logger log = LoggerFactory.getLogger(InitialAdminConfig.class);

    @Bean
    @ConditionalOnProperty(
            name = "qrvet.bootstrap-admin.enabled",
            havingValue = "true")
    ApplicationRunner initialAdminRunner(
            BootstrapService bootstrapService,
            @Value("${qrvet.bootstrap-admin.name}") String name,
            @Value("${qrvet.bootstrap-admin.email}") String email,
            @Value("${qrvet.bootstrap-admin.password}") String password) {
        return arguments -> {
            boolean created = bootstrapService.createInitialAdmin(
                    new BootstrapAdminRequest(name, email, password));
            if (created) {
                log.info("Initial administrator account created for {}", email);
            } else {
                log.info("An administrator already exists; initial account creation was skipped");
            }
        };
    }
}
