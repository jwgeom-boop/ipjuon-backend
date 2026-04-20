package com.ipjuon.backend.invite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, UUID> {
    @Query("SELECT i FROM Invite i ORDER BY i.sentAt DESC")
    List<Invite> findAllOrdered();
}
