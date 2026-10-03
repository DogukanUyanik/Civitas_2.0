package org.example.civitaswebapp.integration;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberLanguage;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.UnionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves {@code db/migration/V2__member_language_not_null.sql} against the production shape of the
 * column: {@code language} created nullable by {@code ddl-auto=update} before {@code @NotNull} existed.
 * The script is run by hand here (Flyway is off in {@link MySqlIntegrationTest}); that the full
 * migration chain builds a valid schema is covered by {@code FlywayBaselineIntegrationTest}.
 */
class MemberLanguageNotNullMigrationIntegrationTest extends MySqlIntegrationTest {

    private static final Path SCRIPT = Path.of("src/main/resources/db/migration/V2__member_language_not_null.sql");
    private static final String NULLABLE_COLUMN =
            "ALTER TABLE member MODIFY COLUMN language ENUM('EN','NL','TR') DEFAULT NULL";

    @Autowired JdbcTemplate jdbc;
    @Autowired MemberRepository memberRepository;
    @Autowired UnionRepository unionRepository;

    private String memberTableDdl() {
        return (String) jdbc.queryForMap("SHOW CREATE TABLE member").get("Create Table");
    }

    private void runScript() throws Exception {
        String sql = String.join("\n", Files.readAllLines(SCRIPT).stream()
                .filter(line -> !line.trim().startsWith("--")).toList()).trim();
        assertThat(sql).as("script should be the single ALTER statement").startsWith("ALTER TABLE member");
        jdbc.execute(sql.endsWith(";") ? sql.substring(0, sql.length() - 1) : sql);
    }

    private Member saveMember(Union union, MemberLanguage language) {
        return memberRepository.saveAndFlush(Member.builder()
                .firstName("Test").lastName("Member").email(UUID.randomUUID() + "@example.com")
                .phoneNumber("+32470000000").address("Straat 1").memberStatus(MemberStatus.ACTIVE)
                .language(language).union(union).build());
    }

    @Test
    void migrationMakesTheLegacyNullableColumnNotNull_andKeepsExistingLanguages() throws Exception {
        Union union = unionRepository.save(Union.builder().name("Legacy " + UUID.randomUUID()).address("x").build());
        try {
            Member tr = saveMember(union, MemberLanguage.TR);
            Member en = saveMember(union, MemberLanguage.EN);
            jdbc.execute(NULLABLE_COLUMN);
            assertThat(memberTableDdl()).containsIgnoringCase("`language` enum('EN','NL','TR') DEFAULT NULL");

            runScript();
            runScript(); // a no-op on databases that already have NOT NULL (local dev, V1-built)

            assertThat(memberTableDdl()).containsIgnoringCase("`language` enum('EN','NL','TR') NOT NULL");
            assertThat(memberRepository.findById(tr.getId()).orElseThrow().getLanguage()).isEqualTo(MemberLanguage.TR);
            assertThat(memberRepository.findById(en.getId()).orElseThrow().getLanguage()).isEqualTo(MemberLanguage.EN);
            assertThatThrownBy(() -> jdbc.update("UPDATE member SET language = NULL WHERE id = ?", tr.getId()))
                    .isInstanceOf(DataAccessException.class);
        } finally {
            cleanUp(union);
        }
    }

    @Test
    void migrationFailsInsteadOfGuessingWhenALanguageIsNull() {
        Union union = unionRepository.save(Union.builder().name("Legacy " + UUID.randomUUID()).address("x").build());
        try {
            Member legacy = saveMember(union, MemberLanguage.NL);
            jdbc.execute(NULLABLE_COLUMN);
            jdbc.update("UPDATE member SET language = NULL WHERE id = ?", legacy.getId());

            assertThatThrownBy(this::runScript).isInstanceOf(DataAccessException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM member WHERE language IS NULL", Integer.class))
                    .as("the NULL row is left for a human decision, not silently converted").isEqualTo(1);
        } finally {
            cleanUp(union);
        }
    }

    // Leave the shared database in the same shape a fresh build creates.
    private void cleanUp(Union union) {
        jdbc.update("DELETE FROM member WHERE union_id = (SELECT id FROM unions WHERE name = ?)", union.getName());
        jdbc.execute("ALTER TABLE member MODIFY COLUMN language ENUM('EN','NL','TR') NOT NULL");
        unionRepository.delete(union);
    }
}
