package org.example.civitaswebapp.integration;

import org.example.civitaswebapp.repository.MyUserRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the migrations in {@code db/migration} (starting from {@code V1__baseline.sql}) build, on an
 * empty database, a schema the entities accept: Flyway runs for real and Hibernate runs with
 * {@code ddl-auto=validate}, exactly as in dev and prod. If an entity and the migrations drift apart,
 * the context fails to start here.
 *
 * <p>Unlike {@link MySqlIntegrationTest} (create-drop, Flyway off) this needs its own empty
 * container, so it doesn't extend that base. Skipped when Docker isn't usable; on Docker Engine 29+
 * run with {@code -DargLine="-Dapi.version=1.44"} (see {@link MySqlIntegrationTest}).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.messages.basename=i18n/messages",
        "stripe.secret.key=sk_test_dummy",
        "stripe.webhook.secret=whsec_dummy",
        "twilio.account-sid=ACdummy",
        "twilio.auth-token=dummy",
        "twilio.whatsapp-number=whatsapp:+10000000000"
})
class FlywayBaselineIntegrationTest {

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

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired MyUserRepository userRepository;

    @Test
    void emptyDatabaseIsBuiltByTheMigrationsAndPassesHibernateValidation() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).isNotEmpty();
        assertThat(applied[0].getVersion().getVersion()).as("V1 baseline runs first").isEqualTo("1");
        assertThat(applied).allSatisfy(m -> assertThat(m.getState()).isEqualTo(MigrationState.SUCCESS));
        assertThat(flyway.info().pending()).isEmpty();

        // The manually applied VIEWER migration is part of the baseline, not a separate step.
        String usersDdl = (String) jdbc.queryForMap("SHOW CREATE TABLE users").get("Create Table");
        assertThat(usersDdl).containsIgnoringCase("enum('ADMIN','VIEWER')");

        // InitDataConfig ran against the Flyway-built schema, so inserts through JPA work too.
        assertThat(userRepository.count()).isPositive();
    }

    @Test
    void existingDatabasesAreNeverBaselinedImplicitly() {
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
        assertThat(flyway.getConfiguration().getBaselineVersion().getVersion()).isEqualTo("1");
        assertThat(flyway.getConfiguration().isCleanDisabled()).isTrue();
    }
}
