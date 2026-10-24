package com.example.splitease.repo;

import com.example.splitease.models.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        assertThat(expenseRepo.findAllActivityForUser("carol@test.com")).hasSize(2);
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
    void calculateNetBalancesNetsDebtsInBothDirections() {
        expense(alice, "90", "30", alice, bob, carol); // bob and carol each owe alice 30
        expense(bob, "20", "10", alice, bob);          // alice owes bob 10
        flushAndClear();

        Map<String, BigDecimal> forAlice = balances(alice);
        // positive = alice owes them, negative = they owe alice
        assertThat(forAlice).containsOnlyKeys(bob.getId().toString(), carol.getId().toString());
        assertThat(forAlice.get(bob.getId().toString())).isEqualByComparingTo("-20");
        assertThat(forAlice.get(carol.getId().toString())).isEqualByComparingTo("-30");

        Map<String, BigDecimal> forBob = balances(bob);
        assertThat(forBob).containsOnlyKeys(alice.getId().toString());
        assertThat(forBob.get(alice.getId().toString())).isEqualByComparingTo("20");
    }

    @Test
    void findAllUnsettledBetweenUsersCoversBothDirectionsOnly() {
        expense(alice, "90", "30", alice, bob, carol);
        expense(bob, "20", "10", alice, bob);
        flushAndClear();

        List<ExpenseSplits> between = expenseSplitRepo.findAllUnsettledBetweenUsers(alice.getId(), bob.getId());

        assertThat(between).hasSize(2)
                .allSatisfy(s -> assertThat(s.getSettled()).isFalse())
                .extracting(s -> s.getUser().getName())
                .containsExactlyInAnyOrder("Bob", "Alice");
    }

    @Test
    void deletingExpenseCascadesToSplits() {
        Expenses e = expense(alice, "90", "30", alice, bob, carol);
        flushAndClear();

        expenseRepo.delete(expenseRepo.findById(e.getId()).orElseThrow());
        flushAndClear();

        assertThat(expenseSplitRepo.findAll()).isEmpty();
    }

    private Map<String, BigDecimal> balances(Users user) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (Object[] row : expenseSplitRepo.calculateNetBalances(user.getId())) {
            result.put(row[0].toString(), new BigDecimal(row[2].toString()));
        }
        return result;
    }
}
