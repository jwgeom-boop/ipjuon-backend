package com.ipjuon.backend.bankprofile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplexBankProfileRepository extends JpaRepository<ComplexBankProfile, UUID> {

    @Query("SELECT p FROM ComplexBankProfile p WHERE p.complex_name = :complex AND p.bank_name = :bank")
    Optional<ComplexBankProfile> findByComplexAndBank(@Param("complex") String complexName,
                                                      @Param("bank") String bankName);

    @Query("SELECT p FROM ComplexBankProfile p WHERE p.bank_name = :bank ORDER BY p.complex_name ASC")
    List<ComplexBankProfile> findAllByBank(@Param("bank") String bankName);

    @Query("SELECT p FROM ComplexBankProfile p WHERE p.complex_name = :complex ORDER BY p.bank_name ASC")
    List<ComplexBankProfile> findAllByComplex(@Param("complex") String complexName);
}
