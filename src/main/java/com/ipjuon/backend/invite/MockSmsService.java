package com.ipjuon.backend.invite;

import org.springframework.stereotype.Service;

@Service
public class MockSmsService implements SmsService {

    @Override
    public SendResult send(String phone, String message) {
        System.out.println("[MockSms] to=" + phone + " msg=" + message);
        return SendResult.ok("SMS");
    }
}
