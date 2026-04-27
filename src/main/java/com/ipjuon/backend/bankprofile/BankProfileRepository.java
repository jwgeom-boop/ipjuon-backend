package com.ipjuon.backend.bankprofile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankProfileRepository extends JpaRepository<BankProfile, UUID> {

    @Query("SELECT b FROM BankProfile b WHERE b.bank_name = :name")
    Optional<BankProfile> findByBank_name(@Param("name") String bankName);

    @Query("SELECT b FROM BankProfile b ORDER BY b.bank_name ASC")
    List<BankProfile> findAllOrdered();
}
