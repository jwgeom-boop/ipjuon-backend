package com.ipjuon.backend.complex;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * 단지×평형 별 관리비 예치금 금액.
 * 입주안내문 예: 45평 307,000원 / 51평 346,000원 / 59A 377,000원 ...
 */
@Entity
@Table(
    name = "complex_template_apt_fees",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_template_apt",
        columnNames = {"template_id", "apt_type"}
    )
)
public class ComplexTemplateAptFee {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID template_id;            // ComplexTemplate.id (FK)

    @Column(name = "apt_type", nullable = false, length = 20)
    private String apt_type;             // "45", "51", "59A", "59B", "74A" ...

    @Column(name = "mgmt_fee_amount", nullable = false)
    private Long mgmt_fee_amount;        // 307000, 346000 ...

    @Column(name = "display_order")
    private Integer display_order;       // UI 정렬용

    public UUID getId() { return id; }

    @JsonProperty("template_id")
    public UUID getTemplate_id() { return template_id; }
    public void setTemplate_id(UUID v) { this.template_id = v; }

    public String getApt_type() { return apt_type; }
    public void setApt_type(String v) { this.apt_type = v; }

    public Long getMgmt_fee_amount() { return mgmt_fee_amount; }
    public void setMgmt_fee_amount(Long v) { this.mgmt_fee_amount = v; }

    public Integer getDisplay_order() { return display_order; }
    public void setDisplay_order(Integer v) { this.display_order = v; }
}
