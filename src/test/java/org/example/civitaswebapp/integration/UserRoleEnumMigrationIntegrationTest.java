package org.example.civitaswebapp.integration;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.repository.UnionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the reason for, and the correctness of, {@code docs/migrations/2026-09-add-viewer-role.sql}.
 * It rebuilds the state of a database created by the previous build (native ENUM('USER','ADMIN')
 * with a USER row in it), shows that a VIEWER can't be stored there, then runs the committed
 * script and checks the result. Runs against a throwaway container only.
 */
class UserRoleEnumMigrationIntegrationTest extends MySqlIntegrationTest {

    private static final Path SCRIPT = Path.of("docs/migrations/2026-09-add-viewer-role.sql");

    @Autowired JdbcTemplate jdbc;
    @Autowired MyUserRepository userRepository;
    @Autowired UnionRepository unionRepository;

    private String roleColumnDdl() {
        return (String) jdbc.queryForMap("SHOW CREATE TABLE users").get("Create Table");
    }

    private void runScript() throws Exception {
        String sql = String.join("\n", Files.readAllLines(SCRIPT).stream()
                .filter(line -> !line.trim().startsWith("--")).toList());
        List<String> statements = Arrays.stream(sql.split(";")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        assertThat(statements).as("script should contain the 3 migration statements").hasSize(3);
        statements.forEach(jdbc::execute);
    }

    @Test
    void migrationTurnsALegacyEnumColumnIntoOneThatAcceptsViewer_andConvertsUserRows() throws Exception {
        Union union = unionRepository.save(Union.builder().name("Legacy " + UUID.randomUUID()).address("x").build());
        String legacyUsername = "legacy-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            // --- Arrange: the schema the previous build created on an existing database -------------
            jdbc.execute("DELETE FROM users WHERE role = 'VIEWER'");
            // Insert through JPA (union_id is a binary UUID) as ADMIN, narrow the column the way the
            // old build had it, then turn the row into the legacy USER it would have been.
            userRepository.saveAndFlush(MyUser.builder()
                    .username(legacyUsername).password("x").role(MyUserRole.ADMIN).union(union).build());
            jdbc.execute("ALTER TABLE users MODIFY COLUMN role ENUM('USER','ADMIN') NOT NULL");
            jdbc.update("UPDATE users SET role = 'USER' WHERE username = ?", legacyUsername);

            // --- Without the migration a VIEWER cannot be stored (this is what update-mode leaves) ---
            assertThatThrownBy(() -> userRepository.saveAndFlush(MyUser.builder()
                    .username("viewer-" + UUID.randomUUID()).password("x").role(MyUserRole.VIEWER).union(union).build()))
                    .isInstanceOf(DataAccessException.class);

            // --- Act: run the committed script (twice: it must be idempotent) -----------------------
            runScript();
            runScript();

            // --- Assert ------------------------------------------------------------------------------
            assertThat(roleColumnDdl()).containsIgnoringCase("enum('ADMIN','VIEWER')");
            assertThat(userRepository.findByUsername(legacyUsername).getRole())
                    .as("legacy USER row becomes VIEWER and loads through Hibernate")
                    .isEqualTo(MyUserRole.VIEWER);

            MyUser viewer = userRepository.saveAndFlush(MyUser.builder()
                    .username("viewer-" + UUID.randomUUID()).password("x").role(MyUserRole.VIEWER).union(union).build());
            assertThat(userRepository.findById(viewer.getId()).orElseThrow().getRole()).isEqualTo(MyUserRole.VIEWER);
        } finally {
            // Leave the shared database in the same shape a fresh build creates.
            jdbc.execute("ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN','VIEWER') NOT NULL");
            userRepository.deleteAll(userRepository.findAllByUnionOrderByUsernameAsc(union));
            unionRepository.delete(union);
        }
    }
}
