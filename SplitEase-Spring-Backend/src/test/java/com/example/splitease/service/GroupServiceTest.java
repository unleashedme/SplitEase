package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.util.*;

import static com.example.splitease.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock private UserRepo userRepo;
    @Mock private GroupRepo groupRepo;
    @Mock private GroupMemberRepo groupMemberRepo;
    @Mock private ExpenseRepo expenseRepo;
    @Mock private ExpenseSplitRepo expenseSplitRepo;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private GroupService groupService;

    private Users alice;
    private Users bob;
    private Users carol;

    @BeforeEach
    void setUp() {
        alice = user("Alice", "alice@test.com");
        bob = user("Bob", "bob@test.com");
        carol = user("Carol", "carol@test.com");
    }

    private CreateGroupRequest createRequest(String name, String... members) {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName(name);
        req.setMembers(new ArrayList<>(List.of(members)));
        return req;
    }

    private void registered(Users... users) {
        for (Users u : users) {
            lenient().when(userRepo.findByEmail(u.getEmail())).thenReturn(u);
        }
    }

    @Test
    void createGroupAddsCreatorAsAdminAndOthersAsMembers() {
        registered(alice, bob, carol);
        UUID generatedId = UUID.randomUUID();
        when(groupRepo.save(any(Groups.class))).thenAnswer(inv -> {
            Groups g = inv.getArgument(0);
            g.setId(generatedId);
            return g;
        });

        UUID id = groupService.createGroup(createRequest("Trip", "bob@test.com", "carol@test.com"), "alice@test.com");

        assertThat(id).isEqualTo(generatedId);
        ArgumentCaptor<Groups> groupCaptor = ArgumentCaptor.forClass(Groups.class);
        verify(groupRepo).save(groupCaptor.capture());
        assertThat(groupCaptor.getValue().getName()).isEqualTo("Trip");
        assertThat(groupCaptor.getValue().getCreatedBy()).isSameAs(alice);

        ArgumentCaptor<GroupMembers> memberCaptor = ArgumentCaptor.forClass(GroupMembers.class);
        verify(groupMemberRepo, times(3)).save(memberCaptor.capture());
        Map<String, String> roles = new HashMap<>();
        memberCaptor.getAllValues().forEach(gm -> roles.put(gm.getUser().getEmail(), gm.getRole()));
        assertThat(roles).containsExactlyInAnyOrderEntriesOf(Map.of(
                "alice@test.com", "admin",
                "bob@test.com", "member",
                "carol@test.com", "member"));
        assertThat(memberCaptor.getAllValues()).allSatisfy(gm -> assertThat(gm.getGroup()).isSameAs(groupCaptor.getValue()));
    }

    @Test
    void createGroupDeduplicatesCreatorAndRepeatedMembers() {
        registered(alice, bob);

        groupService.createGroup(createRequest("Trip", "bob@test.com", "bob@test.com", "alice@test.com"), "alice@test.com");

        verify(groupMemberRepo, times(2)).save(any(GroupMembers.class));
    }

    @Test
    void createGroupNotifiesMembersButNotCreator() {
        alice.setFcmToken("alice-token");
        bob.setFcmToken("bob-token");
        registered(alice, bob, carol);

        groupService.createGroup(createRequest("Trip", "bob@test.com", "carol@test.com"), "alice@test.com");

        verify(notificationService).sendPushNotification(eq("bob-token"), anyString(),
                eq("Alice added you to the group: Trip"));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void createGroupAllowsUpToFiveMembers() {
        Users d = user("D", "d@test.com");
        Users e = user("E", "e@test.com");
        registered(alice, bob, carol, d, e);

        groupService.createGroup(createRequest("Big", "bob@test.com", "carol@test.com", "d@test.com", "e@test.com"),
                "alice@test.com");

        verify(groupMemberRepo, times(5)).save(any(GroupMembers.class));
    }

    @Test
    void createGroupRejectsMoreThanFiveMembersWithMatchingMessage() {
        registered(alice);

        assertThatThrownBy(() -> groupService.createGroup(
                createRequest("Huge", "a@x.com", "b@x.com", "c@x.com", "d@x.com", "e@x.com"), "alice@test.com"))
                .hasMessageContaining("maximum 5 members");
        verifyNoInteractions(groupRepo, groupMemberRepo);
    }

    @Test
    void createGroupRejectsUnregisteredMember() {
        registered(alice);
        when(userRepo.findByEmail("ghost@test.com")).thenReturn(null);

        assertThatThrownBy(() -> groupService.createGroup(createRequest("Trip", "ghost@test.com"), "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).contains("ghost@test.com");
                });
        verifyNoInteractions(groupRepo, groupMemberRepo);
    }

    @Test
    void getGroupsOfUserMapsMemberships() {
        Groups trip = group("Trip", alice);
        Groups flat = group("Flat", bob);
        when(groupMemberRepo.findAllByUserEmail("alice@test.com"))
                .thenReturn(List.of(member(trip, alice, "admin"), member(flat, alice, "member")));

        List<UserGroupResponse> groups = groupService.getGroupsOfUser("alice@test.com");

        assertThat(groups).extracting(UserGroupResponse::getGroupId, UserGroupResponse::getGroupName, UserGroupResponse::getRole)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(trip.getId(), "Trip", "admin"),
                        org.assertj.core.groups.Tuple.tuple(flat.getId(), "Flat", "member"));
    }

    @Test
    void getGroupScreenDataAggregatesAcrossGroups() {
        Groups trip = group("Trip", alice);
        Groups flat = group("Flat", alice);
        when(groupMemberRepo.findByUserEmail("alice@test.com"))
                .thenReturn(List.of(member(trip, alice, "admin"), member(flat, alice, "admin")));

        Expenses hotel = expense(trip, alice, "90.00", "Hotel");
        ExpenseSplits aliceHotel = split(hotel, alice, "45.00", true);
        split(hotel, bob, "45.00", false);
        Expenses rent = expense(flat, carol, "300.00", "Rent");
        ExpenseSplits aliceRent = split(rent, alice, "150.00", true);
        split(rent, carol, "150.00", true);

        when(expenseRepo.findByGroup(trip)).thenReturn(List.of(hotel));
        when(expenseRepo.findByGroup(flat)).thenReturn(List.of(rent));
        when(groupMemberRepo.findByGroup(trip)).thenReturn(List.of(member(trip, alice, "admin"), member(trip, bob, "member")));
        when(groupMemberRepo.findByGroup(flat)).thenReturn(List.of(member(flat, alice, "admin"), member(flat, carol, "member")));
        when(expenseSplitRepo.existsByExpenseGroupAndSettledFalse(trip)).thenReturn(true);
        when(expenseSplitRepo.existsByExpenseGroupAndSettledFalse(flat)).thenReturn(false);
        when(expenseSplitRepo.findByExpenseGroupAndUserEmail(trip, "alice@test.com")).thenReturn(List.of(aliceHotel));
        when(expenseSplitRepo.findByExpenseGroupAndUserEmail(flat, "alice@test.com")).thenReturn(List.of(aliceRent));

        GroupDataResponse res = groupService.getGroupScreenData("alice@test.com");

        assertThat(res.totalGroups()).isEqualTo(2);
        assertThat(res.totalMembersAcrossGroups()).isEqualTo(3); // alice, bob, carol
        assertThat(res.totalExpenses()).isEqualByComparingTo("390.00");
        assertThat(res.activeGroupsCount()).isEqualTo(1);

        GroupDetailResponse tripDetail = res.groups().get(0);
        assertThat(tripDetail.groupId()).isEqualTo(trip.getId());
        assertThat(tripDetail.groupName()).isEqualTo("Trip");
        assertThat(tripDetail.isActive()).isTrue();
        assertThat(tripDetail.totalGroupExpense()).isEqualByComparingTo("90.00");
        assertThat(tripDetail.expenseCount()).isEqualTo(1);
        assertThat(tripDetail.memberCount()).isEqualTo(2);
        assertThat(tripDetail.userShare()).isEqualByComparingTo("45.00");
        assertThat(tripDetail.memberNames()).containsExactly("Alice", "Bob");

        ExpenseDTO hotelDto = tripDetail.expenses().get(0);
        assertThat(hotelDto.description()).isEqualTo("Hotel");
        assertThat(hotelDto.payerName()).isEqualTo("Alice");
        assertThat(hotelDto.userPaid()).isTrue();
        assertThat(hotelDto.splitPerPerson()).isEqualByComparingTo("45.00");

        GroupDetailResponse flatDetail = res.groups().get(1);
        assertThat(flatDetail.isActive()).isFalse();
        assertThat(flatDetail.userShare()).isEqualByComparingTo("150.00");
        assertThat(flatDetail.expenses().get(0).userPaid()).isFalse();
    }

    @Test
    void getGroupScreenDataForUserWithoutGroups() {
        when(groupMemberRepo.findByUserEmail("alice@test.com")).thenReturn(List.of());

        GroupDataResponse res = groupService.getGroupScreenData("alice@test.com");

        assertThat(res.totalGroups()).isZero();
        assertThat(res.totalMembersAcrossGroups()).isZero();
        assertThat(res.totalExpenses()).isEqualByComparingTo("0");
        assertThat(res.activeGroupsCount()).isZero();
        assertThat(res.groups()).isEmpty();
    }

    @Test
    void deleteGroupByCreatorDeletesIt() throws Exception {
        Groups trip = group("Trip", alice);
        when(groupRepo.findById(trip.getId())).thenReturn(Optional.of(trip));

        groupService.deleteGroup(trip.getId(), "alice@test.com");

        verify(groupRepo).delete(trip);
    }

    @Test
    void deleteGroupByNonCreatorIsDenied() {
        Groups trip = group("Trip", alice);
        when(groupRepo.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> groupService.deleteGroup(trip.getId(), "bob@test.com"))
                .isInstanceOf(AccessDeniedException.class);
        verify(groupRepo, never()).delete(any());
    }

    @Test
    void deleteMissingGroupIsNotFound() {
        UUID missing = UUID.randomUUID();
        when(groupRepo.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.deleteGroup(missing, "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
