package com.ipjuon.backend.invite;

public interface SmsService {
    SendResult send(String phone, String message);

    class SendResult {
        public final boolean success;
        public final String method;
        public final String errorMessage;

        public SendResult(boolean success, String method, String errorMessage) {
            this.success = success;
            this.method = method;
            this.errorMessage = errorMessage;
        }

        public static SendResult ok(String method) {
            return new SendResult(true, method, null);
        }

        public static SendResult fail(String method, String errorMessage) {
            return new SendResult(false, method, errorMessage);
        }
    }
}
