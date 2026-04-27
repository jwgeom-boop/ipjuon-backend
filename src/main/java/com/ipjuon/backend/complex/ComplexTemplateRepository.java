package com.ipjuon.backend.complex;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplexTemplateRepository extends JpaRepository<ComplexTemplate, UUID> {

    Optional<ComplexTemplate> findByComplex_name(String complexName);

    List<ComplexTemplate> findAllByOrderByComplex_nameAsc();
}
