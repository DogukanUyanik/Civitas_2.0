package org.example.civitaswebapp.security;

import org.example.civitaswebapp.controller.DashboardRestController;
import org.example.civitaswebapp.controller.EventRestController;
import org.example.civitaswebapp.controller.InvoiceRestController;
import org.example.civitaswebapp.controller.MemberController;
import org.example.civitaswebapp.controller.MemberRestController;
import org.example.civitaswebapp.controller.NotificationController;
import org.example.civitaswebapp.controller.NotificationRestController;
import org.example.civitaswebapp.controller.TransactionRestController;
import org.example.civitaswebapp.controller.UserAdminController;
import org.example.civitaswebapp.domain.Invoice;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MyUserRole;
import org.example.civitaswebapp.dto.user.CreateUserForm;
import org.example.civitaswebapp.service.EventService;
import org.example.civitaswebapp.service.MemberService;
import org.example.civitaswebapp.service.MyUserService;
import org.example.civitaswebapp.service.TransactionService;
import org.example.civitaswebapp.service.accounting.InvoiceService;
import org.example.civitaswebapp.service.accounting.PdfService;
import org.example.civitaswebapp.service.communication.NotificationMessageResolver;
import org.example.civitaswebapp.service.communication.NotificationService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.example.civitaswebapp.service.kpi.DashboardService;
import org.example.civitaswebapp.validator.MemberEmailValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Guards the ADMIN (write) / VIEWER (read-only) split. Role enforcement is native Spring Security
 * ({@code @PreAuthorize} + {@code @EnableMethodSecurity}) on the controller methods, so this boots
 * a minimal Spring context with method security enabled and calls the real (proxied) controllers.
 * Business services are mocks: for a VIEWER none of them may ever be touched.
 *
 * <p>It also pins the deliberate exemptions (a VIEWER's own notification read-flags and dashboard
 * layout) so nobody "fixes" them into ADMIN-only later.
 */
@SpringJUnitConfig(RoleEnforcementTest.Config.class)
class RoleEnforcementTest {

    @Configuration
    @EnableMethodSecurity
    @Import({MemberController.class, MemberRestController.class, EventRestController.class,
            InvoiceRestController.class, TransactionRestController.class, UserAdminController.class,
            NotificationController.class, NotificationRestController.class, DashboardRestController.class})
    static class Config {
        @Bean
        MessageSource messageSource() {
            return Mockito.mock(MessageSource.class, inv -> "getMessage".equals(inv.getMethod().getName()) ? "msg" : null);
        }
    }

    @MockitoBean MemberService memberService;
    @MockitoBean MemberEmailValidator memberEmailValidator;
    @MockitoBean MyUserService myUserService;
    @MockitoBean PdfService pdfService;
    @MockitoBean EventService eventService;
    @MockitoBean InvoiceService invoiceService;
    @MockitoBean TransactionService transactionService;
    @MockitoBean WhatsAppService whatsAppService;
    @MockitoBean NotificationService notificationService;
    @MockitoBean NotificationMessageResolver notificationMessageResolver;
    @MockitoBean DashboardService dashboardService;

    @Autowired MemberController memberController;
    @Autowired MemberRestController memberRestController;
    @Autowired EventRestController eventRestController;
    @Autowired InvoiceRestController invoiceRestController;
    @Autowired TransactionRestController transactionRestController;
    @Autowired UserAdminController userAdminController;
    @Autowired NotificationController notificationController;
    @Autowired NotificationRestController notificationRestController;
    @Autowired DashboardRestController dashboardRestController;

    /** Every endpoint that mutates shared union data, plus the write-form pages. */
    private Map<String, Executable> adminOnly() {
        Map<String, Executable> calls = new LinkedHashMap<>();
        // members
        calls.put("GET  /members/add", () -> memberController.showAddMemberForm(new ExtendedModelMap()));
        calls.put("GET  /members/edit/{id}", () -> memberController.showEditMemberForm(1L, new ExtendedModelMap()));
        calls.put("POST /members", () -> {
            Member member = new Member();
            memberController.saveMember(member, new BeanPropertyBindingResult(member, "member"),
                    new ExtendedModelMap(), new RedirectAttributesModelMap(), null, null);
        });
        calls.put("POST /api/members/import", () ->
                memberRestController.importMembers(new MockMultipartFile("file", new byte[0])));
        // events
        calls.put("POST /api/events", () -> eventRestController.createEvent(null));
        // accounting
        calls.put("POST /api/invoices/scan", () ->
                invoiceRestController.scanInvoice(new MockMultipartFile("file", new byte[0])));
        calls.put("DELETE /api/invoices/{id}", () -> invoiceRestController.deleteInvoice(1L));
        calls.put("POST /api/invoices/confirm", () -> invoiceRestController.confirmInvoice(new Invoice()));
        // transactions
        calls.put("POST /transactions/send-payment", () ->
                transactionRestController.sendPayment(1L, 10.0, "EUR", "DONATION", null));
        calls.put("POST /transactions/{id}/mark-cash", () -> transactionRestController.markAsCash(1L));
        // user & role management (class-level @PreAuthorize)
        calls.put("GET  /settings/users", () -> userAdminController.showUsers(new ExtendedModelMap()));
        calls.put("POST /settings/users", () -> {
            CreateUserForm form = new CreateUserForm();
            userAdminController.createUser(form, new BeanPropertyBindingResult(form, "createUserForm"),
                    new ExtendedModelMap(), new RedirectAttributesModelMap());
        });
        calls.put("POST /settings/users/{id}/role", () ->
                userAdminController.changeRole(2L, MyUserRole.ADMIN, new RedirectAttributesModelMap()));
        return calls;
    }

    /** Endpoints a VIEWER must keep using: reads, plus mutations that only touch their own data. */
    private Map<String, Executable> openToViewers() {
        Map<String, Executable> calls = new LinkedHashMap<>();
        calls.put("GET  /members", () -> memberController.showMembersList(new ExtendedModelMap(), 0, 10, "", ""));
        calls.put("GET  /api/members", () -> memberRestController.getAllMembers());
        calls.put("GET  /api/events", () -> eventRestController.getEvents());
        calls.put("POST /notifications/mark-all-read", () -> notificationController.markAllAsRead());
        calls.put("POST /api/notifications/{id}/read", () -> notificationRestController.markAsRead(1L));
        calls.put("PUT  /api/dashboard/me/tiles", () -> dashboardRestController.saveMyDashboard(new ArrayList<>()));
        calls.put("POST /api/dashboard/me/tiles/{widgetKey}", () -> dashboardRestController.addMyTile("kpi"));
        calls.put("DELETE /api/dashboard/me/tiles/{widgetKey}", () -> dashboardRestController.removeMyTile("kpi"));
        return calls;
    }

    /** Fails only when the security layer denies the call; the mocked business code may still fail. */
    private static void assertNotDeniedBySecurity(String endpoint, Executable call) {
        try {
            call.execute();
        } catch (AccessDeniedException e) {
            fail(endpoint + " was denied by method security but must be allowed: " + e.getMessage());
        } catch (Throwable ignored) {
            // Passed the security check; anything else comes from the mocked collaborators.
        }
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewer_isDeniedOnEveryWriteEndpoint_andNoServiceIsTouched() {
        adminOnly().forEach((endpoint, call) ->
                assertThatThrownBy(call::execute)
                        .as(endpoint)
                        .isInstanceOf(AccessDeniedException.class));

        verifyNoInteractions(memberService, memberEmailValidator, myUserService, pdfService, eventService,
                invoiceService, transactionService, whatsAppService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_isNotDeniedOnAnyWriteEndpoint() {
        adminOnly().forEach(RoleEnforcementTest::assertNotDeniedBySecurity);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewer_keepsReadAccessAndOwnNotificationAndDashboardActions() {
        openToViewers().forEach(RoleEnforcementTest::assertNotDeniedBySecurity);
    }

    @Test
    @WithMockUser(roles = "SOMETHING_ELSE")
    void anyRoleOtherThanAdmin_isReadOnly_failClosed() {
        // Guards the "replace USER by VIEWER" decision: write access is opt-in via ADMIN only.
        adminOnly().forEach((endpoint, call) ->
                assertThatThrownBy(call::execute).as(endpoint).isInstanceOf(AccessDeniedException.class));
    }
}
