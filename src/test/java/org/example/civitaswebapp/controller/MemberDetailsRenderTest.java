package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.service.MemberService;
import org.example.civitaswebapp.service.MyUserService;
import org.example.civitaswebapp.service.accounting.PdfService;
import org.example.civitaswebapp.validator.MemberEmailValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Renders the real {@code members/memberDetails.html} to catch Thymeleaf syntax errors, in
 * particular the {@code data-label} attributes added to the transactions table for the
 * mobile card-view fallback (see {@code memberDetails.css}'s 640px breakpoint).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberDetailsRenderTest {

    @Mock
    private MemberService memberService;
    @Mock
    private MemberEmailValidator memberEmailValidator;
    @Mock
    private MyUserService myUserService;
    @Mock
    private PdfService pdfService;

    @InjectMocks
    private MemberController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/messages");
        messages.setDefaultEncoding("UTF-8");

        GenericWebApplicationContext springContext = new GenericWebApplicationContext(new MockServletContext());
        springContext.refresh();

        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(springContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setTemplateEngineMessageSource(messages);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(engine);
        viewResolver.setCharacterEncoding("UTF-8");

        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();
    }

    @Test
    void rendersTransactionsTableWithDataLabels() throws Exception {
        Member member = Member.builder()
                .id(42L)
                .firstName("Ada").lastName("Lovelace")
                .email("ada@example.com")
                .memberStatus(MemberStatus.ACTIVE)
                .transactions(List.of(Transaction.builder()
                        .id(1L)
                        .amount(25.0)
                        .currency("EUR")
                        .type(TransactionType.MEMBERSHIP_FEE)
                        .status(TransactionStatus.SUCCEEDED)
                        .createdAt(LocalDateTime.of(2026, 1, 15, 10, 30))
                        .build()))
                .build();
        when(memberService.findByIdWithTransactions(42L)).thenReturn(Optional.of(member));

        String html = mockMvc.perform(get("/members/view/42")
                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
                .andReturn().getResponse().getContentAsString();

        // Each transaction cell must carry the data-label the mobile card-view CSS reads via ::before.
        assertThat(html).contains("data-label=\"Date\"");
        assertThat(html).contains("data-label=\"Type\"");
        assertThat(html).contains("data-label=\"Amount\"");
        assertThat(html).contains("data-label=\"Currency\"");
        assertThat(html).contains("data-label=\"Status\"");
        assertThat(html).contains("data-label=\"Actions\"");
        assertThat(html).contains("Ada");
    }
}
