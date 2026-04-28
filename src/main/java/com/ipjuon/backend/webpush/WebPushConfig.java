package com.ipjuon.backend.webpush;

import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.security.Security;

@Configuration
@EnableAsync
public class WebPushConfig {

    private static final Logger log = LoggerFactory.getLogger(WebPushConfig.class);

    @Value("${webpush.enabled:true}")
    private boolean enabled;

    @Value("${webpush.subject:mailto:admin@ipjuon.com}")
    private String subject;

    @Value("${webpush.public-key:}")
    private String publicKey;

    @Value("${webpush.private-key:}")
    private String privateKey;

    @Bean
    public WebPushSettings webPushSettings() {
        return new WebPushSettings(enabled, subject, publicKey, privateKey);
    }

    @Bean
    public PushService pushService() throws Exception {
        // BouncyCastle provider 등록 (web-push 라이브러리 요구)
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (!enabled || publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.warn("[WebPush] VAPID 키 미설정 → 푸시 발송 비활성화. WEBPUSH_PUBLIC_KEY / WEBPUSH_PRIVATE_KEY 환경변수 또는 application.yml 에 설정하세요.");
            // PushService 인스턴스는 만들되 키 없는 상태로 — 호출 시 WebPushService 가 enabled 체크
            return new PushService();
        }

        PushService ps = new PushService();
        ps.setSubject(subject);
        ps.setPublicKey(publicKey);
        ps.setPrivateKey(privateKey);
        log.info("[WebPush] VAPID 설정 완료 — subject={}", subject);
        return ps;
    }

    public record WebPushSettings(boolean enabled, String subject, String publicKey, String privateKey) {
        public boolean isReady() {
            return enabled && publicKey != null && !publicKey.isBlank() && privateKey != null && !privateKey.isBlank();
        }
    }
}
