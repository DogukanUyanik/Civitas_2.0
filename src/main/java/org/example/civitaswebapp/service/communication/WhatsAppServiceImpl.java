package org.example.civitaswebapp.service.communication;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.example.civitaswebapp.dto.events.EventMessageDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        com.twilio.Twilio.init(accountSid, authToken);
    }

    @Override
    public void sendPaymentLink(String toNumber, String stripeLink) {
        validatePhoneNumber(toNumber);
        Message message = Message.creator(
                new PhoneNumber("whatsapp:" + toNumber),
                new PhoneNumber(fromNumber),
                "Hi! Here’s your payment link: " + stripeLink
        ).create();

        System.out.println("WhatsApp message sent with SID: " + message.getSid());
    }

    @Override
    public void sendEventNotification(String toNumber, EventMessageDetails event) {
        String messageText = "📅 New Event: " + event.title() + "\n" +
                "Type: " + event.eventType() + "\n" +
                "Start: " + event.start() + "\n" +
                "End: " + event.end() + "\n" +
                "Location: " + event.location() + "\n" +
                (event.description() != null ? "Description: " + event.description() : "");

        Message message = Message.creator(
                new PhoneNumber("whatsapp:" + toNumber),
                new PhoneNumber(fromNumber),
                messageText
        ).create();

        System.out.println("WhatsApp sent to " + toNumber + " with SID: " + message.getSid());
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
