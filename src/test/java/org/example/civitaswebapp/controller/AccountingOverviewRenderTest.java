package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.Invoice;
import org.example.civitaswebapp.service.accounting.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Renders the real {@code accounting/overview.html} to catch Thymeleaf syntax errors, in
 * particular the {@code data-label} attributes added to the income/expense tables for the
 * mobile card-view fallback (see {@code accounting.css}'s 768px breakpoint).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountingOverviewRenderTest {

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceController controller;

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
    void rendersIncomeAndExpenseTablesWithDataLabels() throws Exception {
        Invoice income = Invoice.builder()
                .id(1L)
                .invoiceDate(LocalDate.of(2026, 1, 15))
                .counterparty("Sponsor A")
                .category("Sponsorship")
                .totalAmount(new BigDecimal("100.00"))
                .fileUrl("invoice1.pdf")
                .build();
        Invoice expense = Invoice.builder()
                .id(2L)
                .invoiceDate(LocalDate.of(2026, 1, 20))
                .counterparty("Hosting Co")
                .category("Infra")
                .totalAmount(new BigDecimal("50.00"))
                .fileUrl("invoice2.pdf")
                .build();
        when(invoiceService.getIncomes()).thenReturn(List.of(income));
        when(invoiceService.getExpenses()).thenReturn(List.of(expense));

        String html = mockMvc.perform(get("/accounting")
                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
                .andReturn().getResponse().getContentAsString();

        // Each table cell must carry the data-label the mobile card-view CSS reads via ::before.
        assertThat(html).contains("data-label=\"Date\"");
        assertThat(html).contains("data-label=\"Counterparty\"");
        assertThat(html).contains("data-label=\"Category\"");
        assertThat(html).contains("data-label=\"Amount\"");
        assertThat(html).contains("data-label=\"Action\"");
        assertThat(html).contains("Sponsor A");
        assertThat(html).contains("Hosting Co");
        assertThat(html).contains("data-table-wrapper");
    }
}
