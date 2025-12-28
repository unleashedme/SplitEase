package com.example.splitease.repo;

import com.example.splitease.models.ExpenseSplits;
import com.example.splitease.models.Groups;
import com.example.splitease.models.Users;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT s FROM ExpenseSplits s WHERE s.user.email = :email")
    List<ExpenseSplits> findAllByUserEmail(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM ExpenseSplits s WHERE s.expense.id = :expenseId")
    void deleteByExpenseId(@Param("expenseId") UUID expenseId);


    @Query("SELECT " +
            "CASE WHEN e.payer.id = :userId THEN s.user.id ELSE e.payer.id END, " +
            "CASE WHEN e.payer.id = :userId THEN s.user.name ELSE e.payer.name END, " +
            "SUM(CASE WHEN s.user.id = :userId THEN s.shareAmount ELSE 0 END) - " +
            "SUM(CASE WHEN e.payer.id = :userId THEN s.shareAmount ELSE 0 END) " +
            "FROM ExpenseSplits s JOIN s.expense e " +
            "WHERE (e.payer.id = :userId OR s.user.id = :userId) " +
            "AND s.settled = false " +
            "GROUP BY 1, 2")
    List<Object[]> calculateNetBalances(@Param("userId") UUID userId);

    @Query("SELECT s FROM ExpenseSplits s WHERE s.settled = false AND (" +
            "(s.user.id = :id1 AND s.expense.payer.id = :id2) OR " +
            "(s.user.id = :id2 AND s.expense.payer.id = :id1)" +
            ")")
    List<ExpenseSplits> findAllUnsettledBetweenUsers(UUID id1, UUID id2);
}
