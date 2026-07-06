package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberLanguage;
import org.example.civitaswebapp.dto.events.EventMessageDetails;

public interface WhatsAppService {
    void sendPaymentLink(Member member, String stripeCheckoutUrl);

    void sendPaymentSuccess(Member member);

    void sendPaymentFailed(Member member);

    void sendEventPlanned(String toNumber, String memberName, MemberLanguage language, EventMessageDetails event);
}
