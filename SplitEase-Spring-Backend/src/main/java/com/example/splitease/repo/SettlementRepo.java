package com.example.splitease.repo;

import com.example.splitease.models.Groups;
import com.example.splitease.models.Settlements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepo extends JpaRepository<Settlements, UUID> {
}
