package com.ipjuon.backend.complex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplexTemplateAptFeeRepository extends JpaRepository<ComplexTemplateAptFee, UUID> {

    @Query("SELECT f FROM ComplexTemplateAptFee f WHERE f.template_id = :tid ORDER BY f.display_order ASC, f.apt_type ASC")
    List<ComplexTemplateAptFee> findByTemplateId(@Param("tid") UUID templateId);

    @Query("SELECT f FROM ComplexTemplateAptFee f WHERE f.template_id = :tid AND f.apt_type = :aptType")
    Optional<ComplexTemplateAptFee> findByTemplateIdAndAptType(@Param("tid") UUID templateId,
                                                                @Param("aptType") String aptType);

    @Modifying
    @Query("DELETE FROM ComplexTemplateAptFee f WHERE f.template_id = :tid")
    void deleteByTemplateId(@Param("tid") UUID templateId);
}
