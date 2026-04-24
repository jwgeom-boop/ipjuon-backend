package com.ipjuon.backend.vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    List<Vendor> findAllByOrderByCreatedAtDesc();
    Optional<Vendor> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    Optional<Vendor> findByVendorName(String vendorName);
    List<Vendor> findAllByParentVendorId(UUID parentVendorId);
    Optional<Vendor> findByVendorNameAndBankManager(String vendorName, String bankManager);
}
