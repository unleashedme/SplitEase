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

    @Query("SELECT SUM(s.shareAmount) FROM ExpenseSplits s " +
            "WHERE s.user.id = :userId " +
            "AND s.expense.payer.id = :payerId " +
            "AND s.expense.group.id = :groupId " +
            "AND s.settled = false")
    BigDecimal findTotalOwedByTo(@Param("userId") UUID userId,
                                 @Param("payerId") UUID payerId,
                                 @Param("groupId") UUID groupId);


    List<ExpenseSplits> findByUserAndExpense_PayerAndExpense_GroupAndSettledFalse(
            Users user, Users payer, Groups group);

    @Query("SELECT COUNT(es) > 0 FROM ExpenseSplits es WHERE es.expense.group.id = :groupId AND es.settled = false")
    boolean existsUnsettledSplits(@Param("groupId") UUID groupId);

    // Navigates from ExpenseSplits -> Expenses -> Group
    boolean existsByExpenseGroupAndSettledFalse(Groups group);

    // Navigates from ExpenseSplits -> Expenses -> Group AND ExpenseSplits -> User
    List<ExpenseSplits> findByExpenseGroupAndUserEmail(Groups group, String email);
}
