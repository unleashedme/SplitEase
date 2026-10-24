package com.example.splitease;

import com.example.splitease.models.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

/** Builders for entities used across unit tests. */
public final class TestData {

    private TestData() {}

    public static Users user(String name, String email) {
        Users u = new Users();
        u.setId(UUID.randomUUID());
        u.setName(name);
        u.setEmail(email);
        u.setPhone(UUID.randomUUID().toString().substring(0, 10));
        u.setPassword("hashed");
        return u;
    }

    public static Groups group(String name, Users createdBy) {
        Groups g = new Groups();
        g.setId(UUID.randomUUID());
        g.setName(name);
        g.setCreatedBy(createdBy);
        return g;
    }

    public static GroupMembers member(Groups group, Users user, String role) {
        GroupMembers gm = new GroupMembers();
        gm.setId(UUID.randomUUID());
        gm.setGroup(group);
        gm.setUser(user);
        gm.setRole(role);
        return gm;
    }

    public static Expenses expense(Groups group, Users payer, String amount, String description) {
        Expenses e = new Expenses();
        e.setId(UUID.randomUUID());
        e.setGroup(group);
        e.setPayer(payer);
        e.setAmount(new BigDecimal(amount));
        e.setDescription(description);
        e.setSplits(new ArrayList<>());
        return e;
    }

    public static ExpenseSplits split(Expenses expense, Users user, String share, Boolean settled) {
        ExpenseSplits s = new ExpenseSplits();
        s.setId(UUID.randomUUID());
        s.setExpense(expense);
        s.setUser(user);
        s.setShareAmount(new BigDecimal(share));
        s.setSettled(settled);
        expense.getSplits().add(s);
        return s;
    }
}
