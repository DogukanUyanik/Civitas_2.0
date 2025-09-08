package org.example.civitaswebapp.service;

public interface WhatsAppService {
    void sendPaymentLink(String toNumber, String stripeLink);
}
