package com.ipjuon.backend.webpush;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Web Push 발송. VAPID 미설정 시 no-op (로깅만).
 * 발송은 별도 스레드(@Async)에서 처리해 API 응답 지연 방지.
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushService pushService;
    private final WebPushConfig.WebPushSettings settings;
    private final PushSubscriptionRepository subRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebPushService(PushService pushService,
                          WebPushConfig.WebPushSettings settings,
                          PushSubscriptionRepository subRepo) {
        this.pushService = pushService;
        this.settings = settings;
        this.subRepo = subRepo;
    }

    /** 특정 phone 으로 등록된 모든 구독자에게 발송 (한 사람이 여러 device 보유 가능) */
    @Async
    public void sendToPhone(String phone, String title, String body, String url) {
        if (!settings.isReady()) {
            log.debug("[WebPush] 비활성 — phone={} title={} (skip)", phone, title);
            return;
        }
        String normalized = phone == null ? null : phone.replaceAll("\\D", "");
        if (normalized == null || normalized.isEmpty()) return;

        List<PushSubscription> subs = subRepo.findByPhone(normalized);
        if (subs.isEmpty()) {
            log.debug("[WebPush] 구독자 없음 — phone={}", normalized);
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("body", body);
        payload.put("url", url);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("[WebPush] payload 직렬화 실패", e);
            return;
        }

        for (PushSubscription sub : subs) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        json.getBytes()
                );
                HttpResponse response = pushService.send(notification);
                int code = response.getStatusLine().getStatusCode();
                if (code == 410 || code == 404) {
                    // 구독 만료/취소 → 정리
                    subRepo.delete(sub);
                    log.info("[WebPush] 만료된 구독 제거 — endpoint={}", abbrev(sub.getEndpoint()));
                } else if (code >= 200 && code < 300) {
                    sub.setLastUsedAt(OffsetDateTime.now());
                    subRepo.save(sub);
                } else {
                    log.warn("[WebPush] 발송 실패 code={} endpoint={}", code, abbrev(sub.getEndpoint()));
                }
            } catch (Exception e) {
                log.warn("[WebPush] 발송 예외 — endpoint={} err={}", abbrev(sub.getEndpoint()), e.getMessage());
            }
        }
    }

    private static String abbrev(String s) {
        if (s == null) return "";
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }
}
