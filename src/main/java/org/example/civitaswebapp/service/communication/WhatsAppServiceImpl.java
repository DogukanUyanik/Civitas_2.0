package org.example.civitaswebapp.service.communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberLanguage;
import org.example.civitaswebapp.dto.events.EventMessageDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private static final String STRIPE_CHECKOUT_BASE_URL = "https://checkout.stripe.com/";

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-number}")
    private String fromNumber;

    @Autowired
    private EventMessageFormatter eventMessageFormatter;

    @Autowired
    private WhatsAppTemplateResolver templateResolver;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        com.twilio.Twilio.init(accountSid, authToken);
    }

    @Override
    public void sendPaymentLink(Member member, String stripeCheckoutUrl) {
        validatePhoneNumber(member.getPhoneNumber());
        String urlRemainder = stripeCheckoutUrl.startsWith(STRIPE_CHECKOUT_BASE_URL)
                ? stripeCheckoutUrl.substring(STRIPE_CHECKOUT_BASE_URL.length())
                : stripeCheckoutUrl;
        String contentSid = templateResolver.resolve("payment-link", member.getLanguage());
        sendTemplate(member.getPhoneNumber(), contentSid, List.of(member.getName(), urlRemainder));
    }

    @Override
    public void sendPaymentSuccess(Member member) {
        validatePhoneNumber(member.getPhoneNumber());
        String contentSid = templateResolver.resolve("payment-success", member.getLanguage());
        sendTemplate(member.getPhoneNumber(), contentSid, List.of(member.getName()));
    }

    @Override
    public void sendPaymentFailed(Member member) {
        validatePhoneNumber(member.getPhoneNumber());
        String contentSid = templateResolver.resolve("payment-failed", member.getLanguage());
        sendTemplate(member.getPhoneNumber(), contentSid, List.of(member.getName()));
    }

    @Override
    public void sendEventPlanned(String toNumber, String memberName, MemberLanguage language, EventMessageDetails event) {
        validatePhoneNumber(toNumber);
        String dateAndExtras = eventMessageFormatter.formatDateAndExtras(event, language);
        String contentSid = templateResolver.resolve("event-planned", language);
        sendTemplate(toNumber, contentSid, List.of(memberName, event.title(), dateAndExtras));
    }

    /**
     * Sends a Twilio Content API template message. Variables are 1-indexed per Twilio's convention
     * ({{1}}, {{2}}, ...). Phone validation happens in the calling method, before the ContentSid is
     * resolved, so a bad number fails fast without needing template config.
     */
    /**
     * Sends a Twilio Content API template message. Variables are 1-indexed per Twilio's convention
     * ({{1}}, {{2}}, ...). Phone validation happens in the calling method, before the ContentSid is
     * resolved, so a bad number fails fast without needing template config.
     */
    private void sendTemplate(String toNumber, String contentSid, List<String> variables) {
        try {
            Message message;

            // Als het een echte, goedgekeurde Twilio code is (begint met HX, en is geen 'test' of 'x' placeholder)
            if (contentSid.startsWith("HX") && !contentSid.contains("x") && !contentSid.contains("test")) {

                Map<String, String> variableMap = new LinkedHashMap<>();
                for (int i = 0; i < variables.size(); i++) {
                    variableMap.put(String.valueOf(i + 1), variables.get(i));
                }

                message = Message.creator(
                                new PhoneNumber("whatsapp:" + toNumber),
                                new PhoneNumber(fromNumber),
                                ""
                        ).setContentSid(contentSid)
                        .setContentVariables(objectMapper.writeValueAsString(variableMap))
                        .create();

            } else {
                // LOKAAL / SANDBOX MODUS: Twilio accepteert lokaal geen nep-templates.
                // We vallen terug op een standaard tekstbericht.
                // Let op: Dit werkt in de Sandbox alleen als je een 24-uurs sessie hebt geopend!
                String fallbackText = "🔔 [Sandbox Testbericht]\nActie uitgevoerd voor: " + variables.get(0) + "\nDetails: " + String.join(" | ", variables);

                message = Message.creator(
                        new PhoneNumber("whatsapp:" + toNumber),
                        new PhoneNumber(fromNumber),
                        fallbackText
                ).create();
            }

            System.out.println("WhatsApp bericht verstuurd naar " + toNumber + " met SID: " + message.getSid());

        } catch (Exception e) {
            throw new IllegalStateException("Failed to send WhatsApp message: " + e.getMessage(), e);
        }
    }

    /**
     * Guards against unroutable numbers before we ever hit Twilio. WhatsApp/Twilio require an
     * E.164 international number (e.g. {@code +32470123456}); a local format such as
     * {@code 0470123456} is rejected by Twilio at runtime. Failing fast here turns that into a
     * deterministic, catchable error that the controller can surface to the UI.
     *
     * @throws IllegalArgumentException if the number is missing or not in international format
     */
    private void validatePhoneNumber(String toNumber) {
        if (toNumber == null || toNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is missing.");
        }
        if (!toNumber.trim().startsWith("+")) {
            throw new IllegalArgumentException(
                    "Invalid phone number format: '" + toNumber
                            + "'. Use international format, e.g. +32470123456.");
        }
    }

}
