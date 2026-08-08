package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.MemberLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves the Twilio Content API ContentSid for a template identifier (e.g. "payment-link") and a
 * member's language. Each (template, language) pair is its own approved WhatsApp template with its
 * own ContentSid - Twilio does not auto-select the language variant for us.
 */
@Component
public class WhatsAppTemplateResolver {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppTemplateResolver.class);

    private final Map<String, Map<String, String>> contentSids;

    public WhatsAppTemplateResolver(Map<String, Map<String, String>> twilioContentSids) {
        this.contentSids = twilioContentSids;
    }

    /**
     * A missing language falls back to {@link MemberLanguage#DEFAULT} rather than failing the send -
     * rows written before the language column existed can still hold NULL. A missing ContentSid is
     * still an {@link IllegalStateException}: that is a deployment problem, not member data.
     */
    public String resolve(String templateKey, MemberLanguage language) {
        MemberLanguage effective = MemberLanguage.orDefault(language);
        if (language == null) {
            log.warn("Member has no language set; falling back to {} for template '{}'",
                    MemberLanguage.DEFAULT, templateKey);
        }

        Map<String, String> byLanguage = contentSids.get(templateKey);
        if (byLanguage == null) {
            throw new IllegalStateException("No Twilio content SIDs configured for template '" + templateKey + "'");
        }

        String sid = byLanguage.get(effective.name().toLowerCase());
        if (sid == null || sid.isBlank()) {
            throw new IllegalStateException(
                    "No Twilio content SID configured for template '" + templateKey + "' and language '" + effective + "'");
        }
        return sid;
    }
}
