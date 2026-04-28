package com.ipjuon.backend.webpush;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {
    List<PushSubscription> findByPhone(String phone);
    Optional<PushSubscription> findByEndpoint(String endpoint);
    long deleteByEndpoint(String endpoint);
}
