package com.ipjuon.backend.b2c;

import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.consultation.ConsultationRequest;
import com.ipjuon.backend.webpush.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * D-Day 자동 리마인더 푸시 서비스.
 *
 * 매일 오전 9시(KST) 에 실행:
 *  - 자서일 D-3, D-1 푸시
 *  - 실행일 D-3, D-1 푸시
 *
 * 중복 방지: signing_d3_reminder_for / signing_d1_reminder_for 등 컬럼에
 *           이미 발송한 자서일/실행일을 기록 → 일정 변경 시 자동 재발송.
 */
@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ConsultationRepository repo;
    private final WebPushService webPush;

    public ReminderService(ConsultationRepository repo, WebPushService webPush) {
        this.repo = repo;
        this.webPush = webPush;
    }

    /** 매일 09:00 KST 실행 */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void runDailyReminders() {
        sendDailyReminders();
    }

    /** 수동 트리거용 (관리자 호출 가능) — 통계 반환 */
    public java.util.Map<String, Integer> sendDailyReminders() {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(kst);
        LocalDate d3 = today.plusDays(3);
        LocalDate d1 = today.plusDays(1);

        int signingD3 = sendSigningReminders(d3, "D-3");
        int signingD1 = sendSigningReminders(d1, "D-1");
        int execD3 = sendExecutionReminders(d3, "D-3");
        int execD1 = sendExecutionReminders(d1, "D-1");

        log.info("[리마인더] 자서 D-3:{} D-1:{} / 실행 D-3:{} D-1:{}",
                signingD3, signingD1, execD3, execD1);
        return java.util.Map.of(
                "signing_d3", signingD3,
                "signing_d1", signingD1,
                "execution_d3", execD3,
                "execution_d1", execD1
        );
    }

    private int sendSigningReminders(LocalDate target, String dDay) {
        List<ConsultationRequest> list = repo.findActiveBySigningDate(target);
        int sent = 0;
        for (ConsultationRequest c : list) {
            if (c.getResident_phone() == null) continue;
            // 이미 같은 자서일에 대해 발송했으면 skip
            LocalDate alreadyFor = "D-3".equals(dDay) ? c.getSigning_d3_reminder_for() : c.getSigning_d1_reminder_for();
            if (target.equals(alreadyFor)) continue;

            String bank = c.getVendor_name() != null ? c.getVendor_name() : "은행";
            String time = c.getSigning_time() != null ? c.getSigning_time() : "";
            String location = c.getSigning_location() != null ? c.getSigning_location() : "";
            String title;
            String body;
            if ("D-1".equals(dDay)) {
                title = "🚨 " + bank + " 자서 내일 (D-1)";
                body = (target + " " + time + " " + location).trim() + " — 신분증·도장·서류 챙기세요";
            } else {
                title = "📅 " + bank + " 자서 " + dDay;
                body = (target + " " + time + " " + location).trim() + " — 준비서류 30개 확인";
            }
            webPush.sendToPhone(c.getResident_phone(), title, body, "/my/consultations/" + c.getId());

            if ("D-3".equals(dDay)) c.setSigning_d3_reminder_for(target);
            else c.setSigning_d1_reminder_for(target);
            repo.save(c);
            sent++;
        }
        return sent;
    }

    private int sendExecutionReminders(LocalDate target, String dDay) {
        List<ConsultationRequest> list = repo.findActiveByExecutionDate(target);
        int sent = 0;
        for (ConsultationRequest c : list) {
            if (c.getResident_phone() == null) continue;
            LocalDate alreadyFor = "D-3".equals(dDay) ? c.getExecution_d3_reminder_for() : c.getExecution_d1_reminder_for();
            if (target.equals(alreadyFor)) continue;

            String bank = c.getVendor_name() != null ? c.getVendor_name() : "은행";
            String title;
            String body;
            if ("D-1".equals(dDay)) {
                title = "🚨 " + bank + " 대출 실행 내일 (D-1)";
                body = target + " 실행일 — 중도금이자 확인 잊지 마세요";
            } else {
                title = "💼 " + bank + " 실행일 " + dDay;
                body = target + " 실행 예정 — 정산 내역·송금 계좌 확인";
            }
            webPush.sendToPhone(c.getResident_phone(), title, body, "/my/consultations/" + c.getId());

            if ("D-3".equals(dDay)) c.setExecution_d3_reminder_for(target);
            else c.setExecution_d1_reminder_for(target);
            repo.save(c);
            sent++;
        }
        return sent;
    }
}
