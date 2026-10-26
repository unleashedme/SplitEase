package com.example.splitease.repo;

import com.example.splitease.models.Groups;
import com.example.splitease.models.Settlements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepo extends JpaRepository<Settlements, UUID> {

    // Payments that still count towards the group's balance
    @Query("SELECT s FROM Settlements s WHERE s.group = :group AND (s.cleared IS NULL OR s.cleared = false)")
    List<Settlements> findOpenByGroup(@Param("group") Groups group);
}
