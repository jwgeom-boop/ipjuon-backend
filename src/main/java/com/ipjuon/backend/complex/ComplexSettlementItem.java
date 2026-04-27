package com.ipjuon.backend.complex;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * 단지 정산 항목별 계좌 정보 (1행 = 1항목).
 * 입주ON 상환조회 화면(은행 사이트)에 들어갈 항목별 표:
 *   구분(중도금/분양잔금/...) / 해당은행 / 계좌번호 / 비고
 *
 * 단지당 N행 (관리자가 자유롭게 추가/삭제). 시드로 기본 10개 항목 자동 생성.
 */
@Entity
@Table(name = "complex_settlement_items")
public class ComplexSettlementItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID template_id;       // ComplexTemplate.id (FK)

    @Column(nullable = false, length = 50)
    private String category;        // "중도금", "분양잔금", "발코니 확장", "선수관리비" 등

    private String bank;            // "국민은행", "농협", "현금/수입인지" 등 (선택)
    private String account;         // "101437-04-002570", "수표상환", "—" 등 (선택)
    private String note;            // "원금", "시행사 입금", "별매품1" 등 (선택)

    @Column(name = "display_order")
    private Integer display_order;  // UI 정렬 순서

    public UUID getId() { return id; }

    @JsonProperty("template_id")
    public UUID getTemplate_id() { return template_id; }
    public void setTemplate_id(UUID v) { this.template_id = v; }

    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getBank() { return bank; }
    public void setBank(String v) { this.bank = v; }
    public String getAccount() { return account; }
    public void setAccount(String v) { this.account = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }

    public Integer getDisplay_order() { return display_order; }
    public void setDisplay_order(Integer v) { this.display_order = v; }
}
