package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberLanguage;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.service.MemberService;
import org.example.civitaswebapp.service.MyUserService;
import org.example.civitaswebapp.validator.MemberEmailValidator;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Renders the real {@code members/memberForm.html} so the language select is verified as actual
 * markup, not just as a controller contract. A broken Thymeleaf expression here would otherwise
 * only surface at runtime, in the browser.
 *
 * <p>Also covers the two fields that shared the language field's root cause: {@code dateOfBirth}
 * had no value binding at all (so editing a member wiped their date of birth), and
 * {@code subscriptionStartDate} was hard-coded to today on every render.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberFormRenderTest {

    @Mock
    private MemberService memberService;
    @Mock
    private MemberEmailValidator memberEmailValidator;
    @Mock
    private MyUserService myUserService;

    @InjectMocks
    private MemberController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/messages");
        messages.setDefaultEncoding("UTF-8");
        ReflectionTestUtils.setField(controller, "messageSource", messages);

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

    private String render(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url)).andReturn();
        return result.getResponse().getContentAsString();
    }

    @Test
    void addForm_preselectsDutch() throws Exception {
        String html = render("/members/add");

        // Labels come from the bundle (default locale here is English), proving the keys resolve.
        assertThat(html).contains("Preferred Language for Communication");
        assertThat(html).contains("<option value=\"NL\" selected=\"selected\">Dutch</option>");
        assertThat(html).contains("<option value=\"EN\">English</option>");
        assertThat(html).contains("<option value=\"TR\">Turkish</option>");
    }

    @Test
    void editForm_preselectsTheStoredLanguage() throws Exception {
        when(memberService.findById(42L)).thenReturn(Optional.of(Member.builder()
                .id(42L)
                .firstName("Ayse").lastName("Yilmaz").email("ayse@example.com")
                .phoneNumber("+32470123456").address("Kerkstraat 1")
                .memberStatus(MemberStatus.ACTIVE)
                .language(MemberLanguage.TR)
                .build()));

        String html = render("/members/edit/42");

        assertThat(html).contains("<option value=\"TR\" selected=\"selected\">Turkish</option>");
        assertThat(html).doesNotContain("<option value=\"NL\" selected=\"selected\">");
    }

    @Test
    void editForm_repopulatesDateOfBirthAndLeavesStartDateEmpty() throws Exception {
        when(memberService.findById(42L)).thenReturn(Optional.of(Member.builder()
                .id(42L)
                .firstName("Ayse").lastName("Yilmaz").email("ayse@example.com")
                .phoneNumber("+32470123456").address("Kerkstraat 1")
                .memberStatus(MemberStatus.ACTIVE)
                .dateOfBirth(LocalDate.of(1990, 4, 17))
                .build()));

        String html = render("/members/edit/42");

        // Previously blank on every edit, which silently cleared the stored value on save.
        assertThat(html).contains("value=\"17/04/1990\"");
        // Must not re-anchor an existing billing cycle to today.
        assertThat(html).contains("id=\"subscriptionStartDate\"");
        assertThat(html).doesNotContain(LocalDate.now().toString());
    }

    @Test
    void addForm_defaultsStartDateToToday() throws Exception {
        String html = render("/members/add");

        assertThat(html).contains(LocalDate.now().toString());
    }
}
