package com.example.splitease.service;

import com.example.splitease.service.DebtSimplifier.Transfer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebtSimplifierTest {

    private final UUID alice = id(1), bob = id(2), carol = id(3), dave = id(4), erin = id(5);

    private static UUID id(int n) {
        return UUID.fromString("00000000-0000-0000-0000-00000000000" + n);
    }

    private static Map<UUID, BigDecimal> balances(Object... idAndAmount) {
        Map<UUID, BigDecimal> map = new LinkedHashMap<>();
        for (int i = 0; i < idAndAmount.length; i += 2) {
            map.put((UUID) idAndAmount[i], new BigDecimal((String) idAndAmount[i + 1]));
        }
        return map;
    }

    private static Transfer pay(UUID from, UUID to, String amount) {
        return new Transfer(from, to, new BigDecimal(amount));
    }

    /** Applies the payments and returns what's left; settling means everyone ends at zero. */
    private static Map<UUID, BigDecimal> after(Map<UUID, BigDecimal> balances, List<Transfer> transfers) {
        Map<UUID, BigDecimal> left = new HashMap<>(balances);
        for (Transfer t : transfers) {
            left.merge(t.from(), t.amount(), BigDecimal::add);
            left.merge(t.to(), t.amount().negate(), BigDecimal::add);
        }
        return left;
    }

    @Test
    void chainOfDebtsCollapsesToOnePayment() {
        // Bob owes Alice 30 and Carol owes Bob 30: Bob is only passing money along
        Map<UUID, BigDecimal> balances = balances(alice, "30", bob, "0", carol, "-30");

        assertThat(DebtSimplifier.simplify(balances)).containsExactly(pay(carol, alice, "30"));
    }

    @Test
    void circleOfEqualDebtsNeedsNoPayments() {
        // Alice owes Bob, Bob owes Carol, Carol owes Alice, all 20: everyone nets to zero
        assertThat(DebtSimplifier.simplify(balances(alice, "0", bob, "0", carol, "0"))).isEmpty();
    }

    @Test
    void largestDebtorPaysLargestCreditorFirst() {
        Map<UUID, BigDecimal> balances = balances(alice, "100", bob, "20", carol, "-70", dave, "-50");

        List<Transfer> transfers = DebtSimplifier.simplify(balances);

        assertThat(transfers).containsExactly(
                pay(carol, alice, "70"),
                pay(dave, alice, "30"),
                pay(dave, bob, "20")
        );
        assertThat(after(balances, transfers).values()).allMatch(b -> b.signum() == 0);
    }

    @Test
    void neverNeedsMoreThanOnePaymentPerPersonMinusOne() {
        // Six pairwise debts between five people
        Map<UUID, BigDecimal> balances = balances(alice, "45.50", bob, "-10.25", carol, "-60", dave, "34.75", erin, "-10");

        List<Transfer> transfers = DebtSimplifier.simplify(balances);

        assertThat(transfers).hasSizeLessThanOrEqualTo(4);
        assertThat(after(balances, transfers).values()).allMatch(b -> b.signum() == 0);
    }

    @Test
    void keepsPaiseExact() {
        Map<UUID, BigDecimal> balances = balances(alice, "33.34", bob, "-16.67", carol, "-16.67");

        List<Transfer> transfers = DebtSimplifier.simplify(balances);

        assertThat(transfers).extracting(Transfer::amount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("16.67"), new BigDecimal("16.67"));
        assertThat(after(balances, transfers).values()).allMatch(b -> b.signum() == 0);
    }

    @Test
    void sameBalancesAlwaysGiveTheSamePlan() {
        // Equal amounts on both sides: the order must not depend on map iteration order
        Map<UUID, BigDecimal> forwards = balances(alice, "10", bob, "10", carol, "-10", dave, "-10");
        Map<UUID, BigDecimal> backwards = balances(dave, "-10", carol, "-10", bob, "10", alice, "10");

        assertThat(DebtSimplifier.simplify(forwards)).isEqualTo(DebtSimplifier.simplify(backwards));
    }

    @Test
    void randomBalancesAlwaysSettleWithinTheLimit() {
        Random random = new Random(42);
        for (int round = 0; round < 500; round++) {
            int people = 2 + random.nextInt(9);
            Map<UUID, BigDecimal> balances = new HashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < people - 1; i++) {
                BigDecimal amount = BigDecimal.valueOf(random.nextInt(200_001) - 100_000, 2);
                balances.put(UUID.randomUUID(), amount);
                total = total.add(amount);
            }
            balances.put(UUID.randomUUID(), total.negate()); // makes them add up to zero

            List<Transfer> transfers = DebtSimplifier.simplify(balances);

            assertThat(transfers).hasSizeLessThanOrEqualTo(people - 1);
            assertThat(transfers).allMatch(t -> t.amount().signum() > 0 && !t.from().equals(t.to()));
            assertThat(after(balances, transfers).values()).allMatch(b -> b.signum() == 0);
        }
    }

    @Test
    void rejectsBalancesThatDoNotAddUpToZero() {
        assertThatThrownBy(() -> DebtSimplifier.simplify(balances(alice, "10", bob, "-5")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
