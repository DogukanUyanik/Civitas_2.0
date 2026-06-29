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

}
