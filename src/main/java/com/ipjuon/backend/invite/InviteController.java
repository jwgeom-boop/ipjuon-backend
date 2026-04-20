package com.ipjuon.backend.invite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

        String message = buildMessage(complex);
        SmsService.SendResult result = smsService.send(phone, message);

        Invite invite = new Invite();
        invite.setComplexName(complex);
        invite.setPhone(phone);
        invite.setStatus(result.success ? "SUCCESS" : "FAILED");
        invite.setMethod(result.method);
        invite.setErrorMessage(result.errorMessage);
        invite.setSentBy(sentBy);
        repository.save(invite);

        return Map.of(
            "success", result.success,
            "method", result.method,
            "error", result.errorMessage == null ? "" : result.errorMessage
        );
    }

    @GetMapping
    public List<Invite> list() {
        return repository.findAllOrdered();
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("\\D", "");
    }

    private String buildMessage(String complex) {
        return "[입주ON] " + complex + " 잔금대출 안내 앱입니다. " + b2cAppUrl;
    }

    public static class SendRequest {
        public String phone;
        public String complexName;
        public String sentBy;
    }
}
