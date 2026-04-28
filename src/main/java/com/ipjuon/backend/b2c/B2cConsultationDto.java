package com.ipjuon.backend.b2c;

import com.ipjuon.backend.consultation.ConsultationRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * B2C 입주민 앱용 상담건 응답 DTO.
 * 민감 필드(주민번호·공동명의자 정보·assignee_vendor_id 등)는 절대 포함하지 않음.
 * 입주민이 자기 진행 상황을 확인하는 데 필요한 정보만 노출.
 */
public class B2cConsultationDto {

    /**
     * 백엔드 loan_status (apply/consulting/reviewing/result/executing/done/cancel) 를
     * 입주민이 이해하는 5단계로 매핑.
     */
    public static String mapStage(String loanStatus) {
        if (loanStatus == null || loanStatus.isEmpty() || "apply".equals(loanStatus)) return "apply";
        if ("consulting".equals(loanStatus) || "reviewing".equals(loanStatus)) return "consulting";
        if ("result".equals(loanStatus)) return "result";
        if ("executing".equals(loanStatus)) return "executing";
        if ("done".equals(loanStatus)) return "done";
        if ("cancel".equals(loanStatus) || "cancel_requested".equals(loanStatus)) return "cancel";
        return "apply";
    }

    public static String stageLabel(String stage) {
        switch (stage) {
            case "apply": return "신청 접수";
            case "consulting": return "상담·심사 중";
            case "result": return "가심사 결과";
            case "executing": return "자서·실행 진행";
            case "done": return "완료";
            case "cancel": return "취소";
            default: return "신청 접수";
        }
    }

    /** 목록용 (가벼운 카드 표시) */
    public static Map<String, Object> toListItem(ConsultationRequest r) {
        String stage = mapStage(r.getLoan_status());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("bank_name", r.getVendor_name());
        m.put("stage", stage);
        m.put("stage_label", stageLabel(stage));
        m.put("loan_status_raw", r.getLoan_status());

        // 가심사 결과 단계 이상이면 핵심 숫자 노출
        if ("result".equals(stage) || "executing".equals(stage) || "done".equals(stage)) {
            m.put("approved_amount", r.getApproved_amount());
            m.put("approved_rate", r.getApproved_rate());
        }

        // 자서·실행 일정
        m.put("signing_date", r.getSigning_date());
        m.put("execution_date", r.getExecution_date());

        // 담당자 (있으면)
        m.put("bank_branch", r.getBank_branch());
        m.put("manager_name", r.getManager());

        m.put("stage_changed_at", r.getStage_changed_at());
        m.put("created_at", r.getCreatedAt());

        if ("cancel".equals(stage)) {
            m.put("canceled_reason", r.getCanceled_reason());
        }
        return m;
    }

    /** 상세용 (전체 노출 가능 필드) */
    public static Map<String, Object> toDetail(ConsultationRequest r) {
        String stage = mapStage(r.getLoan_status());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("bank_name", r.getVendor_name());
        m.put("complex_name", r.getComplex_name());
        m.put("dong", r.getDong());
        m.put("ho", r.getHo());
        m.put("apt_type", r.getApt_type());

        m.put("stage", stage);
        m.put("stage_label", stageLabel(stage));
        m.put("loan_status_raw", r.getLoan_status());
        m.put("stage_changed_at", r.getStage_changed_at());
        m.put("created_at", r.getCreatedAt());

        // 가심사 결과
        m.put("approved_amount", r.getApproved_amount());
        m.put("approved_rate", r.getApproved_rate());
        m.put("approved_notified_at", r.getApproved_notified_at());
        m.put("customer_accepted_at", r.getCustomer_accepted_at());

        // 자서·실행 일정
        m.put("signing_date", r.getSigning_date());
        m.put("signing_time", r.getSigning_time());
        m.put("execution_date", r.getExecution_date());
        m.put("loan_amount", r.getLoan_amount());
        m.put("loan_period", r.getLoan_period());
        m.put("repayment_method", r.getRepayment_method());
        m.put("product", r.getProduct());

        // 담당자 정보
        m.put("bank_branch", r.getBank_branch());
        m.put("manager_name", r.getManager());
        m.put("bank_manager_phone", r.getBank_manager_phone());

        // 정산 (실행 단계부터)
        if ("executing".equals(stage) || "done".equals(stage)) {
            Map<String, Object> settle = new LinkedHashMap<>();
            settle.put("middle_principal", r.getSettle_middle_principal());
            settle.put("middle_interest", r.getSettle_middle_interest());
            settle.put("balance_principal", r.getSettle_balance_principal());
            settle.put("balance_interest", r.getSettle_balance_interest());
            settle.put("balcony", r.getSettle_balcony());
            settle.put("options", r.getSettle_options());
            settle.put("guarantee_fee", r.getSettle_guarantee_fee());
            settle.put("mgmt_fee", r.getSettle_mgmt_fee());
            settle.put("moving_allowance", r.getSettle_moving_allowance());
            settle.put("stamp_duty", r.getSettle_stamp_duty());
            m.put("settlement", settle);
            m.put("execution_completed", r.getExecution_completed());
        }

        // 입주일 (D-day 표시용)
        m.put("moving_in_date", r.getMoving_in_date());

        if ("cancel".equals(stage)) {
            m.put("canceled_reason", r.getCanceled_reason());
        }

        return m;
    }
}
