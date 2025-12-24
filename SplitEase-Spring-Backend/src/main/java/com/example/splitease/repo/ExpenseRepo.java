package com.example.splitease.repo;

import com.example.splitease.models.Expenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepo extends JpaRepository<Expenses, UUID> {

    @Query("""
        SELECT e
        FROM Expenses e
        WHERE e.group.id = :groupId
    """)
    List<Expenses> findByGroupId(@Param("groupId") UUID groupId);
}
