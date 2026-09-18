package org.example.civitaswebapp.config;

import org.example.civitaswebapp.service.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
// Role enforcement (ADMIN = write, VIEWER = read-only) is done natively with @PreAuthorize on
// controller methods. Union scoping is a separate concern and stays manual in the service layer.
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(new BCryptPasswordEncoder());
    }


    // Tracks live sessions per user so a role change can force that user to log in again
    // (see UserSessionExpirer). Needs HttpSessionEventPublisher to drop destroyed sessions.
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http.csrf(csrf -> csrf
                        // The webhook is called by Stripe; the admin endpoints are non-browser
                        // (curl/Postman) operational endpoints already locked down to ROLE_ADMIN.
                        .ignoringRequestMatchers("/stripe/webhook", "/api/admin/subscriptions/trigger-job",
                                "/api/admin/subscriptions/cleanup-duplicate-pending")
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/", "/login**", "/css/**", "/js/**", "/error", "/stripe/webhook", "/payment-success", "/payment-cancel").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true) // redirect here after successful login
                        .usernameParameter("username")
                        .passwordParameter("password")
                )
                .sessionManagement(session -> session
                        // -1 = no cap on concurrent logins; the registry exists only so sessions
                        // can be expired on demand. Expired sessions are sent back to the login page.
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl("/login?expired")
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .exceptionHandling(handling -> handling
                        .accessDeniedPage("/error?errorCode=403&errorMessage=Access+Denied")
                );

        return http.build();
    }
}
