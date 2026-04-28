package com.ipjuon.backend.webpush;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 입주민 앱 Web Push 구독 등록/해제.
 */
@RestController
@RequestMapping("/api/b2c/push-subscriptions")
@CrossOrigin(origins = "*")
public class PushSubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(PushSubscriptionController.class);

    private final PushSubscriptionRepository repo;
    private final WebPushConfig.WebPushSettings settings;

    public PushSubscriptionController(PushSubscriptionRepository repo,
                                      WebPushConfig.WebPushSettings settings) {
        this.repo = repo;
        this.settings = settings;
    }

    /** VAPID 공개키 — 앱이 구독 등록 전에 조회 */
    @GetMapping("/public-key")
    public Map<String, Object> publicKey() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("public_key", settings.publicKey());
        m.put("enabled", settings.isReady());
        return m;
    }

    /** 구독 등록 — 같은 endpoint 면 phone 만 갱신 */
    @PostMapping
    @Transactional
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpServletRequest req) {
        String phone = normalizePhone(body.get("phone"));
        String endpoint = body.get("endpoint");
        String p256dh = body.get("p256dh");
        String auth = body.get("auth");

        if (phone == null || endpoint == null || p256dh == null || auth == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone, endpoint, p256dh, auth 필수"));
        }

        PushSubscription sub = repo.findByEndpoint(endpoint).orElseGet(PushSubscription::new);
        sub.setPhone(phone);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        sub.setUserAgent(req.getHeader("User-Agent"));
        if (sub.getCreatedAt() == null) sub.setCreatedAt(OffsetDateTime.now());
        repo.save(sub);

        log.info("[WebPush] 구독 등록 phone={} endpoint={}", phone,
                endpoint.length() > 50 ? endpoint.substring(0, 50) + "..." : endpoint);

        return ResponseEntity.ok(Map.of("ok", true, "id", sub.getId()));
    }

    /** 구독 해제 (앱에서 알림 끄기 또는 권한 회수 시) */
    @DeleteMapping
    @Transactional
    public ResponseEntity<?> unregister(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "endpoint 필수"));
        }
        long deleted = repo.deleteByEndpoint(endpoint);
        return ResponseEntity.ok(Map.of("ok", true, "deleted", deleted));
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }
}
