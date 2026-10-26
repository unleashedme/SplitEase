package com.example.splitease.repo;

import com.example.splitease.models.Expenses;
import com.example.splitease.models.Groups;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepo extends JpaRepository<Expenses, UUID> {

    @Query("SELECT DISTINCT e FROM Expenses e " +
            "JOIN FETCH e.group g " +
            "JOIN FETCH e.payer p " +
            "LEFT JOIN FETCH e.splits " +
            "WHERE g.id IN (SELECT gm.group.id FROM GroupMembers gm WHERE gm.user.email = :email) " +
            // EXISTS rather than filtering the fetched splits, which would load only the user's own split
            "AND (p.email = :email OR EXISTS (SELECT 1 FROM ExpenseSplits s WHERE s.expense = e AND s.user.email = :email)) " +
            "ORDER BY e.createdAt DESC")
    List<Expenses> findAllActivityForUser(@Param("email") String email);

    List<Expenses> findByGroup(Groups group);

    List<Expenses> findByGroupIn(List<Groups> groups);
}
