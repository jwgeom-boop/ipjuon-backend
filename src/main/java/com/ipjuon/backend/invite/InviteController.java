package com.ipjuon.backend.invite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invite")
@CrossOrigin(origins = "*")
public class InviteController {

    private final InviteRepository repository;
    private final SmsService smsService;

    @Value("${invite.b2c-app-url:https://ipjuon-app.vercel.app}")
    private String b2cAppUrl;

    public InviteController(InviteRepository repository, SmsService smsService) {
        this.repository = repository;
        this.smsService = smsService;
    }

    @PostMapping
    public Map<String, Object> send(@RequestBody SendRequest req) {
        String phone = normalizePhone(req.phone);
        String complex = req.complexName == null ? "" : req.complexName;
        String sentBy = req.sentBy == null ? "" : req.sentBy;

        Invite invite = new Invite();
        invite.setComplexName(complex);
        invite.setPhone(phone);
        invite.setStatus("PENDING");
        invite.setSentBy(sentBy);
        invite = repository.save(invite);

        String url = buildUrl(complex, invite.getId());
        String message = buildMessage(complex, url);
        SmsService.SendResult result = smsService.send(phone, message);

        invite.setStatus(result.success ? "SUCCESS" : "FAILED");
        invite.setMethod(result.method);
        invite.setErrorMessage(result.errorMessage);
        repository.save(invite);

        return Map.of(
            "success", result.success,
            "method", result.method,
            "inviteId", invite.getId(),
            "error", result.errorMessage == null ? "" : result.errorMessage
        );
    }

    @GetMapping
    public List<Invite> list() {
        return repository.findAllOrdered();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable UUID id) {
        return repository.findById(id)
            .map(i -> Map.<String, Object>of(
                "id", i.getId().toString(),
                "complexName", i.getComplexName() == null ? "" : i.getComplexName(),
                "phone", i.getPhone() == null ? "" : i.getPhone()
            ))
            .orElseGet(Map::of);
    }

    @PatchMapping("/{id}/track")
    public Map<String, Object> track(@PathVariable UUID id, @RequestBody TrackRequest req) {
        return repository.findById(id)
            .map(invite -> {
                String event = req.event == null ? "" : req.event.toLowerCase();
                OffsetDateTime now = OffsetDateTime.now();
                boolean updated = false;
                if ("opened".equals(event) && invite.getOpenedAt() == null) {
                    invite.setOpenedAt(now);
                    updated = true;
                } else if ("registered".equals(event)) {
                    if (invite.getOpenedAt() == null) invite.setOpenedAt(now);
                    if (invite.getRegisteredAt() == null) {
                        invite.setRegisteredAt(now);
                        updated = true;
                    }
                }
                if (updated) repository.save(invite);
                return Map.<String, Object>of(
                    "id", invite.getId().toString(),
                    "event", event,
                    "updated", updated
                );
            })
            .orElseGet(() -> Map.<String, Object>of("updated", false, "error", "not_found"));
    }

    public static class TrackRequest {
        public String event;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("\\D", "");
    }

    private String buildUrl(String complex, UUID inviteId) {
        String encoded = URLEncoder.encode(complex == null ? "" : complex, StandardCharsets.UTF_8);
        return b2cAppUrl + "?complex=" + encoded + "&invite=" + inviteId;
    }

    private String buildMessage(String complex, String url) {
        return "[입주ON] " + complex + " 잔금대출 안내 앱입니다. " + url;
    }

    public static class SendRequest {
        public String phone;
        public String complexName;
        public String sentBy;
    }
}
