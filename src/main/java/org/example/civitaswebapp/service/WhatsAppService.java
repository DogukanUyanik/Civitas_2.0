package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Event;

public interface WhatsAppService {
    void sendPaymentLink(String toNumber, String stripeLink);
    public void sendEventNotification(String toNumber, Event event);

    }
