package com.ipjuon.backend.consent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResidentConsentRepository extends JpaRepository<ResidentConsent, UUID> {
}
