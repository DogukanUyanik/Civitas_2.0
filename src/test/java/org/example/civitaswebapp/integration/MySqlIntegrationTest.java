package org.example.civitaswebapp.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the full application against a throwaway MySQL 8 container. The repo ships no
 * {@code application.properties} (config comes from the environment), so everything the context
 * needs is supplied here with dummy values; nothing reaches Stripe or Twilio.
 *
 * <p>One container is shared by every subclass (started once, reaped by Testcontainers when the
 * JVM exits) so the cached Spring context keeps pointing at a live database.
 *
 * <p>Skipped (not failed) when Docker isn't usable. Note Docker Engine 29+ rejects the API version
 * spoken by Testcontainers 1.21.x ("client version 1.32 is too old"); until Testcontainers is
 * upgraded, run these with {@code -DargLine="-Dapi.version=1.44"}.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.messages.basename=i18n/messages",
        "stripe.secret.key=sk_test_dummy",
        "stripe.webhook.secret=whsec_dummy",
        "twilio.account-sid=ACdummy",
        "twilio.auth-token=dummy",
        "twilio.whatsapp-number=whatsapp:+10000000000"
})
abstract class MySqlIntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
