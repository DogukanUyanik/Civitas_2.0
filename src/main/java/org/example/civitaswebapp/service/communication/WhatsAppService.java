package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.dto.events.EventMessageDetails;

public interface WhatsAppService {
    void sendPaymentLink(String toNumber, String stripeLink);

    void sendEventNotification(String toNumber, EventMessageDetails event);
}
