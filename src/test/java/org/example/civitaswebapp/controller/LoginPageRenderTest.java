package org.example.civitaswebapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Renders the real {@code loginForm.html} to guard against {@code LoginController} regressing
 * to hardcoded English error/logout text. The error/logout messages come from the
 * {@code login.error.invalid} / {@code login.logout.success} i18n keys, resolved via
 * {@code #{${errorKey}}} in the template, so a Turkish or Dutch user must see the message in
 * their own locale rather than English.
 */
class LoginPageRenderTest {

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

        mockMvc = MockMvcBuilders.standaloneSetup(new LoginController()).setViewResolvers(viewResolver).build();
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withCsrf(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"));
    }

    @Test
    void errorParamRendersLocalizedMessage_english() throws Exception {
        String html = mockMvc.perform(withCsrf(get("/login").param("error", "true").locale(Locale.ENGLISH)))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Invalid username or password!");
        assertThat(html).doesNotContain("Invalid username and password!"); // the old hardcoded typo/wording
    }

    @Test
    void errorParamRendersLocalizedMessage_dutch() throws Exception {
        String html = mockMvc.perform(withCsrf(get("/login").param("error", "true").locale(Locale.forLanguageTag("nl"))))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Ongeldige gebruikersnaam of wachtwoord!");
    }

    @Test
    void logoutParamRendersLocalizedMessage_turkish() throws Exception {
        String html = mockMvc.perform(withCsrf(get("/login").param("logout", "true").locale(Locale.forLanguageTag("tr"))))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Başarıyla çıkış yaptınız.");
    }

    @Test
    void noParamsRendersNeitherAlert() throws Exception {
        String html = mockMvc.perform(withCsrf(get("/login").locale(Locale.ENGLISH)))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("alert-error");
        assertThat(html).doesNotContain("alert-success");
    }
}
