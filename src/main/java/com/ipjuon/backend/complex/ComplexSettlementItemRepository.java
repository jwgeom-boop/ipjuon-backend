package com.ipjuon.backend.complex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ComplexSettlementItemRepository extends JpaRepository<ComplexSettlementItem, UUID> {

    @Query("SELECT s FROM ComplexSettlementItem s WHERE s.template_id = :tid ORDER BY s.display_order ASC, s.category ASC")
    List<ComplexSettlementItem> findByTemplateId(@Param("tid") UUID templateId);

    @Modifying
    @Query("DELETE FROM ComplexSettlementItem s WHERE s.template_id = :tid")
    void deleteByTemplateId(@Param("tid") UUID templateId);
}
