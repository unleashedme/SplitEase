package com.example.splitease.repo;

import com.example.splitease.models.*;
import com.example.splitease.requestAndResponse.ReceiptImage;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the hand-written JPQL queries against an in-memory database. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryQueriesTest {

    @Autowired private EntityManager em;
    @Autowired private UserRepo userRepo;
    @Autowired private GroupRepo groupRepo;
    @Autowired private GroupMemberRepo groupMemberRepo;
    @Autowired private ExpenseRepo expenseRepo;
    @Autowired private ExpenseSplitRepo expenseSplitRepo;
    @Autowired private ExpenseReceiptRepo expenseReceiptRepo;
    @Autowired private SettlementRepo settlementRepo;

    private Users alice;
    private Users bob;
    private Users carol;
    private Users outsider;
    private Groups trip;

    private Users newUser(String name, String email, String phone) {
        Users u = new Users();
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPassword("pw");
        return userRepo.save(u);
    }

    private void join(Groups g, Users u, String role) {
        GroupMembers gm = new GroupMembers();
        gm.setGroup(g);
        gm.setUser(u);
        gm.setRole(role);
        groupMemberRepo.save(gm);
    }

    /** Saves an expense paid by payer, split equally among the given users (payer's split settled). */
    private Expenses expense(Users payer, String amount, String share, Users... participants) {
        Expenses e = new Expenses();
        e.setGroup(trip);
        e.setPayer(payer);
        e.setAmount(new BigDecimal(amount));
        e.setDescription("exp");
        expenseRepo.save(e);
        for (Users p : participants) {
            ExpenseSplits s = new ExpenseSplits();
            s.setExpense(e);
            s.setUser(p);
            s.setShareAmount(new BigDecimal(share));
            s.setSettled(p.getId().equals(payer.getId()));
            expenseSplitRepo.save(s);
        }
        return e;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @BeforeEach
    void setUp() {
        alice = newUser("Alice", "alice@test.com", "1");
        bob = newUser("Bob", "bob@test.com", "2");
        carol = newUser("Carol", "carol@test.com", "3");
        outsider = newUser("Out", "out@test.com", "4");

        trip = new Groups();
        trip.setName("Trip");
        trip.setCreatedBy(alice);
        groupRepo.save(trip);
        join(trip, alice, "admin");
        join(trip, bob, "member");
        join(trip, carol, "member");
    }

    @Test
    void findByEmailAndClearFcmToken() {
        alice.setFcmToken("tok");
        bob.setFcmToken("tok");
        flushAndClear();

        assertThat(userRepo.findByEmail("alice@test.com").getName()).isEqualTo("Alice");
        assertThat(userRepo.findByEmail("nobody@test.com")).isNull();

        userRepo.clearFcmTokenByToken("tok");
        em.clear();

        assertThat(userRepo.findByEmail("alice@test.com").getFcmToken()).isNull();
        assertThat(userRepo.findByEmail("bob@test.com").getFcmToken()).isNull();
    }

    @Test
    void groupMembershipQueries() {
        flushAndClear();

        assertThat(groupMemberRepo.findAllByUserEmail("bob@test.com"))
                .singleElement().satisfies(gm -> assertThat(gm.getGroup().getName()).isEqualTo("Trip"));
        assertThat(groupMemberRepo.findByGroupId(trip.getId())).hasSize(3);
        assertThat(groupMemberRepo.findByUserEmail("out@test.com")).isEmpty();
        assertThat(groupRepo.findAllByMemberEmail("carol@test.com")).extracting(Groups::getName).containsExactly("Trip");
    }

    @Test
    void activityQueryReturnsDistinctExpensesForInvolvedMembers() {
        expense(alice, "90", "30", alice, bob, carol);
        expense(bob, "20", "10", bob, carol);
        flushAndClear();

        List<Expenses> aliceActivity = expenseRepo.findAllActivityForUser("alice@test.com");
        assertThat(aliceActivity).hasSize(1);
        assertThat(aliceActivity.get(0).getSplits()).hasSize(3);

        // Carol isn't the payer of either, but every split must still be loaded
        assertThat(expenseRepo.findAllActivityForUser("carol@test.com")).hasSize(2)
                .allSatisfy(e -> assertThat(e.getSplits()).hasSize(e.getAmount().intValue() == 90 ? 3 : 2));
        assertThat(expenseRepo.findAllActivityForUser("out@test.com")).isEmpty();
    }

    @Test
    void splitLookupQueries() {
        expense(alice, "90", "30", alice, bob, carol);
        flushAndClear();

        assertThat(expenseSplitRepo.existsByExpenseGroupAndSettledFalse(trip)).isTrue();
        assertThat(expenseSplitRepo.existsUnsettledSplits(trip.getId())).isTrue();
        assertThat(expenseSplitRepo.findByExpenseGroupAndUserEmail(trip, "bob@test.com")).hasSize(1);
        assertThat(expenseSplitRepo.findAllByUserEmail("carol@test.com")).hasSize(1);
        assertThat(expenseSplitRepo.findTotalOwedByTo(bob.getId(), alice.getId(), trip.getId()))
                .isEqualByComparingTo("30");
    }

    @Test
    void outstandingSplitsOfAGroup() {
        expense(alice, "90", "30", alice, bob, carol); // only alice's own split is settled
        flushAndClear();

        assertThat(expenseSplitRepo.findByExpenseGroupAndSettledFalse(trip))
                .extracting(s -> s.getUser().getName())
                .containsExactlyInAnyOrder("Bob", "Carol");
    }

    @Test
    void openPaymentsOfAGroupExcludeClearedAndOldOnes() {
        Settlements open = payment(bob, alice, "30", trip, null);
        payment(carol, alice, "30", trip, true);   // cleared once the group was square
        payment(carol, bob, "10", null, null);     // recorded before payments belonged to a group
        flushAndClear();

        assertThat(settlementRepo.findOpenByGroup(trip)).extracting(Settlements::getId).containsExactly(open.getId());
    }

    @Test
    void deletingGroupRemovesItsPayments() {
        payment(bob, alice, "30", trip, null);
        flushAndClear();

        groupRepo.delete(groupRepo.findById(trip.getId()).orElseThrow());
        flushAndClear();

        assertThat(settlementRepo.findAll()).isEmpty();
    }

    private Settlements payment(Users from, Users to, String amount, Groups group, Boolean cleared) {
        Settlements s = new Settlements();
        s.setFromUser(from);
        s.setToUser(to);
        s.setAmount(new BigDecimal(amount));
        s.setGroup(group);
        s.setCleared(cleared);
        return settlementRepo.save(s);
    }

    @Test
    void deletingExpenseCascadesToSplits() {
        Expenses e = expense(alice, "90", "30", alice, bob, carol);
        flushAndClear();

        expenseRepo.delete(expenseRepo.findById(e.getId()).orElseThrow());
        flushAndClear();

        assertThat(expenseSplitRepo.findAll()).isEmpty();
    }

    private void attachReceipt(Expenses e, byte[] data, byte[] thumbnail) {
        ExpenseReceipts r = new ExpenseReceipts();
        r.setExpense(e);
        r.setContentType("image/png");
        r.setData(data);
        r.setThumbnail(thumbnail);
        r.setSizeBytes(data.length);
        r.setCreatedAt(Instant.now());
        expenseReceiptRepo.save(r);
    }

    @Test
    void receiptQueriesReturnOnlyTheRequestedVersion() {
        Expenses withBill = expense(alice, "90", "30", alice, bob, carol);
        Expenses withoutBill = expense(bob, "20", "10", bob, carol);
        byte[] full = new byte[200_000]; // bigger than a default varbinary column
        full[0] = 1;
        attachReceipt(withBill, full, new byte[]{9, 9});
        flushAndClear();

        assertThat(expenseReceiptRepo.findExpenseIdsWithReceipt(List.of(withBill.getId(), withoutBill.getId())))
                .containsExactly(withBill.getId());

        ReceiptImage fullImage = expenseReceiptRepo.findFullImage(withBill.getId()).orElseThrow();
        assertThat(fullImage.contentType()).isEqualTo("image/png");
        assertThat(fullImage.bytes()).isEqualTo(full);

        ReceiptImage thumb = expenseReceiptRepo.findThumbnail(withBill.getId()).orElseThrow();
        assertThat(thumb.contentType()).isEqualTo("image/jpeg");
        assertThat(thumb.bytes()).containsExactly(9, 9);

        assertThat(expenseReceiptRepo.findFullImage(withoutBill.getId())).isEmpty();
    }

    @Test
    void deletingExpenseOrGroupRemovesItsReceipt() {
        Expenses first = expense(alice, "90", "30", alice, bob, carol);
        attachReceipt(first, new byte[]{1}, new byte[]{1});
        Expenses second = expense(bob, "20", "10", bob, carol);
        attachReceipt(second, new byte[]{2}, new byte[]{2});
        flushAndClear();

        expenseRepo.delete(expenseRepo.findById(first.getId()).orElseThrow());
        flushAndClear();
        assertThat(expenseReceiptRepo.findAll()).extracting(ExpenseReceipts::getExpenseId).containsExactly(second.getId());

        groupRepo.delete(groupRepo.findById(trip.getId()).orElseThrow());
        flushAndClear();
        assertThat(expenseReceiptRepo.findAll()).isEmpty();
    }
}
