package com.ipjuon.backend.invite;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@Primary
@ConditionalOnProperty(name = "solapi.enabled", havingValue = "true")
public class SolapiSmsService implements SmsService {

    @Value("${solapi.api-key}")
    private String apiKey;

    @Value("${solapi.api-secret}")
    private String apiSecret;

    @Value("${solapi.sender}")
    private String sender;

    @Value("${solapi.api-url:https://api.solapi.com}")
    private String apiUrl;

    private DefaultMessageService messageService;

    @PostConstruct
    void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, apiUrl);
    }

    @Override
    public SendResult send(String phone, String text) {
        Message message = new Message();
        message.setFrom(sender.replaceAll("\\D", ""));
        message.setTo(phone.replaceAll("\\D", ""));
        message.setText(text);

        try {
            SingleMessageSentResponse res = messageService.sendOne(new SingleMessageSendingRequest(message));
            if (res != null && res.getStatusCode() != null && res.getStatusCode().startsWith("2")) {
                return SendResult.ok("SMS");
            }
            String err = res == null ? "no response" : (res.getStatusMessage() == null ? res.getStatusCode() : res.getStatusMessage());
            return SendResult.fail("SMS", err);
        } catch (Exception e) {
            return SendResult.fail("SMS", e.getMessage());
        }
    }
}
