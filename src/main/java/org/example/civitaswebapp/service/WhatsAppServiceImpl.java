package org.example.civitaswebapp.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.example.civitaswebapp.domain.Event;
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
        Message message = Message.creator(
                new PhoneNumber("whatsapp:" + toNumber),
                new PhoneNumber(fromNumber),
                "Hi! Here’s your payment link: " + stripeLink
        ).create();

        System.out.println("WhatsApp message sent with SID: " + message.getSid());
    }

    @Override
    public void sendEventNotification(String toNumber, Event event) {
        String messageText = "📅 New Event: " + event.getTitle() + "\n" +
                "Type: " + event.getEventType() + "\n" +
                "Start: " + event.getStart() + "\n" +
                "End: " + event.getEnd() + "\n" +
                "Location: " + event.getLocation() + "\n" +
                (event.getDescription() != null ? "Description: " + event.getDescription() : "");

        Message message = Message.creator(
                new PhoneNumber("whatsapp:" + toNumber),
                new PhoneNumber(fromNumber),
                messageText
        ).create();

        System.out.println("WhatsApp sent to " + toNumber + " with SID: " + message.getSid());
    }

}
