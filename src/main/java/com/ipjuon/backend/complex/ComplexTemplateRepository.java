package com.ipjuon.backend.complex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplexTemplateRepository extends JpaRepository<ComplexTemplate, UUID> {

    // 메서드 이름의 underscore가 Spring Data JPA 에서 nested property 로 해석되므로
    // @Query 로 직접 JPQL 명시 (ConsultationRepository 와 동일 패턴).
    @Query("SELECT t FROM ComplexTemplate t WHERE t.complex_name = :name")
    Optional<ComplexTemplate> findByComplex_name(@Param("name") String complexName);

    @Query("SELECT t FROM ComplexTemplate t ORDER BY t.complex_name ASC")
    List<ComplexTemplate> findAllByOrderByComplex_nameAsc();
}
