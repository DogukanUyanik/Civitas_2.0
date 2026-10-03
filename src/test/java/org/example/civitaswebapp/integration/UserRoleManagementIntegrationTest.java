package org.example.civitaswebapp.integration;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.repository.UnionRepository;
import org.example.civitaswebapp.service.MyUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check of the ADMIN/VIEWER feature through the real filter chain, real Thymeleaf
 * templates and a real MySQL: schema, 403s on write endpoints, hidden write UI, the
 * user-management screen (including cross-union isolation) and forced re-login after a role change.
 */
class UserRoleManagementIntegrationTest extends MySqlIntegrationTest {

    private static final String PASSWORD = "pw-for-tests-1";

    @Autowired MockMvc mockMvc;
    @Autowired MyUserRepository userRepository;
    @Autowired UnionRepository unionRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired BCryptPasswordEncoder passwordEncoder;
    @Autowired MyUserDetailsService userDetailsService;
    @Autowired JdbcTemplate jdbc;

    private Union unionA;
    private Union unionB;
    private MyUser adminA;
    private MyUser viewerA;
    private MyUser adminB;
    private Member memberA;
    private String tag;

    @BeforeEach
    void seedTwoUnions() {
        tag = UUID.randomUUID().toString().substring(0, 8);
        unionA = unionRepository.save(Union.builder().name("Union A " + tag).address("A street 1").build());
        unionB = unionRepository.save(Union.builder().name("Union B " + tag).address("B street 1").build());
        adminA = saveUser("adminA-" + tag, MyUserRole.ADMIN, unionA);
        viewerA = saveUser("viewerA-" + tag, MyUserRole.VIEWER, unionA);
        adminB = saveUser("adminB-" + tag, MyUserRole.ADMIN, unionB);
        memberA = memberRepository.save(Member.builder()
                .firstName("Mia").lastName("Member").email("mia-" + tag + "@example.com")
                .phoneNumber("+32470000000").address("Somewhere 1").dateOfBirth(LocalDate.of(1990, 1, 1))
                .dateOfLastPayment(LocalDate.now()).memberStatus(MemberStatus.ACTIVE).union(unionA).build());
    }

    @AfterEach
    void cleanUp() {
        memberRepository.delete(memberA);
        for (Union union : List.of(unionA, unionB)) {
            userRepository.deleteAll(userRepository.findAllByUnionOrderByUsernameAsc(union));
            unionRepository.delete(union);
        }
    }

    private MyUser saveUser(String username, MyUserRole role, Union union) {
        return userRepository.save(MyUser.builder()
                .username(username).password(passwordEncoder.encode(PASSWORD)).role(role).union(union).build());
    }

    /** Authenticates a request as the given user with the same principal type a real login produces. */
    private RequestPostProcessor as(MyUser u) {
        return user(userDetailsService.loadUserByUsername(u.getUsername()));
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .param("username", username).param("password", PASSWORD).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    // ---- schema -----------------------------------------------------------------------------

    @Test
    void roleColumnIsANativeMysqlEnumOfAdminAndViewer() {
        String ddl = (String) jdbc.queryForMap("SHOW CREATE TABLE users").get("Create Table");

        // Confirms why the migration script is needed (ddl-auto=update won't alter this column later).
        assertThat(ddl).containsIgnoringCase("enum('ADMIN','VIEWER')");
    }

    // ---- enforcement through the full filter chain -----------------------------------------

    @Test
    void viewerGets403OnWriteEndpoints_evenWithAValidCsrfToken() throws Exception {
        mockMvc.perform(post("/transactions/1/mark-cash").with(as(viewerA)).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/transactions/send-payment").with(as(viewerA)).with(csrf())
                        .param("memberId", "1").param("amount", "10").param("currency", "EUR")
                        .param("paymentType", "DONATION"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/events").with(as(viewerA)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/invoices/confirm").with(as(viewerA)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/invoices/1").with(as(viewerA)).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/invoices/scan")
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1}))
                        .with(as(viewerA)).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/members/import")
                        .file(new MockMultipartFile("file", "a.xlsx", "application/octet-stream", new byte[]{1}))
                        .with(as(viewerA)).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/members").with(as(viewerA)).with(csrf()).param("firstName", "X"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/members/add").with(as(viewerA))).andExpect(status().isForbidden());
        mockMvc.perform(get("/members/edit/" + memberA.getId()).with(as(viewerA))).andExpect(status().isForbidden());
    }

    @Test
    void adminIsNotBlockedByRoleCheck() throws Exception {
        mockMvc.perform(post("/transactions/999999/mark-cash").with(as(adminA)).with(csrf()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
        mockMvc.perform(get("/members/add").with(as(adminA))).andExpect(status().isOk());
        mockMvc.perform(get("/members/edit/" + memberA.getId()).with(as(adminA))).andExpect(status().isOk());
    }

    // ---- write UI is hidden from viewers, role label is correct ----------------------------

    @Test
    void membersPage_hidesWriteControlsFromViewer_andShowsThemToAdmin() throws Exception {
        mockMvc.perform(get("/members").with(as(viewerA)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("add-member-btn"))))
                .andExpect(content().string(not(containsString("bulkImportInput"))))
                .andExpect(content().string(not(containsString("/members/edit/"))))
                .andExpect(content().string(not(containsString("href=\"/settings/users\""))))
                .andExpect(content().string(containsString("<span class=\"user-role\">Viewer</span>")));

        mockMvc.perform(get("/members").with(as(adminA)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("add-member-btn")))
                .andExpect(content().string(containsString("bulkImportInput")))
                .andExpect(content().string(containsString("/members/edit/")))
                .andExpect(content().string(containsString("href=\"/settings/users\"")))
                .andExpect(content().string(containsString("<span class=\"user-role\">Admin</span>")));
    }

    @Test
    void memberDetailsPage_hidesSendPaymentFromViewer() throws Exception {
        String url = "/members/view/" + memberA.getId();
        mockMvc.perform(get(url).with(as(viewerA)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"sendPaymentBtn\""))))
                .andExpect(content().string(not(containsString("id=\"paymentModal\""))));
        mockMvc.perform(get(url).with(as(adminA)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"sendPaymentBtn\"")))
                .andExpect(content().string(containsString("id=\"paymentModal\"")));
    }

    @Test
    void eventsPage_hidesCreateEventModalFromViewer() throws Exception {
        mockMvc.perform(get("/events").with(as(viewerA)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<div class=\"modal fade\" id=\"eventModal\""))))
                .andExpect(content().string(not(containsString("add-event-btn\" data-bs-toggle"))));
        mockMvc.perform(get("/events").with(as(adminA)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<div class=\"modal fade\" id=\"eventModal\"")));
    }

    @Test
    void accountingPage_hidesUploadAndReviewModalFromViewer() throws Exception {
        mockMvc.perform(get("/accounting").with(as(viewerA)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"fileUpload\""))))
                .andExpect(content().string(not(containsString("id=\"reviewModal\""))));
        mockMvc.perform(get("/accounting").with(as(adminA)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"fileUpload\"")))
                .andExpect(content().string(containsString("id=\"reviewModal\"")));
    }

    // ---- user management screen -------------------------------------------------------------

    @Test
    void usersScreen_isAdminOnly_andListsOnlyTheCallersUnion() throws Exception {
        mockMvc.perform(get("/settings/users").with(as(viewerA))).andExpect(status().isForbidden());

        mockMvc.perform(get("/settings/users").with(as(adminA)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(adminA.getUsername())))
                .andExpect(content().string(containsString(viewerA.getUsername())))
                .andExpect(content().string(not(containsString(adminB.getUsername()))));
    }

    @Test
    void admin_canPromoteAViewer() throws Exception {
        mockMvc.perform(post("/settings/users/{id}/role", viewerA.getId()).param("role", "ADMIN")
                        .with(as(adminA)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/users"))
                .andExpect(flash().attribute("success", containsString("is now Admin")));

        assertThat(userRepository.findById(viewerA.getId()).orElseThrow().getRole()).isEqualTo(MyUserRole.ADMIN);
    }

    @Test
    void admin_cannotChangeARoleInAnotherUnion_andGetsNotFound() throws Exception {
        mockMvc.perform(post("/settings/users/{id}/role", adminB.getId()).param("role", "VIEWER")
                        .with(as(adminA)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "User not found."));

        assertThat(userRepository.findById(adminB.getId()).orElseThrow().getRole()).isEqualTo(MyUserRole.ADMIN);
    }

    @Test
    void lastAdminCannotBeDemoted() throws Exception {
        mockMvc.perform(post("/settings/users/{id}/role", adminA.getId()).param("role", "VIEWER")
                        .with(as(adminA)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "The last admin of the union cannot be demoted."));

        assertThat(userRepository.findById(adminA.getId()).orElseThrow().getRole()).isEqualTo(MyUserRole.ADMIN);
    }

    @Test
    void roleChangeWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/settings/users/{id}/role", viewerA.getId()).param("role", "ADMIN").with(as(adminA)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(viewerA.getId()).orElseThrow().getRole()).isEqualTo(MyUserRole.VIEWER);
    }

    @Test
    void admin_canAddAUser_stampedWithTheirOwnUnion_andThatUserCanLogIn() throws Exception {
        String username = "newbie-" + tag;

        mockMvc.perform(post("/settings/users").with(as(adminA)).with(csrf())
                        .param("username", username).param("password", "Temp-pass-1").param("role", "VIEWER")
                        // Tampering attempt: the union must never come from the request.
                        .param("union", unionB.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/users"));

        MyUser created = userRepository.findByUsername(username);
        assertThat(created.getRole()).isEqualTo(MyUserRole.VIEWER);
        assertThat(created.getUnion().getId()).isEqualTo(unionA.getId());
        assertThat(created.getPassword()).isNotEqualTo("Temp-pass-1");
        assertThat(passwordEncoder.matches("Temp-pass-1", created.getPassword())).isTrue();

        mockMvc.perform(post("/login").param("username", username).param("password", "Temp-pass-1").with(csrf()))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void addUser_rejectsDuplicateUsername_andInvalidInput_withLocalizedMessages() throws Exception {
        mockMvc.perform(post("/settings/users").with(as(adminA)).with(csrf())
                        .param("username", viewerA.getUsername()).param("password", "Temp-pass-1").param("role", "VIEWER"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("This username is already taken.")));

        // Short password: the {users.form.password.size} message must be resolved from the bundle,
        // and the submitted password must never be echoed back into the page.
        mockMvc.perform(post("/settings/users").with(as(adminA)).with(csrf())
                        .param("username", "someone-" + tag).param("password", "short").param("role", "VIEWER"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Password must be between 8 and 72 characters")))
                .andExpect(content().string(not(containsString("{users.form"))))
                .andExpect(content().string(not(containsString("value=\"short\""))));

        assertThat(userRepository.findByUsername("someone-" + tag)).isNull();
    }

    // ---- stale sessions ---------------------------------------------------------------------

    @Test
    void changingARole_forcesTheAffectedUserToLogInAgain_soNewRightsApplyImmediately() throws Exception {
        MockHttpSession viewerSession = login(viewerA.getUsername());
        mockMvc.perform(get("/settings/users").session(viewerSession)).andExpect(status().isForbidden());

        mockMvc.perform(post("/settings/users/{id}/role", viewerA.getId()).param("role", "ADMIN")
                        .with(as(adminA)).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // The old session (with the old VIEWER authority baked in) is expired ...
        mockMvc.perform(get("/settings/users").session(viewerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        // ... and a fresh login picks up the new role.
        MockHttpSession fresh = login(viewerA.getUsername());
        mockMvc.perform(get("/settings/users").session(fresh)).andExpect(status().isOk());
    }

    @Test
    void demotedAdminLosesWriteAccessOnTheNextRequest() throws Exception {
        MyUser secondAdmin = saveUser("second-" + tag, MyUserRole.ADMIN, unionA);
        MockHttpSession adminSession = login(secondAdmin.getUsername());
        mockMvc.perform(get("/members/add").session(adminSession)).andExpect(status().isOk());

        mockMvc.perform(post("/settings/users/{id}/role", secondAdmin.getId()).param("role", "VIEWER")
                        .with(as(adminA)).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/members/add").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));
    }

    @Test
    void loginPage_explainsWhyTheSessionEnded() throws Exception {
        mockMvc.perform(get("/login").param("expired", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("your role or account was changed")));
    }
}
