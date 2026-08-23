package org.example.civitaswebapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Renders the real {@code payment-success.html} / {@code payment-cancel.html} templates so a
 * broken Thymeleaf expression surfaces here, not only in the browser. {@link PaymentPageController}
 * has no dependencies (deliberately no DB lookup, see its javadoc), so a standalone MockMvc setup
 * is enough. Reachability without authentication is covered separately, since this standalone
 * setup bypasses Spring Security entirely.
 */
class PaymentPageControllerTest {

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

        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentPageController())
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void paymentSuccessRendersSuccessView() throws Exception {
        mockMvc.perform(get("/payment-success").param("transactionId", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-success"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment received")));
    }

    @Test
    void paymentSuccessWorksWithoutTransactionIdParam() throws Exception {
        mockMvc.perform(get("/payment-success"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-success"));
    }

    @Test
    void paymentCancelRendersCancelView() throws Exception {
        mockMvc.perform(get("/payment-cancel"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-cancel"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment cancelled")));
    }
}
