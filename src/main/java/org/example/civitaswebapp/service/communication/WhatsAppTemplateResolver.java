package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.MemberLanguage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves the Twilio Content API ContentSid for a template identifier (e.g. "payment-link") and a
 * member's language. Each (template, language) pair is its own approved WhatsApp template with its
 * own ContentSid - Twilio does not auto-select the language variant for us.
 */
@Component
public class WhatsAppTemplateResolver {

    private final Map<String, Map<String, String>> contentSids;

    public WhatsAppTemplateResolver(Map<String, Map<String, String>> twilioContentSids) {
        this.contentSids = twilioContentSids;
    }

    public String resolve(String templateKey, MemberLanguage language) {
        Map<String, String> byLanguage = contentSids.get(templateKey);
        if (byLanguage == null) {
            throw new IllegalStateException("No Twilio content SIDs configured for template '" + templateKey + "'");
        }

        String sid = byLanguage.get(language.name().toLowerCase());
        if (sid == null || sid.isBlank()) {
            throw new IllegalStateException(
                    "No Twilio content SID configured for template '" + templateKey + "' and language '" + language + "'");
        }
        return sid;
    }
}
