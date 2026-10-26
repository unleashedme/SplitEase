package com.example.splitease.repo;

import com.example.splitease.models.ExpenseReceipts;
import com.example.splitease.requestAndResponse.ReceiptImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ExpenseReceiptRepo extends JpaRepository<ExpenseReceipts, UUID> {

    // Lets expense lists show which expenses have a bill without loading any image bytes
    @Query("SELECT r.expenseId FROM ExpenseReceipts r WHERE r.expenseId IN :expenseIds")
    Set<UUID> findExpenseIdsWithReceipt(@Param("expenseIds") Collection<UUID> expenseIds);

    // The two versions are read separately so showing the thumbnail doesn't load the full image
    @Query("SELECT new com.example.splitease.requestAndResponse.ReceiptImage(r.contentType, r.data, r.createdAt) " +
            "FROM ExpenseReceipts r WHERE r.expenseId = :expenseId")
    Optional<ReceiptImage> findFullImage(@Param("expenseId") UUID expenseId);

    @Query("SELECT new com.example.splitease.requestAndResponse.ReceiptImage('image/jpeg', r.thumbnail, r.createdAt) " +
            "FROM ExpenseReceipts r WHERE r.expenseId = :expenseId")
    Optional<ReceiptImage> findThumbnail(@Param("expenseId") UUID expenseId);
}
