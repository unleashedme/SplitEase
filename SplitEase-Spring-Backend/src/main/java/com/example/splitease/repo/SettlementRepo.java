package com.example.splitease.repo;

import com.example.splitease.models.Groups;
import com.example.splitease.models.Settlements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepo extends JpaRepository<Settlements, UUID> {
    List<Settlements> findByGroup(Groups group);
}
