package com.example.splitease.repo;

import com.example.splitease.models.GroupMembers;
import com.example.splitease.models.Groups;
import com.example.splitease.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMemberRepo extends JpaRepository<GroupMembers, UUID> {

    @Query("""
        SELECT gm
        FROM GroupMembers gm
        JOIN FETCH gm.group
        WHERE gm.user.email = :email
    """)
    List<GroupMembers> findAllByUserEmail(String email);

    List<GroupMembers> findByGroupId(UUID groupId);

    List<GroupMembers> findByUserEmail(String email);

    List<GroupMembers> findByGroup(Groups group);

    List<GroupMembers> findByUser(Users user);
}
