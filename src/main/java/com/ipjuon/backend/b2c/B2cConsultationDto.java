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

    /** 주민번호 마스킹: 820103-1****** */
    private static String maskRrn(String rrn) {
        if (rrn == null) return null;
        String digits = rrn.replaceAll("\\D", "");
        if (digits.length() < 7) return rrn; // 형식 이상하면 그대로
        String front = digits.substring(0, 6);
        String genderDigit = digits.length() >= 7 ? digits.substring(6, 7) : "";
        return front + "-" + genderDigit + "******";
    }

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
        m.put("signing_location", r.getSigning_location());
        m.put("execution_date", r.getExecution_date());

        // [v2] 자서 일정 캘린더 워크플로
        m.put("signing_window_start", r.getSigning_window_start());
        m.put("signing_window_end", r.getSigning_window_end());
        m.put("signing_excluded_dates", r.getSigning_excluded_dates());
        m.put("signing_available_times", r.getSigning_available_times());
        m.put("signing_available_locations", r.getSigning_available_locations());
        m.put("signing_selected_date", r.getSigning_selected_date());
        m.put("signing_selected_time", r.getSigning_selected_time());
        m.put("signing_selected_location_str", r.getSigning_selected_location_str());
        m.put("signing_selected_at", r.getSigning_selected_at());
        m.put("signing_confirmed_at", r.getSigning_confirmed_at());
        // Legacy
        m.put("signing_offered_slots", r.getSigning_offered_slots());
        m.put("signing_selected_slot_index", r.getSigning_selected_slot_index());
        m.put("loan_amount", r.getLoan_amount());
        m.put("loan_period", r.getLoan_period());
        m.put("repayment_method", r.getRepayment_method());
        m.put("product", r.getProduct());

        // 담당자 정보
        m.put("bank_branch", r.getBank_branch());
        m.put("manager_name", r.getManager());
        m.put("bank_manager_phone", r.getBank_manager_phone());

        // 정산 (실행 단계부터) — 송금 정보 표 형식으로 노출
        if ("executing".equals(stage) || "done".equals(stage)) {
            Map<String, Object> settle = new LinkedHashMap<>();
            // 중도금 (해당은행 계좌)
            settle.put("middle_principal", r.getSettle_middle_principal());
            settle.put("middle_interest", r.getSettle_middle_interest());
            settle.put("middle_bank", r.getSettle_middle_bank());
            settle.put("middle_account", r.getSettle_middle_account());
            // 입주민 보고 — 중도금이자
            settle.put("reported_middle_interest", r.getReported_middle_interest());
            settle.put("reported_middle_interest_at", r.getReported_middle_interest_at());

            // 분양잔금 (시행사 계좌 — settle_balance_account)
            settle.put("balance_principal", r.getSettle_balance_principal());
            settle.put("balance_interest", r.getSettle_balance_interest());
            settle.put("balance_account", r.getSettle_balance_account());

            // 발코니/옵션/보증수수료 (시행사 계좌 공유)
            settle.put("balcony", r.getSettle_balcony());
            settle.put("options", r.getSettle_options());
            settle.put("guarantee_fee", r.getSettle_guarantee_fee());

            // 선수관리비 (관리사무소 계좌)
            settle.put("mgmt_fee", r.getSettle_mgmt_fee());
            settle.put("mgmt_account", r.getSettle_mgmt_account());

            // 이주비 (이주비 은행 계좌)
            settle.put("moving_allowance", r.getSettle_moving_allowance());
            settle.put("moving_bank", r.getSettle_moving_bank());
            settle.put("moving_account", r.getSettle_moving_account());

            // 인지대 (정액)
            settle.put("stamp_duty", r.getSettle_stamp_duty());
            settle.put("stamp_duty_additional", r.getSettle_stamp_duty_additional());

            m.put("settlement", settle);
            m.put("execution_completed", r.getExecution_completed());
            m.put("additional_loan_amount", r.getAdditional_loan_amount());
            m.put("bank_manager_fax", r.getBank_manager_fax());
        }

        // 입주민 준비서류 체크리스트
        m.put("resident_doc_checks", r.getResident_doc_checks());
        m.put("resident_doc_checks_at", r.getResident_doc_checks_at());
        m.put("resident_last_action_at", r.getResident_last_action_at());
        m.put("resident_last_action_type", r.getResident_last_action_type());
        m.put("b2c_messages", r.getB2c_messages());

        // 입주일 (D-day 표시용)
        m.put("moving_in_date", r.getMoving_in_date());

        // 대출신청서 정보 (입주민이 입력한 가심사 정보)
        m.put("contractor", r.getContractor());
        // 주민번호 마스킹 (820103-1****)
        String rno = r.getResident_no();
        m.put("resident_no_masked", maskRrn(rno));
        m.put("joint_owner_name", r.getJoint_owner_name());
        m.put("joint_owner_rrn_masked", maskRrn(r.getJoint_owner_rrn()));
        m.put("joint_owner_tel", r.getJoint_owner_tel());
        m.put("desired_loan", r.getDesired_loan());
        m.put("loan_period", r.getLoan_period());
        m.put("deferment", r.getDeferment());
        m.put("annual_income_y1", r.getAnnual_income_y1());
        m.put("annual_income_y2", r.getAnnual_income_y2());
        m.put("existing_credit_loan", r.getExisting_credit_loan());
        m.put("existing_collateral_loan", r.getExisting_collateral_loan());
        m.put("notes", r.getNotes());
        m.put("sale_price_amount", r.getSale_price_amount());
        m.put("desired_date", r.getDesired_date());
        m.put("existing_homes", r.getExisting_homes());
        m.put("loan_application_at", r.getLoan_application_at());

        if ("cancel".equals(stage)) {
            m.put("canceled_reason", r.getCanceled_reason());
        }

        return m;
    }
}
