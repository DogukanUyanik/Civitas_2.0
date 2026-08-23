package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberLanguage;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.service.MemberService;
import org.example.civitaswebapp.service.MyUserService;
import org.example.civitaswebapp.validator.MemberEmailValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Regression guard for the member edit round-trip.
 *
 * <p>The form binds the JPA entity directly, so before the language select existed every save
 * rebuilt a fresh {@code Member} whose language fell back to the field default — silently reverting
 * a Turkish- or English-speaking member to Dutch on any unrelated edit, and with it the WhatsApp
 * template they receive. The selected value must reach the service intact.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberControllerLanguageBindingTest {

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
        ReflectionTestUtils.setField(controller, "messageSource", messageSource());
        when(myUserService.getLoggedInUser()).thenReturn(new MyUser());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static ResourceBundleMessageSource messageSource() {
        // The controller resolves a flash message after saving, so it needs the real bundle.
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    private Member captureSavedMember() {
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberService).saveMember(captor.capture(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        return captor.getValue();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder memberForm() {
        return post("/members")
                .param("firstName", "Ayse")
                .param("lastName", "Yilmaz")
                .param("email", "ayse@example.com")
                .param("phoneNumber", "+32470123456")
                .param("address", "Kerkstraat 1")
                .param("memberStatus", "ACTIVE");
    }

    @Test
    void saveMember_carriesTheSelectedLanguageThrough() throws Exception {
        mockMvc.perform(memberForm().param("language", "TR"));

        assertThat(captureSavedMember().getLanguage()).isEqualTo(MemberLanguage.TR);
    }

    @Test
    void saveMember_carriesEnglishThrough() throws Exception {
        mockMvc.perform(memberForm().param("language", "EN"));

        assertThat(captureSavedMember().getLanguage()).isEqualTo(MemberLanguage.EN);
    }

    @Test
    void saveMember_defaultsToDutch_whenTheFieldIsAbsent() throws Exception {
        // Belt and braces: an older cached form without the select must not produce a null language.
        mockMvc.perform(memberForm());

        assertThat(captureSavedMember().getLanguage()).isEqualTo(MemberLanguage.NL);
    }
}
