package com.example.splitease.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Turns everyone's net balance into a short list of payments that settles all of them.
 *
 * Greedy: the person who owes the most pays the person who is owed the most, as much as one of
 * them needs, and the one with money left over goes back in line. Each payment clears at least
 * one person, so n people never need more than n - 1 payments, whatever the debts looked like.
 */
public final class DebtSimplifier {

    private DebtSimplifier() {}

    /** One payment in the plan. */
    public record Transfer(UUID from, UUID to, BigDecimal amount) {}

    private record Party(UUID id, BigDecimal amount) {}

    // Largest amount first; ties broken by id so the same balances always give the same plan,
    // which lets a payment be checked against the plan the user was shown
    private static final Comparator<Party> LARGEST_FIRST = Comparator
            .comparing(Party::amount, Comparator.reverseOrder())
            .thenComparing(p -> p.id().toString());

    /**
     * @param balances net balance per person: positive means they are owed money, negative means they owe.
     *                 Must add up to zero.
     */
    public static List<Transfer> simplify(Map<UUID, BigDecimal> balances) {
        PriorityQueue<Party> creditors = new PriorityQueue<>(LARGEST_FIRST);
        PriorityQueue<Party> debtors = new PriorityQueue<>(LARGEST_FIRST);
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
            BigDecimal amount = entry.getValue();
            total = total.add(amount);
            if (amount.signum() > 0) {
                creditors.add(new Party(entry.getKey(), amount));
            } else if (amount.signum() < 0) {
                debtors.add(new Party(entry.getKey(), amount.negate()));
            }
        }
        if (total.signum() != 0) {
            throw new IllegalArgumentException("Balances must add up to zero but add up to " + total);
        }

        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Party creditor = creditors.poll();
            Party debtor = debtors.poll();
            BigDecimal amount = creditor.amount().min(debtor.amount());
            transfers.add(new Transfer(debtor.id(), creditor.id(), amount));

            BigDecimal creditorLeft = creditor.amount().subtract(amount);
            BigDecimal debtorLeft = debtor.amount().subtract(amount);
            if (creditorLeft.signum() > 0) creditors.add(new Party(creditor.id(), creditorLeft));
            if (debtorLeft.signum() > 0) debtors.add(new Party(debtor.id(), debtorLeft));
        }
        return transfers;
    }
}
