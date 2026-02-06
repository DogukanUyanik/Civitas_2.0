package org.example.civitaswebapp;

import org.example.civitaswebapp.service.*;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.example.civitaswebapp.service.communication.WhatsAppServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

@EnableScheduling
@EnableAsync
@SpringBootApplication
@EntityScan("org.example.civitaswebapp.domain")
@EnableJpaRepositories("org.example.civitaswebapp.repository")
public class CivitasWebAppApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(CivitasWebAppApplication.class, args);
    }

    @Bean
    LocaleResolver localeResolver() {
        CookieLocaleResolver slr = new CookieLocaleResolver();
        slr.setDefaultLocale(Locale.ENGLISH);
        return slr;
    }


    //om taal te kunnen veranderen
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        registry.addInterceptor(localeChangeInterceptor);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/members");
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    MyUserDetailsService myUserDetailsService() {
        return new MyUserDetailsService();
    }

    @Bean
    MemberService memberService() {
        return new MemberServiceImpl();
    }

    @Bean
    TransactionService  transactionService() {
        return new TransactionServiceImpl();
    }

    @Bean
    EventService eventService() {
        return new EventServiceImpl();
    }

    @Bean
    WhatsAppService whatsAppService() {
        return new WhatsAppServiceImpl();
    }

}
