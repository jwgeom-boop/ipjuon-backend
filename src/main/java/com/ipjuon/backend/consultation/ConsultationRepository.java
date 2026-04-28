package com.ipjuon.backend.consultation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<ConsultationRequest, UUID> {

    // 은행 타입 전체 조회
    @Query("SELECT r FROM ConsultationRequest r WHERE r.vendor_type IN ('은행', 'bank') ORDER BY r.createdAt DESC")
    List<ConsultationRequest> findAllBankConsultations();

    // 은행 타입 + 은행명 필터
    @Query("SELECT r FROM ConsultationRequest r WHERE r.vendor_type IN ('은행', 'bank') AND r.vendor_name = :bankName ORDER BY r.createdAt DESC")
    List<ConsultationRequest> findBankConsultationsByVendorName(@Param("bankName") String bankName);

    // 담당 상담사(assignee_vendor_id)로 필터
    @Query("SELECT r FROM ConsultationRequest r WHERE r.vendor_type IN ('은행', 'bank') AND r.assignee_vendor_id = :vendorId ORDER BY r.createdAt DESC")
    List<ConsultationRequest> findByAssigneeVendorId(@Param("vendorId") UUID vendorId);

    // 입주민 본인 상담건 조회 (B2C 앱 — 전화번호 기반)
    // 저장된 phone 의 하이픈/공백을 제거하고 숫자만 비교 (앱은 010-1234-5678 형식, 백엔드 저장은 가변)
    @Query("SELECT r FROM ConsultationRequest r WHERE REPLACE(REPLACE(REPLACE(r.resident_phone, '-', ''), ' ', ''), '+', '') = :phoneDigits ORDER BY r.createdAt DESC")
    List<ConsultationRequest> findByResidentPhone(@Param("phoneDigits") String phoneDigits);
}
