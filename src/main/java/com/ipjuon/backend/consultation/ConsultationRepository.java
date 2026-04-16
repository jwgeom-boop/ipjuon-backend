package com.ipjuon.backend.consultation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<ConsultationRequest, UUID> {
}
