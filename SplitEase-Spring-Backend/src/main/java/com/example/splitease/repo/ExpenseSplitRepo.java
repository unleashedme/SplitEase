package com.example.splitease.repo;

import com.example.splitease.models.ExpenseSplits;
import com.example.splitease.models.Groups;
import com.example.splitease.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseSplitRepo extends JpaRepository<ExpenseSplits, UUID> {
    @Query("""
        SELECT es
        FROM ExpenseSplits es
        WHERE es.expense.group.id = :groupId
    """)
    List<ExpenseSplits> findByGroupId(@Param("groupId") UUID groupId);

    @Query("""
    SELECT es
    FROM ExpenseSplits es
    WHERE es.user.id = :fromUserId
      AND es.expense.payer.id = :toUserId
      AND es.expense.group.id = :groupId
      AND es.settled = false
    ORDER BY es.expense.createdAt ASC
""")
    List<ExpenseSplits> findUnsettledSplits(
            UUID fromUserId,
            UUID toUserId,
            UUID groupId
    );

    List<ExpenseSplits> findAllBySettledFalse();

    // Logic for Calculation: Only use splits where settled is false
    @Query("SELECT SUM(s.shareAmount) FROM ExpenseSplits s " +
            "WHERE s.user.id = :userId " +
            "AND s.expense.payer.id = :payerId " +
            "AND s.expense.group.id = :groupId " +
            "AND s.settled = false")
    BigDecimal findTotalOwedByTo(@Param("userId") UUID userId,
                                 @Param("payerId") UUID payerId,
                                 @Param("groupId") UUID groupId);

    // Logic for Updating: Find the records to mark them true
    List<ExpenseSplits> findByUserAndExpense_PayerAndExpense_GroupAndSettledFalse(
            Users user, Users payer, Groups group);
}
