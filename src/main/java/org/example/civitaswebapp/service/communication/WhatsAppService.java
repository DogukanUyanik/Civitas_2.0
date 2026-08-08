package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberLanguage;
import org.example.civitaswebapp.dto.events.EventMessageDetails;

/**
 * Sends WhatsApp messages via Twilio's Content API.
 *
 * <p>Every method on this interface reports failure through one of four exception types, and callers
 * are expected to distinguish them so the UI can name the actual cause instead of always blaming the
 * phone number. This is a load-bearing contract - do not collapse it behind a single wrapper type:
 *
 * <ul>
 *   <li>{@link IllegalArgumentException} - the number is missing or not in E.164 format. Member data
 *       problem, fixable by an admin.</li>
 *   <li>{@link com.twilio.exception.ApiException} - Twilio accepted the request but rejected the
 *       message (unapproved template, outside the 24h session window, bad credentials). Carries
 *       Twilio's own error code.</li>
 *   <li>{@link com.twilio.exception.ApiConnectionException} - Twilio was unreachable. Both are
 *       {@link com.twilio.exception.TwilioException}s, so catching the supertype covers external
 *       failures as a class.</li>
 *   <li>{@link IllegalStateException} - a ContentSid is missing from configuration, or template
 *       variables could not be serialized. Deployment/ops problem, not member data.</li>
 * </ul>
 *
 * <p>A null {@code language} is never a failure: it falls back to {@link MemberLanguage#DEFAULT}.
 */
public interface WhatsAppService {
    void sendPaymentLink(Member member, String stripeCheckoutUrl);

    void sendPaymentSuccess(Member member);

    void sendPaymentFailed(Member member);

    void sendEventPlanned(String toNumber, String memberName, MemberLanguage language, EventMessageDetails event);
}
