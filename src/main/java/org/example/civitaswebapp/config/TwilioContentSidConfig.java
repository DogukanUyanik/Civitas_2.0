package org.example.civitaswebapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Binds {@code twilio.content-sids.<template>.<language>=<ContentSid>} into a nested map. Each
 * WhatsApp template has one ContentSid per language (Twilio does not auto-select the language
 * variant), so the outer key is the template identifier (e.g. "payment-link") and the inner key is
 * the lowercase language code (e.g. "nl").
 */
@Configuration
public class TwilioContentSidConfig {

    @Bean
    @ConfigurationProperties(prefix = "twilio.content-sids")
    public Map<String, Map<String, String>> twilioContentSids() {
        return new HashMap<>();
    }
}
